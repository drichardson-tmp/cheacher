package com.cheacher.app.ui.screens

import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.tinyRepertoire
import com.cheacher.app.progress.InMemoryProgressStore
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.training.OpeningStanding
import com.cheacher.app.training.StudyKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The journal side of guided study: what a session banks is what the *next* session
 * opens holding. Leaving a lesson half-done and coming back must resume, not restart.
 */
class GuidedViewModelTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())

    /** Applies journal transforms synchronously; the test's view of persisted truth. */
    private class RecordingJournal {
        var record: TrainingRecord = TrainingRecord.empty("tiny")
        val journal: Journal = { transform -> record = transform(record) }
    }

    private fun session(journal: RecordingJournal, scope: CoroutineScope): GuidedViewModel {
        val standing = OpeningStanding(tree, journal.record)
        return GuidedViewModel(
            tree = tree,
            progress = InMemoryProgressStore(),
            lineIndices = null,
            kind = StudyKind.LEARN,
            priorCredits = standing.bankedCredits,
            scope = scope,
            journal = journal.journal,
        )
    }

    private fun GuidedViewModel.walk(vararg uci: String) {
        uci.forEach { onMove(assertNotNull(Move.fromUci(it))) }
    }

    @Test
    fun aSecondVisitResumesWhereTheLessonStopped() = runTest {
        val journal = RecordingJournal()
        val scope = CoroutineScope(Job())

        // Visit one: line 0 answered cleanly, then the learner leaves for the shelf.
        session(journal, scope).walk("e2e4", "e7e5", "g1f3")
        assertEquals(1.0, journal.record.creditOf("e2e4/e7e5/g1f3"))

        // Visit two: the banked line is still on the score and is not walked again.
        val second = session(journal, scope)
        assertEquals(listOf(1), second.state.value.passLines)
        assertEquals(1.0, second.state.value.sessionScore, "the first visit still counts")
        assertEquals(2, second.state.value.deal.size)

        second.walk("e2e4", "c7c5", "g1f3")
        assertTrue(second.state.value.finished)
        assertNotNull(journal.record.openingReview, "finishing the book puts it on the ladder")
        scope.cancel()
    }

    @Test
    fun aBookAnsweredButNeverClosedClosesItself() = runTest {
        val journal = RecordingJournal()
        val scope = CoroutineScope(Job())
        // Every line banked clean, but the closing session never landed — the learner
        // quit on the last board. The next visit must not re-walk the whole book.
        journal.record = tree.lines.fold(journal.record) { acc, line ->
            acc.recordLineCredit(line.last().id, 1.0)
        }

        val resumed = session(journal, scope)

        assertTrue(resumed.state.value.finished)
        assertEquals(1, journal.record.guidedSessionsCompleted)
        assertNotNull(journal.record.openingReview, "the opening moves onto the review ladder")
        scope.cancel()
    }
}
