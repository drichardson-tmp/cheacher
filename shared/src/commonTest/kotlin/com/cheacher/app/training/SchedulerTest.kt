package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.repertoire
import com.cheacher.app.progress.TrainingRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

private const val DAY = TrainingRecord.DAY_MILLIS

/** Same shape as the progression fixture: two deep siblings, one shallow fork. */
private fun scheduleRepertoire() = repertoire("ladder", "Ladder", Color.WHITE) {
    move("e4", "King's Pawn Opening", "Centre.") {
        move("e5", "Open Game", "Symmetry.") {
            move("Nf3", "King's Knight Opening", "Hit e5.") {
                move("Nc6", "Normal Variation", "Guard e5.") {
                    move("Bc4", "Italian Game", "Eye f7.")
                    move("Bb5", "Ruy Lopez", "Pin pressure.")
                }
            }
        }
        move("c5", "Sicilian Defence", "Asymmetry.") {
            move("Nf3", "Open Sicilian, Preparation", "Prepare d4.")
        }
    }
}

class SchedulerTest {
    private val tree = OpeningTree.resolve(scheduleRepertoire())
    private val leaf0 = tree.lines[0].last().id
    private val leaf1 = tree.lines[1].last().id
    private val leaf2 = tree.lines[2].last().id

    private fun mastered(record: TrainingRecord, leafId: String, at: Long): TrainingRecord =
        record.recordLineCompleted(leafId).recordBranchLineCompleted(leafId, at)

    private fun syllabus(record: TrainingRecord, now: Long): Syllabus =
        Progression(tree, record).syllabusAt(now)

    @Test
    fun ladderGrowsOneThreeSevenFourteenThirtyAndCaps() {
        assertEquals(0L, ReviewLadder.intervalMillis(0), "no streak means no rest earned")
        assertEquals(1 * DAY, ReviewLadder.intervalMillis(1))
        assertEquals(3 * DAY, ReviewLadder.intervalMillis(2))
        assertEquals(7 * DAY, ReviewLadder.intervalMillis(3))
        assertEquals(14 * DAY, ReviewLadder.intervalMillis(4))
        assertEquals(30 * DAY, ReviewLadder.intervalMillis(5))
        assertEquals(30 * DAY, ReviewLadder.intervalMillis(12), "thirty days is the ceiling")
    }

    @Test
    fun emptyRecordOffersJustTheFirstNewLine() {
        val syllabus = syllabus(TrainingRecord.empty("ladder"), now = 0L)
        assertEquals(listOf(SyllabusReason.NEW), syllabus.sessionLines.map { it.reason })
        assertEquals(1, syllabus.newCount)
        assertEquals(0, syllabus.reviewCount)
        assertEquals(listOf(0), syllabus.guidedLineIndices)
        assertEquals(tree.lines[0].map { it.id }.toSet(), syllabus.branchAllowedNodeIds)
    }

    @Test
    fun legacyRecordDecodesAndItsMasteredLinesAreTopReviewPriority() {
        // Written before line_reviews existed: mastered on counts, no review history.
        val json = Json { ignoreUnknownKeys = true }
        val blob = """
            {"repertoire_id":"ladder",
             "line_completions":{"$leaf0":2},
             "branch_line_completions":{"$leaf0":1}}
        """.trimIndent()
        val record = json.decodeFromString<TrainingRecord>(blob)

        val syllabus = syllabus(record, now = 99 * DAY)
        assertEquals(
            listOf(SyllabusReason.DUE, SyllabusReason.NEW),
            syllabus.sessionLines.map { it.reason },
            "no history reads as due immediately — the honest interpretation",
        )
        assertEquals(listOf(0, 1), syllabus.guidedLineIndices)
    }

    @Test
    fun cleanReviewsExpandTheInterval() {
        var record = mastered(TrainingRecord.empty("ladder"), leaf0, at = 0L)

        // Streak 1: resting within a day, due after it.
        assertEquals(SyllabusReason.FRESH, syllabus(record, now = DAY - 1).lines[0].reason)
        assertEquals(SyllabusReason.DUE, syllabus(record, now = DAY).lines[0].reason)

        // A second clean recall at the due moment: streak 2, resting for three days.
        record = record.recordBranchLineCompleted(leaf0, atEpochMillis = DAY)
        assertEquals(SyllabusReason.FRESH, syllabus(record, now = DAY + 3 * DAY - 1).lines[0].reason)
        assertEquals(SyllabusReason.DUE, syllabus(record, now = DAY + 3 * DAY).lines[0].reason)
    }

