package com.cheacher.app.chess

/** Forsyth–Edwards Notation: the wire format for a position in repertoire JSON. */
object Fen {
    const val START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun parse(fen: String): Position = parseOrNull(fen) ?: error("invalid FEN: $fen")

    fun parseOrNull(fen: String): Position? {
        val fields = fen.trim().split(" ").filter { it.isNotEmpty() }
        if (fields.size !in 4..6) return null

        val board = arrayOfNulls<Piece>(Squares.COUNT)
        val ranks = fields[0].split("/")
        if (ranks.size != 8) return null

        // FEN lists rank 8 first; our rank 0 is White's back rank.
        ranks.forEachIndexed { rowFromTop, row ->
            val rank = 7 - rowFromTop
            var file = 0
            for (c in row) {
                when {
                    c in '1'..'8' -> file += c - '0'
                    else -> {
                        val piece = Piece.fromFenChar(c) ?: return null
                        if (file > 7) return null
                        board[Squares.of(file, rank)] = piece
                        file++
                    }
                }
            }
            if (file != 8) return null
        }

        val sideToMove = when (fields[1]) {
            "w" -> Color.WHITE
            "b" -> Color.BLACK
            else -> return null
        }

        val castling = CastlingRights.fromFenOrNull(fields[2]) ?: return null
        val enPassantSquare = when (val field = fields[3]) {
            "-" -> null
            else -> {
                val square = Squares.parse(field) ?: return null
                val expectedRank = if (sideToMove == Color.WHITE) 5 else 2
                if (Squares.rankOf(square) != expectedRank) return null
                square
            }
        }
        val halfmoveClock = fields.getOrNull(4)?.toIntOrNull() ?: if (fields.size >= 5) return null else 0
        val fullmoveNumber = fields.getOrNull(5)?.toIntOrNull() ?: if (fields.size >= 6) return null else 1
        if (halfmoveClock < 0 || fullmoveNumber < 1) return null

        return Position(
            board = board.toList(),
            sideToMove = sideToMove,
            castling = castling,
            enPassantSquare = enPassantSquare,
            halfmoveClock = halfmoveClock,
            fullmoveNumber = fullmoveNumber,
        )
    }

    fun format(position: Position): String = buildString {
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val piece = position[Squares.of(file, rank)]
                if (piece == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        append(empty)
                        empty = 0
                    }
                    append(piece.fenChar)
                }
            }
            if (empty > 0) append(empty)
            if (rank > 0) append('/')
        }
        append(' ')
        append(if (position.sideToMove == Color.WHITE) 'w' else 'b')
        append(' ')
        append(position.castling.fen)
        append(' ')
        append(position.enPassantSquare?.let { Squares.name(it) } ?: "-")
        append(' ')
        append(position.halfmoveClock)
        append(' ')
        append(position.fullmoveNumber)
    }
}

fun Position.toFen(): String = Fen.format(this)
