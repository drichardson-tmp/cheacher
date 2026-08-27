package com.cheacher.app.ui.screens

import com.cheacher.app.chess.Move
import com.cheacher.app.progress.currentEpochMillis
import com.cheacher.app.progress.recordRound
import com.cheacher.app.training.BlitzState
import com.cheacher.app.training.MoveDrillCard
import com.cheacher.app.training.QuietCard
import com.cheacher.app.training.QuietState
import com.cheacher.app.training.dealBlitz
import com.cheacher.app.training.revealHint
import com.cheacher.app.training.submit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlitzViewModel(
    private val bank: List<MoveDrillCard>,
    private val journal: Journal,
    now: () -> Long = ::currentEpochMillis,
) {
    private val clock = now
    private val _playCount = MutableStateFlow(BlitzState.DEFAULT_PLAYS)
    val playCount: StateFlow<Int> = _playCount.asStateFlow()
    private val _state = MutableStateFlow(newRound(_playCount.value))
    val state: StateFlow<BlitzState> = _state.asStateFlow()
    private val _attempts = MutableStateFlow(0)
    val attempts: StateFlow<Int> = _attempts.asStateFlow()

    fun setPlayCount(count: Int) {
        if (count == _playCount.value) return
        _playCount.value = count
        again()
    }

    fun onMove(move: Move) {
        val current = _state.value
        val next = current.submit(move, clock())
        if (next == current) return
        _state.value = next
        _attempts.value++
        if (next.finished && !current.finished) {
            journal { record ->
                val moveDrill = record.moveDrill ?: com.cheacher.app.progress.MoveDrillRecord()
                record.copy(moveDrill = moveDrill.copy(blitz = moveDrill.blitz.recordRound(next.summary)))
            }
        }
    }

    fun again() {
        _state.value = newRound(_playCount.value)
        _attempts.value = 0
    }

    private fun newRound(count: Int) = BlitzState.start(dealBlitz(bank, count), clock())
}

class QuietViewModel(private val bank: List<QuietCard>) {
    private var nextIndex = 0
    private val _state = MutableStateFlow(newLine())
    val state: StateFlow<QuietState> = _state.asStateFlow()
    private val _attempts = MutableStateFlow(0)
    val attempts: StateFlow<Int> = _attempts.asStateFlow()

    fun onMove(move: Move) {
        val current = _state.value
        val next = current.submit(move)
        if (next == current) return
        _state.value = next
        _attempts.value++
    }

    fun revealHint() { _state.value = _state.value.revealHint() }

    fun nextLine() {
        _state.value = newLine()
        _attempts.value = 0
    }

    private fun newLine(): QuietState {
        require(bank.isNotEmpty()) { "quiet mode needs at least one authored line" }
        val card = bank[nextIndex % bank.size]
        nextIndex++
        return QuietState(card)
    }
}
