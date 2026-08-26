package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.repertoire
import com.cheacher.app.domain.tinyRepertoire
import com.cheacher.app.progress.TrainingRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A book with a real road in: every line runs through 1.e4 e5 2.Nf3 Nc6 3.Bc4. */
private fun italianish() = repertoire("italianish", "Italianish", Color.WHITE) {
    move("e4", "King's Pawn Opening") {
        move("e5", "Open Game") {
            move("Nf3", "King's Knight Opening") {
                move("Nc6", "Normal Variation") {
                    move("Bc4", "Italian Game") {
                        move("Bc5", "Giuoco Piano") {
                            move("c3", "Giuoco Pianissimo")
                        }
                        move("Nf6", "Two Knights Defence") {
                            move("Ng5", "Knight Attack")
                        }
                    }
                }
            }
        }
    }
}

class OpeningEntryTest {
    private val tiny = OpeningTree.resolve(tinyRepertoire())
    private val italian = OpeningTree.resolve(italianish())

    private fun move(uci: String): Move = assertNotNull(Move.fromUci(uci))

    private fun recordWithCleanLine(tree: OpeningTree): TrainingRecord =
        TrainingRecord.empty(tree.repertoire.id)
            .recordLineCredit(tree.lines.first().last().id, 1.0)

    @Test
    fun trunkIsTheSharedRoadIn() {
        assertEquals(listOf("0"), tiny.trunk().map { it.id }, "both tiny lines share only 1.e4")
        assertEquals(
            listOf("e4", "e5", "Nf3", "Nc6", "Bc4"),
            italian.trunk().map { it.san },
            "the fork is Bc5 / Nf6, so the trunk runs to the Italian bishop",
        )
    }

    @Test
    fun aSingleLineBookAlwaysKeepsAMoveToPlay() {
        val oneLine = OpeningTree.resolve(
            repertoire("one", "One", Color.WHITE) {
                move("e4", "King's Pawn Opening") { move("e5", "Open Game") }
            },
        )
        assertEquals(listOf("e4"), oneLine.trunk().map { it.san })
    }

    @Test
    fun theRoadInIsWalkedUntilOneLineReadsClean() {
        val fresh = OpeningEntry(italian, TrainingRecord.empty("italianish"))
        assertFalse(fresh.proven)
        assertEquals(0, fresh.entryPly)
        assertNull(fresh.entryNode)

        // Half credit is a hinted walk: it proves the names were shown, not that they were found.
        val hinted = OpeningEntry(
            italian,
            TrainingRecord.empty("italianish").recordLineCredit(italian.lines[0].last().id, 0.5),
        )
        assertFalse(hinted.proven, "an aided walk does not pay the toll")
    }

    @Test
    fun oneCleanWalkOpensTheDoorForEveryOtherLine() {
        val entry = OpeningEntry(italian, recordWithCleanLine(italian))
        assertTrue(entry.proven)
        assertEquals(5, entry.entryPly)
        assertEquals("Italian Game", entry.entryName)
        assertEquals("0.0.0.0.0", assertNotNull(entry.entryNode).id)
    }

    @Test
    fun aFumbledRewalkHandsBackTheWholeRoad() {
        val lapsed = recordWithCleanLine(italian)
            .recordLineCredit(italian.lines.first().last().id, 0.0)
        assertFalse(OpeningEntry(italian, lapsed).proven, "no line reads clean any more")
    }

    @Test
    fun guidedOpensAtTheEntryAndScoresFromThere() {
        val state = GuidedState.start(italian, entryPly = 5)
        assertEquals("Giuoco Piano", state.prompt?.name, "the first ask is the fork, not 1.e4")
        assertEquals(italian.node("0.0.0.0.0")?.position, state.position)
        assertEquals(5, state.played.size, "the road in is context on the move strip")
        assertEquals(0, state.progress.plyNumber, "no work claimed for moves that were skipped")
        assertEquals(2, state.progress.plyCount)
    }

    @Test
    fun guidedReturnsToTheEntryBetweenLinesAndOnRestart() {
        val walked = GuidedState.start(italian, entryPly = 5)
            .submit(move("f8c5"))
            .submit(move("c2c3"))
        assertIs<GuidedEvent.LineComplete>(walked.lastEvent)
        assertEquals(5, walked.plyIndex, "the next line resumes inside the Italian")
        assertEquals("Two Knights Defence", walked.prompt?.name)

        val restarted = walked.submit(move("g8f6")).restartLine()
        assertEquals(5, restarted.plyIndex)
        assertEquals("Two Knights Defence", restarted.prompt?.name)
    }

    @Test
    fun guidedClampsAnEntryDeeperThanTheDeal() {
        val state = GuidedState.start(tiny, entryPly = 99)
        assertEquals(2, state.entryPly, "a line must always have a move left to play")
        assertEquals("King's Knight Opening", state.prompt?.name)
    }

