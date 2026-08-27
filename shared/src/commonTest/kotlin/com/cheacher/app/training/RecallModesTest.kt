package com.cheacher.app.training

import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.tinyRepertoire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecallModesTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())
    private fun move(uci: String): Move = assertNotNull(Move.fromUci(uci))

    @Test
    fun blitzReplacesTheWholeFormationAfterEachCorrectMove() {
        val cards = moveDrillBank(listOf(tree)).take(2)
        val state = BlitzState.start(cards, 100L).submit(cards.first().move, 250L)
        assertEquals(1, state.index)
        assertEquals(cards[1].positionBefore, state.card?.positionBefore)
        assertEquals(150L, state.answered.single().millis)
    }

    @Test
    fun quietNamesOnlyTheDestinationAndNeverAdvancesOnAMiss() {
        val card = quietBank(listOf(tree)).first()
        val start = QuietState(card)
        assertEquals(card.line.last().name, start.card.targetName)
        assertNull(start.hint)

        val missed = start.submit(move("d2d4"))
        assertEquals(0, missed.index)
        assertFalse(missed.lastCorrect!!)
        assertTrue(missed.hint?.contains(card.line.first().san) == true)
        assertFalse(missed.hint.orEmpty().contains(card.line.drop(1).first().name))
    }

    @Test
    fun quietRequiresTheExactSequenceToReachTheTarget() {
        var state = QuietState(quietBank(listOf(tree)).first())
        state.card.line.forEach { node -> state = state.submit(node.move) }
        assertTrue(state.finished)
        assertEquals(1f, state.progress)
    }
}
