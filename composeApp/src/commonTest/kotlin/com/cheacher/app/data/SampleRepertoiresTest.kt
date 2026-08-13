package com.cheacher.app.data

import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.Repertoire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

/**
 * The samples are content, and content is where bugs hide. Resolution alone proves
 * every SAN token legal in its exact position; the rest checks editorial standards.
 */
class SampleRepertoiresTest {
    @Test
    fun everySampleResolvesWithoutErrors() {
        for (repertoire in SampleRepertoires.all) {
            val tree = OpeningTree.resolve(repertoire) // throws on any illegal move
            assertTrue(tree.lines.isNotEmpty(), "'${repertoire.id}' must contain at least one line")
        }
    }

    @Test
    fun everyMoveHasANameAndAnIdea() {
        for (repertoire in SampleRepertoires.all) {
            for (node in OpeningTree.resolve(repertoire).allNodes) {
                assertTrue(node.name.isNotBlank(), "unnamed move ${node.id} in '${repertoire.id}'")
                assertTrue(node.idea.isNotBlank(), "move ${node.id} in '${repertoire.id}' has no idea text")
            }
        }
    }

    @Test
    fun sicilianBranchesIntoOpenAndClosed() {
        val tree = OpeningTree.resolve(SampleRepertoires.sicilianCrossroads)
        val afterC5 = tree.node("0.0") ?: error("missing 1...c5")
        assertEquals("Sicilian Defence", afterC5.name)
        assertEquals(
            listOf("Nf3", "Nc3"),
            afterC5.children.map { it.san },
            "the crossroads: Open Sicilian prep vs Closed Sicilian",
        )
    }

    @Test
    fun italianCoversThreeBlackReplies() {
        val tree = OpeningTree.resolve(SampleRepertoires.italianGame)
        val italianPosition = tree.allNodes.first { it.name == "Italian Game" }
        assertEquals(
            listOf("Giuoco Piano", "Two Knights Defence", "Hungarian Defence"),
            italianPosition.children.map { it.name },
        )
    }

    @Test
    fun repertoiresSurviveJsonRoundTrip() {
        val json = Json { ignoreUnknownKeys = true }
        for (repertoire in SampleRepertoires.all) {
            val decoded = json.decodeFromString<Repertoire>(json.encodeToString(repertoire))
            assertEquals(repertoire, decoded)
            // And the decoded copy still resolves to the same tree shape.
            assertEquals(
                OpeningTree.resolve(repertoire).allNodes.map { it.id },
                OpeningTree.resolve(decoded).allNodes.map { it.id },
            )
        }
    }
}
