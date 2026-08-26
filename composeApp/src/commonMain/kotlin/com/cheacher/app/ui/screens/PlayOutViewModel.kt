package com.cheacher.app.ui.screens

import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.engine.SparringPartner
import com.cheacher.app.engine.createSparringEngine
import com.cheacher.app.engine.materialDeficit
import com.cheacher.app.engine.rubberBanded
import com.cheacher.app.progress.currentEpochMillis
import com.cheacher.app.training.PlayOutState
import com.cheacher.app.training.play
import com.cheacher.app.training.resign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Shell around the pure [PlayOutState] reducer, plus the one impure thing this mode
 * has that the others don't: an opponent. Composition-scoped like the other session
 * models — one visit, one model, one game history — with the engine torn down by the
 * screen's DisposableEffect, since a Stockfish process must not outlive its game.
 *
 * The engine plays at [engineElo] — the learner's sparring rating, snapshotted at
 * navigation — softened in-game by the rubber band when the learner falls behind.
 * Exactly one [com.cheacher.app.progress.TrainingRecord.recordSparringGame] lands per
 * finished game, guarded by [journalledOutcome]; a rematch resets the guard.
 */
class PlayOutViewModel(
    private val tree: OpeningTree,
    private val leafId: String,
    val engineElo: Int,
    private val scope: CoroutineScope,
    private val journal: Journal,
) {
    private val partner = SparringPartner(createSparringEngine(Random(currentEpochMillis())), Random(currentEpochMillis()))

    private val _state = MutableStateFlow(PlayOutState.start(tree, leafId))
    val state: StateFlow<PlayOutState> = _state.asStateFlow()

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    private var journalledOutcome = false

    init {
        journal { it.recordSessionStart(currentEpochMillis()) }
        maybeEngineTurn()
    }

    fun onMove(move: Move) {
        val current = _state.value
        if (!current.isLearnerTurn) return
        val next = current.play(move)
        if (next === current) return
        _state.value = next
        settleOrContinue()
    }

    fun resign() {
        _state.value = _state.value.resign()
        settleOrContinue()
    }

    fun rematch() {
        journalledOutcome = false
        _state.value = PlayOutState.start(tree, leafId)
        journal { it.recordSessionStart(currentEpochMillis()) }
        maybeEngineTurn()
    }

    fun dispose() = partner.close()

    private fun settleOrContinue() {
        val outcome = _state.value.outcome
        if (outcome == null) {
            maybeEngineTurn()
            return
        }
        if (journalledOutcome) return
        journalledOutcome = true
        journal { it.recordSparringGame(outcome.result.score) }
    }

    private fun maybeEngineTurn() {
        val snapshot = _state.value
        if (!snapshot.isEngineTurn) return
        scope.launch {
            _thinking.value = true
            try {
                // A beat of "thought" even when the search is instant: an opponent who
                // replies in zero frames reads as a UI glitch, not a player.
                delay(450)
                val elo = rubberBanded(engineElo, materialDeficit(snapshot.position, snapshot.learnerSide))
                val move = withContext(Dispatchers.Default) { partner.move(snapshot.position, elo) }
                val current = _state.value
                // The learner can only have resigned meanwhile; a finished game takes no move.
                if (move != null && current.isEngineTurn) {
                    _state.value = current.play(move)
                    settleOrContinue()
                }
            } finally {
                _thinking.value = false
            }
        }
    }
}
