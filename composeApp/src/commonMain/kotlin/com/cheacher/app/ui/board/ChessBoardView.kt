package com.cheacher.app.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Piece
import com.cheacher.app.chess.PieceType
import com.cheacher.app.chess.Position
import com.cheacher.app.chess.Squares
import com.cheacher.app.ui.theme.CheacherColors
import com.cheacher.app.ui.theme.CheacherTheme
import com.cheacher.app.ui.theme.Motion
import com.cheacher.app.chess.Color as ChessColor
import kotlin.math.roundToInt

/**
 * The board: tap a piece, see its legal squares, tap a destination.
 *
 * Presentation-only — every submitted [Move] is legal in [position], but whether it is
 * the *right* move is the trainers' business. Pieces glide between squares on springs;
 * how they look is delegated to [PieceRenderer] so glyphs can later be swapped for
 * real vector assets without touching this file.
 *
 * @param shakeTrigger increment to play the wrong-move shake (a rejected idea should
 *   *feel* rejected, not just be silently ignored).
 */
@Composable
fun ChessBoardView(
    position: Position,
    lastMove: Move?,
    orientation: ChessColor,
    onMove: (Move) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shakeTrigger: Int = 0,
    pieceRenderer: PieceRenderer = GlyphPieceRenderer,
) {
    var selected by remember(position) { mutableStateOf<Int?>(null) }

    val legalMoves = remember(position) { position.legalMoves() }
    val targets = remember(selected, legalMoves) {
        selected?.let { from -> legalMoves.filter { it.from == from } } ?: emptyList()
    }
    val checkedKing = remember(position) {
        position.kingSquare(position.sideToMove)?.takeIf { position.isInCheck() }
    }

    // Wrong-move shake: a quick damped horizontal wobble.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            shake.snapTo(0f)
            for (kick in listOf(14f, -11f, 7f, -4f, 0f)) {
                shake.animateTo(kick, Motion.snap())
            }
        }
    }

    val pieces = rememberTrackedPieces(position)
    val colors = CheacherTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .offset { IntOffset(shake.value.roundToInt(), 0) }
            .clip(RoundedCornerShape(10.dp)),
    ) {
        val density = LocalDensity.current
        val squarePx = with(density) { maxWidth.toPx() } / 8f
        val squareDp = maxWidth / 8

        fun screenOffset(square: Int): Offset {
            val file = Squares.fileOf(square)
            val rank = Squares.rankOf(square)
            val x = if (orientation == ChessColor.WHITE) file else 7 - file
            val y = if (orientation == ChessColor.WHITE) 7 - rank else rank
            return Offset(x * squarePx, y * squarePx)
        }

        fun squareAt(tap: Offset): Int? {
            val x = (tap.x / squarePx).toInt().coerceIn(0, 7)
            val y = (tap.y / squarePx).toInt().coerceIn(0, 7)
            val file = if (orientation == ChessColor.WHITE) x else 7 - x
            val rank = if (orientation == ChessColor.WHITE) 7 - y else y
            return Squares.of(file, rank)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSquares(squarePx, orientation, colors)
            lastMove?.let {
                drawSquareFill(screenOffset(it.from), squarePx, colors.lastMoveGlow)
                drawSquareFill(screenOffset(it.to), squarePx, colors.lastMoveGlow)
            }
            selected?.let { drawSquareFill(screenOffset(it), squarePx, colors.selectedGlow) }
            checkedKing?.let { drawCheckGlow(screenOffset(it), squarePx, colors.checkGlow) }
            for (target in targets.distinctBy { it.to }) {
                val origin = screenOffset(target.to)
                val centre = origin + Offset(squarePx / 2, squarePx / 2)
                if (position[target.to] != null) {
                    // Capture: a ring around the victim rather than a dot on top of it.
                    drawCircle(
                        color = colors.targetDot,
                        radius = squarePx * 0.46f,
                        center = centre,
                        style = Stroke(width = squarePx * 0.09f),
                    )
                } else {
                    drawCircle(color = colors.targetDot, radius = squarePx * 0.15f, center = centre)
                }
            }
        }

        for (placed in pieces) {
            key(placed.id) {
                val target by animateOffsetAsState(
                    targetValue = screenOffset(placed.square),
                    animationSpec = Motion.pieceTravel,
                    label = "piece-${placed.id}",
                )
                val lift by animateFloatAsState(
                    targetValue = if (placed.square == selected) 1.12f else 1f,
                    animationSpec = Motion.snap(),
                    label = "lift-${placed.id}",
                )
                pieceRenderer.Render(
                    piece = placed.piece,
                    size = squareDp,
                    modifier = Modifier
                        .size(squareDp)
                        .offset { IntOffset(target.x.roundToInt(), target.y.roundToInt()) }
                        .scale(lift),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(position, enabled, orientation) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { tap ->
                        val square = squareAt(tap) ?: return@detectTapGestures
                        val from = selected
                        val chosen = if (from != null) {
                            targets.filter { it.to == square }
                        } else {
                            emptyList()
                        }
                        when {
                            chosen.isNotEmpty() -> {
                                selected = null
                                // Openings never promote, but if one ever does: auto-queen.
                                onMove(chosen.firstOrNull { it.promotion == PieceType.QUEEN } ?: chosen.first())
                            }
                            position[square]?.color == position.sideToMove -> {
                                selected = if (from == square) null else square
                            }
                            else -> selected = null
                        }
                    }
                },
        )
    }
}

