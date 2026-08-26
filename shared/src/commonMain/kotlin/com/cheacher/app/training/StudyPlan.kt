package com.cheacher.app.training

import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.progress.TrainingRecord
import kotlin.math.roundToInt

/** What a dealt session is for. */
enum class StudyKind {
    /**
     * Depth-first study of the active opening: the deal is its not-yet-accounted lines,
     * and the session keeps re-dealing imperfect ones until every line has been walked
     * unaided. Finishing a LEARN session *is* finishing the opening.
     */
    LEARN,

    /**
     * One pass over a finished opening, on the spacing ladder's schedule. The score is
     * the score — a clean look pushes the next one further out, a slip pulls the whole
     * opening back to the front of the queue.
     */
    REVIEW,
}

/**
 * One opening's standing on the shelf, derived from tree + record — the opening-level
 * sibling of [Progression]'s line ladder. "Accounted" is the credit rule: a line counts
 * 1.0 found unaided, 0.5 found with the hint, 0.0 after a wrong move, latest walk wins.
 */
class OpeningStanding(val tree: OpeningTree, val record: TrainingRecord) {
    /** Sum of every line's latest credit — "3½ of 6" is this over [OpeningTree.lines]. */
    val creditTotal: Double = tree.lines.sumOf { record.creditOf(it.last().id) }

    val percent: Int =
        if (tree.lines.isEmpty()) 0 else (creditTotal / tree.lines.size * 100).roundToInt()

    /** True once the opening has been fully accounted at least once — it lives on the review ladder now. */
    val learned: Boolean = record.openingReview != null

    /** When the ladder suggests the next look at this opening. Meaningful only when [learned]. */
    val dueAtEpochMillis: Long = ReviewLadder.dueAt(record.openingReview)

    /** Lines whose latest walk was not a clean, unaided pass — the LEARN deal, in DFS order. */
    val unaccountedLineIndices: List<Int> =
        tree.lines.indices.filter { record.creditOf(tree.lines[it].last().id) < 1.0 }

    /**
     * What a LEARN session should walk. Normally the unaccounted lines; in the rare gap
     * where every line reads clean but the finishing session never landed (quit at the
     * last board), the whole book — an empty deal would finish instantly and deal again,
     * forever.
     */
    val learnDeal: List<Int> = unaccountedLineIndices.ifEmpty { tree.lines.indices.toList() }
}

/** One dealt session: which opening, why, and (for LEARN) exactly which lines. */
data class StudyTask(
    val tree: OpeningTree,
    val kind: StudyKind,
    /** Null walks the whole book (reviews always do); otherwise exactly these lines. */
    val lineIndices: List<Int>?,
)

/**
 * Deals the study queue across the whole shelf: every due review first — most overdue,
 * then shakiest — and then the first unlearned opening, whose not-yet-accounted lines
 * are the deal. One opening at a time is the point: the shelf order is the curriculum,
 * and nothing on it is skipped or reordered by stats.
 *
 * Never empty while the shelf has content: when everything is learned and nothing is
 * due, the opening resting nearest its due date is dealt anyway — "come back later" is
 * scheduling *at* the learner, and Cheacher does not do that.
 */
fun studyPlan(
    trees: List<OpeningTree>,
    records: Map<String, TrainingRecord>,
    nowEpochMillis: Long,
): List<StudyTask> {
    val standings = trees
        .filter { it.lines.isNotEmpty() }
        .map { OpeningStanding(it, records[it.repertoire.id] ?: TrainingRecord.empty(it.repertoire.id)) }

    val byUrgency = compareBy<OpeningStanding>(
        { it.dueAtEpochMillis },
        { it.record.openingReview?.streak ?: 0 },
    )
    val due = standings
        .filter { it.learned && it.dueAtEpochMillis <= nowEpochMillis }
        .sortedWith(byUrgency)
        .map { StudyTask(it.tree, StudyKind.REVIEW, lineIndices = null) }

    val learn = standings.firstOrNull { !it.learned }
        ?.let { StudyTask(it.tree, StudyKind.LEARN, it.learnDeal) }

    val plan = due + listOfNotNull(learn)
    if (plan.isNotEmpty()) return plan

    // The whole shelf is learned and rested: keep retrieval warm with the nearest-due book.
    return standings.sortedWith(byUrgency).take(1)
        .map { StudyTask(it.tree, StudyKind.REVIEW, lineIndices = null) }
}
