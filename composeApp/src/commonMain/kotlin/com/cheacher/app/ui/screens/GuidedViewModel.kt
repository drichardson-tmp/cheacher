package com.cheacher.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.progress.ProgressStore
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.progress.currentEpochMillis
import com.cheacher.app.training.GuidedEvent
import com.cheacher.app.training.GuidedState
import com.cheacher.app.training.restartLine
import com.cheacher.app.training.revealIdea
import com.cheacher.app.training.submit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Thin shell around the pure [GuidedState] reducer: holds the value, forwards intents,
 * and journals what happened into the [ProgressStore] — misses per node, lines walked,
 * sessions finished. The reducer stays pure; history is a side effect that lives here.
 *
 * [wrongShakes] is a monotonic counter (not derived from state) because two identical
 * wrong attempts produce equal events — the UI needs a value that always changes to
 * replay the shake.
 */
class GuidedViewModel(
    tree: OpeningTree,
    private val progress: ProgressStore,
) : ViewModel() {
    private val repertoireId = tree.repertoire.id

    private val _state = MutableStateFlow(GuidedState.start(tree))
    val state: StateFlow<GuidedState> = _state.asStateFlow()

    private val _wrongShakes = MutableStateFlow(0)
    val wrongShakes: StateFlow<Int> = _wrongShakes.asStateFlow()

    init {
        record { it.recordSessionStart(currentEpochMillis()) }
    }

    fun onMove(move: Move) {
        val current = _state.value
        val next = current.submit(move)
        _state.value = next
        when (val event = next.lastEvent) {
            is GuidedEvent.Wrong -> {
                _wrongShakes.update { it + 1 }
                record { it.recordMiss(event.expected.id) }
            }
            is GuidedEvent.LineComplete -> record { it.recordLineCompleted(event.line.last().id) }
            GuidedEvent.SessionComplete -> if (!current.finished) {
                val lastLine = current.currentLine
                record { r ->
                    (lastLine.lastOrNull()?.let { r.recordLineCompleted(it.id) } ?: r)
                        .recordGuidedSessionCompleted()
                }
            }
            else -> Unit
        }
    }

    fun revealIdea() = _state.update { it.revealIdea() }

    fun restartLine() = _state.update { it.restartLine() }

    fun restartSession() {
        _state.update { GuidedState.start(it.tree) }
        record { it.recordSessionStart(currentEpochMillis()) }
    }

    private fun record(transform: (TrainingRecord) -> TrainingRecord) {
        viewModelScope.launch { progress.update(repertoireId, transform) }
    }
}
