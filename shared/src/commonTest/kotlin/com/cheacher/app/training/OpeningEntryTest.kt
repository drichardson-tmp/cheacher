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

/** Two levels of forks: the Giuoco subgroup sits inside the Italian fork. */
private fun nestedItalianish() = repertoire("nested-italianish", "Nested Italianish", Color.WHITE) {
    move("e4", "King's Pawn Opening") {
        move("e5", "Open Game") {
            move("Nf3", "King's Knight Opening") {
                move("Nc6", "Normal Variation") {
                    move("Bc4", "Italian Game") {
                        move("Bc5", "Giuoco Piano") {
                            move("c3", "Giuoco Pianissimo")
                            move("d3", "Quiet Italian")
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

    private fun withRoadInProven(tree: OpeningTree): TrainingRecord =
        TrainingRecord.empty(tree.repertoire.id).recordTrunkCleared()

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
    fun theRoadInIsWalkedUntilItIsWalkedPerfectly() {
        val fresh = OpeningEntry(italian, TrainingRecord.empty("italianish"))
        assertFalse(fresh.proven)
        assertEquals(0, fresh.entryPly)
        assertNull(fresh.entryNode)

        // Finishing lines is the study, not the toll: credits alone open nothing.
        val studied = TrainingRecord.empty("italianish")
            .recordLineCredit(italian.lines[0].last().id, 1.0)
            .recordLineCompleted(italian.lines[0].last().id)
        assertFalse(OpeningEntry(italian, studied).proven, "the road in is what is asked for")
    }

    @Test
    fun gettingThereCleanlyIsTheWholeToll() {
        val entry = OpeningEntry(italian, withRoadInProven(italian))
        assertTrue(entry.proven)
        assertEquals(5, entry.entryPly)
        assertEquals("Italian Game", entry.entryName)
        assertEquals("0.0.0.0.0", assertNotNull(entry.entryNode).id)
    }

    @Test
    fun aFumbleOnTheRoadInHandsTheWholeRoadBack() {
        val lapsed = withRoadInProven(italian).recordTrunkCleared().recordTrunkFumbled()
        assertEquals(0, lapsed.trunkClears, "a stumble revokes outright, it does not decrement")
        assertFalse(OpeningEntry(italian, lapsed).proven)
        assertTrue(
            OpeningEntry(italian, lapsed.recordTrunkCleared()).proven,
            "re-earning costs one clean walk, same as the first time",
        )
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
    fun guidedEarnsTheForkImmediatelyAndUsesItForTheNextLine() {
        val walked = GuidedState.start(italian)
            .submit(move("e2e4"))
            .submit(move("e7e5"))
            .submit(move("g1f3"))
            .submit(move("b8c6"))
            .submit(move("f1c4")) // arrived at the Italian fork cleanly
            .submit(move("f8c5"))
            .submit(move("c2c3"))

        assertIs<GuidedEvent.LineComplete>(walked.lastEvent)
        assertEquals(5, walked.plyIndex, "the checkpoint applies in the session that earned it")
        assertEquals("Two Knights Defence", walked.prompt?.name)
        assertEquals(0, walked.progress.plyNumber)
    }

    @Test
    fun aHintBeforeTheForkDoesNotEarnTheCheckpoint() {
        var walked = GuidedState.start(italian).revealIdea()
        for (uci in listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "c2c3")) {
            walked = walked.submit(move(uci))
        }

        assertIs<GuidedEvent.LineComplete>(walked.lastEvent)
        assertEquals(0, walked.plyIndex)
        assertEquals("King's Pawn Opening", walked.prompt?.name)
    }

    @Test
    fun aHintAfterTheForkDoesNotTakeTheEarnedCheckpointAway() {
        var walked = GuidedState.start(italian)
        for (uci in listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4")) {
            walked = walked.submit(move(uci))
        }
        walked = walked.revealIdea().submit(move("f8c5")).submit(move("c2c3"))

        assertEquals(5, walked.plyIndex, "only the unaided arrival matters")
        assertEquals("Two Knights Defence", walked.prompt?.name)
    }

    @Test
    fun nestedSubgroupsResumeDeepThenFallBackToTheirParentFork() {
        val tree = OpeningTree.resolve(nestedItalianish())
        var state = GuidedState.start(tree)
        for (uci in listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "c2c3")) {
            state = state.submit(move(uci))
        }
        assertEquals(6, state.plyIndex, "the next Giuoco line resumes after ...Bc5")
        assertEquals("Quiet Italian", state.prompt?.name)

        state = state.submit(move("d2d3"))
        assertEquals(5, state.plyIndex, "leaving the subgroup falls back to the Italian fork")
        assertEquals("Two Knights Defence", state.prompt?.name)
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
        val entry = OpeningEntry(forked, TrainingRecord.empty("forked").recordTrunkCleared())
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
    fun aFumbleOnANeverEarnedRoadChangesNothing() {
        val fresh = TrainingRecord.empty("italianish")
        assertEquals(fresh, fresh.recordTrunkFumbled(), "you cannot lose what you never held")
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
        val entry = OpeningEntry(unnamed, TrainingRecord.empty("unnamed").recordTrunkCleared())
        assertEquals("0", assertNotNull(entry.entryNode).id)
        assertNull(entry.entryName, "a blank name is not a name")
    }

    @Test
    fun theEarnedEntryIsWhatAGuidedSessionOpensOn() {
        // The round trip the app makes: one clean walk in, then the next session's start.
        val entry = OpeningEntry(italian, withRoadInProven(italian))
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

    @Test
    fun guidedPaysTheTollOnArrivalNotOnFinishingTheLine() {
        var state = GuidedState.start(italian)
        assertEquals(5, state.trunkPly)
        assertFalse(state.roadInWalkedClean, "nothing walked yet")

        listOf("e2e4", "e7e5", "g1f3", "b8c6").forEach { state = state.submit(move(it)) }
        assertFalse(state.roadInWalkedClean, "one move short of the Italian is not there yet")

        state = state.submit(move("f1c4"))
        assertTrue(state.roadInWalkedClean, "arrival is the toll — the fork is still unplayed")
        assertEquals("Giuoco Piano", state.prompt?.name)
    }

    @Test
    fun aMissOnTheRoadInWithholdsTheToll() {
        var state = GuidedState.start(italian).submit(move("d2d4")) // not the book's first move
        listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4").forEach { state = state.submit(move(it)) }
        assertFalse(state.roadInWalkedClean, "a stumble on the way in is not a clean arrival")

        // The next line's walk is its own fresh run at the road.
        var second = state.submit(move("f8c5")).submit(move("c2c3"))
        listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4").forEach { second = second.submit(move(it)) }
        assertTrue(second.roadInWalkedClean)
    }

    @Test
    fun aHintOnTheRoadInWithholdsTheTollToo() {
        var state = GuidedState.start(italian).revealIdea()
        listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4").forEach { state = state.submit(move(it)) }
        assertFalse(state.roadInWalkedClean, "shown is not found")
    }

    @Test
    fun aSlipAfterArrivalDoesNotUnpayTheToll() {
        var state = GuidedState.start(italian)
        listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4").forEach { state = state.submit(move(it)) }
        assertTrue(state.roadInWalkedClean, "arrived clean — the toll is paid and journalled")
        val slipped = state.submit(move("g8f6")) // wrong for this line, but past the road in
        assertFalse(
            slipped.roadInWalkedClean,
            "the flag tracks the walk in progress; the paid toll already left as a record write",
        )
    }

    @Test
    fun aBookWithNoRoadInNeverPaysAToll() {
        val forked = OpeningTree.resolve(
            repertoire("forked", "Forked", Color.WHITE) {
                move("e4", "King's Pawn Opening") { move("e5", "Open Game") }
                move("d4", "Queen's Pawn Opening") { move("d5", "Closed Game") }
            },
        )
        val state = GuidedState.start(forked).submit(move("e2e4"))
        assertEquals(0, state.trunkPly)
        assertFalse(state.roadInWalkedClean, "there is no shared road to have walked")
    }

    @Test
    fun blindRecallPaysTheTollOnArrivalAsWell() {
        var state = BranchState.start(italian)
        assertEquals("0.0.0.0.0", state.trunkLastId)
        listOf("e2e4", "e7e5", "g1f3", "b8c6").forEach { state = state.submit(move(it)) }
        assertFalse(state.roadInCleared)

        state = state.submit(move("f1c4"))
        assertTrue(state.roadInCleared, "reaching the Italian blind is the toll, lines or no lines")
        assertEquals(0, state.progress.closedLines, "and nothing has been pruned yet")
    }

    @Test
    fun aWrongMoveOnTheWayInCostsTheRecallToll() {
        val fumbled = BranchState.start(italian).submit(move("d2d4"))
        assertTrue(fumbled.roadInFumbled)
        var state = fumbled
        listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4").forEach { state = state.submit(move(it)) }
        assertFalse(state.roadInCleared, "the first line's walk in was not clean")
    }

    @Test
    fun aFumbledWalkInSpoilsTheWholeRound() {
        // A recall round walks the road in once — later lines rejoin at the fork — so a
        // stumble on the way costs this round's toll, not just this line's.
        var state = BranchState.start(italian).submit(move("d2d4"))
        assertTrue(state.roadInFumbled)
        assertNull(state.cursorId, "still at the starting position; no line was spent")
        listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4").forEach { state = state.submit(move(it)) }
        assertFalse(state.roadInCleared, "the round's one walk in was not clean")

        // The next round is a clean slate.
        var fresh = BranchState.start(italian)
        listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4").forEach { fresh = fresh.submit(move(it)) }
        assertTrue(fresh.roadInCleared)
    }

    @Test
    fun aMissPastTheOpeningNeverCostsTheRoadIn() {
        var state = BranchState.start(italian)
        listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4").forEach { state = state.submit(move(it)) }
        val slipped = state.submit(move("g8h6"))
        assertIs<BranchEvent.BranchFailed>(slipped.lastEvent)
        assertFalse(slipped.roadInFumbled, "the stumble was inside the opening, not on the way")
        assertTrue(slipped.roadInCleared)
    }

    @Test
    fun aForgivenMissOnTheWayInStillCostsTheToll() {
        val missed = BranchState.start(italian, policy = MistakePolicy.ONE_ALLOWANCE)
            .submit(move("d2d4"))
        assertIs<BranchEvent.Missed>(missed.lastEvent)
        assertTrue(missed.roadInFumbled, "forgiven on the board is not proven on the road")
        assertEquals(2, missed.progress.totalLines, "and no line was spent on it")
    }

    @Test
    fun theRoadInIsNotWalkedAtAllOnceItIsEarned() {
        val state = BranchState.start(italian, entryNodeId = "0.0.0.0.0")
        assertFalse(state.roadInCleared, "nothing to prove — the round opens past it")
        assertFalse(state.roadInFumbled)
    }

    @Test
    fun theRoadInAskIsOnlyForMovesBeforeTheFork() {
        assertTrue(italian.isReachingForRoadIn(null), "the very first move is the way in")
        assertTrue(italian.isReachingForRoadIn(TrainingRecord.ROOT_NODE_KEY))
        assertTrue(italian.isReachingForRoadIn("0.0.0.0"), "3.Bc4 is still the way in")
        assertFalse(
            italian.isReachingForRoadIn("0.0.0.0.0"),
            "standing on the Italian bishop, the next move is the opening itself",
        )
        assertFalse(italian.isReachingForRoadIn("0.0.0.0.0.0"))
        assertFalse(tiny.isReachingForRoadIn("0"), "tiny forks straight after 1.e4")
        assertEquals(setOf("0"), tiny.trunkNodeIds())
    }
}
