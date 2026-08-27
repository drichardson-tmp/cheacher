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
 * Phase 2 — Recall.
 *
 * The prompt is still a name, because names are the whole point of the app; what goes
 * away is the scaffolding. Guided mode names the line one move at a time; recall names
 * only the *destination* — "play the Najdorf Variation" — and you walk every ply to it
 * unaided: no per-move prompt, no idea sentence, no diagram to read the answer off.
 *
 * Reaching the target closes it out and the next unclosed line becomes the target, which
 * is what makes the round feel like pruning: the book shrinks as you name your way
 * through it. The lines come in [OpeningTree.lines] (DFS) order, so consecutive targets
 * share the longest possible prefix and you rejoin at the fork rather than replaying the
 * opening moves ten times.
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
    /**
     * The shared trunk's last node, once the book's road in has been earned — see
     * [OpeningEntry]. The round opens there and snaps back no further than there, so the
     * ten Italian lines are pruned from the Italian rather than from move one. Null is
     * the ordinary case: the round starts and re-forms at the true starting position.
     */
    val entryNodeId: String? = null,
    /** The last node of the shared road in, or null when the book forks at move one. */
    val trunkLastId: String? = null,
    /** How many plies the road in runs for — a miss shallower than this is a miss on it. */
    val trunkPly: Int = 0,
    /**
     * A wrong move on the way to the opening. Sticky for the round, because the round
     * only ever walks the road in once — everything after the first line rejoins at the
     * fork — so "this round's walk in was clean" is a question with a single answer.
     */
    val roadInFumbled: Boolean = false,
    /**
     * True once this round has walked the road in without a wrong move — the entry toll,
     * paid blind. See [OpeningEntry]; the ViewModel journals the transition.
     */
    val roadInCleared: Boolean = false,
    val strikes: Int = 0,
    val lastEvent: BranchEvent? = null,
    val finished: Boolean = false,
) {
    val cursor: TreeNode? get() = cursorId?.let { tree.node(it) }

    val position: Position get() = tree.positionAt(cursor)

    fun statusOf(node: TreeNode): NodeStatus = statuses[node.id] ?: NodeStatus.UNVISITED

    /** Children of the cursor that have not been closed out yet. */
    val openMoves: List<TreeNode> get() = tree.childrenOf(cursor).filterNot { statusOf(it).isClosed }

    /**
     * The line being asked for right now — the first one this round has not closed out.
     * Its [TreeNode.name] is the prompt, and it is the only line the board will accept:
     * with a name on screen, wandering onto a *different* real line is a wrong answer,
     * not a shortcut.
     */
    val targetLeaf: TreeNode?
        get() = tree.lines.map { it.last() }.firstOrNull { !statusOf(it).isClosed }

    /** Node ids on the way to [targetLeaf], including the leaf itself. */
    val targetPathIds: Set<String>
        get() {
            val leaf = targetLeaf ?: return emptySet()
            return generateSequence(leaf) { it.parentId?.let(tree::node) }.map { it.id }.toSet()
        }

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
         *
         * [entryNodeId] is either the earned trunk end ([OpeningEntry.entryNode]) or the
         * parent of a focused per-move review. The round opens with that path already
         * walked. It is ignored unless the node is unlocked and is an ancestor of every
         * active line — a gate can never skip into unrelated chess.
         */
        fun start(
            tree: OpeningTree,
            policy: MistakePolicy = MistakePolicy.STRICT,
            autoReplyFor: Color? = null,
            allowedNodeIds: Set<String>? = null,
            entryNodeId: String? = null,
        ): BranchState {
            val locked = if (allowedNodeIds == null) {
                emptyMap()
            } else {
                tree.allNodes.filter { it.id !in allowedNodeIds }
                    .associate { it.id to NodeStatus.LOCKED }
            }
            val trunk = tree.trunk()
            val unlockedLeaves = tree.lines.map { it.last() }
                .filter { locked[it.id]?.isClosed != true }
            val entryNode = entryNodeId?.let(tree::node)
            val entryPath = entryNode?.let { node ->
                generateSequence(node) { it.parentId?.let(tree::node) }.toList().asReversed()
            }.orEmpty()
            val entry = entryNode
                ?.takeIf { node ->
                    node.id !in locked &&
                        unlockedLeaves.isNotEmpty() &&
                        unlockedLeaves.all { leaf ->
                            generateSequence(leaf) { it.parentId?.let(tree::node) }
                                .any { it.id == node.id }
                        }
                }
                ?.id
            // Everything down to the entry reads as travelled, so the diagram and the move
            // strip show the road in as history rather than as something still to find.
            val walkedIn = if (entry == null) {
                emptyMap()
            } else {
                entryPath.associate { it.id to NodeStatus.IN_PROGRESS }
            }
            return BranchState(
                tree = tree,
                cursorId = entry,
                statuses = locked + walkedIn,
                policy = policy,
                autoReplyFor = autoReplyFor,
                entryNodeId = entry,
                trunkLastId = trunk.lastOrNull()?.id,
                trunkPly = trunk.size,
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

    /** The final line closed; [leaf] lets the board show its completed position. */
    data class SessionComplete(val leaf: TreeNode) : BranchEvent
}

/**
 * Feeds a move into the recall round.
 *
 * The three interesting outcomes all live here: advance, close-and-snap, and miss.
 */
fun BranchState.submit(move: Move): BranchState {
    if (finished) return this

    val children = tree.childrenOf(cursor)
    val match = children.firstOrNull { it.move == move }

    if (match == null) return penalise(move)
    // A locked branch is not a wrong move — the learner found a real repertoire move,
    // just one the ladder has not reached. No strike, no snap-back, no board change.
    if (statusOf(match) == NodeStatus.LOCKED) return copy(lastEvent = BranchEvent.Locked(match))
    if (statusOf(match).isClosed) return copy(lastEvent = BranchEvent.AlreadyClosed(match))
    // A real move, but not the one the named line asks for: the prompt said "Najdorf",
    // so the Dragon is the wrong answer here even though it is in the book.
    val target = targetPathIds
    if (target.isNotEmpty() && match.id !in target) return penalise(move)

    val advanced = copy(
        cursorId = match.id,
        statuses = statuses + (match.id to NodeStatus.IN_PROGRESS),
        strikes = 0,
        lastEvent = BranchEvent.Advanced(match),
    ).markRoadIn(match)
    return if (match.isLeaf) advanced.close(match, NodeStatus.COMPLETED) else advanced.autoReply()
}

/** Arriving at the opening proper with a clean sheet pays the entry toll for this book. */
private fun BranchState.markRoadIn(arrived: TreeNode): BranchState =
    if (arrived.id == trunkLastId && !roadInFumbled) copy(roadInCleared = true) else this

/** Plays the opponent's move for the learner when the session is set to one-sided practice. */
private fun BranchState.autoReply(): BranchState {
    val replyFor = autoReplyFor ?: return this
    var state = this
    while (!state.finished && state.tree.sideToMoveAt(state.cursor) == replyFor) {
        // Follow the line that is actually being asked for, so the app's own replies can
        // never walk the learner off the target and into a miss they did not make.
        val onTarget = state.targetPathIds
        val next = state.openMoves.firstOrNull { it.id in onTarget }
            ?: state.openMoves.firstOrNull()
            ?: break
        state = state.copy(
            cursorId = next.id,
            statuses = state.statuses + (next.id to NodeStatus.IN_PROGRESS),
            lastEvent = BranchEvent.Advanced(next),
        ).markRoadIn(next)
        if (next.isLeaf) return state.close(next, NodeStatus.COMPLETED)
    }
    return state
}

/**
 * A miss costs *the line you were asked for* — not the whole subtree under your feet.
 * The card said "Najdorf"; you did not produce the Najdorf; that line is lost for the
 * round and the next name comes up. Everything else in the book is still on the table,
 * which is what makes one slip at move one survivable.
 */
private fun BranchState.penalise(move: Move): BranchState {
    // Depth of the move being reached for: shallower than the road in means the learner
    // fumbled on the way to the opening, which is what costs the entry.
    val fumbledRoadIn = roadInFumbled || (cursor?.depth?.plus(1) ?: 0) < trunkPly

    val forgiving = policy == MistakePolicy.ONE_ALLOWANCE && strikes == 0
    if (forgiving) {
        return copy(
            strikes = 1,
            roadInFumbled = fumbledRoadIn,
            lastEvent = BranchEvent.Missed(move, 1),
        )
    }

    val here = cursor
    val doomed = targetLeaf
    return if (here == null || doomed == null) {
        // Nothing played yet: reject the move rather than spend a line on it.
        copy(
            strikes = strikes + 1,
            roadInFumbled = fumbledRoadIn,
            lastEvent = BranchEvent.Missed(move, strikes + 1),
        )
    } else {
        val closed = copy(roadInFumbled = fumbledRoadIn).close(doomed, NodeStatus.FAILED)
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
        // A junction is only green if nothing under it was lost; one failed child below
        // and the whole fork reads as failed on the end-of-round tree.
        val anyFailed = ancestor.children.any { updated[it.id] == NodeStatus.FAILED }
        updated[ancestor.id] = if (anyFailed) NodeStatus.FAILED else NodeStatus.COMPLETED
        ancestor = ancestor.parentId?.let(tree::node)
    }

    val rootClosed = tree.rootChildren.all { updated[it.id]?.isClosed == true }
    val junction = if (rootClosed) null else nearestOpenJunction(node, updated)

    val closeEvent = if (rootClosed) {
        BranchEvent.SessionComplete(node)
    } else {
        BranchEvent.BranchClosed(node, junction)
    }
    val snapped = copy(
        cursorId = junction?.id,
        statuses = updated,
        strikes = 0,
        finished = rootClosed,
        lastEvent = closeEvent,
    )
    if (rootClosed) return snapped

    // Auto-replies may advance the cursor after the snap, but closing the branch is still
    // the user-visible event. Preserve it so celebration, journalling, and reset motion
    // do not disappear merely because Cheacher also dealt the opponent's next move.
    return snapped.autoReply().copy(lastEvent = closeEvent)
}

/**
 * Walks up from [from] to the first ancestor that still has an unclosed child.
 * Null means "back to the starting position" — or, once the road in has been earned,
 * back to the entry node, which is as far out of the opening as a round ever reels.
 */
private fun BranchState.nearestOpenJunction(from: TreeNode, statuses: Map<String, NodeStatus>): TreeNode? {
    // Standing on the entry already: there is nowhere further out to go, so "back to the
    // junction" from inside the Italian never drops you outside the Italian.
    if (from.id == entryNodeId) return from
    var candidate = from.parentId?.let(tree::node)
    while (candidate != null) {
        if (candidate.id == entryNodeId) return candidate
        if (candidate.children.any { statuses[it.id]?.isClosed != true }) return candidate
        candidate = candidate.parentId?.let(tree::node)
    }
    return entryNodeId?.let(tree::node)
}

/** Abandons the current line without penalty and returns to the nearest open junction. */
fun BranchState.backToJunction(): BranchState {
    val here = cursor ?: return this
    val junction = nearestOpenJunction(here, statuses)
    return copy(cursorId = junction?.id, strikes = 0, lastEvent = null)
}