    @Test
    fun branchOpensAtTheEntryWithTheRoadInAlreadyTravelled() {
        val state = BranchState.start(italian, entryNodeId = "0.0.0.0.0")
        assertEquals("0.0.0.0.0", state.cursorId)
        assertEquals(5, state.path.size)
        assertEquals(
            NodeStatus.IN_PROGRESS,
            state.statusOf(assertNotNull(italian.node("0"))),
            "1.e4 reads as history, not as something still to find",
        )
        assertEquals(italian.lines[0].last().id, state.targetLeaf?.id, "line one is still the ask")
        assertEquals(2, state.openMoves.size)
    }

    @Test
    fun branchNeverReelsBackPastTheEntry() {
        val closed = BranchState.start(italian, entryNodeId = "0.0.0.0.0")
            .submit(move("f8c5"))
            .submit(move("c2c3"))
        val event = assertIs<BranchEvent.BranchClosed>(closed.lastEvent)
        assertEquals("0.0.0.0.0", event.snappedTo?.id, "the snap stops at the Italian")
        assertEquals("0.0.0.0.0", closed.cursorId)
        assertEquals("0.0.0.0.0", closed.backToJunction().cursorId)

        val done = closed.submit(move("g8f6")).submit(move("f3g5"))
        assertIs<BranchEvent.SessionComplete>(done.lastEvent)
        assertTrue(done.finished)
        assertEquals(2, done.progress.closedLines)
    }

    @Test
    fun branchIgnoresAnEntryTheGateHasNotReached() {
        // A progression gate that has not unlocked the trunk cannot be skipped past.
        val gated = BranchState.start(
            italian,
            allowedNodeIds = setOf("0.1"),
            entryNodeId = "0.0.0.0.0",
        )
        assertNull(gated.cursorId, "an unearned road in is no road in")
        assertNull(gated.entryNodeId)
    }

    @Test
    fun aBookThatForksAtMoveOneHasNoRoadIn() {
        val forked = OpeningTree.resolve(
            repertoire("forked", "Forked", Color.WHITE) {
                move("e4", "King's Pawn Opening") { move("e5", "Open Game") }
                move("d4", "Queen's Pawn Opening") { move("d5", "Closed Game") }
            },
        )
        assertTrue(forked.trunk().isEmpty())
        val entry = OpeningEntry(
            forked,
            TrainingRecord.empty("forked").recordLineCredit(forked.lines[0].last().id, 1.0),
        )
        assertFalse(entry.proven, "there is no shared road to have earned")
        assertEquals(0, entry.entryPly)
        assertNull(entry.entryName)
    }

    @Test
    fun anEmptyBookHasNoTrunkAndNoEntry() {
        val empty = OpeningTree.resolve(repertoire("empty", "Empty", Color.WHITE) {})
        assertTrue(empty.trunk().isEmpty())
        assertFalse(OpeningEntry(empty, TrainingRecord.empty("empty")).proven)
    }

    @Test
    fun anyCleanLinePaysTheToll() {
        val lastLine = TrainingRecord.empty("italianish")
            .recordLineCredit(italian.lines.last().last().id, 1.0)
        assertTrue(
            OpeningEntry(italian, lastLine).proven,
            "the road in is the same road whichever line proved it",
        )
    }

    @Test
    fun anUnnamedEntryHasNoNameToAnnounce() {
        val unnamed = OpeningTree.resolve(
            repertoire("unnamed", "Unnamed", Color.WHITE) {
                move("e4", "") {
                    move("e5", "Open Game")
                    move("c5", "Sicilian Defence")
                }
            },
        )
        val entry = OpeningEntry(
            unnamed,
            TrainingRecord.empty("unnamed").recordLineCredit(unnamed.lines[0].last().id, 1.0),
        )
        assertEquals("0", assertNotNull(entry.entryNode).id)
        assertNull(entry.entryName, "a blank name is not a name")
    }

    @Test
    fun theEarnedEntryIsWhatAGuidedSessionOpensOn() {
        // The round trip the app makes: one clean walk, then the next session's start.
        val entry = OpeningEntry(italian, recordWithCleanLine(italian))
        val next = GuidedState.start(italian, entryPly = entry.entryPly)
        assertEquals("Italian Game", next.played.last().name)
        assertEquals("Giuoco Piano", next.prompt?.name)
    }

    @Test
    fun guidedClampsAgainstTheDealNotTheWholeBook() {
        val state = GuidedState.start(tiny, lineIndices = listOf(1), entryPly = 5)
        assertEquals(2, state.entryPly)
        assertEquals("Open Sicilian, Preparation", state.prompt?.name)
        assertEquals(1, state.progress.plyCount, "one move dealt, one move to find")
    }

