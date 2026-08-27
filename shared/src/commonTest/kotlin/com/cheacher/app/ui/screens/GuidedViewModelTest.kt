package com.cheacher.app.ui.screens

import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.tinyRepertoire
import com.cheacher.app.progress.InMemoryProgressStore
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.training.StudyKind
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GuidedViewModelTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())
    private fun move(uci: String): Move = assertNotNull(Move.fromUci(uci))

    private class RecordingJournal {
        var record = TrainingRecord.empty("tiny")
        val journal: Journal = { transform -> record = transform(record) }
    }

    @Test
    fun missAndLeafCompletionAreJournalledAgainstTheExpectedNodes() = runTest {
        val journal = RecordingJournal()
        val vm = GuidedViewModel(
            tree = tree,
            progress = InMemoryProgressStore(),
            lineIndices = listOf(0),
            kind = StudyKind.REVIEW,
            scope = backgroundScope,
            journal = journal.journal,
        )

        vm.onMove(move("d2d4"))
        assertEquals(1, journal.record.missCounts[tree.lines[0].first().id])
        listOf("e2e4", "e7e5", "g1f3").forEach { vm.onMove(move(it)) }
        assertEquals(1, journal.record.lineCompletions[tree.lines[0].last().id])
        assertEquals(1, journal.record.guidedSessionsCompleted)
        assertTrue(journal.record.sessionStarts.single() > 0L)
    }

    @Test
    fun unlockBannerTracksStoreAdvanceWithoutChangingTheSessionSyllabus() = runTest {
        val store = InMemoryProgressStore()
        val journal = RecordingJournal()
        val vm = GuidedViewModel(
            tree = tree,
            progress = store,
            lineIndices = listOf(1),
            kind = StudyKind.LEARN,
            scope = backgroundScope,
            journal = journal.journal,
        )
        runCurrent()

        val firstLeaf = tree.lines[0].last().id
        store.update("tiny") {
            it.recordLineCompleted(firstLeaf).recordBranchLineCompleted(firstLeaf, atEpochMillis = 42L)
        }
        runCurrent()

        assertEquals(listOf(1), vm.state.value.lineIndices)
        assertEquals("Sicilian Defence", vm.unlock.value?.advance?.unlockedLine?.name)
    }

    @Test
    fun everyGuidedLeafOffersItsExactPlayOutBeforeTheSessionEnds() = runTest {
        val vm = GuidedViewModel(
            tree = tree,
            progress = InMemoryProgressStore(),
            lineIndices = null,
            kind = StudyKind.REVIEW,
            scope = backgroundScope,
            journal = RecordingJournal().journal,
        )
        listOf("e2e4", "e7e5", "g1f3").forEach { vm.onMove(move(it)) }
        assertEquals(tree.lines[0].last().id, vm.playOutOffer.value)
        vm.dismissPlayOutOffer()
        listOf("c7c5", "g1f3").forEach { vm.onMove(move(it)) }
        assertEquals(tree.lines[1].last().id, vm.playOutOffer.value)
    }
}
