package com.roseau.opening.training

import com.roseau.opening.chess.Move
import com.roseau.opening.chess.Squares
import com.roseau.opening.domain.OpeningTree
import com.roseau.opening.domain.tinyRepertoire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuidedSessionTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())

    private fun move(uci: String): Move = assertNotNull(Move.fromUci(uci))

    @Test
    fun startPromptsTheFirstName() {
        val state = GuidedState.start(tree)
        assertEquals("King's Pawn Opening", state.prompt?.name)
        assertNull(state.prompt?.idea, "the idea is a paid reveal, not a freebie")
        assertEquals(tree.root, state.position)
        assertFalse(state.finished)
    }

    @Test
    fun correctMoveAdvancesTheBoard() {
        val state = GuidedState.start(tree).submit(move("e2e4"))
        assertIs<GuidedEvent.Correct>(state.lastEvent)
        assertEquals("Open Game", state.prompt?.name)
        assertEquals(1, state.played.size)
        assertEquals("e4", state.played.single().san)
    }

    @Test
    fun wrongMoveStaysPutAndUnlocksTheIdea() {
        val state = GuidedState.start(tree).submit(move("d2d4"))
        val event = assertIs<GuidedEvent.Wrong>(state.lastEvent)
        assertEquals("Centre, quickly.", event.idea)
        assertEquals(0, state.plyIndex, "a wrong move must never advance")
        assertEquals(1, state.wrongAttempts)
        assertTrue(state.ideaRevealed)
        assertEquals("Centre, quickly.", state.prompt?.idea)
    }

    @Test
    fun ideaCanBeAskedForWithoutCostingAnAttempt() {
        val state = GuidedState.start(tree).revealIdea()
        assertEquals("Centre, quickly.", state.prompt?.idea)
        assertEquals(0, state.wrongAttempts)
    }

    @Test
    fun ideaRevealResetsOnTheNextMove() {
        val state = GuidedState.start(tree)
            .submit(move("d2d4")) // miss: idea revealed
            .submit(move("e2e4")) // found
        assertFalse(state.ideaRevealed)
        assertNull(state.prompt?.idea)
        assertEquals(0, state.wrongAttempts)
    }

    @Test
    fun finishingALineLoadsTheNextAndResetsTheBoard() {
        val state = GuidedState.start(tree)
            .submit(move("e2e4"))
            .submit(move("e7e5"))
            .submit(move("g1f3"))
        val event = assertIs<GuidedEvent.LineComplete>(state.lastEvent)
        assertEquals(listOf("0", "0.0", "0.0.0"), event.line.map { it.id })
        assertEquals(1, state.lineIndex)
        assertEquals(0, state.plyIndex)
        assertEquals(tree.root, state.position, "the next line starts from the root again")
        assertEquals("King's Pawn Opening", state.prompt?.name)
    }

    @Test
    fun finishingTheLastLineFinishesTheSession() {
        var state = GuidedState.start(tree)
        for (uci in listOf("e2e4", "e7e5", "g1f3", "e2e4", "c7c5", "g1f3")) {
            state = state.submit(move(uci))
        }
        assertTrue(state.finished)
        assertEquals(GuidedEvent.SessionComplete, state.lastEvent)
        assertNull(state.prompt)
    }

    @Test
    fun restartLineDropsBackToTheLineStart() {
        val state = GuidedState.start(tree)
            .submit(move("e2e4"))
            .submit(move("d2d4")) // miss mid-line
            .restartLine()
        assertEquals(0, state.plyIndex)
        assertFalse(state.ideaRevealed)
        assertEquals(tree.root, state.position)
        assertNull(state.lastEvent)
    }

    @Test
    fun promptCarriesMoverAndMoveNumber() {
        val state = GuidedState.start(tree).submit(move("e2e4"))
        val prompt = assertNotNull(state.prompt)
        assertEquals(com.roseau.opening.chess.Color.BLACK, prompt.mover)
        assertEquals("1...", prompt.moveNumberLabel)
        // Sanity: the expected node really is the e5 reply.
        assertEquals(Squares.parse("e5"), state.expected?.move?.to)
    }
}
