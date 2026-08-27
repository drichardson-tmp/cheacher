package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.progress.LineReview
import com.cheacher.app.progress.TrainingRecord

/**
 * The expanding review ladder: streak n clean blind recalls → an item's next review is
 * suggested roughly 1, 3, 7, 14, then 30 days out (capped at 30). Items are whole lines
 * on the ordinary syllabus and individual troublesome moves on focused recall.
 *
 * Why this curve: each successful retrieval roughly doubles-to-triples how long a memory
 * holds, so the classic Leitner ladder tracks the psychology closely enough — and unlike
 * a fractional SM-2 ease factor, "come back in a week" is a sentence a learner can say
 * out loud. Legibility is the product here, the same bet as one-sentence ideas. A lapse
 * resets the streak to zero, which puts that item at the front of the queue.
 *
 * The ladder is a *prioritiser*, never a gate: dueness decides which reviews join today's
 * suggested session and in what order, and nothing is ever locked away or postponed at
 * the learner — the full tree is always one tap away.
 */
object ReviewLadder {
    val intervalDays: List<Long> = listOf(1, 3, 7, 14, 30)

    /** Suggested millis between review n and review n+1 of an item with [streak] clean recalls. */
    fun intervalMillis(streak: Int): Long =
        if (streak <= 0) 0L
        else intervalDays[(streak - 1).coerceAtMost(intervalDays.lastIndex)] * TrainingRecord.DAY_MILLIS

    /**
     * When an item earns its next look. No history reads as due since forever — legacy
     * records and explicitly lapsed items go straight to the front of the queue.
     */
    fun dueAt(review: LineReview?): Long =
        review?.let { it.lastReviewedAt + intervalMillis(it.streak) } ?: 0L
}

/** Why a line is on (or near) today's syllabus. */
enum class SyllabusReason {
    /** The frontier — the one genuinely new fork. */
    NEW,

    /** A mastered line whose review interval has elapsed. */
    DUE,

    /** A mastered line still inside its interval — recently proven, resting. */
    FRESH,
}

/** One line's standing at the moment the syllabus was drawn. */
data class SyllabusLine(
    val lineIndex: Int,
    val reason: SyllabusReason,
    /** When the ladder suggests this line's next review. Internal ordering only — never UI copy. */
    val dueAtEpochMillis: Long,
    /** The line's clean-recall streak when the syllabus was drawn — the tiebreak. */
    val streak: Int = 0,
)

/** One troublesome move whose own review interval has elapsed. */
data class NodeReviewTarget(
    val nodeId: String,
    /** A mastered line that contains the move and can carry the focused recall. */
    val lineIndex: Int,
    /** Start here so [nodeId] itself is still the move the learner must find. */
    val entryNodeId: String?,
    val dueAtEpochMillis: Long,
    val streak: Int,
    val missCount: Int,
)

/**
 * Today's suggested session over one repertoire: the frontier line plus the reviews that
 * earned a seat, drawn once from (tree, record, now) and then frozen — a session's
 * syllabus is a snapshot, exactly like the navigation-time gates it feeds.
 *
 * Composition rule: every DUE line joins, ordered most-overdue-then-weakest first, and
 * the frontier is always included until the tree is exhausted. When nothing is due but
 * mastered lines exist, the weakest FRESH line joins anyway — a session should always
 * offer a review mix, because "nothing to do, come back later" is scheduling *at* the
 * learner, and Cheacher does not do that.
 */
data class Syllabus(
    val progression: Progression,
    /** Every non-suggested-locked line's standing, in tree (DFS) order. */
    val lines: List<SyllabusLine>,
    /** The lines in today's suggested session: reviews in priority order, frontier last. */
    val sessionLines: List<SyllabusLine>,
) {
    val newCount: Int = sessionLines.count { it.reason == SyllabusReason.NEW }

    /** Everything in the session that is not the new line — DUE plus any FRESH fill. */
    val reviewCount: Int = sessionLines.size - newCount

    /**
     * What a guided session walks, in tree order so shared prefixes interleave naturally:
     * the due lines' names mixed with the frontier line, never blocked apart.
     */
    val guidedLineIndices: List<Int> = sessionLines.map { it.lineIndex }.sorted()

    /** Absolute indices of the review lines — the UI's subtle "this one is a review" marker. */
    val reviewLineIndices: Set<Int> =
        sessionLines.filter { it.reason != SyllabusReason.NEW }.map { it.lineIndex }.toSet()

    /** The branch-recall world: every node on a session line, reviews and frontier mixed. */
    val branchAllowedNodeIds: Set<String> = buildSet {
        guidedLineIndices.forEach { index ->
            progression.tree.lines[index].forEach { add(it.id) }
        }
    }
}

