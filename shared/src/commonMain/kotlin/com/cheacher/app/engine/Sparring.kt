package com.cheacher.app.engine

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.PieceType
import com.cheacher.app.chess.Position
import kotlin.random.Random

/**
 * A chess opponent for the play-out phase: hand it a position and a strength dial, get
 * a move back. Implementations are Stockfish over UCI where the platform can run it and
 * [PocketFish] everywhere else; callers never know which they got, because the strength
 * dial — not the engine's pedigree — is the product.
 */
interface SparringEngine {
    val name: String

    /** The engine's choice in [position], or null when it has none (game over, engine died). */
    suspend fun bestMove(position: Position, tuning: EngineTuning): Move?

    fun close() {}
}

/**
 * Platform factory. Android tries the packaged Stockfish binary and falls back to
 * [PocketFish]; iOS is [PocketFish] until a compiled engine lands there. [random] seeds
 * whatever weakening the implementation does, so tests can pin it.
 */
expect fun createSparringEngine(random: Random): SparringEngine

/**
 * One Elo number translated into engine behaviour.
 *
 * Stockfish's own `UCI_Elo` refuses to go below 1320, and the learner starts around 700
 * — so weakness is manufactured here instead: a shallow search, a softmax [temperatureCp]
 * over root scores (PocketFish), a capped [skillLevel] and [movetimeMs] (Stockfish), and
 * a [blunderChance] applied *outside* any engine by [SparringPartner]. The mapping is
 * deliberately rough; the adaptive rating loop corrects it per learner.
 */
data class EngineTuning(
    val elo: Int,
    /** PocketFish search depth in plies. */
    val depth: Int,
    /** Stockfish thinking budget. */
    val movetimeMs: Int,
    /** Softmax temperature in centipawns over root move scores; higher is drunker. */
    val temperatureCp: Int,
    /** Chance a move is replaced with a uniformly random legal one. The 700-Elo special. */
    val blunderChance: Double,
    /** Stockfish `Skill Level` 0..20. */
    val skillLevel: Int,
)

fun tuningFor(elo: Int): EngineTuning {
    val e = elo.coerceIn(SparringElo.MIN, SparringElo.MAX)
    return EngineTuning(
        elo = e,
        depth = when {
            e < 800 -> 1
            e < 1150 -> 2
            else -> 3
        },
        movetimeMs = (40 + (e - 400) / 4).coerceAtMost(400),
        temperatureCp = ((1600 - e) / 4).coerceIn(8, 260),
        blunderChance = ((1200 - e) / 2500.0).coerceIn(0.0, 0.25),
        skillLevel = ((e - 600) / 100).coerceIn(0, 20),
    )
}

/**
 * The sparring rating: one number per repertoire, starting where an adult beginner
 * starts. The engine always plays *at* the learner's rating, so the expected score is
 * ½ and the update collapses to ±[K]/2 per decisive game — legible enough to say out
 * loud, which is the same bet as the review ladder.
 */
object SparringElo {
    const val START = 700
    const val MIN = 400
    const val MAX = 2000
    const val K = 64

    fun updated(rating: Int, score: Double): Int {
        val delta = (K * (score - 0.5)).toInt()
        return (rating + delta).coerceIn(MIN, MAX)
    }
}

private val PIECE_PAWNS = mapOf(
    PieceType.PAWN to 1,
    PieceType.KNIGHT to 3,
    PieceType.BISHOP to 3,
    PieceType.ROOK to 5,
    PieceType.QUEEN to 9,
    PieceType.KING to 0,
)

/** How many pawns of material [side] is down in [position]; never negative. */
fun materialDeficit(position: Position, side: Color): Int {
    var balance = 0
    for (piece in position.board) {
        if (piece == null) continue
        val value = PIECE_PAWNS.getValue(piece.type)
        balance += if (piece.color == side) value else -value
    }
    return (-balance).coerceAtLeast(0)
}

/**
 * The in-game mercy rule: when the learner is down material the engine drops toward
 * their level of despair — 60 Elo per pawn, up to five pawns. Winning never makes the
 * engine harder; a learner ahead has earned the win they are converting.
 */
fun rubberBanded(rating: Int, learnerDeficitPawns: Int): Int =
    (rating - 60 * learnerDeficitPawns.coerceIn(0, 5)).coerceAtLeast(SparringElo.MIN)

/**
 * The opponent the ViewModel actually talks to: an engine plus the blunder governor.
 * The governor lives here, engine-agnostic, because full-strength-then-random-lapse is
 * how ~700 humans actually lose games — and it is the only way to get a real Stockfish
 * down to that neighbourhood at all.
 */
class SparringPartner(
    private val engine: SparringEngine,
    private val random: Random,
) {
    /** A legal move for the side to move in [position] at [elo] strength, or null if none exist. */
    suspend fun move(position: Position, elo: Int): Move? {
        val legal = position.legalMoves()
        if (legal.isEmpty()) return null
        val tuning = tuningFor(elo)
        if (random.nextDouble() < tuning.blunderChance) return legal.random(random)
        val chosen = engine.bestMove(position, tuning)
        // An engine hiccup (dead process, illegal UCI reply) must never stall the game.
        return if (chosen != null && chosen in legal) chosen else legal.random(random)
    }

    fun close() = engine.close()
}
