package com.cheacher.app.domain

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Fen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Two lines sharing 1.e4: enough shape to exercise ids, lines and junctions. */
internal fun tinyRepertoire(): Repertoire = repertoire("tiny", "Tiny", Color.WHITE) {
    move("e4", "King's Pawn Opening", "Centre, quickly.") {
        move("e5", "Open Game", "Symmetry.") {
            move("Nf3", "King's Knight Opening", "Hit e5.")
        }
        move("c5", "Sicilian Defence", "Asymmetry.") {
            move("Nf3", "Open Sicilian, Preparation", "Prepare d4.")
        }
    }
}

class OpeningTreeTest {
    @Test
    fun idsFollowCanonicalMovePaths() {
        val tree = OpeningTree.resolve(tinyRepertoire())
        assertEquals(
            listOf(
                "e2e4",
                "e2e4/e7e5",
                "e2e4/e7e5/g1f3",
                "e2e4/c7c5",
                "e2e4/c7c5/g1f3",
            ),
            tree.allNodes.map { it.id },
        )
        assertEquals("e2e4", tree.node("e2e4/c7c5")?.parentId)
        assertNull(tree.node("e2e4/d7d5"))
    }

    @Test
    fun linesAreRootToLeafPaths() {
        val tree = OpeningTree.resolve(tinyRepertoire())
        assertEquals(2, tree.lines.size)
        assertEquals(listOf("e2e4", "e2e4/e7e5", "e2e4/e7e5/g1f3"), tree.lines[0].map { it.id })
        assertEquals(listOf("e2e4", "e2e4/c7c5", "e2e4/c7c5/g1f3"), tree.lines[1].map { it.id })
    }

    @Test
    fun positionsAreAttachedAndConsistent() {
        val tree = OpeningTree.resolve(tinyRepertoire())
        val sicilian = assertNotNull(tree.node("e2e4/c7c5"))
        assertEquals(Color.WHITE, tree.sideToMoveAt(sicilian))
        assertEquals(
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
            Fen.format(sicilian.position),
        )
        assertEquals(sicilian.positionBefore, assertNotNull(tree.node("e2e4")).position)
        assertEquals("1...c5", sicilian.toString())
    }

    @Test
    fun idsSurviveSiblingInsertionAndReordering() {
        val edited = repertoire("tiny", "Tiny", Color.WHITE) {
            move("e4", "King's Pawn Opening") {
                move("d6", "Modern Defence")
                move("c5", "Sicilian Defence") { move("Nf3", "Open Sicilian, Preparation") }
                move("e5", "Open Game") { move("Nf3", "King's Knight Opening") }
            }
        }
        val before = OpeningTree.resolve(tinyRepertoire())
        val after = OpeningTree.resolve(edited)

        val originalIds = listOf(
            "e2e4",
            "e2e4/e7e5",
            "e2e4/e7e5/g1f3",
            "e2e4/c7c5",
            "e2e4/c7c5/g1f3",
        )
        for (id in originalIds) {
            assertNotNull(before.node(id))
            assertNotNull(after.node(id), "$id changed when a sibling moved")
        }
    }

    @Test
    fun duplicateMovePathsFailAtResolveTime() {
        val duplicate = repertoire("duplicate", "Duplicate", Color.WHITE) {
            move("e4", "First copy")
            move("e4", "Second copy")
        }
        val failure = assertFailsWith<RepertoireFormatException> { OpeningTree.resolve(duplicate) }
        assertEquals(true, failure.message?.contains("move path 'e2e4' more than once"))
    }

    @Test
    fun sanIsCanonicalisedFromSloppyAuthoring() {
        val tree = OpeningTree.resolve(
            repertoire("sloppy", "Sloppy", Color.WHITE) {
                move("e2e4", "King's Pawn Opening") // authored as UCI
            },
        )
        assertEquals("e4", tree.rootChildren.single().san)
    }

    @Test
    fun illegalSanThrowsAtResolveTime() {
        val bad = repertoire("bad", "Bad", Color.WHITE) {
            move("e4", "King's Pawn Opening") {
                move("e4", "Impossible") // white pawn already there, and it is Black's move
            }
        }
        val failure = assertFailsWith<RepertoireFormatException> { OpeningTree.resolve(bad) }
        assertEquals(true, failure.message?.contains("'e4' is not legal"))
    }

    @Test
    fun invalidStartFenThrows() {
        val bad = Repertoire(id = "x", title = "X", perspective = Color.WHITE, startFen = "junk")
        assertFailsWith<RepertoireFormatException> { OpeningTree.resolve(bad) }
    }
}
