package com.cheacher.app.chess

/**
 * Core value types for the board.
 *
 * Squares are plain `Int` indices 0..63 laid out a1 = 0, b1 = 1, ... h8 = 63, so
 * `file = index % 8` and `rank = index / 8`. Rank 0 is White's back rank.
 */

enum class Color {
    WHITE,
    BLACK;

    val opposite: Color get() = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val letter: Char) {
    PAWN('P'),
    KNIGHT('N'),
    BISHOP('B'),
    ROOK('R'),
    QUEEN('Q'),
    KING('K');

    companion object {
        fun fromLetter(c: Char): PieceType? = entries.firstOrNull { it.letter == c.uppercaseChar() }
    }
}

data class Piece(val type: PieceType, val color: Color) {
    /** Uppercase for White, lowercase for Black — the FEN convention. */
    val fenChar: Char
        get() = if (color == Color.WHITE) type.letter else type.letter.lowercaseChar()

    companion object {
        fun fromFenChar(c: Char): Piece? {
            val type = PieceType.fromLetter(c) ?: return null
            return Piece(type, if (c.isUpperCase()) Color.WHITE else Color.BLACK)
        }
    }
}

object Squares {
    const val COUNT = 64

    fun of(file: Int, rank: Int): Int = rank * 8 + file

    fun fileOf(square: Int): Int = square % 8

    fun rankOf(square: Int): Int = square / 8

    fun isOnBoard(file: Int, rank: Int): Boolean = file in 0..7 && rank in 0..7

    fun name(square: Int): String = "${'a' + fileOf(square)}${rankOf(square) + 1}"

    fun parse(name: String): Int? {
        if (name.length != 2) return null
        val file = name[0] - 'a'
        val rank = name[1] - '1'
        return if (isOnBoard(file, rank)) of(file, rank) else null
    }
}

data class Move(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
) {
    /** Long algebraic form, e.g. `e2e4` or `e7e8q`. Useful as a stable key. */
    val uci: String
        get() = Squares.name(from) + Squares.name(to) + (promotion?.letter?.lowercaseChar() ?: "")

    companion object {
        fun fromUci(uci: String): Move? {
            if (uci.length !in 4..5) return null
            val from = Squares.parse(uci.substring(0, 2)) ?: return null
            val to = Squares.parse(uci.substring(2, 4)) ?: return null
            val promotion = if (uci.length == 5) PieceType.fromLetter(uci[4]) else null
            return Move(from, to, promotion)
        }
    }
}

data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true,
) {
    fun kingSide(color: Color): Boolean = if (color == Color.WHITE) whiteKingSide else blackKingSide

    fun queenSide(color: Color): Boolean = if (color == Color.WHITE) whiteQueenSide else blackQueenSide

    fun withoutAll(color: Color): CastlingRights = if (color == Color.WHITE) {
        copy(whiteKingSide = false, whiteQueenSide = false)
    } else {
        copy(blackKingSide = false, blackQueenSide = false)
    }

    fun withoutKingSide(color: Color): CastlingRights = if (color == Color.WHITE) {
        copy(whiteKingSide = false)
    } else {
        copy(blackKingSide = false)
    }

    fun withoutQueenSide(color: Color): CastlingRights = if (color == Color.WHITE) {
        copy(whiteQueenSide = false)
    } else {
        copy(blackQueenSide = false)
    }

    val fen: String
        get() = buildString {
            if (whiteKingSide) append('K')
            if (whiteQueenSide) append('Q')
            if (blackKingSide) append('k')
            if (blackQueenSide) append('q')
            if (isEmpty()) append('-')
        }

    companion object {
        val NONE = CastlingRights(false, false, false, false)

        fun fromFen(field: String): CastlingRights = CastlingRights(
            whiteKingSide = field.contains('K'),
            whiteQueenSide = field.contains('Q'),
            blackKingSide = field.contains('k'),
            blackQueenSide = field.contains('q'),
        )
    }
}
