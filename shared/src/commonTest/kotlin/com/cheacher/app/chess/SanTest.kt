package com.cheacher.app.chess

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SanTest {
    private fun Position.san(sanText: String): Move =
        assertNotNull(moveFromSan(sanText), "'$sanText' should resolve")

    private fun Position.after(line: String): Position {
        var position = this
        for (move in assertNotNull(playLine(line))) position = position.applyUnchecked(move)
        return position
    }

    @Test
    fun rendersSimpleMovesAndCaptures() {
        val start = Position.INITIAL
        assertEquals("e4", start.sanOf(start.san("e4")))
        assertEquals("Nf3", start.sanOf(start.san("Nf3")))

        val italian = start.after("1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6 4. d4")
        assertEquals("exd4", italian.sanOf(italian.san("exd4")))
        val recapture = italian.after("exd4")
        assertEquals("Nxd4", recapture.sanOf(recapture.san("Nxd4")))
    }

    @Test
    fun rendersCastlingBothWays() {
        val position = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val kingSide = Move(Squares.parse("e1")!!, Squares.parse("g1")!!)
        val queenSide = Move(Squares.parse("e1")!!, Squares.parse("c1")!!)
        assertEquals("O-O", position.sanOf(kingSide))
        assertEquals("O-O-O", position.sanOf(queenSide))
    }

    @Test
    fun rendersPromotionWithCheck() {
        val position = Fen.parse("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")
        val queen = Move(Squares.parse("a7")!!, Squares.parse("a8")!!, PieceType.QUEEN)
        val knight = Move(Squares.parse("a7")!!, Squares.parse("a8")!!, PieceType.KNIGHT)
        assertEquals("a8=Q+", position.sanOf(queen)) // new queen sees e8 along the rank
        assertEquals("a8=N", position.sanOf(knight))
    }

    @Test
    fun fileDisambiguationBetweenTwinKnights() {
        val position = Fen.parse("4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1")
        val fromB1 = Move(Squares.parse("b1")!!, Squares.parse("d2")!!)
        val fromF1 = Move(Squares.parse("f1")!!, Squares.parse("d2")!!)
        assertEquals("Nbd2", position.sanOf(fromB1))
        assertEquals("Nfd2", position.sanOf(fromF1))
    }

    @Test
    fun rankDisambiguationBetweenStackedRooks() {
        val position = Fen.parse("4k3/8/8/R7/8/8/8/R3K3 w - - 0 1")
        val fromA1 = Move(Squares.parse("a1")!!, Squares.parse("a3")!!)
        val fromA5 = Move(Squares.parse("a5")!!, Squares.parse("a3")!!)
        assertEquals("R1a3", position.sanOf(fromA1))
        assertEquals("R5a3", position.sanOf(fromA5))
    }

    @Test
    fun parsingForgivesSloppyInput() {
        val start = Position.INITIAL
        val e4 = start.san("e4")
        assertEquals(e4, start.san("e4!?"))
        assertEquals(e4, start.san("e4+")) // wrong check mark, still unambiguous
        assertEquals(e4, start.san("e2e4")) // raw UCI

        val castled = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertEquals(castled.san("O-O"), castled.san("0-0"))
    }

    @Test
    fun parsingRejectsNonsenseAndIllegalMoves() {
        val start = Position.INITIAL
        assertNull(start.moveFromSan("Ke2")) // king cannot move at the start
        assertNull(start.moveFromSan("xyzzy"))
        assertNull(start.moveFromSan(""))
    }

    @Test
    fun playLineHandlesMoveNumbersAndResults() {
        val moves = Position.INITIAL.playLine("1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 1-0")
        assertNotNull(moves)
        assertEquals(8, moves.size)
    }

    @Test
    fun playLineFailsLoudlyOnATypo() {
        assertNull(Position.INITIAL.playLine("1. e4 c5 2. Nf33"))
    }

    /**
     * The strongest property we can ask of SAN: for every legal move in a bag of real
     * positions, rendering then parsing must give the move back.
     */
    @Test
    fun sanRoundTripsForEveryLegalMove() {
        val positions = listOf(
            Position.INITIAL,
            Position.INITIAL.after("1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6"),
            Position.INITIAL.after("1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6"),
            Fen.parse("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"),
            Fen.parse("4k3/P7/8/8/8/8/8/4K3 w - - 0 1"),
        )
        for (position in positions) {
            for (move in position.legalMoves()) {
                val san = position.sanOf(move)
                assertEquals(move, position.moveFromSan(san), "round trip failed for $san")
            }
        }
    }
}
