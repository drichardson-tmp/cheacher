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
 * **The entry rule — get there perfectly, once.** The toll is the road in and nothing
 * more: one walk down the trunk found unaided, no hint, no wrong move
 * ([TrainingRecord.trunkClears]). What happens *after* the Italian bishop is the
 * opening, and the opening is the thing you are here to study — being made to finish a
 * whole line before you are trusted to arrive would charge for the wrong skill.
 *
 * Losing it costs exactly one stumble on the road in, because the claim it stands on is
 * "you can get here without thinking" and a stumble is the counter-example. Re-earning
 * it costs one clean walk, the same as the first time.
 *
 * Spaced reviews deliberately ignore the entry (see `App`): a review asks whether you
 * still know the whole thing, and the whole thing includes getting there — which is
 * also where an entry that has gone stale is caught and handed back.
 */
class OpeningEntry(val tree: OpeningTree, val record: TrainingRecord) {
    /** The shared road in, whether or not it has been earned yet. */
    val trunk: List<TreeNode> = tree.trunk()

    /** True once the road in has been walked perfectly — the toll is paid. */
    val proven: Boolean = trunk.isNotEmpty() && record.trunkClears > 0

    /** The node sessions resume from, or null while the trunk is still being earned. */
    val entryNode: TreeNode? = if (proven) trunk.last() else null

    /** How many plies of every line are already behind you when a session opens. */
    val entryPly: Int = if (proven) trunk.size else 0

    /** The name to put on the entry — "Italian Game", the position sessions now open in. */
    val entryName: String? = entryNode?.name?.takeIf { it.isNotBlank() }
}

/** Ids of the shared road in, for asking whether a miss happened on the way to the opening. */
fun OpeningTree.trunkNodeIds(): Set<String> = trunk().mapTo(mutableSetOf()) { it.id }

/**
 * True when the move still to be found *at* [nodeId] — null or
 * [TrainingRecord.ROOT_NODE_KEY] meaning the starting position — is part of the road in.
 * Standing on the trunk's last node the answer is false: the next move is the fork, and
 * the fork is the opening rather than the way to it.
 */
fun OpeningTree.isReachingForRoadIn(nodeId: String?): Boolean {
    val trunk = trunk()
    if (trunk.isEmpty()) return false
    if (nodeId == null || nodeId == TrainingRecord.ROOT_NODE_KEY) return true
    return trunk.dropLast(1).any { it.id == nodeId }
}
