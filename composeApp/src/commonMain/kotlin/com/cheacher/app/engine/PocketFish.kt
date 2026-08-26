package com.cheacher.app.engine

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.PieceType
import com.cheacher.app.chess.Position
import kotlin.math.exp
import kotlin.random.Random

/**
 * The pocket-sized sparring engine: material + piece-square negamax with alpha-beta,
 * a few plies deep, choosing among root moves by softmax over their scores.
 *
 * It exists for two reasons. Platforms without a Stockfish binary (iOS today, an
 * emulator ABI tomorrow) still need an opponent; and *no* engine plays 700-Elo chess
 * honestly at full strength, so a small engine whose weakness is a tunable temperature
 * is closer to the actual product than a grandmaster in shackles. At high temperature it
 * plays plausible-but-loose club chess; at zero it is a solid low-club player.
 */
class PocketFish(private val random: Random) : SparringEngine {
    override val name: String = "PocketFish"

    override suspend fun bestMove(position: Position, tuning: EngineTuning): Move? {
        val moves = position.legalMoves()
        if (moves.isEmpty()) return null

        val depth = tuning.depth.coerceAtLeast(1)
        val scored = moves.map { move ->
            move to -negamax(position.applyUnchecked(move), depth - 1, -INFINITY, INFINITY, ply = 1)
        }

        val best = scored.maxOf { it.second }
        if (tuning.temperatureCp <= 1) {
            return scored.filter { it.second == best }.random(random).first
        }

        // Softmax over centipawn scores: a 700-Elo temperature makes a pawn's difference
        // barely matter; a cold one makes it decisive. Mate scores dwarf everything.
        val temperature = tuning.temperatureCp.toDouble()
        val weights = scored.map { (_, score) -> exp((score - best) / temperature) }
        var pick = random.nextDouble() * weights.sum()
        for (index in scored.indices) {
            pick -= weights[index]
            if (pick <= 0) return scored[index].first
        }
        return scored.last().first
    }

    private fun negamax(position: Position, depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        val moves = position.legalMoves()
        if (moves.isEmpty()) return if (position.isInCheck()) -(MATE - ply) else 0
        if (depth == 0) return evaluate(position)

        var alpha = alphaIn
        var best = -INFINITY
        for (move in moves.sortedByDescending { captureValue(position, it) }) {
            val score = -negamax(position.applyUnchecked(move), depth - 1, -beta, -alpha, ply + 1)
            if (score > best) best = score
            if (best > alpha) alpha = best
            if (alpha >= beta) break
        }
        return best
    }

    /** Rough MVV ordering: look at loud moves first so alpha-beta earns its keep. */
    private fun captureValue(position: Position, move: Move): Int =
        position[move.to]?.let { pieceValue(it.type) } ?: 0

    /** Centipawns from the side-to-move's perspective. */
    private fun evaluate(position: Position): Int {
        var score = 0
        for (square in position.board.indices) {
            val piece = position.board[square] ?: continue
            val sign = if (piece.color == position.sideToMove) 1 else -1
            val table = square.forColor(piece.color)
            score += sign * (pieceValue(piece.type) + pieceSquareBonus(piece.type, table))
        }
        return score
    }

    companion object {
        private const val INFINITY = 1_000_000
        private const val MATE = 100_000

        private fun pieceValue(type: PieceType): Int = when (type) {
            PieceType.PAWN -> 100
            PieceType.KNIGHT -> 320
            PieceType.BISHOP -> 330
            PieceType.ROOK -> 500
            PieceType.QUEEN -> 900
            PieceType.KING -> 0
        }

        /** PSTs are authored for White (a1 = index 0); Black reads the mirrored rank. */
        private fun Int.forColor(color: Color): Int = if (color == Color.WHITE) this else this xor 56

        private fun pieceSquareBonus(type: PieceType, square: Int): Int = when (type) {
            PieceType.PAWN -> PAWN_PST[square]
            PieceType.KNIGHT -> KNIGHT_PST[square]
            PieceType.BISHOP -> BISHOP_PST[square]
            PieceType.ROOK -> ROOK_PST[square]
            PieceType.QUEEN -> QUEEN_PST[square]
            PieceType.KING -> KING_PST[square]
        }

        // Rank 1 is the first row; values are the usual "centre good, edge sad" shapes,
        // kept coarse on purpose — nuance is Stockfish's job, temperament is ours.
        private val PAWN_PST = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, -20, -20, 10, 10, 5,
            5, -5, -10, 0, 0, -10, -5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, 5, 10, 25, 25, 10, 5, 5,
            10, 10, 20, 30, 30, 20, 10, 10,
            50, 50, 50, 50, 50, 50, 50, 50,
            0, 0, 0, 0, 0, 0, 0, 0,
        )
        private val KNIGHT_PST = intArrayOf(
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50,
        )
        private val BISHOP_PST = intArrayOf(
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 5, 5, 10, 10, 5, 5, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -20, -10, -10, -10, -10, -10, -10, -20,
        )
        private val ROOK_PST = intArrayOf(
            0, 0, 0, 5, 5, 0, 0, 0,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            5, 10, 10, 10, 10, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0,
        )
        private val QUEEN_PST = intArrayOf(
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -10, 5, 5, 5, 5, 5, 0, -10,
            0, 0, 5, 5, 5, 5, 0, -5,
            -5, 0, 5, 5, 5, 5, 0, -5,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -20, -10, -10, -5, -5, -10, -10, -20,
        )
        private val KING_PST = intArrayOf(
            20, 30, 10, 0, 0, 10, 30, 20,
            20, 20, 0, 0, 0, 0, 20, 20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
        )
    }
}
