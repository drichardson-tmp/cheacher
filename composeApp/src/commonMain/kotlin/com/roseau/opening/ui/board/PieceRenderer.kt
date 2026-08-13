package com.roseau.opening.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.roseau.opening.chess.Piece
import com.roseau.opening.chess.PieceType
import com.roseau.opening.chess.Color as ChessColor

/**
 * The one seam between the board and how pieces look.
 *
 * The board only ever asks a renderer to draw "this piece, this big". Swapping in a set
 * of user-provided vector assets later means writing one new implementation of this
 * interface and passing it to [ChessBoardView] — nothing else changes.
 */
interface PieceRenderer {
    @Composable
    fun Render(piece: Piece, size: Dp, modifier: Modifier)
}

/**
 * Default renderer: the Unicode chess glyphs, styled as ink on wood.
 *
 * Both colours use the *filled* (black) glyph shapes so the silhouettes match; colour
 * carries the side. Cream pieces get a dark edge shadow so they read on light squares.
 */
object GlyphPieceRenderer : PieceRenderer {
    private val glyphs = mapOf(
        PieceType.KING to "♚",
        PieceType.QUEEN to "♛",
        PieceType.ROOK to "♜",
        PieceType.BISHOP to "♝",
        PieceType.KNIGHT to "♞",
        PieceType.PAWN to "♟",
    )

    @Composable
    override fun Render(piece: Piece, size: Dp, modifier: Modifier) {
        val white = piece.color == ChessColor.WHITE
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = glyphs.getValue(piece.type),
                style = TextStyle(
                    fontSize = (size.value * 0.68f).sp,
                    color = if (white) Color(0xFFFBF3E4) else Color(0xFF241A12),
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = if (white) Color(0xB3241A12) else Color(0x66000000),
                        offset = Offset(0f, if (white) 0f else 2f),
                        blurRadius = if (white) 3f else 4f,
                    ),
                ),
            )
        }
    }
}
