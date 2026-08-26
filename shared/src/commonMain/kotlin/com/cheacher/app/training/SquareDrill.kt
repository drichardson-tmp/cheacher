package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Squares
import kotlin.random.Random

/**
 * The square drill: a name, an empty board, and a clock.
 *
 * Everything else in Cheacher assumes you can already *see* squares — "King's Knight
 * Opening" only works as vocabulary if f3 is a place you look at rather than a place you
 * count to from a1. This drill isolates that one skill and puts a number on it, so a slow
 * answer in recall can be diagnosed instead of guessed at: did you not know the name, or
 * did you know it and lose four seconds finding the square?
 *
 * Two rules earn their keep here:
 * - **No coordinates on the board.** With the a–h/1–8 edge labels on, this is reading,
 *   not recall; the whole exercise is holding the grid in your head.
 * - **The table turns at the halfway mark.** Finding e4 from Black's side is a separate
 *   skill that almost nobody drills, and the board already knows how to flip.
 *
 * Deliberately *not* on the opening ladder — it belongs to no repertoire and unlocks
 * nothing. It is a ninety-second warm-up you can take whenever, and its record lives
 * beside the openings rather than inside one.
 *
 * Pure like the other reducers: prompts are dealt in, the clock is passed in, every
 * transition is a value.
 */
data class SquareDrillState(
    /** The squares to find, in order. Dealt at start so the reducer stays pure. */
    val prompts: List<Int>,
    val index: Int = 0,
    val answered: List<DrillRep> = emptyList(),
    /** Wrong taps spent on the *current* prompt. A rep is only clean if this is zero. */
    val wrongTaps: Int = 0,
    /** When the current prompt went up — the start of the stopwatch for this rep. */
    val promptShownAt: Long,
    val lastEvent: DrillEvent? = null,
) {
    val target: Int? get() = prompts.getOrNull(index)

    /** The prompt as the learner reads it: "f3". */
    val targetName: String? get() = target?.let(Squares::name)

    val finished: Boolean get() = index >= prompts.size

    /** White's view for the first half of the round, Black's for the second. */
    val orientation: Color
        get() = if (index < prompts.size / 2) Color.WHITE else Color.BLACK

    val repNumber: Int get() = (index + 1).coerceAtMost(prompts.size)

    val summary: DrillSummary get() = DrillSummary.of(answered)

    companion object {
        /** Twenty squares: long enough for an honest median, short enough to retake. */
        const val ROUND_LENGTH = 20

        fun start(prompts: List<Int>, startedAt: Long): SquareDrillState =
            SquareDrillState(prompts = prompts, promptShownAt = startedAt)
    }
}

/** One answered prompt: which square, how long it took, and whether it took a wrong tap. */
data class DrillRep(val square: Int, val millis: Long, val clean: Boolean)

sealed interface DrillEvent {
    /** Found it. [millis] is this rep's time; the screen flashes the square green. */
    data class Found(val square: Int, val millis: Long) : DrillEvent

    /** Wrong square. The prompt stays up — you do not get to skip one you cannot find. */
    data class Missed(val square: Int) : DrillEvent
}

/**
 * The scoreboard. **Median, not mean**: one distracted six-second rep should not decide
 * how a round reads, and the median is the number that actually moves as the grid becomes
 * automatic.
 */
data class DrillSummary(
    val reps: Int,
    val cleanReps: Int,
    val medianMillis: Long?,
    val bestMillis: Long?,
) {
    val accuracy: Float get() = if (reps == 0) 0f else cleanReps.toFloat() / reps

    companion object {
        fun of(answered: List<DrillRep>): DrillSummary {
            return ofTimes(
                times = answered.map { it.millis },
                cleanReps = answered.count { it.clean },
            )
        }

        /** Shared timed-summary math for drills whose answer is not literally a square. */
        fun ofTimes(times: List<Long>, cleanReps: Int): DrillSummary {
            val sortedTimes = times.sorted()
            val median = when {
                sortedTimes.isEmpty() -> null
                sortedTimes.size % 2 == 1 -> sortedTimes[sortedTimes.size / 2]
                else -> (sortedTimes[sortedTimes.size / 2 - 1] + sortedTimes[sortedTimes.size / 2]) / 2
            }
            return DrillSummary(
                reps = sortedTimes.size,
                cleanReps = cleanReps.coerceIn(0, sortedTimes.size),
                medianMillis = median,
                bestMillis = sortedTimes.firstOrNull(),
            )
        }
    }
}

/**
 * Feeds a tap into the round. A wrong tap costs the rep its clean flag and leaves the
 * clock running — the prompt stays up until it is found, so every rep's time is the true
 * time-to-find rather than the time-to-give-up.
 */
fun SquareDrillState.tap(square: Int, nowMillis: Long): SquareDrillState {
    val wanted = target ?: return this
    if (square != wanted) {
        return copy(wrongTaps = wrongTaps + 1, lastEvent = DrillEvent.Missed(square))
    }
    val elapsed = (nowMillis - promptShownAt).coerceAtLeast(0)
    return copy(
        index = index + 1,
        answered = answered + DrillRep(square, elapsed, clean = wrongTaps == 0),
        wrongTaps = 0,
        promptShownAt = nowMillis,
        lastEvent = DrillEvent.Found(square, elapsed),
    )
}

/**
 * Deals a round's prompts. Consecutive repeats are dropped — "f3, f3" measures a memory
 * of the last tap, not knowledge of the grid — but the same square recurring later in a
 * round is left alone, because that is just the dice.
 */
fun drillPrompts(
    count: Int = SquareDrillState.ROUND_LENGTH,
    random: Random = Random.Default,
): List<Int> {
    val dealt = mutableListOf<Int>()
    while (dealt.size < count) {
        val next = random.nextInt(Squares.COUNT)
        if (next != dealt.lastOrNull()) dealt += next
    }
    return dealt
}
