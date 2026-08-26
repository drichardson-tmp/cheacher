package com.cheacher.app.engine

import com.cheacher.app.chess.Fen
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Position
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PocketFishTest {
    private fun cold(depth: Int = 2) = EngineTuning(
        elo = 2000,
        depth = depth,
        movetimeMs = 0,
        temperatureCp = 0,
        blunderChance = 0.0,
        skillLevel = 20,
    )

    @Test
    fun findsMateInOneWhenPlayedCold() = runTest {
        val engine = PocketFish(Random(1))
        val position = Fen.parse("k7/8/1K6/8/8/8/8/7R w - - 0 1")
        assertEquals(Move.fromUci("h1h8"), engine.bestMove(position, cold()))
    }

    @Test
    fun takesAHangingQueenOverAQuietMove() = runTest {
        val engine = PocketFish(Random(1))
        // Black queen sits on e4 in front of the e-pawn's diagonal… of the d3 pawn.
        val position = Fen.parse("k7/8/8/8/4q3/3P4/8/K7 w - - 0 1")
        assertEquals(Move.fromUci("d3e4"), engine.bestMove(position, cold()))
    }

    @Test
    fun returnsNullWithNoLegalMoves() = runTest {
        val engine = PocketFish(Random(1))
        val mated = Fen.parse("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1")
        assertNull(engine.bestMove(mated, cold()))
    }

    @Test
    fun isDeterministicUnderOneSeed() = runTest {
        val hot = tuningFor(700)
        val first = PocketFish(Random(42)).bestMove(Position.INITIAL, hot)
        val second = PocketFish(Random(42)).bestMove(Position.INITIAL, hot)
        assertEquals(first, second)
    }

    @Test
    fun alwaysAnswersWithALegalMoveEvenAtItsDrunkest() = runTest {
        val engine = PocketFish(Random(7))
        var position = Position.INITIAL
        val tuning = tuningFor(SparringElo.MIN)
        repeat(30) {
            val move = engine.bestMove(position, tuning) ?: return@runTest
            assertTrue(move in position.legalMoves(), "ply $it offered ${move.uci}")
            position = position.applyUnchecked(move)
        }
    }

    @Test
    fun sparringPartnerBlundersLegallyAndSurvivesADeadEngine() = runTest {
        val deadEngine = object : SparringEngine {
            override val name = "Dead"
            override suspend fun bestMove(position: Position, tuning: EngineTuning): Move? = null
        }
        val partner = SparringPartner(deadEngine, Random(3))
        repeat(20) {
            val move = partner.move(Position.INITIAL, 700)
            assertTrue(move != null && move in Position.INITIAL.legalMoves())
        }
        val mated = Fen.parse("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1")
        assertNull(partner.move(mated, 700), "no legal moves means no move, not a crash")
    }
}
