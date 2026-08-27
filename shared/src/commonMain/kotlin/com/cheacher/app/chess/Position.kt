package com.cheacher.app.chess

/**
 * An immutable chess position: the board plus everything FEN records about it.
 *
 * Applying a move returns a new [Position]; nothing here mutates. That makes the
 * repertoire tree cheap to walk in both directions — we never need an "unmake",
 * we just hold on to the parent position.
 */
data class Position(
    val board: List<Piece?>,
    val sideToMove: Color,
    val castling: CastlingRights,
    val enPassantSquare: Int?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
) {
    init {
        require(board.size == Squares.COUNT) { "board must have ${Squares.COUNT} squares, got ${board.size}" }
    }

    operator fun get(square: Int): Piece? = board[square]

    fun kingSquare(color: Color): Int? =
        board.indexOfFirst { it != null && it.type == PieceType.KING && it.color == color }
            .takeIf { it >= 0 }

    // ---------------------------------------------------------------- attacks

    /** True if [square] is attacked by any piece of [byColor]. Ignores pins and turn order. */
    fun isAttacked(square: Int, byColor: Color): Boolean {
        val file = Squares.fileOf(square)
        val rank = Squares.rankOf(square)

        // Pawns: look backwards from the target along the attacker's capture direction.
        val pawnRank = rank - if (byColor == Color.WHITE) 1 else -1
        for (df in intArrayOf(-1, 1)) {
            val f = file + df
            if (Squares.isOnBoard(f, pawnRank)) {
                val piece = board[Squares.of(f, pawnRank)]
                if (piece != null && piece.color == byColor && piece.type == PieceType.PAWN) return true
            }
        }

        if (hasStepAttacker(file, rank, KNIGHT_OFFSETS, PieceType.KNIGHT, byColor)) return true
        if (hasStepAttacker(file, rank, KING_OFFSETS, PieceType.KING, byColor)) return true
        if (hasSlidingAttacker(file, rank, ROOK_DIRECTIONS, PieceType.ROOK, byColor)) return true
        if (hasSlidingAttacker(file, rank, BISHOP_DIRECTIONS, PieceType.BISHOP, byColor)) return true

        return false
    }

    private fun hasStepAttacker(
        file: Int,
        rank: Int,
        offsets: Array<IntArray>,
        type: PieceType,
        byColor: Color,
    ): Boolean = offsets.any { (df, dr) ->
        val f = file + df
        val r = rank + dr
        if (!Squares.isOnBoard(f, r)) {
            false
        } else {
            val piece = board[Squares.of(f, r)]
            piece != null && piece.color == byColor && piece.type == type
        }
    }

    /** Queens slide like both rooks and bishops, so [type] is checked alongside [PieceType.QUEEN]. */
    private fun hasSlidingAttacker(
        file: Int,
        rank: Int,
        directions: Array<IntArray>,
        type: PieceType,
        byColor: Color,
    ): Boolean {
        for ((df, dr) in directions) {
            var f = file + df
            var r = rank + dr
            while (Squares.isOnBoard(f, r)) {
                val piece = board[Squares.of(f, r)]
                if (piece != null) {
                    if (piece.color == byColor && (piece.type == type || piece.type == PieceType.QUEEN)) return true
                    break
                }
                f += df
                r += dr
            }
        }
        return false
    }

    fun isInCheck(color: Color = sideToMove): Boolean {
        val king = kingSquare(color) ?: return false
        return isAttacked(king, color.opposite)
    }

    // ------------------------------------------------------------ move making

    /**
     * Applies [move] without checking that it is legal.
     *
     * [legalMoves] uses this to test each candidate for king safety; callers outside
     * the generator should prefer [applyMove].
     */
    fun applyUnchecked(move: Move): Position {
        val moving = board[move.from] ?: error("no piece on ${Squares.name(move.from)}")
        val captured = board[move.to]
        val next = board.toMutableList()

        next[move.from] = null
        next[move.to] = if (move.promotion != null) Piece(move.promotion, moving.color) else moving

        val forward = if (moving.color == Color.WHITE) 1 else -1
        var isEnPassantCapture = false

        if (moving.type == PieceType.PAWN && move.to == enPassantSquare && captured == null &&
            Squares.fileOf(move.from) != Squares.fileOf(move.to)
        ) {
            isEnPassantCapture = true
            next[move.to - forward * 8] = null
        }

        if (moving.type == PieceType.KING) {
            val delta = Squares.fileOf(move.to) - Squares.fileOf(move.from)
            if (delta == 2) { // king side: rook h-file -> f-file
                val rank = Squares.rankOf(move.from)
                next[Squares.of(5, rank)] = next[Squares.of(7, rank)]
                next[Squares.of(7, rank)] = null
            } else if (delta == -2) { // queen side: rook a-file -> d-file
                val rank = Squares.rankOf(move.from)
                next[Squares.of(3, rank)] = next[Squares.of(0, rank)]
                next[Squares.of(0, rank)] = null
            }
        }

        var rights = castling
        if (moving.type == PieceType.KING) rights = rights.withoutAll(moving.color)
        if (moving.type == PieceType.ROOK) rights = rights.withoutRookOn(move.from)
        // A rook captured on its home square loses that side's rights too.
        rights = rights.withoutRookOn(move.to)

        val doubleStep = moving.type == PieceType.PAWN &&
            kotlin.math.abs(Squares.rankOf(move.to) - Squares.rankOf(move.from)) == 2
        val nextEnPassant = if (doubleStep) move.from + forward * 8 else null

        val resetClock = moving.type == PieceType.PAWN || captured != null || isEnPassantCapture

        return Position(
            board = next,
            sideToMove = sideToMove.opposite,
            castling = rights,
            enPassantSquare = nextEnPassant,
            halfmoveClock = if (resetClock) 0 else halfmoveClock + 1,
            fullmoveNumber = if (sideToMove == Color.BLACK) fullmoveNumber + 1 else fullmoveNumber,
        )
    }

    /** Applies [move] if it is legal in this position, otherwise returns null. */
    fun applyMove(move: Move): Position? =
        if (legalMoves().any { it == move }) applyUnchecked(move) else null

    // --------------------------------------------------------- move generation

    fun legalMoves(): List<Move> = pseudoLegalMoves().filter { move ->
        val after = applyUnchecked(move)
        val king = after.kingSquare(sideToMove) ?: return@filter false
        !after.isAttacked(king, sideToMove.opposite)
    }

    fun pseudoLegalMoves(): List<Move> {
        val moves = mutableListOf<Move>()
        for (square in 0 until Squares.COUNT) {
            val piece = board[square] ?: continue
            if (piece.color != sideToMove) continue
            when (piece.type) {
                PieceType.PAWN -> generatePawnMoves(square, piece.color, moves)
                PieceType.KNIGHT -> generateStepMoves(square, piece.color, KNIGHT_OFFSETS, moves)
                PieceType.KING -> {
                    generateStepMoves(square, piece.color, KING_OFFSETS, moves)
                    generateCastles(square, piece.color, moves)
                }
                PieceType.BISHOP -> generateSlidingMoves(square, piece.color, BISHOP_DIRECTIONS, moves)
                PieceType.ROOK -> generateSlidingMoves(square, piece.color, ROOK_DIRECTIONS, moves)
                PieceType.QUEEN -> generateSlidingMoves(square, piece.color, ALL_DIRECTIONS, moves)
            }
        }
        return moves
    }

    private fun generatePawnMoves(from: Int, color: Color, out: MutableList<Move>) {
        val forward = if (color == Color.WHITE) 1 else -1
        val startRank = if (color == Color.WHITE) 1 else 6
        val file = Squares.fileOf(from)
        val rank = Squares.rankOf(from)

        val oneUp = rank + forward
        if (Squares.isOnBoard(file, oneUp) && board[Squares.of(file, oneUp)] == null) {
            addPawnMove(from, Squares.of(file, oneUp), color, out)
            val twoUp = rank + 2 * forward
            if (rank == startRank && board[Squares.of(file, twoUp)] == null) {
                out += Move(from, Squares.of(file, twoUp))
            }
        }

        for (df in intArrayOf(-1, 1)) {
            val f = file + df
            if (!Squares.isOnBoard(f, oneUp)) continue
            val target = Squares.of(f, oneUp)
            val occupant = board[target]
            if (occupant != null && occupant.color != color) {
                addPawnMove(from, target, color, out)
            } else if (occupant == null && target == enPassantSquare) {
                out += Move(from, target)
            }
        }
    }

    private fun addPawnMove(from: Int, to: Int, color: Color, out: MutableList<Move>) {
        val lastRank = if (color == Color.WHITE) 7 else 0
        if (Squares.rankOf(to) == lastRank) {
            for (type in PROMOTION_TYPES) out += Move(from, to, type)
        } else {
            out += Move(from, to)
        }
    }

    private fun generateStepMoves(from: Int, color: Color, offsets: Array<IntArray>, out: MutableList<Move>) {
        val file = Squares.fileOf(from)
        val rank = Squares.rankOf(from)
        for ((df, dr) in offsets) {
            val f = file + df
            val r = rank + dr
            if (!Squares.isOnBoard(f, r)) continue
            val target = Squares.of(f, r)
            if (board[target]?.color != color) out += Move(from, target)
        }
    }

    private fun generateSlidingMoves(from: Int, color: Color, directions: Array<IntArray>, out: MutableList<Move>) {
        val file = Squares.fileOf(from)
        val rank = Squares.rankOf(from)
        for ((df, dr) in directions) {
            var f = file + df
            var r = rank + dr
            while (Squares.isOnBoard(f, r)) {
                val target = Squares.of(f, r)
                val occupant = board[target]
                if (occupant == null) {
                    out += Move(from, target)
                } else {
                    if (occupant.color != color) out += Move(from, target)
                    break
                }
                f += df
                r += dr
            }
        }
    }

    private fun generateCastles(from: Int, color: Color, out: MutableList<Move>) {
        val homeRank = if (color == Color.WHITE) 0 else 7
        if (from != Squares.of(4, homeRank)) return
        if (isAttacked(from, color.opposite)) return

        if (castling.kingSide(color) &&
            board[Squares.of(7, homeRank)] == Piece(PieceType.ROOK, color) &&
            board[Squares.of(5, homeRank)] == null &&
            board[Squares.of(6, homeRank)] == null &&
            !isAttacked(Squares.of(5, homeRank), color.opposite)
        ) {
            out += Move(from, Squares.of(6, homeRank))
        }

        if (castling.queenSide(color) &&
            board[Squares.of(0, homeRank)] == Piece(PieceType.ROOK, color) &&
            board[Squares.of(1, homeRank)] == null &&
            board[Squares.of(2, homeRank)] == null &&
            board[Squares.of(3, homeRank)] == null &&
            !isAttacked(Squares.of(3, homeRank), color.opposite)
        ) {
            out += Move(from, Squares.of(2, homeRank))
        }
    }

    // -------------------------------------------------------------- terminal states

    fun isCheckmate(): Boolean = isInCheck() && legalMoves().isEmpty()

    fun isStalemate(): Boolean = !isInCheck() && legalMoves().isEmpty()

    companion object {
        val KNIGHT_OFFSETS = arrayOf(
            intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
            intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2),
        )
        val KING_OFFSETS = arrayOf(
            intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 0), intArrayOf(1, -1),
            intArrayOf(0, -1), intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1),
        )
        val ROOK_DIRECTIONS = arrayOf(
            intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(-1, 0),
        )
        val BISHOP_DIRECTIONS = arrayOf(
            intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, -1), intArrayOf(-1, 1),
        )
        val ALL_DIRECTIONS = ROOK_DIRECTIONS + BISHOP_DIRECTIONS

        val PROMOTION_TYPES = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)

        /** The standard starting position. */
        val INITIAL: Position get() = Fen.parse(Fen.START)
    }
}

private fun CastlingRights.withoutRookOn(square: Int): CastlingRights = when (square) {
    Squares.of(0, 0) -> withoutQueenSide(Color.WHITE)
    Squares.of(7, 0) -> withoutKingSide(Color.WHITE)
    Squares.of(0, 7) -> withoutQueenSide(Color.BLACK)
    Squares.of(7, 7) -> withoutKingSide(Color.BLACK)
    else -> this
}
