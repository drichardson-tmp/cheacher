package com.cheacher.app.progress

import com.cheacher.app.training.DrillSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What Cheacher remembers about the square drill.
 *
 * The drill belongs to no repertoire, so this rides in the record stored under
 * [TrainingRecord.DRILL_RECORD_ID] — a reserved id that is not on the shelf. Same store,
 * same JSON discipline, no second persistence layer for six numbers.
 *
 * Both a *last* and a *best* median are kept: the best is the brag, the last is the
 * honest answer to "where am I now", and a learner who only ever sees their record round
 * is being flattered rather than coached.
 */
@Serializable
data class DrillRecord(
    @SerialName("rounds")
    val rounds: Int = 0,
    @SerialName("reps")
    val reps: Int = 0,
    /** Reps found without a wrong tap first. */
    @SerialName("clean_reps")
    val cleanReps: Int = 0,
    @SerialName("last_median_millis")
    val lastMedianMillis: Long? = null,
    @SerialName("best_median_millis")
    val bestMedianMillis: Long? = null,
    /** The single fastest square ever found. */
    @SerialName("best_rep_millis")
    val bestRepMillis: Long? = null,
) {
    val accuracy: Float get() = if (reps == 0) 0f else cleanReps.toFloat() / reps
}

/** Folds a finished round into the record. Bests only ever improve. */
fun DrillRecord.recordRound(summary: DrillSummary): DrillRecord {
    val median = summary.medianMillis ?: return this
    return copy(
        rounds = rounds + 1,
        reps = reps + summary.reps,
        cleanReps = cleanReps + summary.cleanReps,
        lastMedianMillis = median,
        bestMedianMillis = minOfNullable(bestMedianMillis, median),
        bestRepMillis = minOfNullable(bestRepMillis, summary.bestMillis),
    )
}

private fun minOfNullable(current: Long?, candidate: Long?): Long? = when {
    candidate == null -> current
    current == null -> candidate
    else -> minOf(current, candidate)
}
