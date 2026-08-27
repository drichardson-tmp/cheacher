package com.cheacher.app.ui.screens

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.progress.ProgressStore
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.progress.currentEpochMillis
import com.cheacher.app.training.BranchEvent
import com.cheacher.app.training.BranchState
import com.cheacher.app.training.MistakePolicy
import com.cheacher.app.training.NodeStatus
import com.cheacher.app.training.backToJunction
import com.cheacher.app.training.isReachingForRoadIn
import com.cheacher.app.training.leafIdsThrough
import com.cheacher.app.training.submit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shell around the pure [BranchState] reducer, journalling into the training history.
 * Like [GuidedViewModel], this is composition-scoped rather than an androidx ViewModel:
 * one visit, one model, one [scope] — stale reuse and orphaned watchers are impossible
 * by construction, and only [Journal] writes outlive the visit.
 *
 * Line completions are found by *diffing leaf statuses* between states rather than by
 * pattern-matching events, because the reducer's final leaf reports `SessionComplete`
 * instead of `BranchClosed` — the diff catches every banked line regardless of which
 * event carried the news. They are journalled as *branch* completions, the second half
 * of the mastery rule — but a line that took a miss earlier in the round banks with
 * `cleanRecall = false`: finishing after a stumble is still a finish, it just proves
 * nothing to the spacing ladder.
 *
 * [allowedNodeIds] is the progression gate, decided at navigation time; null is the
 * whole tree. A locked door still shakes the board — feedback, not punishment — but
 * records no miss.
 *
 * [entryNodeId] is the earned road in ([com.cheacher.app.training.OpeningEntry]), the
 * other navigation-time snapshot: the round opens there and never reels back past it.
 *
 * The two counters exist for the same reason as in [GuidedViewModel]: equal events must
 * still replay their effects. [wrongShakes] drives the red shake, [closeFlashes] the
 * green "branch banked" flash.
 */
class BranchViewModel(
    private val tree: OpeningTree,
    policy: MistakePolicy,
    autoReplyFor: Color?,
    progress: ProgressStore,
    private val allowedNodeIds: Set<String>?,
    private val entryNodeId: String? = null,
    /** True for a below-line review: journal moves, but never claim the whole line. */
    private val focusedReview: Boolean = false,
    scope: CoroutineScope,
    private val journal: Journal,
) {
    private val _state =
        MutableStateFlow(BranchState.start(tree, policy, autoReplyFor, allowedNodeIds, entryNodeId))
    val state: StateFlow<BranchState> = _state.asStateFlow()

    private val _wrongShakes = MutableStateFlow(0)
    val wrongShakes: StateFlow<Int> = _wrongShakes.asStateFlow()

    private val _closeFlashes = MutableStateFlow(0)
    val closeFlashes: StateFlow<Int> = _closeFlashes.asStateFlow()

    private val _unlock = MutableStateFlow<UnlockBanner?>(null)

    /** The "new branch unlocked" moment, when this session's completions move the frontier. */
    val unlock: StateFlow<UnlockBanner?> = _unlock.asStateFlow()

    /** Leaves whose line took a miss this round: they may still complete, but prove nothing. */
    private val lapsedThisRound = mutableSetOf<String>()

    /** A move earns at most one spacing success per round, even if the board revisits it. */
    private val recalledThisRound = mutableSetOf<String>()

    /** A move missed this round cannot earn a clean recall until a fresh round. */
    private val lapsedNodesThisRound = mutableSetOf<String>()

    init {
        journal { it.recordSessionStart(currentEpochMillis(), policy) }
        watchFrontier(scope, tree, tree.repertoire.id, progress, _unlock)
    }

    fun onMove(move: Move) {
        val current = _state.value
        val recalledNode = tree.childrenOf(current.cursor).firstOrNull { candidate ->
            candidate.move == move &&
                !current.statusOf(candidate).isClosed &&
                candidate.id !in recalledThisRound &&
                candidate.id !in lapsedNodesThisRound &&
                (current.targetPathIds.isEmpty() || candidate.id in current.targetPathIds)
        }
        val next = current.submit(move)
        _state.value = next

        // Only the learner's accepted move counts. Auto-replies are scaffolding, and
        // guided moves are study; neither is evidence for a blind per-move review clock.
        recalledNode?.let { node ->
            recalledThisRound += node.id
            val at = currentEpochMillis()
            journal { it.recordNodeRecalled(node.id, at) }
        }

        // Blind recall pays the entry toll too — reaching the opening with a clean sheet.
        if (next.roadInCleared && !current.roadInCleared) {
            journal { it.recordTrunkCleared() }
        }

        when (val event = next.lastEvent) {
            // A miss lapses the exact move's clock. The surrounding line keeps its own
            // history; a below-line review should not manufacture a ten-move redeal.
            is BranchEvent.Missed -> recordMissAt(expectedNodeId(current), current.cursorId)
            is BranchEvent.BranchFailed -> recordMissAt(expectedNodeId(current), event.at?.id)
            is BranchEvent.Locked -> _wrongShakes.update { it + 1 }
            is BranchEvent.BranchClosed, is BranchEvent.SessionComplete -> _closeFlashes.update { it + 1 }
            else -> Unit
        }

        val newlyCompleted = next.tree.lines.map { it.last() }.filter { leaf ->
            next.statusOf(leaf) == NodeStatus.COMPLETED && current.statusOf(leaf) != NodeStatus.COMPLETED
        }
        if (newlyCompleted.isNotEmpty() && !focusedReview) {
            val at = currentEpochMillis()
            journal { r ->
                newlyCompleted.fold(r) { acc, leaf ->
                    acc.recordBranchLineCompleted(leaf.id, at, cleanRecall = leaf.id !in lapsedThisRound)
                }
            }
        }
        if (next.finished && !current.finished && !focusedReview) {
            journal { it.recordBranchSessionCompleted(cleanSweep = next.progress.failedLines == 0) }
        }
    }

    private fun expectedNodeId(state: BranchState): String? =
        tree.childrenOf(state.cursor).firstOrNull { it.id in state.targetPathIds }?.id

    private fun recordMissAt(missedNodeId: String?, cursorNodeId: String?) {
        _wrongShakes.update { it + 1 }
        val nodeId = missedNodeId ?: TrainingRecord.ROOT_NODE_KEY
        lapsedNodesThisRound += nodeId
        lapsedThisRound += tree.leafIdsThrough(nodeId)
        journal { it.recordMiss(nodeId) }
        // A stumble on the way in hands back the entry, so the next session walks the
        // road again rather than being dropped past a door that just proved sticky.
        if (tree.isReachingForRoadIn(cursorNodeId)) journal { it.recordTrunkFumbled() }
    }

    fun backToJunction() = _state.update { it.backToJunction() }

    fun restartSession() {
        lapsedThisRound.clear()
        recalledThisRound.clear()
        lapsedNodesThisRound.clear()
        _state.update {
            BranchState.start(it.tree, it.policy, it.autoReplyFor, allowedNodeIds, entryNodeId)
        }
        journal { it.recordSessionStart(currentEpochMillis(), _state.value.policy) }
    }

    fun dismissUnlock() = _unlock.update { null }
}
