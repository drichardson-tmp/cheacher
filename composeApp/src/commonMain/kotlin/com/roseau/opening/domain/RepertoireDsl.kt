package com.roseau.opening.domain

import com.roseau.opening.chess.Color
import com.roseau.opening.chess.Fen

/**
 * A tiny builder so hand-authored repertoires read like the board.
 *
 * ```
 * repertoire("sicilian", "The Open Sicilian", Color.WHITE) {
 *     move("e4", "King's Pawn Opening", "Takes the centre and opens two pieces.") {
 *         move("c5", "Sicilian Defence", "Fights for d4 without symmetry.")
 *     }
 * }
 * ```
 */
fun repertoire(
    id: String,
    title: String,
    perspective: Color,
    subtitle: String = "",
    startFen: String = Fen.START,
    build: RepertoireScope.() -> Unit,
): Repertoire = Repertoire(
    id = id,
    title = title,
    subtitle = subtitle,
    perspective = perspective,
    startFen = startFen,
    moves = RepertoireScope().apply(build).moves,
)

class RepertoireScope {
    internal val moves = mutableListOf<RepertoireMove>()

    fun move(san: String, name: String, idea: String = "", build: RepertoireScope.() -> Unit = {}) {
        moves += RepertoireMove(
            san = san,
            name = name,
            idea = idea,
            children = RepertoireScope().apply(build).moves,
        )
    }
}
