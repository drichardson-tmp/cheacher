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

/**
 * Three DFS lines with a deep junction and a shallow one — the smallest tree where
 * "the next branch opens at the deepest open junction" is actually visible:
 * line 0 (…Bc4) and line 1 (…Bb5) share four plies; line 2 (…c5) shares only 1.e4.
 */
private fun ladderRepertoire() = repertoire("ladder", "Ladder", Color.WHITE) {
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

class ProgressionTest {
    private val tree = OpeningTree.resolve(ladderRepertoire())
    private val leaf0 = tree.lines[0].last().id // …Bc4
    private val leaf1 = tree.lines[1].last().id // …Bb5
    private val leaf2 = tree.lines[2].last().id // …Nf3 vs the Sicilian

    private fun mastered(record: TrainingRecord, leafId: String): TrainingRecord =
        record.recordLineCompleted(leafId).recordBranchLineCompleted(leafId, atEpochMillis = 0L)

    @Test
    fun emptyRecordUnlocksExactlyTheFirstLine() {
        val progression = Progression(tree, TrainingRecord.empty("ladder"))
        assertEquals(listOf(LineStatus.UNLOCKED, LineStatus.LOCKED, LineStatus.LOCKED), progression.lineStatuses)
        assertEquals(0, progression.frontierIndex)
        assertEquals(0, progression.masteredCount)
        assertEquals(tree.lines[0].map { it.id }.toSet(), progression.unlockedNodeIds)
        assertEquals(listOf(0), progression.guidedLineIndices)
    }

    @Test
    fun guidedAloneDoesNotMaster() {
        val record = TrainingRecord.empty("ladder").recordLineCompleted(leaf0)
        assertEquals(0, Progression(tree, record).frontierIndex)
    }

    @Test
    fun branchAloneDoesNotMaster() {
        // recordBranchLineCompleted bumps both maps, so the guided count stays zero.
        val record = TrainingRecord.empty("ladder").recordBranchLineCompleted(leaf0, atEpochMillis = 0L)
        assertEquals(0, record.guidedCompletionsOf(leaf0))
        assertEquals(0, Progression(tree, record).frontierIndex)
    }

    @Test
    fun namedOnceAndRecalledOnceMastersAndUnlocksTheNextFork() {
        val record = mastered(TrainingRecord.empty("ladder"), leaf0)
        val progression = Progression(tree, record)
        assertEquals(listOf(LineStatus.MASTERED, LineStatus.UNLOCKED, LineStatus.LOCKED), progression.lineStatuses)
        assertEquals(1, progression.frontierIndex)
        assertEquals(1, progression.masteredCount)
        // The newly unlocked line is the deepest open junction's fork: 4...Bb5.
        assertEquals("Ruy Lopez", progression.nextUpName)
        assertTrue(tree.lines[1].last().id in progression.unlockedNodeIds)
        assertTrue(tree.lines[2][1].id !in progression.unlockedNodeIds, "the Sicilian stays locked")
    }

    @Test
    fun statsBeyondTheFrontierDoNotJumpTheQueue() {
        // Mastery-grade stats on line 2 (full-tree practice) while line 0 is untouched.
        val record = mastered(TrainingRecord.empty("ladder"), leaf2)
        val progression = Progression(tree, record)
        assertEquals(0, progression.frontierIndex)
        assertEquals(LineStatus.LOCKED, progression.statusOf(2), "the ladder is the ladder")

        // But the banked credit counts the moment the chain reaches it: mastering
        // lines 0 and 1 rolls the frontier straight past line 2 to the end.
        val caughtUp = mastered(mastered(record, leaf0), leaf1)
        assertNull(Progression(tree, caughtUp).frontierIndex)
    }

    @Test
    fun fullyMasteredBookOpensEverythingAsReview() {
        val record = mastered(mastered(mastered(TrainingRecord.empty("ladder"), leaf0), leaf1), leaf2)
        val progression = Progression(tree, record)
        assertNull(progression.frontierIndex)
        assertEquals(List(3) { LineStatus.MASTERED }, progression.lineStatuses)
        assertEquals(listOf(0, 1, 2), progression.guidedLineIndices)
        assertEquals(tree.allNodes.map { it.id }.toSet(), progression.unlockedNodeIds)
        assertNull(progression.nextUpName)
    }

    @Test
    fun singleLineTreeIsUnlockedThenMastered() {
        val single = OpeningTree.resolve(
            repertoire("one", "One", Color.WHITE) {
                move("e4", "King's Pawn Opening", "Centre.")
            },
        )
        assertEquals(listOf(LineStatus.UNLOCKED), Progression(single, TrainingRecord.empty("one")).lineStatuses)
        val done = Progression(single, mastered(TrainingRecord.empty("one"), single.lines[0].last().id))
        assertEquals(listOf(LineStatus.MASTERED), done.lineStatuses)
        assertNull(done.frontierIndex)
    }

    @Test
    fun divergenceNodeIsTheFirstUnsharedMove() {
        val progression = Progression(tree, TrainingRecord.empty("ladder"))
        assertEquals("King's Pawn Opening", progression.divergenceNode(0).name)
        assertEquals("Ruy Lopez", progression.divergenceNode(1).name)
        assertEquals("Sicilian Defence", progression.divergenceNode(2).name)
    }

    @Test
    fun advanceFiresExactlyWhenTheFrontierMoves() {
        val empty = Progression(tree, TrainingRecord.empty("ladder"))
        val stillEmpty = Progression(tree, TrainingRecord.empty("ladder").recordMiss("0"))
        assertNull(stillEmpty.advanceFrom(empty), "misses never move the frontier")

        val one = Progression(tree, mastered(TrainingRecord.empty("ladder"), leaf0))
        val advance = assertNotNull(one.advanceFrom(empty))
        assertEquals("Ruy Lopez", advance.unlockedLine?.name)
        assertEquals(false, advance.repertoireMastered)
        assertNull(one.advanceFrom(one), "no repeat announcements")
    }

    @Test
    fun masteringTheLastLineAnnouncesTheWholeBook() {
        val twoDown = Progression(tree, mastered(mastered(TrainingRecord.empty("ladder"), leaf0), leaf1))
        val allDone = Progression(tree, mastered(twoDown.record, leaf2))
        val advance = assertNotNull(allDone.advanceFrom(twoDown))
        assertNull(advance.unlockedLine)
        assertTrue(advance.repertoireMastered)
        assertNull(allDone.advanceFrom(allDone), "mastery announces once, then stays quiet")
    }
}