    @Test
    fun aMissBringsBackTheMoveWithoutRewritingTheLineClock() {
        val reviewedAt = 10 * DAY
        var record = mastered(TrainingRecord.empty("ladder"), leaf0, at = 0L)
            .recordBranchLineCompleted(leaf0, atEpochMillis = 5 * DAY)
            .recordBranchLineCompleted(leaf0, atEpochMillis = reviewedAt) // streak 3: a week of rest
        assertEquals(SyllabusReason.FRESH, syllabus(record, now = reviewedAt + DAY).lines[0].reason)

        // Miss 2...Nc6 — its move clock lapses, while the full Italian stays rested.
        val sharedNode = tree.lines[0][3].id
        record = record
            .recordNodeRecalled(sharedNode, atEpochMillis = reviewedAt)
            .recordMiss(sharedNode)
        assertEquals(3, record.reviewStreakOf(leaf0), "the full-line proof remains true")
        assertEquals(SyllabusReason.FRESH, syllabus(record, now = reviewedAt + DAY).lines[0].reason)
        assertEquals(0, record.nodeReviewStreakOf(sharedNode))
        assertEquals(
            sharedNode,
            assertNotNull(Progression(tree, record).nodeReviewTargetAt(reviewedAt + DAY)).nodeId,
        )
    }

    @Test
    fun aSharedTroubleMoveProducesOneFocusedTargetNotTwoLapsedLines() {
        val record = mastered(mastered(TrainingRecord.empty("ladder"), leaf0, 0L), leaf1, 0L)
            .recordMiss(tree.lines[0][1].id) // 1...e5 belongs to both mastered lines.
        val target = assertNotNull(Progression(tree, record).nodeReviewTargetAt(DAY / 2))

        assertEquals(tree.lines[0][1].id, target.nodeId)
        assertEquals(0, target.lineIndex, "one mastered carrier line is enough")
        assertEquals(1, record.reviewStreakOf(leaf0))
        assertEquals(1, record.reviewStreakOf(leaf1))
    }

    @Test
    fun dueReviewsInterleaveWithTheFrontier() {
        // Lines 0 and 1 mastered long ago; line 2 is the frontier.
        val record = mastered(mastered(TrainingRecord.empty("ladder"), leaf0, 0L), leaf1, 2L)
        val syllabus = syllabus(record, now = 50 * DAY)

        assertEquals(
            listOf(SyllabusReason.DUE, SyllabusReason.DUE, SyllabusReason.NEW),
            syllabus.sessionLines.map { it.reason },
        )
        assertEquals(listOf(0, 1), syllabus.sessionLines.take(2).map { it.lineIndex }, "most overdue first")
        assertEquals(listOf(0, 1, 2), syllabus.guidedLineIndices, "tree order: prefixes interleave naturally")
        assertEquals(setOf(0, 1), syllabus.reviewLineIndices)
        assertEquals(tree.allNodes.map { it.id }.toSet(), syllabus.branchAllowedNodeIds)
        assertEquals(1, syllabus.newCount)
        assertEquals(2, syllabus.reviewCount)
    }

    @Test
    fun frontierIsAlwaysOnTheSyllabusUntilTheTreeIsExhausted() {
        var record = TrainingRecord.empty("ladder")
        for (leaf in listOf(leaf0, leaf1)) {
            assertTrue(
                syllabus(record, now = 40 * DAY).sessionLines.any { it.reason == SyllabusReason.NEW },
                "an unmastered book always deals the new line",
            )
            record = mastered(record, leaf, at = 40 * DAY)
        }
        record = mastered(record, leaf2, at = 40 * DAY)
        assertTrue(
            syllabus(record, now = 40 * DAY).sessionLines.none { it.reason == SyllabusReason.NEW },
            "a finished book has no new line to deal",
        )
    }

