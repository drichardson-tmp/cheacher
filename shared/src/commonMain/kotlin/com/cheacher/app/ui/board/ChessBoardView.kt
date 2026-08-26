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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
 * Changing [orientation] *turns the table*: the whole slab rotates 180° — wood, sheen,
 * highlights and pieces together — the way you would spin a physical board to face the
 * other chair. It shrinks just enough to keep its corners inside the frame as it goes,
 * and the glyphs counter-rotate so they are never read upside down. Coordinates belong
 * to whichever chair is nearest: they fade out through the turn and come back relabelled.
 *
 * @param shakeTrigger increment to play the wrong-move shake (a rejected idea should
 *   *feel* rejected, not just be silently ignored).
 * @param onSquareTap when set, the board stops being a chessboard and becomes a grid of
 *   64 buttons: taps report their square and nothing selects, moves, or drags. The square
 *   drill's whole interface.
 * @param spotlight a square to wash green (found) or red (wrong) — the drill's feedback,
 *   which has no [Move] to highlight the way a session does.
 * @param showCoordinates a–h/1–8 on the board edges. On while the names are being
 *   learned, off in recall and in the square drill, where finding the square *is* the
 *   skill being measured.
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
    showCoordinates: Boolean = true,
    onSquareTap: ((Int) -> Unit)? = null,
    spotlight: Spotlight? = null,
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

    // The table turn, as one angle: 0° is white at the bottom, 180° is black. The board
    // layer rides this rotation, so the flip is one sweeping motion rather than 32
    // independently travelling pieces.
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
            .offset { IntOffset(shake.value.roundToInt(), 0) },
    ) {
        val density = LocalDensity.current
        val squarePx = with(density) { maxWidth.toPx() } / 8f
        val squareDp = maxWidth / 8
        val boardPx = squarePx * 8f

        // Everything below lives in the board's own frame — white always at the bottom,
        // the turn applied once to the layer that holds it. That is also why gestures
        // need no un-rotating: Compose hit-tests through the layer's inverse transform,
        // so a tap mid-turn lands on the square visibly under the finger. See [BoardTurn].
        fun squareOffset(square: Int) = BoardTurn.squareOrigin(square, squarePx)
        fun squareAt(point: Offset): Int? = BoardTurn.squareAt(point, squarePx)

        val coordinateLayouts = rememberCoordinateLayouts(textMeasurer, squarePx)
        val hovered = drag?.let { squareAt(it.pointer) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = flipDegrees
                    // Shrunk to stay inside its own bounding box as it goes round, and
                    // shrunk in one dip rather than two — see [BoardTurn.scaleAt].
                    val fit = BoardTurn.scaleAt(flipDegrees)
                    scaleX = fit
                    scaleY = fit
                }
                .clip(RoundedCornerShape(10.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSquares(squarePx, colors)
                drawSheen(boardPx)
                if (showCoordinates) drawCoordinates(coordinateLayouts, squarePx, flipDegrees, colors)
                lastMove?.let {
                    drawSquareFill(squareOffset(it.from), squarePx, colors.lastMoveGlow)
                    drawSquareFill(squareOffset(it.to), squarePx, colors.lastMoveGlow)
                }
                spotlight?.let {
                    val ink = if (it.correct) colors.verdictCorrect else colors.verdictMiss
                    drawSquareFill(squareOffset(it.square), squarePx, ink.copy(alpha = 0.55f))
                }
                activeFrom?.let { drawSquareFill(squareOffset(it), squarePx, colors.selectedGlow) }
                // Under-the-finger square: the drop preview, so a drag can be aimed.
                hovered?.let { drawSquareOutline(squareOffset(it), squarePx, colors.selectedGlow) }
                checkedKing?.let { drawCheckGlow(squareOffset(it), squarePx, colors.checkGlow) }
                for (target in targets.distinctBy { it.to }) {
                    val origin = squareOffset(target.to)
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
                            squareOffset(placed.square)
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
                    Box(
                        modifier = Modifier
                            .size(squareDp)
                            // The piece in hand rides above the rest of the board.
                            .zIndex(if (dragged) 1f else 0f)
                            .offset { IntOffset(target.x.roundToInt(), target.y.roundToInt()) }
                            // Carried around by the slab's rotation, spun back on its own
                            // centre: the piece travels the arc but stays the right way up.
                            .graphicsLayer { rotationZ = -flipDegrees },
                    ) {
                        pieceRenderer.Render(
                            piece = placed.piece,
                            size = squareDp,
                            modifier = Modifier
                                .size(squareDp)
                                .graphicsLayer {
                                    scaleX = lift
                                    scaleY = lift
                                    rotationZ = tilt
                                    transformOrigin = TransformOrigin(0.5f, 0.82f)
                                },
                        )
                    }
                }
            }

            // The two gestures live in separate handlers: the drag detector (inner, so it sees
            // events first) never consumes the down, and once it passes touch slop its consumed
            // moves cancel the tap detector — so a press is a tap and a pull is a drag.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(position, enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures { tap ->
                            val square = squareAt(tap) ?: return@detectTapGestures
                            // Grid-of-buttons mode: the tap *is* the answer, so none of the
                            // selection machinery below should run.
                            if (onSquareTap != null) {
                                onSquareTap(square)
                                return@detectTapGestures
                            }
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
                    .pointerInput(position, enabled) {
                        // Nothing to drag when the board is a grid of buttons.
                        if (!enabled || onSquareTap != null) return@pointerInput
                        detectDragGestures(
                            onDragStart = { start ->
                                val square = squareAt(start)
                                if (square != null && position[square]?.color == position.sideToMove) {
                                    selected = square
                                    drag = DragState(square, start)
                                }
                            },
                            onDrag = { change, _ ->
                                drag?.let {
                                    change.consume()
                                    // The live position, not an accumulated delta: the
                                    // frame this is read in may be rotated further than
                                    // the last one, and summed deltas would drift out
                                    // from under a finger that never moved.
                                    drag = it.copy(pointer = change.position)
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
}

// Drawn in the board's own frame — the layer above turns it. The checker pattern happens
// to be its own 180°-rotation image, so only the sheen betrays that the wood has moved.
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
 * A single raking light across the slab: a highlight on one diagonal, a shadow on the
 * other. Barely there at rest, but it is what makes the turn read as a solid board being
 * spun rather than a checker pattern that never changes — the lit corner sweeps across
 * and settles opposite, exactly as it would on a real table.
 */
private fun DrawScope.drawSheen(boardPx: Float) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.06f),
            ),
            start = Offset.Zero,
            end = Offset(boardPx, boardPx),
        ),
        size = Size(boardPx, boardPx),
    )
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

    // Pinned to the wood, but pre-turned by the settled angle rather than the live one:
    // the labels ride the slab around without sliding out from under its clip, and land
    // upright along the near edges. Quantising is free because the swap happens at 90°,
    // where they have already faded to nothing — the same crossing at which the a-file
    // becomes the h-file.
    rotate(degrees = if (whiteChair) 0f else -180f, pivot = Offset(squarePx * 4, squarePx * 4)) {
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

/** The drill's one-square verdict wash: green for found, red for a wrong tap. */
data class Spotlight(val square: Int, val correct: Boolean)

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
