package com.cheacher.app.chess

/**
 * Standard Algebraic Notation.
 *
 * Parsing works by generating every legal move and comparing rendered SAN rather than
 * by decoding the string directly. It is a little slower and a lot harder to get wrong,
 * and at repertoire sizes (tens of moves, not millions) the difference is invisible.
 */

/** Renders [move] as SAN in this position, including check/mate suffix. */
fun Position.sanOf(move: Move): String {
    val piece = this[move.from] ?: error("no piece on ${Squares.name(move.from)}")
    val body = when {
        piece.type == PieceType.KING && Squares.fileOf(move.to) - Squares.fileOf(move.from) == 2 -> "O-O"
        piece.type == PieceType.KING && Squares.fileOf(move.to) - Squares.fileOf(move.from) == -2 -> "O-O-O"
        piece.type == PieceType.PAWN -> pawnSan(move)
        else -> pieceSan(piece, move)
    }
    val after = applyUnchecked(move)
    val suffix = when {
        after.isCheckmate() -> "#"
        after.isInCheck() -> "+"
        else -> ""
    }
    return body + suffix
}

private fun Position.pawnSan(move: Move): String {
    val isCapture = Squares.fileOf(move.from) != Squares.fileOf(move.to)
    val promotion = move.promotion?.let { "=${it.letter}" } ?: ""
    val prefix = if (isCapture) "${'a' + Squares.fileOf(move.from)}x" else ""
    return prefix + Squares.name(move.to) + promotion
}

private fun Position.pieceSan(piece: Piece, move: Move): String {
    val capture = if (this[move.to] != null) "x" else ""
    return "${piece.type.letter}${disambiguation(piece, move)}$capture${Squares.name(move.to)}"
}

/** The shortest file/rank/square hint that separates [move] from same-piece rivals. */
private fun Position.disambiguation(piece: Piece, move: Move): String {
    val rivals = legalMoves().filter { other ->
        other.from != move.from &&
            other.to == move.to &&
            this[other.from]?.let { it.type == piece.type && it.color == piece.color } == true
    }
    if (rivals.isEmpty()) return ""

    val fileClashes = rivals.any { Squares.fileOf(it.from) == Squares.fileOf(move.from) }
    val rankClashes = rivals.any { Squares.rankOf(it.from) == Squares.rankOf(move.from) }
    return when {
        !fileClashes -> "${'a' + Squares.fileOf(move.from)}"
        !rankClashes -> "${Squares.rankOf(move.from) + 1}"
        else -> Squares.name(move.from)
    }
}

/**
 * Resolves [san] against the legal moves here. Accepts sloppy input: decorations
 * (`!`, `?`), missing or wrong check marks, `0-0` for castling, and raw UCI (`e2e4`).
 */
fun Position.moveFromSan(san: String): Move? {
    val wanted = normalizeSan(san)
    if (wanted.isEmpty()) return null
    val legal = legalMoves()
    return legal.firstOrNull { normalizeSan(sanOf(it)) == wanted }
        ?: legal.firstOrNull { it.uci == san.trim().lowercase() }
}

/** Strips everything that does not change which move is meant. */
private fun normalizeSan(san: String): String = san.trim()
    .removeSuffix("e.p.")
    .filterNot { it in "+#!?x " }
    .replace('0', 'O')

/**
 * Walks a space-separated SAN line (move numbers optional) from this position.
 *
 * Returns null if any token fails to resolve — a repertoire with a typo should fail
 * loudly at load time, not silently teach the wrong move.
 */
fun Position.playLine(line: String): List<Move>? {
    var position = this
    val moves = mutableListOf<Move>()
    for (token in line.split(' ', '\n', '\t').filter { it.isNotBlank() }) {
        if (token.first().isDigit() && token.any { it == '.' }) continue // "1." / "1..."
        if (token in PGN_RESULTS) continue
        val move = position.moveFromSan(token) ?: return null
        moves += move
        position = position.applyUnchecked(move)
    }
    return moves
}

private val PGN_RESULTS = setOf("1-0", "0-1", "1/2-1/2", "*")
