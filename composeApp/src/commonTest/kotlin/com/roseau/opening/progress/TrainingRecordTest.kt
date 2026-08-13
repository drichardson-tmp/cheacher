package com.roseau.opening.progress

import com.roseau.opening.training.MistakePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

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
    fun recordSurvivesJsonRoundTrip() {
        val json = Json { ignoreUnknownKeys = true }
        val record = TrainingRecord.empty("sicilian")
            .recordSessionStart(42L, MistakePolicy.STRICT)
            .recordMiss("0.0")
            .recordLineCompleted("0.0.1")
            .recordBranchSessionCompleted(cleanSweep = false)
        assertEquals(record, json.decodeFromString<TrainingRecord>(json.encodeToString(record)))
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
