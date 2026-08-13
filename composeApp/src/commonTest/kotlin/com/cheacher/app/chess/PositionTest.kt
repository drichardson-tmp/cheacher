package com.cheacher.app.chess

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Counts leaf nodes of the legal-move tree — the classic move generator acid test. */
private fun perft(position: Position, depth: Int): Long =
    if (depth == 0) {
        1L
    } else {
        position.legalMoves().sumOf { perft(position.applyUnchecked(it), depth - 1) }
    }

class PositionTest {
    @Test
    fun perftFromTheStartingPosition() {
        val start = Position.INITIAL
        assertEquals(20L, perft(start, 1))
        assertEquals(400L, perft(start, 2))
        assertEquals(8902L, perft(start, 3))
    }

    @Test
    fun perftKiwipete() {
        // Kiwipete: the standard stress test for castling, en passant, pins and promotions.
        val position = Fen.parse("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        assertEquals(48L, perft(position, 1))
        assertEquals(2039L, perft(position, 2))
    }

    @Test
    fun bothCastlesAreGeneratedWhenLegal() {
        val position = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val castles = position.legalMoves().filter {
            it.from == Squares.parse("e1") && (it.to == Squares.parse("g1") || it.to == Squares.parse("c1"))
        }
        assertEquals(2, castles.size)
    }

    @Test
    fun castlingThroughAttackIsIllegal() {
        // Black rook on f8 covers f1: king side is off, queen side survives.
        val position = Fen.parse("r4rk1/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        val kingMoves = position.legalMoves().filter { it.from == Squares.parse("e1") }
        assertNull(kingMoves.firstOrNull { it.to == Squares.parse("g1") })
        assertNotNull(kingMoves.firstOrNull { it.to == Squares.parse("c1") })
    }

    @Test
    fun castlingRightsDieWithTheRook() {
        val position = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val captureA8 = Move(Squares.parse("a1")!!, Squares.parse("a8")!!)
        val after = position.applyUnchecked(captureA8)
        // Black lost queen side (rook captured), White lost queen side (rook moved away).
        assertFalse(after.castling.queenSide(Color.BLACK))
        assertFalse(after.castling.queenSide(Color.WHITE))
        assertTrue(after.castling.kingSide(Color.BLACK))
        assertTrue(after.castling.kingSide(Color.WHITE))
    }

    @Test
    fun enPassantIsGeneratedAndRemovesTheVictim() {
        val start = Position.INITIAL
        val moves = start.playLine("1. e4 Nf6 2. e5 d5")
        assertNotNull(moves)
        var position = start
        for (move in moves) position = position.applyUnchecked(move)

        val ep = position.moveFromSan("exd6")
        assertNotNull(ep, "en passant capture must resolve")
        val after = position.applyUnchecked(ep)
        assertNull(after[Squares.parse("d5")!!], "the passed pawn must be removed")
        assertEquals(Piece(PieceType.PAWN, Color.WHITE), after[Squares.parse("d6")!!])
    }

    @Test
    fun promotionsGenerateAllFourPieces() {
        val position = Fen.parse("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")
        val promotions = position.legalMoves().filter { it.from == Squares.parse("a7") }
        assertEquals(
            setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT),
            promotions.mapNotNull { it.promotion }.toSet(),
        )
    }

    @Test
    fun foolsMateIsCheckmate() {
        val start = Position.INITIAL
        val moves = start.playLine("1. f3 e5 2. g4 Qh4#")
        assertNotNull(moves)
        var position = start
        for (move in moves) position = position.applyUnchecked(move)
        assertTrue(position.isCheckmate())
        assertFalse(position.isStalemate())
    }

    @Test
    fun classicCornerStalemate() {
        // Ka8 against Kb6 + Qc7: not in check, nowhere to go.
        val position = Fen.parse("k7/2Q5/1K6/8/8/8/8/8 b - - 0 1")
        assertFalse(position.isInCheck())
        assertTrue(position.legalMoves().isEmpty())
        assertTrue(position.isStalemate())
        assertFalse(position.isCheckmate())
    }

    @Test
    fun applyMoveRejectsIllegalMoves() {
        val start = Position.INITIAL
        assertNull(start.applyMove(Move(Squares.parse("e2")!!, Squares.parse("e5")!!)))
        assertNotNull(start.applyMove(Move(Squares.parse("e2")!!, Squares.parse("e4")!!)))
    }
}