private fun DrawScope.drawSquares(squarePx: Float, orientation: ChessColor, colors: CheacherColors) {
    for (x in 0..7) {
        for (y in 0..7) {
            val file = if (orientation == ChessColor.WHITE) x else 7 - x
            val rank = if (orientation == ChessColor.WHITE) 7 - y else y
            val light = (file + rank) % 2 == 1
            drawRect(
                color = if (light) colors.boardLight else colors.boardDark,
                topLeft = Offset(x * squarePx, y * squarePx),
                size = Size(squarePx, squarePx),
            )
        }
    }
}

private fun DrawScope.drawSquareFill(origin: Offset, squarePx: Float, color: Color) {
    drawRect(color = color, topLeft = origin, size = Size(squarePx, squarePx))
}

private fun DrawScope.drawCheckGlow(origin: Offset, squarePx: Float, color: Color) {
    drawRoundRect(
        color = color,
        topLeft = origin + Offset(squarePx * 0.04f, squarePx * 0.04f),
        size = Size(squarePx * 0.92f, squarePx * 0.92f),
        cornerRadius = CornerRadius(squarePx * 0.2f),
        style = Stroke(width = squarePx * 0.1f),
    )
}

// ------------------------------------------------------------------ piece identity

/** A piece with an identity that survives moving between squares — the animation key. */
data class PlacedPiece(val id: Int, val piece: Piece, val square: Int)

/**
 * Gives each piece a stable id across position changes by diffing consecutive boards:
 * a piece that vanished from one square and an identical piece that appeared on another
 * are assumed to be the same piece. That single rule animates ordinary moves, castling
 * (two pieces move), captures, en passant, and even multi-ply snap-backs — pieces glide
 * home instead of teleporting.
 */
@Composable
fun rememberTrackedPieces(position: Position): List<PlacedPiece> {
    val tracker = remember { PieceTracker() }
    return remember(position) { tracker.update(position) }
}

private class PieceTracker {
    private var previous: Position? = null
    private var ids = arrayOfNulls<Int>(Squares.COUNT)
    private var nextId = 0

    fun update(position: Position): List<PlacedPiece> {
        val old = previous
        val newIds = arrayOfNulls<Int>(Squares.COUNT)

        // Pieces that did not move keep their square and id.
        for (square in 0 until Squares.COUNT) {
            val piece = position[square] ?: continue
            if (old != null && old[square] == piece && ids[square] != null) {
                newIds[square] = ids[square]
            }
        }

        // Match each appeared piece to a vacated square holding the same piece.
        if (old != null) {
            val vacated = (0 until Squares.COUNT)
                .filter { old[it] != null && old[it] != position[it] && ids[it] != null }
                .toMutableList()
            for (square in 0 until Squares.COUNT) {
                val piece = position[square] ?: continue
                if (newIds[square] != null) continue
                val fromIndex = vacated.indexOfFirst { old[it] == piece }
                // Promotion: no identical vacated piece, but a vacated pawn of ours will do.
                val pawnIndex = if (fromIndex >= 0) -1 else vacated.indexOfFirst {
                    old[it]?.type == PieceType.PAWN && old[it]?.color == piece.color
                }
                val match = if (fromIndex >= 0) fromIndex else pawnIndex
                if (match >= 0) {
                    newIds[square] = ids[vacated.removeAt(match)]
                }
            }
        }

        for (square in 0 until Squares.COUNT) {
            if (position[square] != null && newIds[square] == null) newIds[square] = nextId++
        }

        previous = position
        ids = newIds
        return (0 until Squares.COUNT).mapNotNull { square ->
            position[square]?.let { PlacedPiece(newIds[square]!!, it, square) }
        }
    }
}
