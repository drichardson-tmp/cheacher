package com.cheacher.app.ui.screens

import com.cheacher.app.progress.DrillRecord
import com.cheacher.app.progress.recordRound
import com.cheacher.app.progress.currentEpochMillis
import com.cheacher.app.training.SquareDrillState
import com.cheacher.app.training.drillPrompts
import com.cheacher.app.training.tap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shell around [SquareDrillState]: deals the prompts, holds the clock, and writes one
 * journal entry per finished round.
 *
 * Composition-scoped like the session models. The round is journalled *once, at the end*
 * rather than per rep — a half-finished round is not a result, and a median computed from
 * three reps would poison the record it is meant to measure.
 */
class SquareDrillViewModel(
    private val journal: Journal,
    now: () -> Long = ::currentEpochMillis,
) {
    private val clock = now

    private val _state = MutableStateFlow(
        SquareDrillState.start(drillPrompts(), clock()),
    )
    val state: StateFlow<SquareDrillState> = _state.asStateFlow()

    /** Bumped on every tap so equal verdicts still replay their flash. */
    private val _taps = MutableStateFlow(0)
    val taps: StateFlow<Int> = _taps.asStateFlow()

    fun onSquareTap(square: Int) {
        val current = _state.value
        if (current.finished) return
        val next = current.tap(square, clock())
        _state.value = next
        _taps.update { it + 1 }

        if (next.finished && !current.finished) {
            val summary = next.summary
            journal { it.copy(squareDrill = (it.squareDrill ?: DrillRecord()).recordRound(summary)) }
        }
    }

    /** A fresh deal — never a replay of the same twenty squares. */
    fun again() {
        _state.value = SquareDrillState.start(drillPrompts(), clock())
        _taps.value = 0
    }
}
