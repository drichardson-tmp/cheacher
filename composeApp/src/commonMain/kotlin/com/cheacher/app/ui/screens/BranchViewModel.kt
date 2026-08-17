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
import com.cheacher.app.training.lapseLinesThrough
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
    scope: CoroutineScope,
    private val journal: Journal,
) {
    private val _state = MutableStateFlow(BranchState.start(tree, policy, autoReplyFor, allowedNodeIds))
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

    init {
        journal { it.recordSessionStart(currentEpochMillis(), policy) }
        watchFrontier(scope, tree, tree.repertoire.id, progress, _unlock)
    }

    fun onMove(move: Move) {
        val current = _state.value
        val next = current.submit(move)
        _state.value = next

        when (val event = next.lastEvent) {
            // A miss also lapses every line through the missed node: the review streak
            // resets, so the trouble line comes back sooner on the coach's plan.
            is BranchEvent.Missed -> recordMissAt(current.cursorId)
            is BranchEvent.BranchFailed -> recordMissAt(event.at?.id)
            is BranchEvent.Locked -> _wrongShakes.update { it + 1 }
            is BranchEvent.BranchClosed, BranchEvent.SessionComplete -> _closeFlashes.update { it + 1 }
            else -> Unit
        }

        val newlyCompleted = next.tree.lines.map { it.last() }.filter { leaf ->
            next.statusOf(leaf) == NodeStatus.COMPLETED && current.statusOf(leaf) != NodeStatus.COMPLETED
        }
        if (newlyCompleted.isNotEmpty()) {
            val at = currentEpochMillis()
            journal { r ->
                newlyCompleted.fold(r) { acc, leaf ->
                    acc.recordBranchLineCompleted(leaf.id, at, cleanRecall = leaf.id !in lapsedThisRound)
                }
            }
        }
        if (next.finished && !current.finished) {
            journal { it.recordBranchSessionCompleted(cleanSweep = next.progress.failedLines == 0) }
        }
    }

    private fun recordMissAt(cursorNodeId: String?) {
        _wrongShakes.update { it + 1 }
        val nodeId = cursorNodeId ?: TrainingRecord.ROOT_NODE_KEY
        lapsedThisRound += tree.leafIdsThrough(nodeId)
        journal { it.recordMiss(nodeId).lapseLinesThrough(tree, nodeId) }
    }

    fun backToJunction() = _state.update { it.backToJunction() }

    fun restartSession() {
        lapsedThisRound.clear()
        _state.update { BranchState.start(it.tree, it.policy, it.autoReplyFor, allowedNodeIds) }
        journal { it.recordSessionStart(currentEpochMillis(), _state.value.policy) }
    }

    fun dismissUnlock() = _unlock.update { null }
}
