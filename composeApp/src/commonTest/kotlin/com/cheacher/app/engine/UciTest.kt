package com.cheacher.app.engine

import com.cheacher.app.chess.Fen
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Position
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Answers like a well-behaved Stockfish, remembers everything it was told. */
private class FakeTransport(private val bestmove: String = "bestmove e2e4 ponder e7e5") : UciTransport {
    val written = mutableListOf<String>()
    var closed = false
    private val replies = ArrayDeque<String>()

    override suspend fun writeLine(line: String) {
        written += line
        when {
            line == "uci" -> replies.addAll(listOf("id name Fake 1", "option name Skill Level type spin", "uciok"))
            line == "isready" -> replies.add("readyok")
            line.startsWith("go") -> replies.addAll(listOf("info depth 1 score cp 30", bestmove))
        }
    }

    override suspend fun readLine(): String? = replies.removeFirstOrNull()

    override fun close() {
        closed = true
    }
}

class UciTest {
    @Test
    fun speaksTheProtocolAndParsesTheBestmove() = runTest {
        val transport = FakeTransport()
        val engine = UciSparringEngine("Fake") { transport }
        val tuning = tuningFor(700)

        val move = engine.bestMove(Position.INITIAL, tuning)

        assertEquals(Move.fromUci("e2e4"), move)
        assertEquals(
            listOf(
                "uci",
                "isready",
                "setoption name Skill Level value ${tuning.skillLevel}",
                "position fen ${Fen.START}",
                "go movetime ${tuning.movetimeMs}",
            ),
            transport.written,
        )
    }

    @Test
    fun handshakesOnceAndRetunesSkillOnlyWhenItChanges() = runTest {
        val transport = FakeTransport()
        val engine = UciSparringEngine("Fake") { transport }

        engine.bestMove(Position.INITIAL, tuningFor(700))
        engine.bestMove(Position.INITIAL, tuningFor(700))
        engine.bestMove(Position.INITIAL, tuningFor(1500))

        assertEquals(1, transport.written.count { it == "uci" })
        assertEquals(
            listOf(
                "setoption name Skill Level value ${tuningFor(700).skillLevel}",
                "setoption name Skill Level value ${tuningFor(1500).skillLevel}",
            ),
            transport.written.filter { it.startsWith("setoption") },
        )
    }

    @Test
    fun closeTearsDownTheTransport() = runTest {
        val transport = FakeTransport()
        val engine = UciSparringEngine("Fake") { transport }
        engine.bestMove(Position.INITIAL, tuningFor(700))
        engine.close()
        assertTrue(transport.closed)
    }

    @Test
    fun resilientFallsBackWhenThePrimaryDiesOrTalksNonsense() = runTest {
        val dying = object : SparringEngine {
            override val name = "Dying"
            override suspend fun bestMove(position: Position, tuning: EngineTuning): Move? =
                error("process is gone")
        }
        val engine = resilient(dying, PocketFish(Random(5)))
        val move = engine.bestMove(Position.INITIAL, tuningFor(700))
        assertTrue(move != null && move in Position.INITIAL.legalMoves())

        // A closed pipe mid-handshake surfaces as an error too — same net.
        val silent = UciSparringEngine("Silent") {
            object : UciTransport {
                override suspend fun writeLine(line: String) {}
                override suspend fun readLine(): String? = null
                override fun close() {}
            }
        }
        val rescued = resilient(silent, PocketFish(Random(5)))
        val second = rescued.bestMove(Position.INITIAL, tuningFor(700))
        assertTrue(second != null && second in Position.INITIAL.legalMoves())
    }
}
