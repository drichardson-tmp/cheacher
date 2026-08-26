package com.cheacher.app.engine

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Fen
import com.cheacher.app.chess.Position
import com.cheacher.app.progress.TrainingRecord
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SparringTest {
    @Test
    fun eloMovesHalfKPerDecisiveGameAndClamps() {
        assertEquals(732, SparringElo.updated(700, 1.0))
        assertEquals(668, SparringElo.updated(700, 0.0))
        assertEquals(700, SparringElo.updated(700, 0.5))
        assertEquals(SparringElo.MIN, SparringElo.updated(SparringElo.MIN, 0.0))
        assertEquals(SparringElo.MAX, SparringElo.updated(SparringElo.MAX, 1.0))
    }

    @Test
    fun tuningGetsStrongerAsEloRises() {
        val beginner = tuningFor(700)
        val club = tuningFor(1500)
        assertTrue(beginner.depth < club.depth)
        assertTrue(beginner.temperatureCp > club.temperatureCp)
        assertTrue(beginner.blunderChance > club.blunderChance)
        assertTrue(beginner.skillLevel < club.skillLevel)
        assertTrue(beginner.movetimeMs < club.movetimeMs)
        assertEquals(0.0, club.blunderChance, "no manufactured blunders above 1200")
        assertEquals(0.2, beginner.blunderChance, 1e-9)
    }

    @Test
    fun materialDeficitReadsThePositionFromOneSide() {
        assertEquals(0, materialDeficit(Position.INITIAL, Color.WHITE))
        // Black is missing the queen.
        val position = Fen.parse("rnb1kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(9, materialDeficit(position, Color.BLACK))
        assertEquals(0, materialDeficit(position, Color.WHITE), "being ahead is not a deficit")
    }

    @Test
    fun rubberBandEasesTheEngineWhenTheLearnerIsBehind() {
        assertEquals(700, rubberBanded(700, 0))
        assertEquals(580, rubberBanded(700, 2))
        assertEquals(SparringElo.MIN, rubberBanded(700, 9), "capped at five pawns and the floor")
        assertEquals(1700, rubberBanded(1700, 0), "winning never hardens the engine")
    }

    @Test
    fun sparringGamesUpdateTheRecordLedger() {
        val record = TrainingRecord.empty("r")
            .recordSparringGame(1.0)
            .recordSparringGame(0.0)
            .recordSparringGame(0.5)
        assertEquals(700, record.sparring.rating, "win then loss then draw is a round trip")
        assertEquals(3, record.sparring.gamesPlayed)
        assertEquals(1, record.sparring.wins)
        assertEquals(1, record.sparring.draws)
        assertEquals(1, record.sparring.losses)
    }

    @Test
    fun sparringLedgerSurvivesASerializationRoundTrip() {
        val record = TrainingRecord.empty("r").recordSparringGame(1.0).recordSparringGame(1.0)
        val decoded = Json.decodeFromString<TrainingRecord>(Json.encodeToString(TrainingRecord.serializer(), record))
        assertEquals(record, decoded)
        assertEquals(764, decoded.sparring.rating)
    }

    @Test
    fun legacyRecordsDecodeWithADefaultSparringLedger() {
        val json = Json { ignoreUnknownKeys = true }
        val record = json.decodeFromString<TrainingRecord>("""{"repertoire_id":"old"}""")
        assertEquals(SparringElo.START, record.sparring.rating)
        assertEquals(0, record.sparring.gamesPlayed)
    }
}
