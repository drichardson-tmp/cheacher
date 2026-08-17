package com.cheacher.app.training

import com.cheacher.app.data.SampleRepertoires
import com.cheacher.app.domain.OpeningTree
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Property test for the branch-recall reducer: hundreds of seeded random walks — real
 * moves, wrong moves, retreats — against the invariants no sequence of inputs may break.
 * Example-based tests prove the paths we thought of; this hunts the ones we didn't.
 * The seed is fixed, so a failure is reproducible, never flaky.
 */
class BranchSessionFuzzTest {
    private val trees = SampleRepertoires.all.map(OpeningTree::resolve)

    @Test
    fun random_walks_preserve_invariants_on_every_tree_and_policy() {
        val random = Random(20260814)
        for (tree in trees) {
            for (policy in MistakePolicy.entries) {
                repeat(25) {
                    fuzzOneRound(tree, policy, allowedNodeIds = null, random)
                }
            }
        }
    }

    @Test
    fun random_walks_preserve_invariants_under_a_progression_gate() {
        val random = Random(20260815)
        for (tree in trees) {
            // The tightest interesting gate: just the first line, like a fresh learner's round.
            val firstLineOnly = tree.lines.first().map { it.id }.toSet()
            for (policy in MistakePolicy.entries) {
                repeat(25) {
                    fuzzOneRound(tree, policy, allowedNodeIds = firstLineOnly, random)
                }
            }
        }
    }

    private fun fuzzOneRound(
        tree: OpeningTree,
        policy: MistakePolicy,
        allowedNodeIds: Set<String>?,
        random: Random,
    ) {
        var state = BranchState.start(tree, policy, autoReplyFor = null, allowedNodeIds = allowedNodeIds)
        val closedForever = mutableSetOf<String>()
        var steps = 0

        while (!state.finished && steps < 3_000) {
            steps++
            state = when (random.nextInt(10)) {
                // Retreat: allowed any time, never a penalty.
                0 -> state.backToJunction()
                // Noise: any legal chess move — usually wrong, occasionally right.
                in 1..3 -> state.submit(state.position.legalMoves().random(random))
                // Progress: one of the open repertoire moves.
                else -> state.openMoves.randomOrNull(random)?.let { state.submit(it.move) } ?: state
            }

            // Closed is forever: no input sequence may reopen a pruned branch.
            state.statuses.forEach { (id, status) -> if (status.isClosed) closedForever += id }
            closedForever.forEach { id ->
                val node = tree.node(id) ?: error("fuzz produced unknown node id $id")
                assertTrue(state.statusOf(node).isClosed, "reopened closed node $node after step $steps")
            }

            // The board always offers progress: an unfinished round has an open door.
            if (!state.finished) {
                assertTrue(state.openMoves.isNotEmpty(), "stranded with no open moves at ${state.cursor}")
            }

            // The gate holds: the cursor's whole path stays inside the allowed world.
            if (allowedNodeIds != null) {
                state.path.forEach { node ->
                    assertTrue(node.id in allowedNodeIds, "walked into locked node $node")
                }
            }

            // Strikes never exceed the policy's allowance.
            val maxStrikes = if (policy == MistakePolicy.ONE_ALLOWANCE) 1 else Int.MAX_VALUE
            assertTrue(state.strikes <= maxStrikes || state.cursorId == null, "strikes overflowed at $steps")
        }

        assertTrue(state.finished, "round did not terminate in $steps steps (${tree.repertoire.id}, $policy)")
        val progress = state.progress
        assertTrue(
            progress.closedLines == progress.totalLines,
            "finished round left ${progress.totalLines - progress.closedLines} lines open",
        )
    }
}
