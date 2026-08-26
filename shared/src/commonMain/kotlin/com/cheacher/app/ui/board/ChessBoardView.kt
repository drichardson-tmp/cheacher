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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.snap as instantly
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Piece
import com.cheacher.app.chess.PieceType
import com.cheacher.app.chess.Position
import com.cheacher.app.chess.Squares
import com.cheacher.app.ui.theme.CheacherColors
import com.cheacher.app.ui.theme.CheacherTheme
import com.cheacher.app.ui.theme.Motion
import com.cheacher.app.chess.Color as ChessColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The board: tap a piece, see its legal squares, tap a destination — or just drag the
 * piece there. Both gestures drive the same selection state, so a drag that is released
 * short of a legal square simply leaves the piece selected and the tap flow carries on.
 *
 * Presentation-only — every submitted [Move] is legal in [position], but whether it is
 * the *right* move is the trainers' business. Pieces glide between squares on springs;
 * how they look is delegated to [PieceRenderer] so glyphs can later be swapped for
 * real vector assets without touching this file.
 *
 * Changing [orientation] *turns the table*: every piece and highlight swings around the
 * board's centre to the mirrored square, like rotating a physical board 180°. The
 * checker pattern is symmetric under that turn, so the wood itself never moves — only
 * what sits on it. Coordinates fade through the turn and come back relabelled.
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
    var drag by remember(position) { mutableStateOf<DragState?>(null) }

    val legalMoves = remember(position) { position.legalMoves() }
    // A drag owns the origin while it lasts; otherwise the tap selection does.
    val activeFrom = drag?.from ?: selected
    val targets = remember(activeFrom, legalMoves) {
        activeFrom?.let { from -> legalMoves.filter { it.from == from } } ?: emptyList()
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

    // The table turn, as one angle: 0° is white at the bottom, 180° is black. Everything
    // placed on the board rides this rotation, so the flip is a single sweeping motion.
    val flipDegrees by animateFloatAsState(
        targetValue = if (orientation == ChessColor.WHITE) 0f else 180f,
        animationSpec = Motion.tableTurn,
        label = "board-flip",
    )

    val pieces = rememberTrackedPieces(position)
    val colors = CheacherTheme.colors
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .offset { IntOffset(shake.value.roundToInt(), 0) }
            .clip(RoundedCornerShape(10.dp)),
    ) {
        val density = LocalDensity.current
        val squarePx = with(density) { maxWidth.toPx() } / 8f
        val squareDp = maxWidth / 8
        val boardPx = squarePx * 8f

        // A square's centre in white orientation, swung around the board centre by the
        // flip angle. At 180° this lands exactly on the black-orientation layout.
        val flipRadians = flipDegrees * (PI.toFloat() / 180f)
        val flipCos = cos(flipRadians)
        val flipSin = sin(flipRadians)
        fun screenOffset(square: Int): Offset {
            val cx = Squares.fileOf(square) * squarePx + squarePx / 2 - boardPx / 2
            val cy = (7 - Squares.rankOf(square)) * squarePx + squarePx / 2 - boardPx / 2
            return Offset(
                cx * flipCos - cy * flipSin + boardPx / 2 - squarePx / 2,
                cx * flipSin + cy * flipCos + boardPx / 2 - squarePx / 2,
            )
        }

        // Null off the board: a drag released past the edge is a cancelled move, not a
        // move to the nearest edge square.
        //
        // Reads [flipDegrees] at call time (not the values captured above) because
        // gesture handlers outlive the composition that created them: mid-turn, hits
        // must map through the same animated rotation the pieces are drawn with, or a
        // tap on a visibly moving piece lands on a different logical square. Undoing
        // the rotation puts the point in the white-orientation frame, so no
        // per-orientation branch remains — at rest, 0° and 180° are exactly the old
        // white and black mappings.
        fun squareAt(point: Offset): Int? {
            val radians = flipDegrees * (PI.toFloat() / 180f)
            val c = cos(radians)
            val s = sin(radians)
            val px = point.x - boardPx / 2
            val py = point.y - boardPx / 2
            val x = ((px * c + py * s + boardPx / 2) / squarePx).toInt()
            val y = ((-px * s + py * c + boardPx / 2) / squarePx).toInt()
            if (x !in 0..7 || y !in 0..7) return null
            return Squares.of(x, 7 - y)
        }

        val coordinateLayouts = rememberCoordinateLayouts(textMeasurer, squarePx)
        val hovered = drag?.let { squareAt(it.pointer) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSquares(squarePx, colors)
            drawCoordinates(coordinateLayouts, squarePx, flipDegrees, colors)
            lastMove?.let {
                drawSquareFill(screenOffset(it.from), squarePx, colors.lastMoveGlow)
                drawSquareFill(screenOffset(it.to), squarePx, colors.lastMoveGlow)
            }
            activeFrom?.let { drawSquareFill(screenOffset(it), squarePx, colors.selectedGlow) }
            // Under-the-finger square: the drop preview, so a drag can be aimed.
            hovered?.let { drawSquareOutline(screenOffset(it), squarePx, colors.selectedGlow) }
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
                val dragged = drag?.from == placed.square
                // While dragged the piece tracks the finger with no spring lag; released,
                // the spec flips back and it either glides to its new square or springs home.
                val target by animateOffsetAsState(
                    targetValue = if (dragged) {
                        drag!!.pointer - Offset(squarePx / 2, squarePx / 2)
                    } else {
                        screenOffset(placed.square)
                    },
                    animationSpec = if (dragged) instantly() else Motion.pieceTravel,
                    label = "piece-${placed.id}",
                )
                val lift by animateFloatAsState(
                    targetValue = when {
                        dragged -> 1.20f
                        placed.square == selected -> 1.12f
                        else -> 1f
                    },
                    animationSpec = Motion.snap(),
                    label = "lift-${placed.id}",
                )
                // A hair of tilt, pivoting at the piece's foot — the way a real piece
                // leans when it is pinched off the board rather than sliding on it.
                val tilt by animateFloatAsState(
                    targetValue = if (dragged) -4.5f else 0f,
                    animationSpec = Motion.snap(),
                    label = "tilt-${placed.id}",
                )
                pieceRenderer.Render(
                    piece = placed.piece,
                    size = squareDp,
                    modifier = Modifier
                        .size(squareDp)
                        // The piece in hand rides above the rest of the board.
                        .zIndex(if (dragged) 1f else 0f)
                        .offset { IntOffset(target.x.roundToInt(), target.y.roundToInt()) }
                        .graphicsLayer {
                            scaleX = lift
                            scaleY = lift
                            rotationZ = tilt
                            transformOrigin = TransformOrigin(0.5f, 0.82f)
                        },
                )
            }
        }

        // The two gestures live in separate handlers: the drag detector (inner, so it sees
        // events first) never consumes the down, and once it passes touch slop its consumed
        // moves cancel the tap detector — so a press is a tap and a pull is a drag.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(position, enabled, orientation) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { tap ->
                        val square = squareAt(tap) ?: return@detectTapGestures
                        val from = selected
                        // Recomputed from [legalMoves], never read from the captured
                        // `targets`: this gesture block is keyed on [position], so a
                        // value derived from `selected` would still hold the empty list
                        // captured when nothing was selected, and no tap could ever
                        // complete a move.
                        val chosen = if (from != null) {
                            legalMoves.filter { it.from == from && it.to == square }
                        } else {
                            emptyList()
                        }
                        when {
                            chosen.isNotEmpty() -> {
                                selected = null
                                onMove(pickMove(chosen))
                            }
                            position[square]?.color == position.sideToMove -> {
                                selected = if (from == square) null else square
                            }
                            else -> selected = null
                        }
                    }
                }
                .pointerInput(position, enabled, orientation) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { start ->
                            val square = squareAt(start)
                            if (square != null && position[square]?.color == position.sideToMove) {
                                selected = square
                                drag = DragState(square, start)
                            }
                        },
                        onDrag = { change, amount ->
                            drag?.let {
                                change.consume()
                                drag = it.copy(pointer = it.pointer + amount)
                            }
                        },
                        onDragEnd = {
                            val released = drag ?: return@detectDragGestures
                            drag = null
                            val square = squareAt(released.pointer)
                            val chosen = legalMoves.filter { it.from == released.from && it.to == square }
                            if (chosen.isNotEmpty()) {
                                selected = null
                                onMove(pickMove(chosen))
                            } else {
                                // Dropped nowhere useful: the piece springs home but stays
                                // picked up, so the tap flow can finish the move.
                                selected = released.from
                            }
                        },
                        onDragCancel = { drag = null },
                    )
                },
        )
    }
}

