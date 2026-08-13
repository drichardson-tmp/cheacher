package com.cheacher.app.chess

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FenTest {
    @Test
    fun startPositionRoundTrips() {
        assertEquals(Fen.START, Fen.format(Fen.parse(Fen.START)))
    }

    @Test
    fun typicalMiddlegameFensRoundTrip() {
        val fens = listOf(
            // Italian
            "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
            // Najdorf tabiya
            "rnbqkb1r/1p2pppp/p2p1n2/8/3NP3/2N5/PPP2PPP/R1BQKB1R w KQkq - 0 6",
            // En passant square set
            "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 2",
            // No castling anywhere
            "4k3/8/8/8/8/8/8/4K2R b - - 12 40",
        )
        for (fen in fens) {
            assertEquals(fen, Fen.format(Fen.parse(fen)), "round trip failed for $fen")
        }
    }

    @Test
    fun parsedFieldsAreCorrect() {
        val position = Fen.parse("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR b KQkq e3 5 12")
        assertEquals(Color.BLACK, position.sideToMove)
        assertEquals(Squares.parse("e3"), position.enPassantSquare)
        assertEquals(5, position.halfmoveClock)
        assertEquals(12, position.fullmoveNumber)
        assertEquals(Piece(PieceType.PAWN, Color.WHITE), position[Squares.parse("e5")!!])
        assertTrue(position.castling.kingSide(Color.WHITE))
        assertTrue(position.castling.queenSide(Color.BLACK))
    }

    @Test
    fun partialCastlingRightsSurvive() {
        val position = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w Kq - 0 1")
        assertTrue(position.castling.kingSide(Color.WHITE))
        assertTrue(!position.castling.queenSide(Color.WHITE))
        assertTrue(!position.castling.kingSide(Color.BLACK))
        assertTrue(position.castling.queenSide(Color.BLACK))
        assertEquals("Kq", position.castling.fen)
    }

    @Test
    fun garbageIsRejected() {
        assertNull(Fen.parseOrNull(""))
        assertNull(Fen.parseOrNull("not a fen at all"))
        assertNull(Fen.parseOrNull("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1")) // 7 ranks
        assertNull(Fen.parseOrNull("rnbqkbnr/pppppppp/9/8/8/8/8/PPPPPPPP w KQkq - 0 1")) // bad digit
        assertNull(Fen.parseOrNull("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1")) // bad side
    }
}
