package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Position
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.TreeNode

/**
 * Phase 1 — Named Concept Walkthrough.
 *
 * The learner is shown the *name* of the line the next move creates ("Sicilian Defence")
 * and has to find it on the board. Get it wrong and the one-sentence idea unlocks
 * underneath. Lines are walked root-to-leaf, one at a time, from both sides.
 *
 * Pure state: [submit] returns a new [GuidedState] and touches nothing else, so the whole
 * mode is testable without a board, a clock, or a coroutine.
 */
data class GuidedState(
    val tree: OpeningTree,
    /**
     * Which of [OpeningTree.lines] this session walks, in order. Null means the whole
     * book; progression mode passes the frontier line so the session is "replay the
     * shared prefix you know, then learn the new fork". Chosen at [start] and immutable
     * after — a session's syllabus does not change under the learner's feet.
     */
    val lineIndices: List<Int>? = null,
    val lineIndex: Int = 0,
    val plyIndex: Int = 0,
    /** True once the human-language hint has been unlocked for the current move. */
    val ideaRevealed: Boolean = false,
    val wrongAttempts: Int = 0,
    val lastEvent: GuidedEvent? = null,
    val finished: Boolean = false,
) {
    /** The lines this session walks, in authored order. */
    val lines: List<List<TreeNode>>
        get() = lineIndices?.map { tree.lines[it] } ?: tree.lines

    val currentLine: List<TreeNode> get() = lines.getOrElse(lineIndex) { emptyList() }

    /** The node the learner must play next, or null when the session is over. */
    val expected: TreeNode? get() = currentLine.getOrNull(plyIndex)

    /** Board state in front of the learner right now. */
    val position: Position
        get() = currentLine.getOrNull(plyIndex - 1)?.position ?: tree.root

    /** Moves already played in this line — the ones drawn on the move strip. */
    val played: List<TreeNode> get() = currentLine.take(plyIndex)

    val prompt: GuidedPrompt?
        get() = expected?.let {
            GuidedPrompt(
                name = it.name,
                idea = it.idea.takeIf { _ -> ideaRevealed && it.idea.isNotBlank() },
                mover = it.mover,
                moveNumberLabel = it.moveNumberLabel,
            )
        }

    val progress: GuidedProgress
        get() = GuidedProgress(
            lineNumber = (lineIndex + 1).coerceAtMost(lines.size),
            lineCount = lines.size,
            plyNumber = plyIndex,
            plyCount = currentLine.size,
        )

    companion object {
        /** [lineIndices] restricts the session to those lines; null walks the whole book. */
        fun start(tree: OpeningTree, lineIndices: List<Int>? = null): GuidedState {
            val state = GuidedState(tree = tree, lineIndices = lineIndices)
            return state.copy(finished = state.lines.isEmpty())
        }
    }
}

data class GuidedPrompt(
    val name: String,
    val idea: String?,
    val mover: Color,
    val moveNumberLabel: String,
)

data class GuidedProgress(
    val lineNumber: Int,
    val lineCount: Int,
    val plyNumber: Int,
    val plyCount: Int,
)

sealed interface GuidedEvent {
    /** The expected move was played. */
    data class Correct(val node: TreeNode) : GuidedEvent

    /** A legal but unwanted move. [idea] is non-null once the allowance has been spent. */
    data class Wrong(val played: Move, val expected: TreeNode, val idea: String?) : GuidedEvent

    /** The whole line was walked; the next line is now loaded. */
    data class LineComplete(val line: List<TreeNode>) : GuidedEvent

    data object SessionComplete : GuidedEvent
}

/**
 * Feeds a move into the walkthrough.
 *
 * A wrong move never advances the board — it unlocks the idea line and waits. That is the
 * whole pedagogical bet of phase 1: you should not be able to brute-force past a name.
 */
fun GuidedState.submit(move: Move): GuidedState {
    val target = expected ?: return copy(lastEvent = GuidedEvent.SessionComplete, finished = true)

    if (move != target.move) {
        return copy(
            wrongAttempts = wrongAttempts + 1,
            ideaRevealed = true,
            lastEvent = GuidedEvent.Wrong(move, target, target.idea.takeIf { it.isNotBlank() }),
        )
    }

    val nextPly = plyIndex + 1
    if (nextPly < currentLine.size) {
        return copy(
            plyIndex = nextPly,
            ideaRevealed = false,
            wrongAttempts = 0,
            lastEvent = GuidedEvent.Correct(target),
        )
    }

    val finishedLine = currentLine
    val nextLine = lineIndex + 1
    return if (nextLine < lines.size) {
        copy(
            lineIndex = nextLine,
            plyIndex = 0,
            ideaRevealed = false,
            wrongAttempts = 0,
            lastEvent = GuidedEvent.LineComplete(finishedLine),
        )
    } else {
        copy(
            plyIndex = nextPly,
            ideaRevealed = false,
            wrongAttempts = 0,
            finished = true,
            lastEvent = GuidedEvent.SessionComplete,
        )
    }
}

/** Spends the human-language allowance on demand, without costing a wrong attempt. */
fun GuidedState.revealIdea(): GuidedState = copy(ideaRevealed = true)

/** Drops the learner back to the start of the current line. */
fun GuidedState.restartLine(): GuidedState =
    copy(plyIndex = 0, ideaRevealed = false, wrongAttempts = 0, lastEvent = null)
