package com.cheacher.app.ui.screens

import com.cheacher.app.chess.Move
import com.cheacher.app.progress.MoveDrillRecord
import com.cheacher.app.progress.currentEpochMillis
import com.cheacher.app.progress.recordRound
import com.cheacher.app.training.MoveDrillCard
import com.cheacher.app.training.MoveDrillEvent
import com.cheacher.app.training.MoveDrillMode
import com.cheacher.app.training.MoveDrillState
import com.cheacher.app.training.dealMoveDrill
import com.cheacher.app.training.fuzzyOpeningNames
import com.cheacher.app.training.submitMove
import com.cheacher.app.training.submitName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** UI shell around the pure, shelf-wide move drill reducer. */
class MoveDrillViewModel(
    private val bank: List<MoveDrillCard>,
    private val journal: Journal,
    now: () -> Long = ::currentEpochMillis,
) {
    private val clock = now
    private val allNames = bank.map { it.name }.distinct()

    private val _state = MutableStateFlow(newRound(MoveDrillMode.FIND_MOVE))
    val state: StateFlow<MoveDrillState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    /** Incremented for board shake / answer feedback, including repeated equal events. */
    private val _attempts = MutableStateFlow(0)
    val attempts: StateFlow<Int> = _attempts.asStateFlow()

    fun setMode(mode: MoveDrillMode) {
        if (_state.value.mode == mode) return
        _state.value = newRound(mode)
        clearQuery()
        _attempts.value = 0
    }

    fun onMove(move: Move) {
        val current = _state.value
        val next = current.submitMove(move, clock())
        acceptTransition(current, next)
    }

    fun onQueryChange(value: String) {
        _query.value = value
        _suggestions.value = fuzzyOpeningNames(value, allNames)
    }

    /** A suggestion tap is the answer; no second confirmation tap is required. */
    fun submitName(name: String) {
        val current = _state.value
        val next = current.submitName(name, clock())
        acceptTransition(current, next)
        if (next.lastEvent is MoveDrillEvent.Correct) clearQuery()
    }

    /** Entering a partial or misspelled name checks the highest-ranked full-bank match. */
    fun submitClosest() {
        val answer = _suggestions.value.firstOrNull() ?: _query.value
        if (answer.isBlank()) return
        submitName(answer)
    }

    fun again() {
        _state.value = newRound(_state.value.mode)
        clearQuery()
        _attempts.value = 0
    }

    private fun newRound(mode: MoveDrillMode): MoveDrillState =
        MoveDrillState.start(dealMoveDrill(bank), mode, clock())

    private fun acceptTransition(current: MoveDrillState, next: MoveDrillState) {
        if (next === current || next == current) return
        _state.value = next
        _attempts.update { it + 1 }
        if (next.finished && !current.finished) {
            val summary = next.summary
            journal { record ->
                val moveDrill = record.moveDrill ?: MoveDrillRecord()
                record.copy(
                    moveDrill = when (next.mode) {
                        MoveDrillMode.FIND_MOVE -> moveDrill.copy(
                            findMove = moveDrill.findMove.recordRound(summary),
                        )
                        MoveDrillMode.NAME_IT -> moveDrill.copy(
                            nameIt = moveDrill.nameIt.recordRound(summary),
                        )
                    },
                )
            }
        }
    }

    private fun clearQuery() {
        _query.value = ""
        _suggestions.value = emptyList()
    }
}