    @Test
    fun theMasteryLoopRedealsFromTheEntryToo() {
        val fumbled = GuidedState.start(italian, masteryLoop = true, entryPly = 5)
            .submit(move("g8f6")) // a real move, but not the line being named
            .submit(move("f8c5"))
            .submit(move("c2c3"))
        val first = assertIs<GuidedEvent.LineComplete>(fumbled.lastEvent)
        assertEquals(0.0, first.credit)

        val redealt = fumbled.submit(move("g8f6")).submit(move("f3g5"))
        assertFalse(redealt.finished, "an imperfect line owes another pass")
        assertEquals(listOf(0), redealt.passLines)
        assertEquals(5, redealt.plyIndex, "the re-deal opens inside the Italian as well")
        assertEquals("Giuoco Piano", redealt.prompt?.name)
    }

    @Test
    fun theHintStillCostsHalfAPointFromTheEntry() {
        val aided = GuidedState.start(italian, entryPly = 5)
            .revealIdea()
            .submit(move("f8c5"))
            .submit(move("c2c3"))
        val event = assertIs<GuidedEvent.LineComplete>(aided.lastEvent)
        assertEquals(0.5, event.credit, "a shorter walk is not a cheaper one")
    }

    @Test
    fun branchPlaysTheOtherSideFromTheEntry() {
        val state = BranchState.start(
            italian,
            autoReplyFor = Color.BLACK,
            entryNodeId = "0.0.0.0.0",
        )
        assertEquals("0.0.0.0.0.0", state.cursorId, "the app answers 3.Bc4 with 3...Bc5")
        assertEquals(Color.WHITE, italian.sideToMoveAt(state.cursor))

        val closed = state.submit(move("c2c3"))
        assertIs<BranchEvent.BranchClosed>(closed.lastEvent)
        assertEquals(
            "0.0.0.0.0.1",
            closed.cursorId,
            "the snap lands on the entry and the app deals the next line's reply",
        )
    }

    @Test
    fun aMissFromTheEntryIsChargedToTheEntryNotToTheRoot() {
        val missed = BranchState.start(italian, entryNodeId = "0.0.0.0.0")
            .submit(move("g8h6")) // legal, and nowhere in the book
        val event = assertIs<BranchEvent.BranchFailed>(missed.lastEvent)
        assertEquals("0.0.0.0.0", event.at?.id, "the trouble map blames the Italian, not 'root'")
        assertEquals("0.0.0.0.0", event.snappedTo?.id)
        assertEquals(NodeStatus.FAILED, missed.statusOf(italian.lines[0].last()))
        assertEquals(italian.lines[1].last().id, missed.targetLeaf?.id, "the ask moves to line two")
    }

    @Test
    fun theForgivingPolicyStillForgivesInPlaceAtTheEntry() {
        val missed = BranchState.start(
            italian,
            policy = MistakePolicy.ONE_ALLOWANCE,
            entryNodeId = "0.0.0.0.0",
        ).submit(move("g8h6"))
        val event = assertIs<BranchEvent.Missed>(missed.lastEvent)
        assertEquals(1, event.strikes)
        assertEquals("0.0.0.0.0", missed.cursorId, "a forgiven miss does not move the board")
        assertEquals(2, missed.progress.totalLines)
    }

    @Test
    fun aLockedForkUnderTheEntryStillOnlyRattles() {
        val state = BranchState.start(
            italian,
            allowedNodeIds = italian.lines[0].map { it.id }.toSet(),
            entryNodeId = "0.0.0.0.0",
        )
        assertEquals("0.0.0.0.0", state.cursorId, "the trunk is unlocked, so the entry holds")
        assertEquals(1, state.progress.totalLines, "a locked line is off the scoreboard")

        val rattled = state.submit(move("g8f6"))
        assertIs<BranchEvent.Locked>(rattled.lastEvent)
        assertEquals("0.0.0.0.0", rattled.cursorId)
        assertEquals(0, rattled.strikes, "a locked door costs nothing")
    }

    @Test
    fun anEntryPastTheForkIsNotAnEntry() {
        val state = BranchState.start(italian, entryNodeId = "0.0.0.0.0.0")
        assertNull(state.entryNodeId, "only the shared road may be skipped")
        assertNull(state.cursorId)
    }

    @Test
    fun withoutAnEntryTheRoundBehavesExactlyAsBefore() {
        val state = BranchState.start(italian)
        assertNull(state.cursorId)
        assertTrue(state.statuses.isEmpty())
        val closed = state
            .submit(move("e2e4"))
            .submit(move("e7e5"))
            .submit(move("g1f3"))
            .submit(move("b8c6"))
            .submit(move("f1c4"))
            .submit(move("f8c5"))
            .submit(move("c2c3"))
        val event = assertIs<BranchEvent.BranchClosed>(closed.lastEvent)
        assertEquals("0.0.0.0.0", event.snappedTo?.id, "the fork is still the nearest open junction")
    }
}
