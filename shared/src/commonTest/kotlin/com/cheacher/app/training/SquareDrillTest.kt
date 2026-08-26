package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Squares
import com.cheacher.app.progress.DrillRecord
import com.cheacher.app.progress.recordRound
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SquareDrillTest {
    private fun square(name: String): Int = assertNotNull(Squares.parse(name))

    private val fourSquares = listOf("e4", "f3", "c6", "h8").map(::square)

    @Test
    fun theRightSquareBanksTheRepAndMovesOn() {
        val state = SquareDrillState.start(fourSquares, startedAt = 1_000L)
            .tap(square("e4"), nowMillis = 2_200L)
        val event = assertIs<DrillEvent.Found>(state.lastEvent)
        assertEquals(1_200L, event.millis)
        assertEquals("f3", state.targetName)
        assertEquals(listOf(DrillRep(square("e4"), 1_200L, clean = true)), state.answered)
    }

    @Test
    fun aWrongTapKeepsThePromptUpAndCostsTheRepItsCleanFlag() {
        val missed = SquareDrillState.start(fourSquares, startedAt = 0L)
            .tap(square("e5"), nowMillis = 500L)
        assertIs<DrillEvent.Missed>(missed.lastEvent)
        assertEquals("e4", missed.targetName, "you do not get to skip a square you can't find")
        assertEquals(1, missed.wrongTaps)
        assertTrue(missed.answered.isEmpty())

        // The clock keeps running through the miss: the rep's time is time-to-find.
        val found = missed.tap(square("e4"), nowMillis = 1_500L)
        assertEquals(DrillRep(square("e4"), 1_500L, clean = false), found.answered.single())
        assertEquals(0, found.wrongTaps, "the next prompt starts clean")
    }

    @Test
    fun theTableTurnsAtTheHalfwayMark() {
        var state = SquareDrillState.start(fourSquares, startedAt = 0L)
        assertEquals(Color.WHITE, state.orientation)
        state = state.tap(square("e4"), 100L)
        assertEquals(Color.WHITE, state.orientation)
        state = state.tap(square("f3"), 200L)
        assertEquals(Color.BLACK, state.orientation, "second half is drilled from the other side")
    }

    @Test
    fun theRoundFinishesOnTheLastSquare() {
        var state = SquareDrillState.start(fourSquares, startedAt = 0L)
        var at = 0L
        for (target in fourSquares) {
            assertFalse(state.finished)
            at += 1_000L
            state = state.tap(target, at)
        }
        assertTrue(state.finished)
        assertNull(state.target)
        assertEquals(4, state.summary.reps)
        // Taps after the round are inert rather than an error.
        assertEquals(state, state.tap(square("a1"), at + 500L))
    }

    @Test
    fun theSummaryReportsTheMedianNotTheMean() {
        // 1s, 1s, 1s, 9s: the mean is 3s, but three of the four reps were quick.
        var state = SquareDrillState.start(fourSquares, startedAt = 0L)
        for ((index, target) in fourSquares.withIndex()) {
            val at = if (index == 3) 12_000L else (index + 1) * 1_000L
            state = state.tap(target, at)
        }
        val summary = state.summary
        assertEquals(1_000L, summary.medianMillis)
        assertEquals(1_000L, summary.bestMillis)
        assertEquals(4, summary.cleanReps)
        assertEquals(1f, summary.accuracy)
    }

    @Test
    fun promptsNeverRepeatBackToBack() {
        val prompts = drillPrompts(count = 200, random = Random(7))
        assertEquals(200, prompts.size)
        assertTrue(prompts.all { it in 0 until Squares.COUNT })
        assertTrue(prompts.zipWithNext().none { (a, b) -> a == b })
    }

    @Test
    fun aRoundFoldsIntoTheRecordAndBestsOnlyImprove() {
        val fast = DrillSummary(reps = 4, cleanReps = 4, medianMillis = 900L, bestMillis = 600L)
        val slow = DrillSummary(reps = 4, cleanReps = 2, medianMillis = 2_400L, bestMillis = 1_800L)

        val afterFast = DrillRecord().recordRound(fast)
        assertEquals(1, afterFast.rounds)
        assertEquals(900L, afterFast.bestMedianMillis)

        val afterSlow = afterFast.recordRound(slow)
        assertEquals(2, afterSlow.rounds)
        assertEquals(8, afterSlow.reps)
        assertEquals(6, afterSlow.cleanReps)
        assertEquals(2_400L, afterSlow.lastMedianMillis, "the last round is the honest one")
        assertEquals(900L, afterSlow.bestMedianMillis, "the best is still the best")
        assertEquals(600L, afterSlow.bestRepMillis)
    }

    /** The drill screen builds its board from this FEN at class-init time: it must parse. */
    @Test
    fun theEmptyBoardFenIsValid() {
        val board = assertNotNull(com.cheacher.app.chess.Fen.parseOrNull("8/8/8/8/8/8/8/8 w - - 0 1"))
        assertTrue((0 until Squares.COUNT).all { board[it] == null })
        assertTrue(board.legalMoves().isEmpty())
    }

    @Test
    fun anUnfinishedRoundIsNotAResult() {
        val nothing = DrillSummary.of(emptyList())
        assertNull(nothing.medianMillis)
        assertEquals(DrillRecord(), DrillRecord().recordRound(nothing))
    }
}
