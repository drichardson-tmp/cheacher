package com.cheacher.app.ui.screens

import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.tinyRepertoire
import com.cheacher.app.progress.InMemoryProgressStore
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.training.MistakePolicy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BranchViewModelTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())
    private val line = tree.lines.first()
    private val leaf = line.last()

    private fun move(uci: String): Move = assertNotNull(Move.fromUci(uci))

    private class RecordingJournal {
        var record: TrainingRecord = TrainingRecord.empty("tiny")
        val journal: Journal = { transform -> record = transform(record) }
    }

    @Test
    fun focusedReviewClocksTheMoveWithoutClaimingTheWholeLine() = runTest {
        val journal = RecordingJournal()
        val vm = BranchViewModel(
            tree = tree,
            policy = MistakePolicy.STRICT,
            autoReplyFor = null,
            progress = InMemoryProgressStore(),
            allowedNodeIds = line.mapTo(mutableSetOf()) { it.id },
            entryNodeId = leaf.parentId,
            focusedReview = true,
            scope = backgroundScope,
            journal = journal.journal,
        )

        vm.onMove(leaf.move)

        assertEquals(1, journal.record.nodeReviewStreakOf(leaf.id))
        assertEquals(0, journal.record.branchCompletionsOf(leaf.id))
        assertEquals(0, journal.record.branchSessionsCompleted)
    }

    @Test
    fun forgivenMissIsAttributedToTheExpectedMoveAndEarnsNoCleanRecall() = runTest {
        val journal = RecordingJournal()
        val vm = BranchViewModel(
            tree = tree,
            policy = MistakePolicy.ONE_ALLOWANCE,
            autoReplyFor = null,
            progress = InMemoryProgressStore(),
            allowedNodeIds = line.mapTo(mutableSetOf()) { it.id },
            entryNodeId = leaf.parentId,
            focusedReview = true,
            scope = backgroundScope,
            journal = journal.journal,
        )

        vm.onMove(move("a2a3"))
        vm.onMove(leaf.move)

        assertEquals(1, journal.record.missCounts[leaf.id])
        assertNull(journal.record.nodeReviews[leaf.id], "a corrected miss is not a clean recall")
        assertEquals(0, journal.record.branchCompletionsOf(leaf.id))
    }
}