    @Test
    fun nothingDueStillOffersAReviewMixNeverAnEmptySession() {
        // Whole book mastered moments ago; nothing has come due.
        val record = mastered(
            mastered(mastered(TrainingRecord.empty("ladder"), leaf0, 100L), leaf1, 200L),
            leaf2,
            300L,
        )
        val syllabus = syllabus(record, now = 400L)
        assertEquals(1, syllabus.sessionLines.size, "the weakest resting line keeps retrieval warm")
        assertEquals(SyllabusReason.FRESH, syllabus.sessionLines.single().reason)
        assertEquals(0, syllabus.sessionLines.single().lineIndex, "earliest due wins the seat")
        assertEquals(listOf(0), syllabus.guidedLineIndices)
        assertEquals(1, syllabus.reviewCount)
    }

    @Test
    fun syllabusIsASnapshotThatDoesNotShiftMidRound() {
        val record = mastered(TrainingRecord.empty("ladder"), leaf0, at = 0L)
        val drawn = syllabus(record, now = 2 * DAY)
        val gate = drawn.guidedLineIndices
        val session = GuidedState.start(tree, lineIndices = gate)

        // The record moves on mid-round — more mastery, a new review, whatever.
        val evolved = mastered(record, leaf1, at = 2 * DAY)
        val redrawn = syllabus(evolved, now = 2 * DAY)
        assertTrue(redrawn.guidedLineIndices != gate, "the world changed")

        // But the drawn syllabus and the session built from it are frozen values.
        assertEquals(listOf(0, 1), gate)
        assertEquals(gate, session.lineIndices)
        assertEquals(gate, session.submit(tree.lines[0][0].move).lineIndices)
    }

    @Test
    fun aMissedMoveGetsAFocusedReviewStartingAtItsParent() {
        val shaky = tree.lines[0][3] // 2...Nc6, not the whole Italian line.
        val record = mastered(TrainingRecord.empty("ladder"), leaf0, at = 0L)
            .recordMiss(shaky.id)
            .recordMiss(shaky.id)

        val target = assertNotNull(Progression(tree, record).nodeReviewTargetAt(nowEpochMillis = DAY))
        assertEquals(shaky.id, target.nodeId)
        assertEquals(0, target.lineIndex)
        assertEquals(shaky.parentId, target.entryNodeId, "the shaky move must remain the next ask")
        assertEquals(2, target.missCount)
    }

    @Test
    fun aCleanFocusedRecallRestsThatMoveOnItsOwnInterval() {
        val shaky = tree.lines[0][3]
        val record = mastered(TrainingRecord.empty("ladder"), leaf0, at = 0L)
            .recordMiss(shaky.id)
            .recordNodeRecalled(shaky.id, atEpochMillis = 10 * DAY)

        assertNull(
            Progression(tree, record).nodeReviewTargetAt(nowEpochMillis = 11 * DAY - 1),
            "streak one earns the move a full day of rest",
        )
        assertEquals(
            shaky.id,
            assertNotNull(Progression(tree, record).nodeReviewTargetAt(nowEpochMillis = 11 * DAY)).nodeId,
        )
    }

    @Test
    fun troubleInsideAnUnmasteredLineStaysOnTheOrdinaryLearningDeal() {
        val shaky = tree.lines[0][3]
        val record = TrainingRecord.empty("ladder").recordMiss(shaky.id)

        assertNull(Progression(tree, record).nodeReviewTargetAt(nowEpochMillis = 100 * DAY))
    }

    @Test
    fun oneSidedRecallOnlyTargetsMovesTheLearnerWillActuallyPlay() {
        val blacksMove = tree.lines[0][3] // 2...Nc6
        val record = mastered(TrainingRecord.empty("ladder"), leaf0, at = 0L)
            .recordMiss(blacksMove.id)
        val progression = Progression(tree, record)

        assertNull(progression.nodeReviewTargetAt(DAY, learnerColor = Color.WHITE))
        assertEquals(
            blacksMove.id,
            assertNotNull(progression.nodeReviewTargetAt(DAY, learnerColor = Color.BLACK)).nodeId,
        )
    }
}
