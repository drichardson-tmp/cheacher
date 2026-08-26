package com.cheacher.app.progress

import com.cheacher.app.training.DrillSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The common scoreboard for a timed drill direction.
 *
 * The square drill uses one directly; the move drill nests one per direction in
 * [MoveDrillRecord]. Both belong to no repertoire, so they ride in the record stored
 * under [TrainingRecord.DRILL_RECORD_ID] — one store and no second persistence layer.
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
    /** Reps found without a wrong first attempt. */
    @SerialName("clean_reps")
    val cleanReps: Int = 0,
    @SerialName("last_median_millis")
    val lastMedianMillis: Long? = null,
    @SerialName("best_median_millis")
    val bestMedianMillis: Long? = null,
    /** The single fastest rep ever answered. */
    @SerialName("best_rep_millis")
    val bestRepMillis: Long? = null,
) {
    val accuracy: Float get() = if (reps == 0) 0f else cleanReps.toFloat() / reps
}

/** Separate scoreboards for the two directions of the shelf-wide move drill. */
@Serializable
data class MoveDrillRecord(
    @SerialName("find_move")
    val findMove: DrillRecord = DrillRecord(),
    @SerialName("name_it")
    val nameIt: DrillRecord = DrillRecord(),
)

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
