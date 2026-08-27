package com.cheacher.app.progress

import com.cheacher.app.training.MistakePolicy
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class TrainingRecordTest {
    @Test
    fun missesAccumulatePerNode() {
        val record = TrainingRecord.empty("italian")
            .recordMiss("0.1")
            .recordMiss("0.1")
            .recordMiss("0.0.0")
        assertEquals(mapOf("0.1" to 2, "0.0.0" to 1), record.missCounts)
        assertEquals(3, record.totalMisses)
    }

    @Test
    fun troubleSpotsAreWorstFirstWithStableTies() {
        val record = TrainingRecord.empty("x")
            .recordMiss("a").recordMiss("a").recordMiss("a")
            .recordMiss("b")
            .recordMiss("c").recordMiss("c")
            .recordMiss("d").recordMiss("d")
        assertEquals(listOf("a" to 3, "c" to 2, "d" to 2), record.troubleSpots(limit = 3))
        assertEquals(listOf("a" to 3), record.troubleSpots(limit = 1))
    }

    @Test
    fun lineCompletionsAccumulatePerLeaf() {
        val record = TrainingRecord.empty("x")
            .recordLineCompleted("0.0.0")
            .recordLineCompleted("0.0.0")
            .recordLineCompleted("0.1.0")
        assertEquals(mapOf("0.0.0" to 2, "0.1.0" to 1), record.lineCompletions)
    }

    @Test
    fun sessionStartsKeepOrderAndPolicyOnlyUpgrades() {
        val record = TrainingRecord.empty("x")
            .recordSessionStart(1_000L, MistakePolicy.STRICT)
            .recordSessionStart(2_000L) // guided session: no policy, last one sticks
            .recordSessionStart(3_000L, MistakePolicy.ONE_ALLOWANCE)
        assertEquals(listOf(1_000L, 2_000L, 3_000L), record.sessionStarts)
        assertEquals(MistakePolicy.ONE_ALLOWANCE, record.lastPolicy)

        assertNull(TrainingRecord.empty("y").recordSessionStart(1L).lastPolicy)
    }

    @Test
    fun completionCountersSplitByModeAndCleanliness() {
        val record = TrainingRecord.empty("x")
            .recordGuidedSessionCompleted()
            .recordBranchSessionCompleted(cleanSweep = true)
            .recordBranchSessionCompleted(cleanSweep = false)
        assertEquals(1, record.guidedSessionsCompleted)
        assertEquals(2, record.branchSessionsCompleted)
        assertEquals(1, record.branchCleanSweeps)
        assertEquals(3, record.sessionsCompleted)
    }

    @Test
    fun branchCompletionsCountInBothMaps() {
        val record = TrainingRecord.empty("x")
            .recordLineCompleted("0.0.0") // guided walk
            .recordBranchLineCompleted("0.0.0", atEpochMillis = 10L) // branch recall
            .recordBranchLineCompleted("0.1.0", atEpochMillis = 20L)
        assertEquals(mapOf("0.0.0" to 2, "0.1.0" to 1), record.lineCompletions, "still the all-modes total")
        assertEquals(mapOf("0.0.0" to 1, "0.1.0" to 1), record.branchLineCompletions)
        assertEquals(1, record.guidedCompletionsOf("0.0.0"))
        assertEquals(1, record.branchCompletionsOf("0.0.0"))
        assertEquals(0, record.guidedCompletionsOf("0.1.0"), "branch-only lines have no guided credit")
    }

    @Test
    fun legacyRecordWithoutBranchMapStillDecodes() {
        // A blob written before branch_line_completions existed: it must decode, and its
        // completions must all read as guided — nothing unlocks for free, nothing is lost.
        val json = Json { ignoreUnknownKeys = true }
        val blob = """{"repertoire_id":"italian","line_completions":{"0.0.0":3},"guided_sessions_completed":2}"""
        val record = json.decodeFromString<TrainingRecord>(blob)
        assertEquals(emptyMap(), record.branchLineCompletions)
        assertEquals(3, record.guidedCompletionsOf("0.0.0"))
        assertEquals(0, record.branchCompletionsOf("0.0.0"))
    }

    @Test
    fun legacyRecordWithoutTrunkClearsStillDecodes() {
        // A blob written before the opening entry existed: it decodes as "road in not yet
        // proven", so the learner walks it once more and earns the entry back mid-session.
        val json = Json { ignoreUnknownKeys = true }
        val blob = """{"repertoire_id":"italian","line_credits":{"0.0.0":1.0}}"""
        val record = json.decodeFromString<TrainingRecord>(blob)
        assertEquals(0, record.trunkClears)
        assertEquals(1, record.recordTrunkCleared().trunkClears)
    }

    @Test
    fun theRoadInIsEarnedByWalkingItAndLostBySlippingOnIt() {
        val proven = TrainingRecord.empty("italian").recordTrunkCleared().recordTrunkCleared()
        assertEquals(2, proven.trunkClears, "every clean walk counts, for later analysis")
        assertEquals(0, proven.recordTrunkFumbled().trunkClears, "one slip revokes it outright")

        val fresh = TrainingRecord.empty("italian")
        assertEquals(fresh, fresh.recordTrunkFumbled(), "nothing to lose, nothing written")
    }

    @Test
    fun recordSurvivesJsonRoundTrip() {
        val json = Json { ignoreUnknownKeys = true }
        val record = TrainingRecord.empty("sicilian")
            .recordSessionStart(42L, MistakePolicy.STRICT)
            .recordMiss("0.0")
            .recordLineCompleted("0.0.1")
            .recordBranchLineCompleted("0.0.1", atEpochMillis = 43L)
            .recordBranchSessionCompleted(cleanSweep = false)
            .recordTrunkCleared()
            .copy(
                moveDrill = MoveDrillRecord(
                    findMove = DrillRecord(rounds = 1, reps = 20, cleanReps = 18, lastMedianMillis = 900L),
                ),
            )
        assertEquals(record, json.decodeFromString<TrainingRecord>(json.encodeToString(record)))
    }

    @Test
    fun branchCompletionsGrowTheReviewStreak() {
        val record = TrainingRecord.empty("x")
            .recordBranchLineCompleted("0.0.0", atEpochMillis = 100L)
            .recordBranchLineCompleted("0.0.0", atEpochMillis = 200L)
        assertEquals(LineReview(lastReviewedAt = 200L, streak = 2), record.lineReviews["0.0.0"])
        assertEquals(2, record.reviewStreakOf("0.0.0"))
        assertEquals(0, record.reviewStreakOf("0.1.0"), "unreviewed lines have no streak")
    }

    @Test
    fun lapseResetsTheStreakButKeepsTheTimestamp() {
        val record = TrainingRecord.empty("x")
            .recordBranchLineCompleted("0.0.0", atEpochMillis = 100L)
            .recordBranchLineCompleted("0.0.0", atEpochMillis = 200L)
            .recordLineLapsed("0.0.0")
        assertEquals(LineReview(lastReviewedAt = 200L, streak = 0), record.lineReviews["0.0.0"])
        assertEquals(
            TrainingRecord.empty("x"),
            TrainingRecord.empty("x").recordLineLapsed("0.0.0"),
            "lapsing a line with no history is a no-op",
        )
    }

    @Test
    fun legacyRecordWithoutLineReviewsStillDecodes() {
        // A blob written before spacing existed: mastered lines simply have no review
        // history, which the scheduler reads as top review priority.
        val json = Json { ignoreUnknownKeys = true }
        val blob = """{"repertoire_id":"italian","line_completions":{"0.0.0":3},"branch_line_completions":{"0.0.0":1}}"""
        val record = json.decodeFromString<TrainingRecord>(blob)
        assertEquals(emptyMap(), record.lineReviews)
        assertEquals(0, record.reviewStreakOf("0.0.0"))
    }

    @Test
    fun dayStreakCountsConsecutiveDaysAndForgivesAnUnplayedToday() {
        val day = TrainingRecord.DAY_MILLIS
        val record = TrainingRecord.empty("x")
            .recordSessionStart(1 * day + 5)
            .recordSessionStart(2 * day + 5)
            .recordSessionStart(2 * day + 900) // two sessions in one day count once
            .recordSessionStart(3 * day + 5)
        assertEquals(3, record.dayStreak(3 * day + 999, TimeZone.UTC), "practised today: streak runs through today")
        assertEquals(3, record.dayStreak(4 * day + 5, TimeZone.UTC), "not yet practised today: yesterday's streak lives")
        assertEquals(0, record.dayStreak(5 * day + 5, TimeZone.UTC), "a skipped day ends the streak")
        assertEquals(0, TrainingRecord.empty("y").dayStreak(nowEpochMillis = 0L, TimeZone.UTC))
    }

    @Test
    fun dayStreakUsesTheLearnersCalendarDays() {
        val newYork = TimeZone.of("America/New_York")
        val record = TrainingRecord.empty("x")
            // Jan 1 at 8pm and Jan 2 at 6pm locally, but both are Jan 2 in UTC.
            .recordSessionStart(Instant.parse("2026-01-02T01:00:00Z").toEpochMilliseconds())
            .recordSessionStart(Instant.parse("2026-01-02T23:00:00Z").toEpochMilliseconds())
        val jan3Morning = Instant.parse("2026-01-03T13:00:00Z").toEpochMilliseconds()

        assertEquals(2, record.dayStreak(jan3Morning, newYork))
        assertEquals(1, record.dayStreak(jan3Morning, TimeZone.UTC), "UTC collapses both local days")
    }

    @Test
    fun dayStreakUsesTheZonesOffsetAtEachSession() {
        val newYork = TimeZone.of("America/New_York")
        val record = TrainingRecord.empty("x")
            // Mar 7 at 11:30pm EST, shortly before the spring-forward transition.
            .recordSessionStart(Instant.parse("2026-03-08T04:30:00Z").toEpochMilliseconds())
            // Mar 8 at 6pm EDT, after the offset has changed from -05:00 to -04:00.
            .recordSessionStart(Instant.parse("2026-03-08T22:00:00Z").toEpochMilliseconds())
        val mar9Morning = Instant.parse("2026-03-09T13:00:00Z").toEpochMilliseconds()

        assertEquals(2, record.dayStreak(mar9Morning, newYork))
    }

    @Test
    fun lineCreditsAreLatestWins() {
        val record = TrainingRecord.empty("x")
            .recordLineCredit("0.0.0", 1.0)
            .recordLineCredit("0.0.0", 0.5) // a hinted walk yesterday outranks a clean one last month
        assertEquals(0.5, record.creditOf("0.0.0"))
        assertEquals(0.0, record.creditOf("0.1.0"), "never walked reads as nothing banked")
    }

    @Test
    fun openingOutcomeStartsGrowsAndResetsTheStreak() {
        val leaves = listOf("0.0.0", "0.1.0")
        val half = TrainingRecord.empty("x").recordLineCredit("0.0.0", 1.0)
        assertNull(
            half.recordOpeningOutcome(leaves, atEpochMillis = 10L).openingReview,
            "a partial score on a never-accounted opening cannot lapse it",
        )

        val learned = half.recordLineCredit("0.1.0", 1.0).recordOpeningOutcome(leaves, atEpochMillis = 20L)
        assertEquals(LineReview(lastReviewedAt = 20L, streak = 1), learned.openingReview)

        val reviewed = learned.recordOpeningOutcome(leaves, atEpochMillis = 30L)
        assertEquals(LineReview(lastReviewedAt = 30L, streak = 2), reviewed.openingReview)

        val slipped = reviewed.recordLineCredit("0.1.0", 0.5).recordOpeningOutcome(leaves, atEpochMillis = 40L)
        assertEquals(LineReview(lastReviewedAt = 40L, streak = 0), slipped.openingReview)
    }

    @Test
    fun legacyRecordWithoutCreditsStillDecodes() {
        // A blob written before credits existed: every line reads unaccounted, so the
        // learner re-earns the book on the new ladder — honest, and nothing is deleted.
        val json = Json { ignoreUnknownKeys = true }
        val blob = """{"repertoire_id":"italian","line_completions":{"0.0.0":3}}"""
        val record = json.decodeFromString<TrainingRecord>(blob)
        assertEquals(emptyMap(), record.lineCredits)
        assertNull(record.openingReview)
    }

    @Test
    fun unknownJsonKeysAreIgnoredForForwardCompatibility() {
        val json = Json { ignoreUnknownKeys = true }
        val blob = """{"repertoire_id":"x","miss_counts":{"0.1":4},"future_field":true}"""
        val record = json.decodeFromString<TrainingRecord>(blob)
        assertEquals("x", record.repertoireId)
        assertEquals(4, record.missCounts["0.1"])
    }
}
