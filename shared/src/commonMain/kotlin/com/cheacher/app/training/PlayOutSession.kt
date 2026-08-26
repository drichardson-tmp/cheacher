package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.PieceType
import com.cheacher.app.chess.Position
import com.cheacher.app.chess.Squares
import com.cheacher.app.chess.sanOf
import com.cheacher.app.chess.toFen
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.TreeNode

/**
 * The optional epilogue — the book ran out, now close out the whole game.
 *
 * Same architecture as the other two modes: a pure value, reduced by [play] for either
 * side. The engine is *not* in here; a ViewModel asks the engine and feeds its move
 * through the same reducer the learner's moves take, so legality, termination, and
 * history live in exactly one place and the whole endgame is testable without a search.
 */
data class PlayOutState(
    /** The side the learner played through the book — the side they keep. */
    val learnerSide: Color,
    /** The book prefix, drawn dimmed on the strip: this part was memory, not battle. */
    val bookMoves: List<PlayedMove>,
    /** Everything since the book ran out. */
    val freshMoves: List<PlayedMove> = emptyList(),
    val position: Position,
    /**
     * Position-key → times seen, for threefold repetition. Keys drop the move clocks
     * (FEN fields five and six) because repetition law cares about the *position*.
     */
    val repetitionCounts: Map<String, Int>,
    val outcome: PlayOutOutcome? = null,
) {
    val allMoves: List<PlayedMove> get() = bookMoves + freshMoves

    val lastMove: Move? get() = allMoves.lastOrNull()?.move

    val isLearnerTurn: Boolean get() = outcome == null && position.sideToMove == learnerSide

    val isEngineTurn: Boolean get() = outcome == null && position.sideToMove != learnerSide

    companion object {
        /**
         * Starts from the end of [leafId]'s line in [tree]. The repetition table is
         * seeded with every book position — a repetition that straddles the book
         * boundary is still a repetition.
         */
        fun start(tree: OpeningTree, leafId: String): PlayOutState {
            val line = tree.lines.firstOrNull { it.last().id == leafId }
                ?: error("no line in '${tree.repertoire.id}' ends at node '$leafId'")
            val counts = mutableMapOf(repetitionKey(tree.root) to 1)
            for (node in line) counts[repetitionKey(node.position)] = (counts[repetitionKey(node.position)] ?: 0) + 1
            return PlayOutState(
                learnerSide = tree.repertoire.perspective,
                bookMoves = line.map { it.asPlayed() },
                position = line.last().position,
                repetitionCounts = counts,
            )
        }

        fun repetitionKey(position: Position): String =
            position.toFen().split(" ").take(4).joinToString(" ")
    }
}

/** One move on the strip, book or fresh. */
data class PlayedMove(
    val move: Move,
    val san: String,
    val mover: Color,
    val moveNumberLabel: String,
)

private fun TreeNode.asPlayed() = PlayedMove(move, san, mover, moveNumberLabel)

enum class GameResult {
    LEARNER_WIN,
    DRAW,
    ENGINE_WIN;

    /** The learner's score for the Elo update. */
    val score: Double
        get() = when (this) {
            LEARNER_WIN -> 1.0
            DRAW -> 0.5
            ENGINE_WIN -> 0.0
        }
}

enum class EndReason {
    CHECKMATE,
    STALEMATE,
    THREEFOLD_REPETITION,
    FIFTY_MOVE_RULE,
    DEAD_POSITION,
    RESIGNATION,
}

data class PlayOutOutcome(val result: GameResult, val reason: EndReason)

/**
 * Applies one move for whichever side [PlayOutState.position] says is up. Illegal moves
 * return the state unchanged — the board only offers legal ones, and a misbehaving
 * engine must not corrupt the game.
 */
fun PlayOutState.play(move: Move): PlayOutState {
    if (outcome != null) return this
    val after = position.applyMove(move) ?: return this

    val san = position.sanOf(move)
    val played = PlayedMove(
        move = move,
        san = san,
        mover = position.sideToMove,
        moveNumberLabel = "${position.fullmoveNumber}${if (position.sideToMove == Color.WHITE) "." else "..."}",
    )
    val key = PlayOutState.repetitionKey(after)
    val counts = repetitionCounts + (key to (repetitionCounts[key] ?: 0) + 1)

    val next = copy(
        freshMoves = freshMoves + played,
        position = after,
        repetitionCounts = counts,
    )
    return next.copy(outcome = next.decideOutcome(seenCount = counts.getValue(key)))
}

/** The learner concedes. Always available, never punished beyond the Elo ledger. */
fun PlayOutState.resign(): PlayOutState =
    if (outcome != null) this else copy(outcome = PlayOutOutcome(GameResult.ENGINE_WIN, EndReason.RESIGNATION))

private fun PlayOutState.decideOutcome(seenCount: Int): PlayOutOutcome? {
    val mover = position.sideToMove.opposite // the side that just played
    return when {
        position.isCheckmate() -> PlayOutOutcome(
            if (mover == learnerSide) GameResult.LEARNER_WIN else GameResult.ENGINE_WIN,
            EndReason.CHECKMATE,
        )
        position.isStalemate() -> PlayOutOutcome(GameResult.DRAW, EndReason.STALEMATE)
        seenCount >= 3 -> PlayOutOutcome(GameResult.DRAW, EndReason.THREEFOLD_REPETITION)
        position.halfmoveClock >= 100 -> PlayOutOutcome(GameResult.DRAW, EndReason.FIFTY_MOVE_RULE)
        position.isDeadDraw() -> PlayOutOutcome(GameResult.DRAW, EndReason.DEAD_POSITION)
        else -> null
    }
}

/**
 * Insufficient mating material: bare kings, a lone minor piece, or same-coloured
 * bishops only. The classic FIDE dead positions — anything richer plays on.
 */
fun Position.isDeadDraw(): Boolean {
    val others = board.withIndex().filter { (_, piece) -> piece != null && piece.type != PieceType.KING }
    if (others.isEmpty()) return true
    if (others.size == 1) {
        val type = others.single().value!!.type
        return type == PieceType.KNIGHT || type == PieceType.BISHOP
    }
    if (others.all { it.value!!.type == PieceType.BISHOP }) {
        val shades = others.map { (square, _) -> (Squares.fileOf(square) + Squares.rankOf(square)) % 2 }
        return shades.distinct().size == 1
    }
    return false
}
