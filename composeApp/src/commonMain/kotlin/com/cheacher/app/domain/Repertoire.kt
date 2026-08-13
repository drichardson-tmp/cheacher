package com.cheacher.app.domain

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Fen
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The authored, on-disk shape of a repertoire: a tree of SAN moves, each tagged with
 * the canonical name of the line it creates and one plain-English sentence about the idea.
 *
 * Deliberately *not* the runtime shape. Nothing here knows about board state — see
 * [OpeningTree] for the resolved version with positions attached.
 */
@Serializable
data class Repertoire(
    val id: String,
    val title: String,
    val subtitle: String = "",
    /** Whose repertoire this is. Drives default board orientation. */
    val perspective: Color,
    @SerialName("start_fen")
    val startFen: String = Fen.START,
    /** Candidate first moves. Usually one; more than one models a choice at move 1. */
    val moves: List<RepertoireMove> = emptyList(),
)

@Serializable
data class RepertoireMove(
    /** SAN as authored, e.g. `Nf3`. Resolved against the parent position at load time. */
    val san: String,
    /**
     * Canonical chess vocabulary for the position this move creates — "Sicilian Defence",
     * "Open Sicilian", "Najdorf Variation". This is the primary prompt in guided mode.
     */
    val name: String,
    /**
     * The single human-language allowance: one sentence on *why*, revealed as a subtitle
     * on a second attempt or when the learner asks for it.
     */
    val idea: String = "",
    val children: List<RepertoireMove> = emptyList(),
)
