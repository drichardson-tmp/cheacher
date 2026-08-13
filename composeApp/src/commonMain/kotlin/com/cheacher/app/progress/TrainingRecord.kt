package com.cheacher.app.progress

import com.cheacher.app.training.MistakePolicy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Everything Cheacher remembers about a learner and one repertoire.
 *
 * Pure data plus pure accumulation functions — no store, no clock, no tree. The shape is
 * deliberately analysis-friendly: [missCounts] is keyed by tree-node id, which is exactly
 * the input a future on-device model needs to say "you keep forgetting the Najdorf's
 * ...a6 — here's why it matters". Persist generously now, interpret later.
 */
@Serializable
data class TrainingRecord(
    @SerialName("repertoire_id")
    val repertoireId: String,
    /** Node id → times the learner failed to find that node's move. The trouble map. */
    @SerialName("miss_counts")
    val missCounts: Map<String, Int> = emptyMap(),
    /** Leaf node id → times that full line was walked to its end. */
    @SerialName("line_completions")
    val lineCompletions: Map<String, Int> = emptyMap(),
    /** Epoch-millis of each session start, oldest first. The spacing curve lives here. */
    @SerialName("session_starts")
    val sessionStarts: List<Long> = emptyList(),
    @SerialName("guided_sessions_completed")
    val guidedSessionsCompleted: Int = 0,
    @SerialName("branch_sessions_completed")
    val branchSessionsCompleted: Int = 0,
    /** Branch rounds finished with zero failed lines. The number worth bragging about. */
    @SerialName("branch_clean_sweeps")
    val branchCleanSweeps: Int = 0,
    @SerialName("last_policy")
    val lastPolicy: MistakePolicy? = null,
) {
    val totalMisses: Int get() = missCounts.values.sum()

    val sessionsCompleted: Int get() = guidedSessionsCompleted + branchSessionsCompleted

    /** Node ids sorted by miss count, worst first — the seed for "trouble spots". */
    fun troubleSpots(limit: Int = 3): List<Pair<String, Int>> =
        missCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { it.key to it.value }

    fun recordMiss(nodeId: String): TrainingRecord =
        copy(missCounts = missCounts + (nodeId to (missCounts[nodeId] ?: 0) + 1))

    fun recordLineCompleted(leafId: String): TrainingRecord =
        copy(lineCompletions = lineCompletions + (leafId to (lineCompletions[leafId] ?: 0) + 1))

    fun recordSessionStart(atEpochMillis: Long, policy: MistakePolicy? = null): TrainingRecord =
        copy(
            sessionStarts = sessionStarts + atEpochMillis,
            lastPolicy = policy ?: lastPolicy,
        )

    fun recordGuidedSessionCompleted(): TrainingRecord =
        copy(guidedSessionsCompleted = guidedSessionsCompleted + 1)

    fun recordBranchSessionCompleted(cleanSweep: Boolean): TrainingRecord =
        copy(
            branchSessionsCompleted = branchSessionsCompleted + 1,
            branchCleanSweeps = if (cleanSweep) branchCleanSweeps + 1 else branchCleanSweeps,
        )

    companion object {
        /** Miss attribution when the learner blunders before the first move of the tree. */
        const val ROOT_NODE_KEY = "root"

        fun empty(repertoireId: String): TrainingRecord = TrainingRecord(repertoireId)
    }
}
