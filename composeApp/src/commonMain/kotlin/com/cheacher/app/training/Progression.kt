package com.cheacher.app.training

import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.TreeNode
import com.cheacher.app.progress.TrainingRecord

/** Where a line stands on the ladder. */
enum class LineStatus {
    /** Named once, recalled once. This rung is behind you. */
    MASTERED,

    /** Open for study — the frontier. */
    UNLOCKED,

    /** Not yet earned. Ghosted on the tree, absent from the board. */
    LOCKED,
}

/**
 * Depth-first progression: the repertoire is conquered one branch at a time.
 *
 * [OpeningTree.lines] is already in DFS order, and consecutive DFS lines share the
 * longest possible prefix — so walking the lines strictly in order means every newly
 * unlocked line is "everything you already know, plus one new fork at the deepest open
 * junction". This value derives the whole ladder from the tree and the persisted
 * [TrainingRecord]; nothing new is stored.
 *
 * **The mastery rule — name it once, recall it once.** A line is mastered when its leaf
 * has at least one guided completion (you found every move from its name) *and* at least
 * one branch completion (you found every move from nothing). One of each, not a streak
 * count, because the rule has to be legible to the learner mid-session: "learn the
 * names, then prune it, and the next branch opens".
 *
 * **The chain rule.** Line 0 is always unlocked; line k+1 unlocks only when line k is
 * mastered. Stats a learner banked beyond the frontier (full-tree mode exists) do not
 * jump the queue — the ladder is the ladder — but they are not lost either: the moment
 * the frontier reaches an already-proven line it counts instantly and the frontier
 * rolls on.
 */
class Progression(val tree: OpeningTree, val record: TrainingRecord) {
    private val masteredByLine: List<Boolean> = tree.lines.map { line ->
        val leafId = line.last().id
        record.guidedCompletionsOf(leafId) >= 1 && record.branchCompletionsOf(leafId) >= 1
    }

    /** First line the chain has not mastered, or null once the whole book is behind you. */
    val frontierIndex: Int? = masteredByLine.indexOfFirst { !it }.takeIf { it >= 0 }

    val lineStatuses: List<LineStatus> = List(tree.lines.size) { index ->
        when {
            frontierIndex == null || index < frontierIndex -> LineStatus.MASTERED
            index == frontierIndex -> LineStatus.UNLOCKED
            else -> LineStatus.LOCKED
        }
    }

    val masteredCount: Int = lineStatuses.count { it == LineStatus.MASTERED }

    fun statusOf(lineIndex: Int): LineStatus = lineStatuses[lineIndex]

    /** Every node on a mastered or frontier line — the world branch recall may show. */
    val unlockedNodeIds: Set<String> = buildSet {
        tree.lines.forEachIndexed { index, line ->
            if (lineStatuses[index] != LineStatus.LOCKED) line.forEach { add(it.id) }
        }
    }

    /**
     * What a progression-mode guided session should walk: just the frontier line — its
     * shared prefix *is* the review of everything mastered before it — or, once the whole
     * book is mastered, every line again as pure review.
     */
    val guidedLineIndices: List<Int> =
        frontierIndex?.let { listOf(it) } ?: tree.lines.indices.toList()

    /**
     * The first node of [lineIndex]'s line that the previous line does not share — the
     * fork that makes this line news. For line 0 that is simply the first move.
     */
    fun divergenceNode(lineIndex: Int): TreeNode {
        val line = tree.lines[lineIndex]
        if (lineIndex == 0) return line.first()
        val previous = tree.lines[lineIndex - 1]
        val firstFresh = line.indexOfFirst { node ->
            previous.getOrNull(node.depth)?.id != node.id
        }
        return line[if (firstFresh >= 0) firstFresh else line.lastIndex]
    }

    /** The name worth announcing next — the frontier line's fork. */
    val nextUpName: String? = frontierIndex?.let { divergenceNode(it).name }
}

/** What changed when the frontier moved. Exactly one of the two fields is the headline. */
data class ProgressionAdvance(
    /** The fork that just opened, or null when there is nothing left to open. */
    val unlockedLine: TreeNode?,
    /** True when the last rung was climbed: every line in the book is mastered. */
    val repertoireMastered: Boolean,
)

/**
 * Compares two ladders over the same tree and reports a frontier move, or null when
 * nothing advanced. Pure, so the "new branch unlocked" moment is a testable value —
 * the ViewModels only decide when to show it.
 */
fun Progression.advanceFrom(previous: Progression): ProgressionAdvance? {
    val before = previous.frontierIndex ?: return null
    val after = frontierIndex
    if (after != null && after <= before) return null
    return ProgressionAdvance(
        unlockedLine = after?.let { divergenceNode(it) },
        repertoireMastered = after == null,
    )
}
