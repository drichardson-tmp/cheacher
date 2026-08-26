package com.cheacher.app.training

import com.cheacher.app.data.SampleRepertoires
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.progress.LineReview
import com.cheacher.app.progress.TrainingRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The two review-integrity rules the codex pass demanded: a completion after a miss
 * banks mastery but proves nothing to the spacing ladder, and equally-due reviews are
 * dealt weakest-streak first.
 */
class ReviewIntegrityTest {
    private val tree = OpeningTree.resolve(SampleRepertoires.italianGame)
    private val day = TrainingRecord.DAY_MILLIS

    private fun leafId(lineIndex: Int): String = tree.lines[lineIndex].last().id

    /** A record with every line mastered (one guided + one branch completion each). */
    private fun allMastered(): TrainingRecord {
        val leaves = tree.lines.map { it.last().id }
        return TrainingRecord(
            repertoireId = tree.repertoire.id,
            lineCompletions = leaves.associateWith { 2 },
            branchLineCompletions = leaves.associateWith { 1 },
        )
    }

    @Test
    fun unclean_completion_banks_mastery_but_never_touches_the_review_ladder() {
        val record = TrainingRecord.empty(tree.repertoire.id)
            .recordBranchLineCompleted(leafId(0), atEpochMillis = 1_000L, cleanRecall = false)

        assertEquals(1, record.branchCompletionsOf(leafId(0)))
        assertEquals(1, record.lineCompletions[leafId(0)])
        assertEquals(0, record.reviewStreakOf(leafId(0)))
        assertNull(record.lineReviews[leafId(0)], "an unclean recall must not stamp a review")
    }

    @Test
    fun unclean_completion_leaves_an_existing_review_exactly_as_it_was() {
        val existing = LineReview(lastReviewedAt = 5_000L, streak = 0)
        val record = TrainingRecord.empty(tree.repertoire.id)
            .copy(lineReviews = mapOf(leafId(0) to existing))
            .recordBranchLineCompleted(leafId(0), atEpochMillis = 9_000L, cleanRecall = false)

        assertEquals(existing, record.lineReviews[leafId(0)])
    }

    @Test
    fun equally_due_reviews_are_dealt_weakest_streak_first() {
        val t = 100L * day
        // Line 0: streak 3, reviewed at t          → due t + 7d.
        // Line 1: streak 1, reviewed at t + 6d     → due t + 7d. Same instant, weaker.
        // Line 2: streak 5, reviewed at t          → due t + 30d. Resting.
        val record = allMastered().copy(
            lineReviews = mapOf(
                leafId(0) to LineReview(lastReviewedAt = t, streak = 3),
                leafId(1) to LineReview(lastReviewedAt = t + 6 * day, streak = 1),
                leafId(2) to LineReview(lastReviewedAt = t, streak = 5),
            ),
        )

        val syllabus = Progression(tree, record).syllabusAt(t + 8 * day)

        assertEquals(
            listOf(1, 0),
            syllabus.sessionLines.map { it.lineIndex },
            "the shakier line must get the earlier seat when due times tie",
        )
    }
}
