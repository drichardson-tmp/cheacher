package com.cheacher.app.training

import com.cheacher.app.data.SampleRepertoires
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.progress.DrillRecord
import com.cheacher.app.progress.MoveDrillRecord
import com.cheacher.app.progress.recordRound
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MoveDrillTest {
    private val trees = SampleRepertoires.all.map(OpeningTree::resolve)
    private val bank = moveDrillBank(trees)

    @Test
    fun theBankContainsEveryAuthoredMoveOnTheShelf() {
        assertEquals(trees.sumOf { it.allNodes.size }, bank.size)
        assertEquals(bank.size, bank.map { it.id }.distinct().size)
        assertTrue(bank.any { it.name == "King's Pawn Opening" })
        assertTrue(bank.any { it.name.contains("Réti") })
    }

    @Test
    fun findMoveTimesUntilTheFirstCorrectMove() {
        val e4 = bank.first { it.id == "kings-pawn:0" }
        val d4 = bank.first { it.id == "first-moves:1" }
        val started = MoveDrillState.start(listOf(e4), MoveDrillMode.FIND_MOVE, startedAt = 1_000L)

        val missed = started.submitMove(d4.move, nowMillis = 1_500L)
        assertIs<MoveDrillEvent.WrongMove>(missed.lastEvent)
        assertFalse(missed.finished)

        val found = missed.submitMove(e4.move, nowMillis = 2_400L)
        assertTrue(found.finished)
        assertEquals(1_400L, found.answered.single().millis, "a miss does not reset the clock")
        assertFalse(found.answered.single().clean)
    }

    @Test
    fun nameItAcceptsDisplayPunctuationAndAccentsForgivingly() {
        val reti = bank.first { it.name == "Réti Opening" }
        val state = MoveDrillState.start(listOf(reti), MoveDrillMode.NAME_IT, startedAt = 0L)
            .submitName("reti-opening", nowMillis = 750L)

        assertTrue(state.finished)
        assertEquals(750L, state.summary.medianMillis)
        assertEquals(1, state.summary.cleanReps)
    }

    @Test
    fun wrongNameStaysOnTheCardAndKeepsItsClockRunning() {
        val najdorf = bank.first { it.name == "Najdorf Variation" }
        val missed = MoveDrillState.start(listOf(najdorf), MoveDrillMode.NAME_IT, startedAt = 100L)
            .submitName("Dragon Variation", nowMillis = 500L)
        assertEquals(najdorf, missed.card)
        assertIs<MoveDrillEvent.WrongName>(missed.lastEvent)

        val found = missed.submitName("Najdorf Variation", nowMillis = 1_100L)
        assertEquals(1_000L, found.answered.single().millis)
        assertFalse(found.answered.single().clean)
    }

    @Test
    fun fuzzyAutocompleteUsesTheWholeNameBank() {
        val names = bank.map { it.name }
        assertEquals("Réti Opening", fuzzyOpeningNames("reti opning", names).first())
        assertEquals("Najdorf Variation", fuzzyOpeningNames("najdrof var", names).first())
        assertEquals("King's Pawn Opening", fuzzyOpeningNames("kings pawn", names).first())
    }

    @Test
    fun aDealSamplesWithoutReplacement() {
        val dealt = dealMoveDrill(bank, count = 20, random = Random(17))
        assertEquals(20, dealt.size)
        assertEquals(20, dealt.map { it.id }.distinct().size)
    }

    @Test
    fun directionRecordsRemainSeparate() {
        val moveRound = DrillSummary(2, 1, 800L, 500L)
        val nameRound = DrillSummary(2, 2, 1_200L, 900L)
        val record = MoveDrillRecord(
            findMove = DrillRecord().recordRound(moveRound),
            nameIt = DrillRecord().recordRound(nameRound),
        )
        assertEquals(800L, record.findMove.lastMedianMillis)
        assertEquals(1_200L, record.nameIt.lastMedianMillis)
        assertEquals(1, record.findMove.cleanReps)
        assertEquals(2, record.nameIt.cleanReps)
    }
}
