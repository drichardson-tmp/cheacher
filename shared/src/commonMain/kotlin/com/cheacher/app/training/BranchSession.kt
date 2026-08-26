package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Position
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.TreeNode

/** How a node reads on the tree diagram. */
enum class NodeStatus {
    UNVISITED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,

    /**
     * Behind the progression frontier: barely-there on the diagram, inert on the board.
     * A locked door is not a mistake — trying it just rattles, it never costs a strike.
     */
    LOCKED;

    /** Closed nodes are pruned from the board: you cannot walk into them again this round. */
    val isClosed: Boolean get() = this == COMPLETED || this == FAILED || this == LOCKED
}

/** What a wrong move costs. */
enum class MistakePolicy {
    /** Red flash, immediate snap back to the last open junction. */
    STRICT,

    /** One miss is forgiven in place; the second closes the branch as failed. */
    ONE_ALLOWANCE,
}

/**
 * Phase 2 — Branch Recall with visual pruning.
 *
 * No names, no hints: just a board and a tree diagram. Walk a line to its end and the
 * branch closes out — dimmed on the tree, unplayable on the board — and you are snapped
 * back to the nearest junction that still has something open. The round ends when the
 * whole tree is closed.
 *
 * Like [GuidedState] this is a pure reducer; every transition is a value.
 */
data class BranchState(
    val tree: OpeningTree,
    /** Node most recently played. Null means "at the root". */
    val cursorId: String? = null,
    val statuses: Map<String, NodeStatus> = emptyMap(),
    val policy: MistakePolicy = MistakePolicy.STRICT,
    /**
     * When set, moves for this colour are played by the app rather than the learner —
     * the "practise it from the other side" mode. Null means the learner plays both sides.
     */
    val autoReplyFor: Color? = null,
    val strikes: Int = 0,
    val lastEvent: BranchEvent? = null,
    val finished: Boolean = false,
) {
    val cursor: TreeNode? get() = cursorId?.let { tree.node(it) }

    val position: Position get() = tree.positionAt(cursor)

    fun statusOf(node: TreeNode): NodeStatus = statuses[node.id] ?: NodeStatus.UNVISITED

    /** Children of the cursor that have not been closed out yet. */
    val openMoves: List<TreeNode> get() = tree.childrenOf(cursor).filterNot { statusOf(it).isClosed }

    /** The path from the root to the cursor — what the move strip shows. */
    val path: List<TreeNode>
        get() = generateSequence(cursor) { it.parentId?.let(tree::node) }.toList().asReversed()

    val progress: BranchProgress
        get() {
            // Locked lines are not on the syllabus this round, so they are not on the
            // scoreboard either: "1 of 1", not "1 of 5 and four you were never shown".
            val leaves = tree.lines.map { it.last() }.filter { statusOf(it) != NodeStatus.LOCKED }
            return BranchProgress(
                closedLines = leaves.count { statusOf(it).isClosed },
                totalLines = leaves.size,
                failedLines = leaves.count { statusOf(it) == NodeStatus.FAILED },
            )
        }

    companion object {
        /**
         * [allowedNodeIds] is the progression gate: nodes outside it start the round
         * [NodeStatus.LOCKED] — unplayable, unpenalised, and outside the score. Null
         * means the whole tree, which keeps full-tree practice exactly as it was.
         */
        fun start(
            tree: OpeningTree,
            policy: MistakePolicy = MistakePolicy.STRICT,
            autoReplyFor: Color? = null,
            allowedNodeIds: Set<String>? = null,
        ): BranchState {
            val locked = if (allowedNodeIds == null) {
                emptyMap()
            } else {
                tree.allNodes.filter { it.id !in allowedNodeIds }
                    .associate { it.id to NodeStatus.LOCKED }
            }
            return BranchState(
                tree = tree,
                statuses = locked,
                policy = policy,
                autoReplyFor = autoReplyFor,
                finished = tree.rootChildren.all { locked[it.id]?.isClosed == true },
            ).autoReply()
        }
    }
}

data class BranchProgress(val closedLines: Int, val totalLines: Int, val failedLines: Int) {
    val fraction: Float get() = if (totalLines == 0) 1f else closedLines.toFloat() / totalLines
}

sealed interface BranchEvent {
    data class Advanced(val node: TreeNode) : BranchEvent

    /** A line reached its authored end. [snappedTo] is the junction we bounced back to. */
    data class BranchClosed(val leaf: TreeNode, val snappedTo: TreeNode?) : BranchEvent

    /** Wrong move, forgiven — the board did not change. */
    data class Missed(val played: Move, val strikes: Int) : BranchEvent

    /** Wrong move that cost the branch. */
    data class BranchFailed(val played: Move, val at: TreeNode?, val snappedTo: TreeNode?) : BranchEvent

    /** Replaying a line that is already closed out. Cheap to do, so we just say so. */
    data class AlreadyClosed(val node: TreeNode) : BranchEvent

