package com.cheacher.app.progress

import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.tinyRepertoire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class TrainingRecordMigrationTest {
    private val tree = OpeningTree.resolve(tinyRepertoire())

    @Test
    fun positionalHistoryMovesToStablePaths() {
        val review = LineReview(lastReviewedAt = 42L, streak = 3)
        val migrated = TrainingRecord(
            repertoireId = "tiny",
            missCounts = mapOf("0.1" to 2),
            lineCompletions = mapOf("0.1.0" to 4),
            branchLineCompletions = mapOf("0.1.0" to 3),
            lineReviews = mapOf("0.1.0" to review),
            lineCredits = mapOf("0.1.0" to 0.5),
        ).withStableNodeIds(tree)

        assertEquals(mapOf("e2e4/c7c5" to 2), migrated.missCounts)
        assertEquals(mapOf("e2e4/c7c5/g1f3" to 4), migrated.lineCompletions)
        assertEquals(mapOf("e2e4/c7c5/g1f3" to 3), migrated.branchLineCompletions)
        assertEquals(mapOf("e2e4/c7c5/g1f3" to review), migrated.lineReviews)
        assertEquals(mapOf("e2e4/c7c5/g1f3" to 0.5), migrated.lineCredits)
    }

    @Test
    fun mixedRecordsMergeCountsAndPreferStableLatestValues() {
        val leaf = "e2e4/e7e5/g1f3"
        val migrated = TrainingRecord(
            repertoireId = "tiny",
            missCounts = mapOf("0.0.0" to 2, leaf to 3),
            lineCredits = mapOf("0.0.0" to 0.5, leaf to 1.0),
        ).withStableNodeIds(tree)

        assertEquals(5, migrated.missCounts[leaf])
        assertEquals(1.0, migrated.lineCredits[leaf])
    }

    @Test
    fun alreadyStableRecordIsReturnedUnchanged() {
        val record = TrainingRecord.empty("tiny").recordMiss("e2e4")
        assertSame(record, record.withStableNodeIds(tree))
    }
}
