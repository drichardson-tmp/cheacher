package com.cheacher.app.ui.board

import androidx.compose.ui.geometry.Offset
import com.cheacher.app.chess.Squares
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardTurnTest {

    @Test
    fun squareOriginsUseOneWhiteAtBottomBoardFrame() {
        assertEquals(Offset(0f, 700f), BoardTurn.squareOrigin(square("a1"), 100f))
        assertEquals(Offset(700f, 700f), BoardTurn.squareOrigin(square("h1"), 100f))
        assertEquals(Offset(0f, 0f), BoardTurn.squareOrigin(square("a8"), 100f))
        assertEquals(Offset(700f, 0f), BoardTurn.squareOrigin(square("h8"), 100f))
    }

    @Test
    fun hitTestingIncludesEverySquareAndRejectsEveryOuterEdge() {
        assertEquals(square("a8"), BoardTurn.squareAt(Offset(0f, 0f), 100f))
        assertEquals(square("h8"), BoardTurn.squareAt(Offset(799.99f, 0f), 100f))
        assertEquals(square("a1"), BoardTurn.squareAt(Offset(0f, 799.99f), 100f))
        assertEquals(square("h1"), BoardTurn.squareAt(Offset(799.99f, 799.99f), 100f))

        assertNull(BoardTurn.squareAt(Offset(-0.01f, 400f), 100f))
        assertNull(BoardTurn.squareAt(Offset(400f, -0.01f), 100f))
        assertNull(BoardTurn.squareAt(Offset(800f, 400f), 100f))
        assertNull(BoardTurn.squareAt(Offset(400f, 800f), 100f))
    }

    @Test
    fun scaleMakesOneDipAndReturnsToFullSize() {
        assertClose(1f, BoardTurn.scaleAt(0f))
        assertClose(BoardTurn.scaleAt(45f), BoardTurn.scaleAt(90f))
        assertClose(BoardTurn.scaleAt(90f), BoardTurn.scaleAt(135f))
        assertClose(1f, BoardTurn.scaleAt(180f))

        for (degrees in 0..180) {
            assertClose(BoardTurn.scaleAt(degrees.toFloat()), BoardTurn.scaleAt(180f - degrees))
        }
    }

    @Test
    fun scaleNeverLetsRotatedCornersLeaveTheFrame() {
        for (degrees in -10..190) {
            val radians = degrees * (PI.toFloat() / 180f)
            assertTrue(
                BoardTurn.scaleAt(degrees.toFloat()) <= BoardTurn.inscribedScale(radians) + TOLERANCE,
                "board does not fit at $degrees degrees",
            )
        }
    }

    private fun square(name: String): Int = requireNotNull(Squares.parse(name))

    private fun assertClose(expected: Float, actual: Float) {
        assertEquals(expected, actual, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.00001f
    }
}
