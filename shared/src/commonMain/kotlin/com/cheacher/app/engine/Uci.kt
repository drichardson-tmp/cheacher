package com.cheacher.app.engine

import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Position
import com.cheacher.app.chess.toFen

/**
 * A line-oriented pipe to a UCI engine process. Platform code owns the process; this
 * interface is what common code (and tests) can see of it.
 */
interface UciTransport {
    suspend fun writeLine(line: String)

    /** The next line from the engine, or null when the pipe is closed. */
    suspend fun readLine(): String?

    fun close()
}

/**
 * The UCI conversation, engine-agnostic and fully testable against a fake transport:
 * handshake once, then `position fen … / go movetime …` per move, weakening via
 * `Skill Level` (never `UCI_Elo`, whose floor is far above our learners).
 *
 * Failure policy: any protocol surprise throws, and [SparringPartner] upstream already
 * treats a thrown/absent move as "play something legal anyway" via the platform factory
 * wrapping this in [resilient].
 */
class UciSparringEngine(
    override val name: String,
    private val openTransport: suspend () -> UciTransport,
) : SparringEngine {
    private var transport: UciTransport? = null
    private var appliedSkill: Int? = null

    override suspend fun bestMove(position: Position, tuning: EngineTuning): Move? {
        val pipe = transport ?: handshake().also { transport = it }
        if (appliedSkill != tuning.skillLevel) {
            pipe.writeLine("setoption name Skill Level value ${tuning.skillLevel}")
            appliedSkill = tuning.skillLevel
        }
        pipe.writeLine("position fen ${position.toFen()}")
        pipe.writeLine("go movetime ${tuning.movetimeMs}")
        while (true) {
            val line = pipe.readLine() ?: error("$name closed the pipe mid-search")
            if (line.startsWith("bestmove")) {
                val token = line.split(" ").getOrNull(1) ?: return null
                return Move.fromUci(token)
            }
        }
    }

    private suspend fun handshake(): UciTransport {
        val pipe = openTransport()
        pipe.writeLine("uci")
        while (true) {
            val line = pipe.readLine() ?: error("$name closed the pipe during handshake")
            if (line == "uciok") break
        }
        pipe.writeLine("isready")
        while (true) {
            val line = pipe.readLine() ?: error("$name never became ready")
            if (line == "readyok") break
        }
        return pipe
    }

    override fun close() {
        transport?.close()
        transport = null
        appliedSkill = null
    }
}

/**
 * [primary] with a net: any exception or null falls through to [fallback]. This is how
 * a missing binary, a killed process, or a garbled reply degrades to PocketFish instead
 * of a hung game.
 */
fun resilient(primary: SparringEngine, fallback: SparringEngine): SparringEngine =
    object : SparringEngine {
        override val name: String get() = primary.name

        override suspend fun bestMove(position: Position, tuning: EngineTuning): Move? =
            try {
                primary.bestMove(position, tuning) ?: fallback.bestMove(position, tuning)
            } catch (_: Exception) {
                fallback.bestMove(position, tuning)
            }

        override fun close() {
            primary.close()
            fallback.close()
        }
    }
