package com.cheacher.app.ui.board

import androidx.compose.ui.geometry.Offset
import com.cheacher.app.chess.Squares
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The geometry the table turn rests on, kept apart from the composable so it can be
 * checked without a screen.
 *
 * Two rules hold the whole flip together. Board content is laid out in one fixed frame —
 * white at the bottom, always — and the turn is a rotation of the layer that content
 * lives in, never a per-square recomputation. And a square rotating inside its own
 * bounding box pokes out at the corners, so the layer shrinks by exactly enough to stay
 * inscribed while it goes round.
 */
internal object BoardTurn {

    /** The top-left corner of [square] in the board's own frame. */
    fun squareOrigin(square: Int, squarePx: Float): Offset = Offset(
        Squares.fileOf(square) * squarePx,
        (7 - Squares.rankOf(square)) * squarePx,
    )

    /**
     * The square under [point], also in the board's frame — Compose hit-tests through the
     * turning layer's inverse transform, so points arrive here already untwisted.
     *
     * Null off the board: a drag released past the edge is a cancelled move, not a move
     * to the nearest edge square.
     */
    fun squareAt(point: Offset, squarePx: Float): Int? {
        if (point.x < 0f || point.y < 0f) return null
        val x = (point.x / squarePx).toInt()
        val y = (point.y / squarePx).toInt()
        if (x !in 0..7 || y !in 0..7) return null
        return Squares.of(x, 7 - y)
    }

    /**
     * How large to draw the board at [degrees] through the turn.
     *
     * The inscribed fit alone ([inscribedScale]) springs back to full size at 90°, so the
     * slab would dip, swell, and dip again. Holding the smallest fit from 45° through
     * 135° makes one motion instead: the board sinks away, stays down through the middle,
     * and rises as it lands. Outside that interval the exact fit also handles the small
     * overshoot allowed by the spring animation.
     */
    fun scaleAt(degrees: Float): Float {
        val radians = degrees * (PI.toFloat() / 180f)
        return if (degrees in 45f..135f) MIN_SCALE else inscribedScale(radians)
    }

    /** How far a square must shrink to stay inside its own bounding box at [radians]. */
    fun inscribedScale(radians: Float): Float =
        1f / (abs(cos(radians)) + abs(sin(radians)))

    private val MIN_SCALE = 1f / sqrt(2f)
}
