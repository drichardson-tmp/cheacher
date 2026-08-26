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
 * Every walked line banks a **credit**: 1.0 found unaided, 0.5 found with the hint
 * (asked for or earned by a miss — a wrong move reveals the idea, so it can never score
 * above a hint), 0.0 after a wrong move. With [masteryLoop] on, finishing the deal is
 * not finishing the session: imperfect lines are re-dealt, pass after pass, until every
 * line has one clean unaided walk — the depth-first traversal unwinds until everything
 * is accounted for.
 *
 * Pure state: [submit] returns a new [GuidedState] and touches nothing else, so the whole
 * mode is testable without a board, a clock, or a coroutine.
 */
data class GuidedState(
    val tree: OpeningTree,
    /**
     * Which of [OpeningTree.lines] this session is about, in order. Null means the whole
     * book; the study plan passes the not-yet-accounted lines. Chosen at [start] and
     * immutable after — a session's syllabus does not change under the learner's feet.
     */
    val lineIndices: List<Int>? = null,
    /** True keeps re-dealing imperfect lines until every dealt line has a clean pass. */
    val masteryLoop: Boolean = false,
    /** The current pass's absolute line indices. The first pass is the whole [deal]. */
    val passLines: List<Int> = emptyList(),
    /**
     * Plies of the shared trunk already behind the learner when a line opens — see
     * [OpeningEntry]. Zero walks every line from the true starting position, which is
     * how a book behaves until its road in has been earned.
     */
    val entryPly: Int = 0,
    val lineIndex: Int = 0,
    val plyIndex: Int = 0,
    /** True once the human-language hint has been unlocked for the current move. */
    val ideaRevealed: Boolean = false,
    val wrongAttempts: Int = 0,
    /** True once the hint has been seen anywhere on the current line's walk — the ½-point flag. */
    val lineAided: Boolean = false,
    /** True once a wrong move has been played anywhere on the current line's walk. */
    val lineMissed: Boolean = false,
    /** Absolute line index → the credit its most recent walk earned this session. */
    val lineCredits: Map<Int, Double> = emptyMap(),
    val lastEvent: GuidedEvent? = null,
    val finished: Boolean = false,
) {
    /** Every line this session is responsible for — the score's denominator. */
    val deal: List<Int> get() = lineIndices ?: tree.lines.indices.toList()

    /** The lines of the current pass, in authored (DFS) order. */
    val lines: List<List<TreeNode>>
        get() = passLines.map { tree.lines[it] }

    val currentLine: List<TreeNode> get() = lines.getOrElse(lineIndex) { emptyList() }

    /** The node the learner must play next, or null when the session is over. */
    val expected: TreeNode? get() = currentLine.getOrNull(plyIndex)

    /** Board state in front of the learner right now. */
    val position: Position
        get() = currentLine.getOrNull(plyIndex - 1)?.position ?: tree.root

    /** Moves already played in this line — the ones drawn on the move strip. */
    val played: List<TreeNode> get() = currentLine.take(plyIndex)

    /** What the walk of the current line has earned so far, by the credit rule. */
    val currentLineCredit: Double
        get() = when {
            lineMissed -> 0.0
            lineAided -> 0.5
            else -> 1.0
        }

    /** Session score over the whole [deal] — "3½ of 6 learned" in the UI. */
    val sessionScore: Double get() = deal.sumOf { lineCredits[it] ?: 0.0 }

    /** True when every dealt line's latest walk this session was clean and unaided. */
    val allClean: Boolean get() = deal.all { (lineCredits[it] ?: 0.0) >= 1.0 }

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
            // Measured from the entry, not from move one: the trunk is context on the
            // move strip, and a bar that opens at 40% claims work that was not done here.
            plyNumber = (plyIndex - entryPly).coerceAtLeast(0),
            plyCount = (currentLine.size - entryPly).coerceAtLeast(0),
        )

    companion object {
        /**
         * [lineIndices] restricts the session to those lines; null walks the whole book.
         * [entryPly] skips that many shared opening plies on every line — clamped so the
         * shortest dealt line still has a move left to play, since a caller's earned entry
         * is a suggestion and the deal is the authority on what is walkable.
         */
        fun start(
            tree: OpeningTree,
            lineIndices: List<Int>? = null,
            masteryLoop: Boolean = false,
            entryPly: Int = 0,
        ): GuidedState {
            val deal = lineIndices ?: tree.lines.indices.toList()
            val shortest = deal.minOfOrNull { tree.lines[it].size } ?: 0
            val entry = entryPly.coerceIn(0, (shortest - 1).coerceAtLeast(0))
            return GuidedState(
                tree = tree,
                lineIndices = lineIndices,
                masteryLoop = masteryLoop,
                passLines = deal,
                entryPly = entry,
                plyIndex = entry,
                finished = deal.isEmpty(),
            )
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

    /** The whole line was walked and banked [credit]; the next line is now loaded. */
    data class LineComplete(val line: List<TreeNode>, val credit: Double = 1.0) : GuidedEvent

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
            lineAided = true,
            lineMissed = true,
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

    // Line finished: bank its credit, then load the next line — of this pass, or of the
    // re-deal the mastery loop owes.
    val finishedLine = currentLine
    val credit = currentLineCredit
    val banked = lineCredits + (passLines[lineIndex] to credit)
    val walkedNext = copy(
        plyIndex = entryPly,
        ideaRevealed = false,
        wrongAttempts = 0,
        lineAided = false,
        lineMissed = false,
        lineCredits = banked,
        lastEvent = GuidedEvent.LineComplete(finishedLine, credit),
    )

    if (lineIndex + 1 < lines.size) return walkedNext.copy(lineIndex = lineIndex + 1)

    // End of the pass. The loop re-deals every line still short of a clean walk; the
    // re-dealt walk starts fresh, so the next pass is the clean shot.
    val retry = if (masteryLoop) deal.filter { (banked[it] ?: 0.0) < 1.0 } else emptyList()
    return if (retry.isNotEmpty()) {
        walkedNext.copy(passLines = retry, lineIndex = 0)
    } else {
        walkedNext.copy(
            plyIndex = nextPly,
            finished = true,
            lastEvent = GuidedEvent.SessionComplete,
        )
    }
}

/**
 * Spends the human-language allowance on demand, without costing a wrong attempt — but
 * the walk is aided now, and an aided line banks half a point.
 */
fun GuidedState.revealIdea(): GuidedState = copy(ideaRevealed = true, lineAided = true)

/**
 * Drops the learner back to the start of the current line. The board resets; the walk's
 * aided/missed flags do not — you have seen the answers, so the clean shot is the
 * re-deal, not the rewind.
 */
fun GuidedState.restartLine(): GuidedState =
    copy(plyIndex = entryPly, ideaRevealed = false, wrongAttempts = 0, lastEvent = null)
