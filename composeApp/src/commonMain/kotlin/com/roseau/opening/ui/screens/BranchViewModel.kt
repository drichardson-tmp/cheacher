package com.roseau.opening.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roseau.opening.chess.Color
import com.roseau.opening.chess.Move
import com.roseau.opening.domain.OpeningTree
import com.roseau.opening.progress.ProgressStore
import com.roseau.opening.progress.TrainingRecord
import com.roseau.opening.progress.currentEpochMillis
import com.roseau.opening.training.BranchEvent
import com.roseau.opening.training.BranchState
import com.roseau.opening.training.MistakePolicy
import com.roseau.opening.training.NodeStatus
import com.roseau.opening.training.backToJunction
import com.roseau.opening.training.submit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shell around the pure [BranchState] reducer, journalling into the [ProgressStore].
 *
 * Line completions are found by *diffing leaf statuses* between states rather than by
 * pattern-matching events, because the reducer's final leaf reports `SessionComplete`
 * instead of `BranchClosed` — the diff catches every banked line regardless of which
 * event carried the news.
 *
 * The two counters exist for the same reason as in [GuidedViewModel]: equal events must
 * still replay their effects. [wrongShakes] drives the red shake, [closeFlashes] the
 * green "branch banked" flash.
 */
class BranchViewModel(
    tree: OpeningTree,
    policy: MistakePolicy,
    autoReplyFor: Color?,
    private val progress: ProgressStore,
) : ViewModel() {
    private val repertoireId = tree.repertoire.id

    private val _state = MutableStateFlow(BranchState.start(tree, policy, autoReplyFor))
    val state: StateFlow<BranchState> = _state.asStateFlow()

    private val _wrongShakes = MutableStateFlow(0)
    val wrongShakes: StateFlow<Int> = _wrongShakes.asStateFlow()

    private val _closeFlashes = MutableStateFlow(0)
    val closeFlashes: StateFlow<Int> = _closeFlashes.asStateFlow()

    init {
        record { it.recordSessionStart(currentEpochMillis(), policy) }
    }

    fun onMove(move: Move) {
        val current = _state.value
        val next = current.submit(move)
        _state.value = next

        when (val event = next.lastEvent) {
            is BranchEvent.Missed -> {
                _wrongShakes.update { it + 1 }
                record { it.recordMiss(current.cursorId ?: TrainingRecord.ROOT_NODE_KEY) }
            }
            is BranchEvent.BranchFailed -> {
                _wrongShakes.update { it + 1 }
                record { it.recordMiss(event.at?.id ?: TrainingRecord.ROOT_NODE_KEY) }
            }
            is BranchEvent.BranchClosed, BranchEvent.SessionComplete -> _closeFlashes.update { it + 1 }
            else -> Unit
        }

        val newlyCompleted = next.tree.lines.map { it.last() }.filter { leaf ->
            next.statusOf(leaf) == NodeStatus.COMPLETED && current.statusOf(leaf) != NodeStatus.COMPLETED
        }
        if (newlyCompleted.isNotEmpty()) {
            record { r -> newlyCompleted.fold(r) { acc, leaf -> acc.recordLineCompleted(leaf.id) } }
        }
        if (next.finished && !current.finished) {
            record { it.recordBranchSessionCompleted(cleanSweep = next.progress.failedLines == 0) }
        }
    }

    fun backToJunction() = _state.update { it.backToJunction() }

    fun restartSession() {
        _state.update { BranchState.start(it.tree, it.policy, it.autoReplyFor) }
        record { it.recordSessionStart(currentEpochMillis(), _state.value.policy) }
    }

    private fun record(transform: (TrainingRecord) -> TrainingRecord) {
        viewModelScope.launch { progress.update(repertoireId, transform) }
    }
}