// The checker pattern is its own 180°-rotation image, so the wood needs no orientation:
// a screen cell's colour is the same from either chair.
private fun DrawScope.drawSquares(squarePx: Float, colors: CheacherColors) {
    for (x in 0..7) {
        for (y in 0..7) {
            val light = (x + y) % 2 == 0
            drawRect(
                color = if (light) colors.boardLight else colors.boardDark,
                topLeft = Offset(x * squarePx, y * squarePx),
                size = Size(squarePx, squarePx),
            )
        }
    }
}

/**
 * One measured layout per coordinate glyph, re-measured only when the board is resized —
 * sixteen labels per frame is a draw, not a layout.
 */
@Composable
private fun rememberCoordinateLayouts(
    textMeasurer: TextMeasurer,
    squarePx: Float,
): Map<Char, TextLayoutResult> {
    val density = LocalDensity.current
    return remember(textMeasurer, squarePx) {
        val style = TextStyle(
            fontSize = with(density) { (squarePx * 0.21f).toSp() },
            fontWeight = FontWeight.SemiBold,
        )
        "abcdefgh12345678".associateWith { textMeasurer.measure(it.toString(), style) }
    }
}

/**
 * Files along the bottom edge, ranks up the left, each glyph in the opposite wood so it
 * reads on its own square. The labels belong to whichever chair is nearest: they fade
 * out through the table turn and come back renamed for the other side.
 */
