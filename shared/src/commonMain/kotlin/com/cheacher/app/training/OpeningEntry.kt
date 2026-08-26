package com.cheacher.app.training

import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.TreeNode
import com.cheacher.app.progress.TrainingRecord

/**
 * The moves every line in a book shares — the road *into* the opening rather than any
 * part of the opening itself. In "The Italian Game" that is 1.e4 e5 2.Nf3 Nc6 3.Bc4:
 * the Pianissimo, the Evans, the Two Knights and the Hungarian all begin there.
 *
 * The final shared node is dropped when the book is a single line, so there is always
 * at least one move left to play; a session that starts on its own last move is not a
 * session.
 */
fun OpeningTree.trunk(): List<TreeNode> {
    val first = lines.firstOrNull() ?: return emptyList()
    val limit = lines.minOf { it.size } - 1
    var shared = 0
    while (shared < limit && lines.all { it[shared].id == first[shared].id }) shared++
    return first.take(shared)
}

/**
 * Where this book's next session should begin.
 *
 * Replaying 1.e4 e5 2.Nf3 Nc6 3.Bc4 before every Italian line is rehearsal the first
 * time and toll the tenth. So the trunk is a rung like any other: walk it, prove it,
 * and you are let in past it — sessions afterwards start *at* the Italian, with the
 * road in already on the move strip as context rather than as work.
 *
 * **The entry rule — one clean full iteration.** The trunk is proven once some line of
 * the book reads clean on its latest walk: [TrainingRecord.creditOf] of 1.0, which is
 * exactly "walked from the true starting position to a leaf, unaided, no misses". That
 * single fact carries both halves of the proof — you found your way *to* the opening,
 * and you went through one whole line *of* it.
 *
 * It is earned, and it is losable in the same breath: credits are latest-wins, so a
 * fumbled walk drops that line back under 1.0 and, once no line reads clean, the book
 * hands you back the full road from move one. Nothing is stored for any of this — it is
 * derived from the same credits the score is drawn from, the same way [Progression] and
 * [OpeningStanding] derive their ladders.
 *
 * Spaced reviews deliberately ignore the entry (see `App`): a review asks whether you
 * still know the whole thing, and the whole thing includes getting there.
 */
class OpeningEntry(val tree: OpeningTree, val record: TrainingRecord) {
    /** The shared road in, whether or not it has been earned yet. */
    val trunk: List<TreeNode> = tree.trunk()

    /** True once a full line has been walked clean and unaided — the toll is paid. */
    val proven: Boolean =
        trunk.isNotEmpty() && tree.lines.any { record.creditOf(it.last().id) >= 1.0 }

    /** The node sessions resume from, or null while the trunk is still being earned. */
    val entryNode: TreeNode? = if (proven) trunk.last() else null

    /** How many plies of every line are already behind you when a session opens. */
    val entryPly: Int = if (proven) trunk.size else 0

    /** The name to put on the entry — "Italian Game", the position sessions now open in. */
    val entryName: String? = entryNode?.name?.takeIf { it.isNotBlank() }
}
