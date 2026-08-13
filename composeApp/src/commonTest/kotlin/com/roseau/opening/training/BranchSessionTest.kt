package com.roseau.opening.training

import com.roseau.opening.chess.Color
import com.roseau.opening.chess.Move
import com.roseau.opening.domain.OpeningTree
import com.roseau.opening.domain.tinyRepertoire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BranchSessionTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())

    private fun move(uci: String): Move = assertNotNull(Move.fromUci(uci))

    @Test
    fun correctMoveAdvancesTheCursor() {
        val state = BranchState.start(tree).submit(move("e2e4"))
        val event = assertIs<BranchEvent.Advanced>(state.lastEvent)
        assertEquals("0", event.node.id)
        assertEquals("0", state.cursorId)
        assertEquals(NodeStatus.IN_PROGRESS, state.statusOf(assertNotNull(tree.node("0"))))
        assertEquals(2, state.openMoves.size, "both replies to 1.e4 are open")
    }

    @Test
    fun closingALeafSnapsBackToTheNearestOpenJunction() {
        val state = BranchState.start(tree)
            .submit(move("e2e4"))
            .submit(move("e7e5"))
            .submit(move("g1f3")) // leaf of line one
        val event = assertIs<BranchEvent.BranchClosed>(state.lastEvent)
        assertEquals("0.0.0", event.leaf.id)
        assertEquals("0", event.snappedTo?.id, "1.e4 still has the Sicilian open")
        assertEquals("0", state.cursorId)
        // Ancestor roll-up: e5 had a single child, so it closes with the leaf.
        assertEquals(NodeStatus.COMPLETED, state.statusOf(assertNotNull(tree.node("0.0"))))
        assertEquals(NodeStatus.COMPLETED, state.statusOf(assertNotNull(tree.node("0.0.0"))))
        // But e4 itself stays open.
        assertEquals(NodeStatus.IN_PROGRESS, state.statusOf(assertNotNull(tree.node("0"))))
        assertEquals(1, state.progress.closedLines)
        assertEquals(2, state.progress.totalLines)
        assertFalse(state.finished)
    }

    @Test
    fun closingEveryLineFinishesTheSession() {
        var state = BranchState.start(tree)
        for (uci in listOf("e2e4", "e7e5", "g1f3", "c7c5", "g1f3")) {
            state = state.submit(move(uci))
        }
        assertTrue(state.finished)
        assertEquals(BranchEvent.SessionComplete, state.lastEvent)
        assertNull(state.cursorId)
        assertEquals(2, state.progress.closedLines)
        assertEquals(0, state.progress.failedLines)
        assertEquals(1f, state.progress.fraction)
        // Completion rolled all the way to the root move.
        assertEquals(NodeStatus.COMPLETED, state.statusOf(assertNotNull(tree.node("0"))))
    }

    @Test
    fun strictPolicyFailsTheBranchOnTheFirstMiss() {
        val state = BranchState.start(tree, MistakePolicy.STRICT)
            .submit(move("e2e4"))
            .submit(move("e7e5"))
            .submit(move("d2d4")) // not in the tree
        val event = assertIs<BranchEvent.BranchFailed>(state.lastEvent)
        assertEquals("0.0", event.at?.id)
        assertEquals("0", event.snappedTo?.id)
        // The failed node and its subtree close as FAILED.
        assertEquals(NodeStatus.FAILED, state.statusOf(assertNotNull(tree.node("0.0"))))
        assertEquals(NodeStatus.FAILED, state.statusOf(assertNotNull(tree.node("0.0.0"))))
        assertEquals(1, state.progress.failedLines)
        assertEquals("0", state.cursorId)
    }

    @Test
    fun oneAllowanceForgivesExactlyOneMiss() {
        val forgiven = BranchState.start(tree, MistakePolicy.ONE_ALLOWANCE)
            .submit(move("e2e4"))
            .submit(move("d7d6")) // miss one: forgiven in place
        val missedEvent = assertIs<BranchEvent.Missed>(forgiven.lastEvent)
        assertEquals(1, missedEvent.strikes)
        assertEquals("0", forgiven.cursorId, "a forgiven miss must not move the board")
        assertEquals(0, forgiven.progress.failedLines)

        val failed = forgiven.submit(move("d7d6")) // miss two: branch lost
        assertIs<BranchEvent.BranchFailed>(failed.lastEvent)
        assertEquals(NodeStatus.FAILED, failed.statusOf(assertNotNull(tree.node("0"))))
        assertTrue(failed.finished, "failing the only root move closes the whole tree")
    }

    @Test
    fun strikesResetAfterACorrectMove() {
        val state = BranchState.start(tree, MistakePolicy.ONE_ALLOWANCE)
            .submit(move("e2e4"))
            .submit(move("d7d6")) // strike one
            .submit(move("e7e5")) // correct: strikes reset
        assertEquals(0, state.strikes)
    }

    @Test
    fun missAtTheRootIsRejectedWithoutClosingAnything() {
        val state = BranchState.start(tree, MistakePolicy.STRICT).submit(move("d2d4"))
        assertIs<BranchEvent.Missed>(state.lastEvent)
        assertNull(state.cursorId)
        assertEquals(NodeStatus.UNVISITED, state.statusOf(assertNotNull(tree.node("0"))))
        assertFalse(state.finished)
    }

    @Test
    fun replayingAClosedBranchIsCalledOut() {
        val state = BranchState.start(tree)
            .submit(move("e2e4"))
            .submit(move("e7e5"))
            .submit(move("g1f3")) // closes line one, snaps to e4
            .submit(move("e7e5")) // that door is shut
        assertIs<BranchEvent.AlreadyClosed>(state.lastEvent)
        assertEquals("0", state.cursorId)
    }

    @Test
    fun autoReplyPlaysTheOtherSide() {
        // Learner is White; Roseau answers for Black.
        var state = BranchState.start(tree, autoReplyFor = Color.BLACK)
        state = state.submit(move("e2e4"))
        // The reply 1...e5 was played automatically; it is White to move again.
        assertEquals("0.0", state.cursorId)
        assertEquals(Color.WHITE, state.position.sideToMove)

        state = state.submit(move("g1f3")) // closes line one; snap to e4, then auto ...c5
        assertEquals("0.1", state.cursorId)
        assertEquals(Color.WHITE, state.position.sideToMove)

        state = state.submit(move("g1f3")) // closes line two: done
        assertTrue(state.finished)
        assertEquals(0, state.progress.failedLines)
    }

    @Test
    fun backToJunctionAbandonsWithoutPenalty() {
        val state = BranchState.start(tree)
            .submit(move("e2e4"))
            .submit(move("e7e5"))
            .backToJunction()
        assertEquals("0", state.cursorId)
        assertEquals(NodeStatus.IN_PROGRESS, state.statusOf(assertNotNull(tree.node("0.0"))))
        assertEquals(0, state.progress.closedLines)
    }

    @Test
    fun pathMirrorsTheCursor() {
        val state = BranchState.start(tree)
            .submit(move("e2e4"))
            .submit(move("c7c5"))
        assertEquals(listOf("0", "0.1"), state.path.map { it.id })
        assertEquals("c5", state.path.last().san)
    }
}
