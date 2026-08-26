package com.cheacher.app.progress

import com.cheacher.app.training.MistakePolicy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One line's review history, keyed by its leaf id in [TrainingRecord.lineReviews].
 *
 * Two numbers, because the scheduler only asks two questions: *when did you last prove
 * this line blind* and *how many times in a row have you proven it*. Full timestamp
 * lists were considered and rejected — they answer questions nothing asks yet, and
 * [TrainingRecord.sessionStarts] already keeps the raw session chronology.
 */
@Serializable
data class LineReview(
    /** Epoch-millis of the last clean branch-recall completion of this line. */
    @SerialName("last_reviewed_at")
    val lastReviewedAt: Long,
    /**
     * Consecutive clean blind recalls since the last miss anywhere on the line.
     * Drives the expanding review ladder; a lapse resets it to zero.
     */
    @SerialName("streak")
    val streak: Int,
)

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
    /** Leaf node id → times that full line was walked to its end, in either mode. */
    @SerialName("line_completions")
    val lineCompletions: Map<String, Int> = emptyMap(),
    /**
     * Leaf node id → times that line was banked in branch recall specifically.
     *
     * A second map rather than a rewrite of [lineCompletions] so records stored before
     * this field existed still decode and still mean what they meant. Legacy completions
     * all read as guided — generous, but progression still demands a branch proof before
     * it calls a line mastered, so nothing unlocks for free.
     */
    @SerialName("branch_line_completions")
    val branchLineCompletions: Map<String, Int> = emptyMap(),
    /**
     * Leaf node id → review history for the spacing scheduler.
     *
     * Defaulted so records written before spacing existed still decode; a mastered line
     * with no entry here simply reads as "never proven blind on the clock", which the
     * scheduler treats as top review priority. Honest, and nothing is withheld either way.
     */
    @SerialName("line_reviews")
    val lineReviews: Map<String, LineReview> = emptyMap(),
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

    /**
     * A branch-recall completion counts in both maps: it is still a walked line. It is
     * also the review event — blind recall is the only proof spacing trusts, so guided
     * walks never touch [lineReviews]; naming your way down a line you were shown is
     * study, not retrieval.
     *
     * [cleanRecall] is false when the line took a miss earlier in the same round: the
     * completion still banks toward mastery — the learner did finish the line — but it
     * proves nothing to the spacing ladder, so [lineReviews] is untouched and the line
     * stays at the front of the review queue.
     */
    fun recordBranchLineCompleted(
        leafId: String,
        atEpochMillis: Long,
        cleanRecall: Boolean = true,
    ): TrainingRecord =
        copy(
            lineCompletions = lineCompletions + (leafId to (lineCompletions[leafId] ?: 0) + 1),
            branchLineCompletions = branchLineCompletions +
                (leafId to (branchLineCompletions[leafId] ?: 0) + 1),
            lineReviews = if (!cleanRecall) {
                lineReviews
            } else {
                lineReviews + (
                    leafId to LineReview(
                        lastReviewedAt = atEpochMillis,
                        streak = (lineReviews[leafId]?.streak ?: 0) + 1,
                    )
                    )
            },
        )

    /**
     * A miss landed somewhere on [leafId]'s line: the streak goes back to zero, so the
     * line jumps to the front of the review queue. [LineReview.lastReviewedAt] survives —
     * the *when* is still true, only the *how solid* changed. A line with no history yet
     * has nothing to lapse.
     */
    fun recordLineLapsed(leafId: String): TrainingRecord {
        val review = lineReviews[leafId] ?: return this
        return copy(lineReviews = lineReviews + (leafId to review.copy(streak = 0)))
    }

    /** Times [leafId]'s line was banked in branch recall. */
    fun branchCompletionsOf(leafId: String): Int = branchLineCompletions[leafId] ?: 0

    /**
     * Times [leafId]'s line was walked outside branch recall — derived by subtraction so
     * [lineCompletions] keeps its original all-modes meaning. Legacy records subtract
     * nothing and read entirely as guided, which is the kind interpretation.
     */
    fun guidedCompletionsOf(leafId: String): Int =
        ((lineCompletions[leafId] ?: 0) - branchCompletionsOf(leafId)).coerceAtLeast(0)

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

    /** Consecutive clean blind recalls of [leafId]'s line, zero if never proven blind. */
    fun reviewStreakOf(leafId: String): Int = lineReviews[leafId]?.streak ?: 0

    /**
     * Consecutive calendar days with at least one session, counting back from *now*.
     * Today does not have to be practised yet — a streak earned through yesterday is
     * still alive this morning, because a streak you lose while asleep is a scold, not
     * an encouragement. Days are UTC buckets: honest enough for a habit counter, and it
     * keeps the function pure (no timezone database in domain code).
     */
    fun dayStreak(nowEpochMillis: Long): Int {
        if (sessionStarts.isEmpty()) return 0
        val days = sessionStarts.map { it / DAY_MILLIS }.toSet()
        val today = nowEpochMillis / DAY_MILLIS
        var cursor = when {
            today in days -> today
            (today - 1) in days -> today - 1
            else -> return 0
        }
        var streak = 1
        while ((cursor - 1) in days) {
            cursor--
            streak++
        }
        return streak
    }

    companion object {
        /** Miss attribution when the learner blunders before the first move of the tree. */
        const val ROOT_NODE_KEY = "root"

        /** One day of epoch-millis — the unit of both the review ladder and day streaks. */
        const val DAY_MILLIS: Long = 24L * 60 * 60 * 1000

        fun empty(repertoireId: String): TrainingRecord = TrainingRecord(repertoireId)
    }
}
