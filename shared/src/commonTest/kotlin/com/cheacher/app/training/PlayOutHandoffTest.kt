package com.cheacher.app.training

import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.tinyRepertoire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The seam the play-out offer stands on: a finished guided session still exposes the
 * line it just walked, and that line's leaf id is a valid [PlayOutState.start] handle.
 */
class PlayOutHandoffTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())

    private fun move(uci: String): Move = Move.fromUci(uci)!!

    @Test
    fun finishedGuidedSessionHandsItsLastLeafToPlayOut() {
        var state = GuidedState.start(tree)
        for (uci in listOf("e2e4", "e7e5", "g1f3", "e2e4", "c7c5", "g1f3")) {
            state = state.submit(move(uci))
        }
        assertTrue(state.finished)
        val leafId = state.currentLine.last().id
        assertEquals("e2e4/c7c5/g1f3", leafId, "the last walked line, not the first")

        val playOut = PlayOutState.start(tree, leafId)
        assertEquals(state.currentLine.last().position, playOut.position)
        assertEquals(state.currentLine.map { it.san }, playOut.bookMoves.map { it.san })
    }

    @Test
    fun aRestrictedSyllabusHandsOverItsOwnFinalLine() {
        var state = GuidedState.start(tree, lineIndices = listOf(0))
        for (uci in listOf("e2e4", "e7e5", "g1f3")) {
            state = state.submit(move(uci))
        }
        assertTrue(state.finished)
        assertEquals("e2e4/e7e5/g1f3", state.currentLine.last().id)
    }

    @Test
    fun everyLeafHandoffPreservesTheExactResolvedPosition() {
        for (line in tree.lines) {
            val leaf = line.last()
            val playOut = PlayOutState.start(tree, leaf.id)
            assertEquals(leaf.position, playOut.position, "wrong position for ${leaf.id}")
            assertEquals(leaf.position.sideToMove, playOut.position.sideToMove)
            assertEquals(leaf.position.castling, playOut.position.castling)
            assertEquals(leaf.position.enPassantSquare, playOut.position.enPassantSquare)
        }
    }
}
