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
 * The play-out sparring ledger: the adaptive engine strength and the game tallies
 * behind it. One rating per repertoire — closing out a King's Indian is a different
 * skill from closing out an Italian, and each opening's engine meets you where that
 * opening left you.
 */
@Serializable
data class SparringRecord(
    /** The engine's current level: it always plays *at* this rating. Starts at 700. */
    @SerialName("rating")
    val rating: Int = com.cheacher.app.engine.SparringElo.START,
    @SerialName("games_played")
    val gamesPlayed: Int = 0,
    @SerialName("wins")
    val wins: Int = 0,
    @SerialName("draws")
    val draws: Int = 0,
    @SerialName("losses")
    val losses: Int = 0,
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
    /**
     * Leaf node id → the credit its *most recent* guided walk earned: 1.0 found unaided,
     * 0.5 with the hint, 0.0 after a wrong move. Latest-wins rather than best-wins,
     * because the number answers "do you know this line *now*" — a clean pass last month
     * followed by a hint yesterday is a half-known line, not a known one.
     */
    @SerialName("line_credits")
    val lineCredits: Map<String, Double> = emptyMap(),
    /**
     * The whole opening's review clock, set the first time every line is accounted for
     * (all credits 1.0) and rolled by each later look: a clean look grows the streak, a
     * slip resets it. Null means the opening has never been fully accounted — it is still
     * being learned, not reviewed. Same [LineReview] shape as the line ladder, same
     * expanding intervals, one level up.
     */
    @SerialName("opening_review")
    val openingReview: LineReview? = null,
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
    /** Defaulted so records written before play-out existed still decode. */
    @SerialName("sparring")
    val sparring: SparringRecord = SparringRecord(),
    /**
     * Square-drill history. Only ever set on the reserved [DRILL_RECORD_ID] record — the
     * drill trains board geometry, which belongs to no opening — and null everywhere
     * else. Defaulted, so records written before the drill existed still decode.
     */
    @SerialName("square_drill")
    val squareDrill: DrillRecord? = null,
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

    /** The credit [leafId]'s line earned on its most recent guided walk. Never walked reads 0. */
    fun creditOf(leafId: String): Double = lineCredits[leafId] ?: 0.0

    /** Overwrites [leafId]'s credit with what the walk just finished actually earned. */
    fun recordLineCredit(leafId: String, credit: Double): TrainingRecord =
        copy(lineCredits = lineCredits + (leafId to credit))

    /**
     * Rolls the opening's review clock after a finished session over [leafIds] (the
     * opening's leaves — passed in because the record stays tree-free). Fully accounted
     * (every credit 1.0) grows the streak, starting it at 1 the first time; anything less
     * on an opening that *was* accounted resets the streak to zero, which puts the whole
     * opening at the front of the review queue. A partial score on a never-accounted
     * opening changes nothing — you cannot lapse what you never held.
     */
    fun recordOpeningOutcome(leafIds: List<String>, atEpochMillis: Long): TrainingRecord {
        val accounted = leafIds.isNotEmpty() && leafIds.all { creditOf(it) >= 1.0 }
        return when {
            accounted -> copy(
                openingReview = LineReview(atEpochMillis, (openingReview?.streak ?: 0) + 1),
            )
            openingReview != null -> copy(openingReview = LineReview(atEpochMillis, 0))
            else -> this
        }
    }

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

    /**
     * One finished play-out game, scored 1 / ½ / 0 for the learner. The engine played
     * at [SparringRecord.rating], so the expected score is ½ and the update collapses
     * to ±K/2 — win a game, the engine climbs ~32 points; lose one, it eases off.
     */
    fun recordSparringGame(score: Double): TrainingRecord =
        copy(
            sparring = sparring.copy(
                rating = com.cheacher.app.engine.SparringElo.updated(sparring.rating, score),
                gamesPlayed = sparring.gamesPlayed + 1,
                wins = sparring.wins + if (score >= 1.0) 1 else 0,
                draws = sparring.draws + if (score == 0.5) 1 else 0,
                losses = sparring.losses + if (score <= 0.0) 1 else 0,
            ),
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

        /**
         * The record id the square drill saves under. Not a repertoire and never on the
         * shelf: the shelf only ever looks records up *by* a tree's id, so a reserved key
         * costs nothing and keeps the drill out of a second store.
         */
        const val DRILL_RECORD_ID = "__square_drill"

        /** One day of epoch-millis — the unit of both the review ladder and day streaks. */
        const val DAY_MILLIS: Long = 24L * 60 * 60 * 1000

        fun empty(repertoireId: String): TrainingRecord = TrainingRecord(repertoireId)
    }
}
