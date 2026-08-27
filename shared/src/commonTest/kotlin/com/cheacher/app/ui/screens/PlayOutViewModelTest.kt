package com.cheacher.app.ui.screens

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.tinyRepertoire
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.training.EndReason
import com.cheacher.app.training.GameResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The impure shell around the play-out reducer: engine replies arrive on their own,
 * exactly one game lands in the journal per finish, and a rematch re-arms both. The
 * engine here is whatever the platform factory returns off-device (PocketFish), which
 * is the point — the ViewModel must not care.
 */
class PlayOutViewModelTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())

    /** Applies journal transforms synchronously; the test's view of persisted truth. */
    private class RecordingJournal {
        var record: TrainingRecord = TrainingRecord.empty("tiny")
        val journal: Journal = { transform -> record = transform(record) }
    }

    @Test
    fun engineOpensTheGameWhenTheBookEndsOnTheLearnersMove() = runTest {
        val journal = RecordingJournal()
        val vm = PlayOutViewModel(tree, "e2e4/e7e5/g1f3", 700, this, journal.journal)
        // The tiny book's line ends on White's 2.Nf3, so Black — the engine — is up.
        val state = vm.state.first { it.freshMoves.isNotEmpty() }
        assertEquals(Color.BLACK, state.freshMoves.single().mover)
        assertTrue(state.isLearnerTurn, "after the engine's reply the learner is on the move")
        vm.dispose()
    }

    @Test
    fun learnerMovesDrawAnEngineReply() = runTest {
        val journal = RecordingJournal()
        val vm = PlayOutViewModel(tree, "e2e4/e7e5/g1f3", 700, this, journal.journal)
        val opened = vm.state.first { it.isLearnerTurn }

        vm.onMove(opened.position.legalMoves().first())
        val replied = vm.state.first { it.freshMoves.size >= 3 || it.outcome != null }

        if (replied.outcome == null) {
            assertEquals(Color.BLACK, replied.freshMoves.last().mover, "the engine answered")
            assertTrue(replied.isLearnerTurn)
        }
        vm.dispose()
    }

    @Test
    fun movesAreIgnoredWhileTheEngineIsOnTheMove() = runTest {
        val journal = RecordingJournal()
        val vm = PlayOutViewModel(tree, "e2e4/e7e5/g1f3", 700, this, journal.journal)
        // The engine's opening think has not run yet — it is Black's move, and any
        // learner input must bounce.
        val before = vm.state.value
        assertTrue(before.isEngineTurn)
        vm.onMove(before.position.legalMoves().first())
        assertEquals(before, vm.state.value)
        vm.dispose()
    }

    @Test
    fun resigningJournalsExactlyOneGameAndSwallowsThePendingEngineMove() = runTest {
        val journal = RecordingJournal()
        val vm = PlayOutViewModel(tree, "e2e4/e7e5/g1f3", 700, this, journal.journal)

        vm.resign() // before the engine's queued think ever runs
        vm.resign() // a second tap must not double-book

        val outcome = assertNotNull(vm.state.value.outcome)
        assertEquals(GameResult.ENGINE_WIN, outcome.result)
        assertEquals(EndReason.RESIGNATION, outcome.reason)
        assertEquals(1, journal.record.sparring.gamesPlayed)
        assertEquals(1, journal.record.sparring.losses)
        assertEquals(668, journal.record.sparring.rating, "one loss at K=64 eases the engine 32")

        // Let the orphaned engine coroutine finish: the game is over, so its move drops.
        testScheduler.advanceUntilIdle()
        assertTrue(vm.state.value.freshMoves.isEmpty())
        vm.dispose()
    }

    @Test
    fun rematchRearmsTheBoardAndTheJournal() = runTest {
        val journal = RecordingJournal()
        val vm = PlayOutViewModel(tree, "e2e4/e7e5/g1f3", 700, this, journal.journal)
        vm.resign()

        vm.rematch()
        assertTrue(vm.state.value.outcome == null && vm.state.value.freshMoves.isEmpty())
        vm.resign()

        assertEquals(2, journal.record.sparring.gamesPlayed)
        assertEquals(2, journal.record.sparring.losses)
        assertEquals(2, journal.record.sessionStarts.size, "the first deal and the rematch")
        vm.dispose()
    }
}
