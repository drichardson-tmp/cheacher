package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Fen
import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.tinyRepertoire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlayOutSessionTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())

    private fun move(uci: String): Move = Move.fromUci(uci)!!

    /** A mid-game state with no book prefix, for termination tests. */
    private fun stateAt(fen: String, learner: Color): PlayOutState {
        val position = Fen.parse(fen)
        return PlayOutState(
            learnerSide = learner,
            bookMoves = emptyList(),
            position = position,
            repetitionCounts = mapOf(PlayOutState.repetitionKey(position) to 1),
        )
    }

    @Test
    fun startCarriesTheBookLineAndItsFinalPosition() {
        val state = PlayOutState.start(tree, "0.0.0")
        assertEquals(Color.WHITE, state.learnerSide)
        assertEquals(listOf("e4", "e5", "Nf3"), state.bookMoves.map { it.san })
        assertEquals(tree.node("0.0.0")!!.position, state.position)
        assertEquals(Color.BLACK, state.position.sideToMove)
        assertTrue(state.isEngineTurn, "book ended on the learner's move; the engine is up")
        assertNull(state.outcome)
    }

    @Test
    fun startRejectsANodeThatIsNotALeaf() {
        val failure = runCatching { PlayOutState.start(tree, "0.0") }
        assertTrue(failure.isFailure)
    }

    @Test
    fun illegalMovesLeaveTheStateUntouched() {
        val state = PlayOutState.start(tree, "0.0.0")
        assertSame(state, state.play(move("e2e4")))
    }

    @Test
    fun legalMovesAppendToTheFreshStrip() {
        val state = PlayOutState.start(tree, "0.0.0").play(move("b8c6"))
        assertEquals(listOf("Nc6"), state.freshMoves.map { it.san })
        assertEquals(3, state.bookMoves.size)
        assertEquals(move("b8c6"), state.lastMove)
        assertTrue(state.isLearnerTurn)
    }

    @Test
    fun checkmateByTheLearnerWinsTheGame() {
        val state = stateAt("k7/8/1K6/8/8/8/8/7R w - - 0 1", learner = Color.WHITE).play(move("h1h8"))
        assertEquals(PlayOutOutcome(GameResult.LEARNER_WIN, EndReason.CHECKMATE), state.outcome)
        assertEquals(1.0, state.outcome!!.result.score)
    }

    @Test
    fun checkmateByTheEngineLosesTheGame() {
        val state = stateAt("k7/8/1K6/8/8/8/8/7R w - - 0 1", learner = Color.BLACK).play(move("h1h8"))
        assertEquals(PlayOutOutcome(GameResult.ENGINE_WIN, EndReason.CHECKMATE), state.outcome)
        assertEquals(0.0, state.outcome!!.result.score)
    }

    @Test
    fun stalemateIsADraw() {
        val state = stateAt("7k/5K2/8/8/8/8/8/6Q1 w - - 0 1", learner = Color.WHITE).play(move("g1g6"))
        assertEquals(PlayOutOutcome(GameResult.DRAW, EndReason.STALEMATE), state.outcome)
        assertEquals(0.5, state.outcome!!.result.score)
    }

    @Test
    fun threefoldRepetitionIsADraw() {
        var state = stateAt("k7/8/8/8/8/8/8/K6R w - - 0 1", learner = Color.WHITE)
        val shuffle = listOf("h1h2", "a8b8", "h2h1", "b8a8")
        repeat(2) { lap ->
            for (uci in shuffle) {
                assertNull(state.outcome, "no draw before the third occurrence (lap $lap)")
                state = state.play(move(uci))
            }
        }
        assertEquals(PlayOutOutcome(GameResult.DRAW, EndReason.THREEFOLD_REPETITION), state.outcome)
    }

    @Test
    fun fiftyQuietMovesAreADraw() {
        val state = stateAt("k7/8/8/8/8/8/8/K6R w - - 99 70", learner = Color.WHITE).play(move("h1h2"))
        assertEquals(PlayOutOutcome(GameResult.DRAW, EndReason.FIFTY_MOVE_RULE), state.outcome)
    }

    @Test
    fun bareKingsAreADeadDraw() {
        val state = stateAt("k7/1r5R/8/8/8/8/8/K7 w - - 0 1", learner = Color.WHITE)
            .play(move("h7b7")) // rook takes rook…
            .play(move("a8b7")) // …king takes rook: nothing left to mate with
        assertEquals(PlayOutOutcome(GameResult.DRAW, EndReason.DEAD_POSITION), state.outcome)
    }

    @Test
    fun resignationLosesAndFreezesTheGame() {
        val resigned = PlayOutState.start(tree, "0.0.0").resign()
        assertEquals(PlayOutOutcome(GameResult.ENGINE_WIN, EndReason.RESIGNATION), resigned.outcome)
        assertSame(resigned, resigned.play(move("b8c6")), "a finished game takes no moves")
        assertSame(resigned, resigned.resign())
    }

    @Test
    fun deadDrawRecognisesLoneMinorsAndSameShadeBishops() {
        assertTrue(Fen.parse("k7/8/8/8/8/8/8/K6N w - - 0 1").isDeadDraw())
        assertTrue(Fen.parse("k7/8/8/8/8/8/8/K6B w - - 0 1").isDeadDraw())
        // Bishops on h1 and f3 share the light squares; b8 adds a dark one.
        assertTrue(Fen.parse("k7/8/8/8/8/5b2/8/K6B w - - 0 1").isDeadDraw())
        assertTrue(!Fen.parse("kb6/8/8/8/8/8/8/K6B w - - 0 1").isDeadDraw())
        assertTrue(!Fen.parse("k7/8/8/8/8/8/8/K6R w - - 0 1").isDeadDraw())
        assertTrue(!Fen.parse("k7/8/8/8/8/8/8/K5NN w - - 0 1").isDeadDraw())
    }
}