/**
 * Draws today's syllabus. Pure: `now` is a parameter, injected at the ViewModel/App
 * layer like every other timestamp — no clock ever runs in domain code.
 *
 * Composes on top of the ladder rather than replacing it: [Progression] still names the
 * frontier and the default depth-first path; this function decides which already-mastered
 * lines ride along as retrieval practice.
 */
fun Progression.syllabusAt(nowEpochMillis: Long): Syllabus {
    val standings = tree.lines.indices.mapNotNull { index ->
        when (statusOf(index)) {
            LineStatus.LOCKED -> null
            LineStatus.UNLOCKED -> SyllabusLine(index, SyllabusReason.NEW, nowEpochMillis)
            LineStatus.MASTERED -> {
                val leafId = tree.lines[index].last().id
                val dueAt = ReviewLadder.dueAt(record.lineReviews[leafId])
                val reason = if (dueAt <= nowEpochMillis) SyllabusReason.DUE else SyllabusReason.FRESH
                SyllabusLine(index, reason, dueAt, streak = record.reviewStreakOf(leafId))
            }
        }
    }

    // Most-overdue first, then weakest streak, then tree order — so when two reviews fall
    // due together, the shakier line gets the earlier seat.
    val byPriority = compareBy<SyllabusLine>({ it.dueAtEpochMillis }, { it.streak }, { it.lineIndex })
    val due = standings.filter { it.reason == SyllabusReason.DUE }.sortedWith(byPriority)
    val reviews = due.ifEmpty {
        // Nothing due — still offer a mix: the weakest resting line keeps retrieval warm.
        standings.filter { it.reason == SyllabusReason.FRESH }.sortedWith(byPriority).take(1)
    }
    val frontier = standings.filter { it.reason == SyllabusReason.NEW }
    return Syllabus(progression = this, lines = standings, sessionLines = reviews + frontier)
}

/**
 * Picks one due move for a below-line recall.
 *
 * Only moves with a recorded miss participate, and only once a line containing the
 * move is mastered: unfinished lines are already on the ordinary learning deal. Most
 * overdue wins, then weakest streak, then the most repeatedly missed move. The entry is
 * the move's parent, so the resulting branch round asks for the troublesome move rather
 * than dropping the learner after it.
 */
fun Progression.nodeReviewTargetAt(
    nowEpochMillis: Long,
    /** In one-sided recall, ignore moves Cheacher would auto-play for the opponent. */
    learnerColor: Color? = null,
): NodeReviewTarget? =
    tree.allNodes.mapNotNull { node ->
        if (learnerColor != null && node.mover != learnerColor) return@mapNotNull null
        val misses = record.missCounts[node.id] ?: return@mapNotNull null
        if (misses <= 0) return@mapNotNull null
        val lineIndex = tree.lines.indices.firstOrNull { index ->
            statusOf(index) == LineStatus.MASTERED && tree.lines[index].any { it.id == node.id }
        } ?: return@mapNotNull null
        val review = record.nodeReviews[node.id]
        val dueAt = ReviewLadder.dueAt(review)
        if (dueAt > nowEpochMillis) return@mapNotNull null
        NodeReviewTarget(
            nodeId = node.id,
            lineIndex = lineIndex,
            entryNodeId = node.parentId,
            dueAtEpochMillis = dueAt,
            streak = review?.streak ?: 0,
            missCount = misses,
        )
    }.minWithOrNull(
        compareBy<NodeReviewTarget>(
            { it.dueAtEpochMillis },
            { it.streak },
            { -it.missCount },
            { it.lineIndex },
        ),
    )

/**
 * The leaf of every line that runs through [nodeId] — the lines a miss at that node
 * touches. [TrainingRecord.ROOT_NODE_KEY] (a blunder before the first move) touches
 * every line.
 */
fun OpeningTree.leafIdsThrough(nodeId: String): List<String> =
    if (nodeId == TrainingRecord.ROOT_NODE_KEY) {
        lines.map { it.last().id }
    } else {
        lines.filter { line -> line.any { it.id == nodeId } }.map { it.last().id }
    }