private fun DrawScope.drawCoordinates(
    layouts: Map<Char, TextLayoutResult>,
    squarePx: Float,
    flipDegrees: Float,
    colors: CheacherColors,
) {
    val whiteChair = flipDegrees < 90f
    val alpha = abs(cos(flipDegrees * (PI.toFloat() / 180f))) * 0.85f
    if (alpha <= 0.02f) return
    val pad = squarePx * 0.06f

    for (x in 0..7) {
        val layout = layouts[if (whiteChair) 'a' + x else 'h' - x] ?: continue
        // Bottom-row cell (x, 7): light wood when x is odd.
        drawText(
            textLayoutResult = layout,
            color = if (x % 2 == 1) colors.boardDark else colors.boardLight,
            topLeft = Offset(
                (x + 1) * squarePx - layout.size.width - pad,
                squarePx * 8 - layout.size.height - pad * 0.5f,
            ),
            alpha = alpha,
        )
    }
    for (y in 0..7) {
        val layout = layouts[if (whiteChair) '0' + (8 - y) else '1' + y] ?: continue
        // Left-column cell (0, y): light wood when y is even.
        drawText(
            textLayoutResult = layout,
            color = if (y % 2 == 0) colors.boardDark else colors.boardLight,
            topLeft = Offset(pad, y * squarePx + pad * 0.5f),
            alpha = alpha,
        )
    }
}

/** The square a drag is hovering: an inset outline, distinct from the filled origin. */
private fun DrawScope.drawSquareOutline(origin: Offset, squarePx: Float, color: Color) {
    val inset = squarePx * 0.05f
    drawRect(
        color = color,
        topLeft = origin + Offset(inset, inset),
        size = Size(squarePx - inset * 2, squarePx - inset * 2),
        style = Stroke(width = squarePx * 0.06f),
    )
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

/** A piece in hand: where it came from, and where the finger is now (board pixels). */
private data class DragState(val from: Int, val pointer: Offset)

/** Openings never promote, but if one ever does: auto-queen. */
private fun pickMove(candidates: List<Move>): Move =
    candidates.firstOrNull { it.promotion == PieceType.QUEEN } ?: candidates.first()

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
