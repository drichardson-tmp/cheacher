package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.repertoire
import com.cheacher.app.domain.tinyRepertoire
import com.cheacher.app.progress.LineReview
import com.cheacher.app.progress.TrainingRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudyPlanTest {
    private val first = OpeningTree.resolve(tinyRepertoire())
    private val second = OpeningTree.resolve(
        repertoire("second", "Second", Color.WHITE) {
            move("d4", "Queen's Pawn Opening", "The other centre.")
        },
    )
    private val trees = listOf(first, second)
    private val day = TrainingRecord.DAY_MILLIS

    /** A record with every line of [tree] accounted and the opening on the review ladder. */
    private fun learned(tree: OpeningTree, at: Long, streak: Int = 1): TrainingRecord =
        tree.lines
            .fold(TrainingRecord.empty(tree.repertoire.id)) { acc, line ->
                acc.recordLineCredit(line.last().id, 1.0)
            }
            .copy(openingReview = LineReview(lastReviewedAt = at, streak = streak))

    @Test
    fun freshShelfDealsTheFirstBooksLines() {
        val plan = studyPlan(trees, emptyMap(), nowEpochMillis = 0L)
        val task = plan.single()
        assertEquals("tiny", task.tree.repertoire.id, "shelf order is the curriculum")
        assertEquals(StudyKind.LEARN, task.kind)
        assertEquals(listOf(0, 1), task.lineIndices)
    }

    @Test
    fun accountedLinesLeaveTheLearnDeal() {
        val record = TrainingRecord.empty("tiny").recordLineCredit(first.lines[0].last().id, 1.0)
        val plan = studyPlan(trees, mapOf("tiny" to record), nowEpochMillis = 0L)
        assertEquals(listOf(1), plan.single().lineIndices, "a clean line is not re-dealt")
    }

    @Test
    fun halfCreditKeepsALineOnTheDeal() {
        val record = TrainingRecord.empty("tiny").recordLineCredit(first.lines[0].last().id, 0.5)
        val plan = studyPlan(trees, mapOf("tiny" to record), nowEpochMillis = 0L)
        assertEquals(listOf(0, 1), plan.single().lineIndices, "half known is not accounted for")
    }

    @Test
    fun finishedOpeningPopsToTheNextBook() {
        val records = mapOf("tiny" to learned(first, at = 0L))
        val plan = studyPlan(trees, records, nowEpochMillis = 1L)
        val task = plan.single()
        assertEquals("second", task.tree.repertoire.id, "the first book rests; the next opens")
        assertEquals(StudyKind.LEARN, task.kind)
    }

    @Test
    fun dueReviewsDealBeforeNewStudy() {
        val records = mapOf("tiny" to learned(first, at = 0L, streak = 1))
        val plan = studyPlan(trees, records, nowEpochMillis = day + 1)
        assertEquals(listOf("tiny", "second"), plan.map { it.tree.repertoire.id })
        assertEquals(StudyKind.REVIEW, plan.first().kind)
        assertNull(plan.first().lineIndices, "reviews walk the whole book")
    }

    @Test
    fun aSlippedReviewComesStraightBack() {
        // Streak zero: the opening lapsed at its last look, so it is due immediately —
        // "you got 83% on this one" means you see it again before anything new.
        val records = mapOf("tiny" to learned(first, at = 5L, streak = 0))
        val plan = studyPlan(trees, records, nowEpochMillis = 6L)
        assertEquals("tiny", plan.first().tree.repertoire.id)
        assertEquals(StudyKind.REVIEW, plan.first().kind)
    }

    @Test
    fun growingStreaksPushReviewsFurtherOut() {
        val records = mapOf(
            "tiny" to learned(first, at = 0L, streak = 3), // next look 7 days out
            "second" to learned(second, at = 0L, streak = 1), // next look 1 day out
        )
        val plan = studyPlan(trees, records, nowEpochMillis = 2 * day)
        assertEquals(listOf("second"), plan.map { it.tree.repertoire.id }, "the solid book rests")
    }

    @Test
    fun aRestedShelfStillDealsTheNearestDueBook() {
        val records = mapOf(
            "tiny" to learned(first, at = 0L, streak = 1),
            "second" to learned(second, at = 0L, streak = 3),
        )
        val plan = studyPlan(trees, records, nowEpochMillis = 10L)
        val task = plan.single()
        assertEquals("tiny", task.tree.repertoire.id, "never an empty hand: the nearest-due book fills in")
        assertEquals(StudyKind.REVIEW, task.kind)
    }

    @Test
    fun standingReportsCreditsAndPercent() {
        val record = TrainingRecord.empty("tiny")
            .recordLineCredit(first.lines[0].last().id, 1.0)
            .recordLineCredit(first.lines[1].last().id, 0.5)
        val standing = OpeningStanding(first, record)
        assertEquals(1.5, standing.creditTotal)
        assertEquals(75, standing.percent)
        assertTrue(!standing.learned)
        assertEquals(listOf(1), standing.unaccountedLineIndices)
    }

    @Test
    fun learnDealFallsBackToTheWholeBook() {
        // Every line clean but the finishing session never landed: the deal must not be
        // empty, or the session would finish instantly and deal again forever.
        val record = first.lines.fold(TrainingRecord.empty("tiny")) { acc, line ->
            acc.recordLineCredit(line.last().id, 1.0)
        }
        assertEquals(listOf(0, 1), OpeningStanding(first, record).learnDeal)
    }
}