    /** A real repertoire move behind the frontier. The door rattles; nothing is lost. */
    data class Locked(val node: TreeNode) : BranchEvent

    data object SessionComplete : BranchEvent
}

/**
 * Feeds a move into the recall round.
 *
 * The three interesting outcomes all live here: advance, close-and-snap, and miss.
 */
fun BranchState.submit(move: Move): BranchState {
    if (finished) return copy(lastEvent = BranchEvent.SessionComplete)

    val children = tree.childrenOf(cursor)
    val match = children.firstOrNull { it.move == move }

    if (match == null) return penalise(move)
    // A locked branch is not a wrong move — the learner found a real repertoire move,
    // just one the ladder has not reached. No strike, no snap-back, no board change.
    if (statusOf(match) == NodeStatus.LOCKED) return copy(lastEvent = BranchEvent.Locked(match))
    if (statusOf(match).isClosed) return copy(lastEvent = BranchEvent.AlreadyClosed(match))

    val advanced = copy(
        cursorId = match.id,
        statuses = statuses + (match.id to NodeStatus.IN_PROGRESS),
        strikes = 0,
        lastEvent = BranchEvent.Advanced(match),
    )
    return if (match.isLeaf) advanced.close(match, NodeStatus.COMPLETED) else advanced.autoReply()
}

/** Plays the opponent's move for the learner when the session is set to one-sided practice. */
private fun BranchState.autoReply(): BranchState {
    val replyFor = autoReplyFor ?: return this
    var state = this
    while (!state.finished && state.tree.sideToMoveAt(state.cursor) == replyFor) {
        val next = state.openMoves.firstOrNull() ?: break
        state = state.copy(
            cursorId = next.id,
            statuses = state.statuses + (next.id to NodeStatus.IN_PROGRESS),
            lastEvent = BranchEvent.Advanced(next),
        )
        if (next.isLeaf) return state.close(next, NodeStatus.COMPLETED)
    }
    return state
}

private fun BranchState.penalise(move: Move): BranchState {
    val forgiving = policy == MistakePolicy.ONE_ALLOWANCE && strikes == 0
    if (forgiving) return copy(strikes = 1, lastEvent = BranchEvent.Missed(move, 1))

    val here = cursor
    return if (here == null) {
        // Nothing to close at the root: just reject the move.
        copy(strikes = strikes + 1, lastEvent = BranchEvent.Missed(move, strikes + 1))
    } else {
        val closed = close(here, NodeStatus.FAILED)
        closed.copy(
            lastEvent = BranchEvent.BranchFailed(move, here, closed.cursor),
        )
    }
}

/**
 * Marks [node] with [status], closes every ancestor whose children are now all closed,
 * and snaps the board back to the nearest junction with something left to play.
 */
private fun BranchState.close(node: TreeNode, status: NodeStatus): BranchState {
    val updated = statuses.toMutableMap()
    updated[node.id] = status

    // Mark descendants closed too — failing a junction retires everything under it.
    fun closeSubtree(current: TreeNode) {
        current.children.forEach {
            if (updated[it.id]?.isClosed != true) updated[it.id] = status
            closeSubtree(it)
        }
    }
    closeSubtree(node)

    // Roll completion upward: a parent is done once every child is done.
    var ancestor = node.parentId?.let(tree::node)
    while (ancestor != null) {
        val allClosed = ancestor.children.all { updated[it.id]?.isClosed == true }
        if (!allClosed) break
        updated[ancestor.id] = NodeStatus.COMPLETED
        ancestor = ancestor.parentId?.let(tree::node)
    }

    val rootClosed = tree.rootChildren.all { updated[it.id]?.isClosed == true }
    val junction = if (rootClosed) null else nearestOpenJunction(node, updated)

    val snapped = copy(
        cursorId = junction?.id,
        statuses = updated,
        strikes = 0,
        finished = rootClosed,
        lastEvent = if (rootClosed) {
            BranchEvent.SessionComplete
        } else {
            BranchEvent.BranchClosed(node, junction)
        },
    )
    return if (rootClosed) snapped else snapped.autoReply()
}

/**
 * Walks up from [from] to the first ancestor that still has an unclosed child.
 * Null means "back to the starting position".
 */
private fun BranchState.nearestOpenJunction(from: TreeNode, statuses: Map<String, NodeStatus>): TreeNode? {
    var candidate = from.parentId?.let(tree::node)
    while (candidate != null) {
        if (candidate.children.any { statuses[it.id]?.isClosed != true }) return candidate
        candidate = candidate.parentId?.let(tree::node)
    }
    return null
}

/** Abandons the current line without penalty and returns to the nearest open junction. */
fun BranchState.backToJunction(): BranchState {
    val here = cursor ?: return this
    val junction = nearestOpenJunction(here, statuses)
    return copy(cursorId = junction?.id, strikes = 0, lastEvent = null)
}
