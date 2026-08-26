package com.cheacher.app.data

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Fen
import com.cheacher.app.chess.sanOf
import com.cheacher.app.domain.OpeningTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Editorial standards for the shelf as a whole. [SampleRepertoiresTest] proves each book
 * is legal chess; this proves the shelf is a coherent, navigable library.
 */
class ShelfTest {
    private val trees = SampleRepertoires.all.map(OpeningTree::resolve)

    @Test
    fun everyBookHasAUniqueIdAndFindsItselfById() {
        val ids = SampleRepertoires.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate repertoire id on the shelf: $ids")
        for (id in ids) assertEquals(id, SampleRepertoires.byId(id).id)
    }

    @Test
    fun everyBookHasATitleASubtitleAndSomethingToStudy() {
        for (tree in trees) {
            val book = tree.repertoire
            assertTrue(book.title.isNotBlank(), "'${book.id}' has no title")
            assertTrue(book.subtitle.isNotBlank(), "'${book.id}' has no subtitle for the shelf card")
            assertTrue(tree.lines.isNotEmpty(), "'${book.id}' has nothing to study")
        }
    }

    /**
     * A book is a session, not a career. Anything much past this and the ladder stops
     * feeling finishable — split it into two books instead.
     */
    @Test
    fun noBookIsUnfinishablyLarge() {
        for (tree in trees) {
            assertTrue(
                tree.lines.size <= 24,
                "'${tree.repertoire.id}' has ${tree.lines.size} lines — split it",
            )
            assertTrue(
                tree.lines.all { it.size <= 20 },
                "'${tree.repertoire.id}' has a line over 20 plies deep",
            )
        }
    }

    @Test
    fun theShelfCoversEveryMajorFirstMoveForWhite() {
        val firstMoves = trees.flatMap { tree -> tree.repertoire.moves.map { it.san } }.toSet()
        for (san in listOf("e4", "d4", "c4", "Nf3")) {
            assertTrue(san in firstMoves, "no book on the shelf opens with $san")
        }
    }

    /** Every book here is written from White's side of the board. */
    @Test
    fun theShelfIsAWhiteRepertoire() {
        assertTrue(SampleRepertoires.all.all { it.perspective == Color.WHITE })
    }

    /**
     * The vocabulary book's promise is completeness: all twenty legal first moves,
     * each with the name it is known by. Nothing less makes the title honest.
     */
    @Test
    fun twentyFirstMovesIsExactlyTheTwentyLegalFirstMoves() {
        val authored = twentyFirstMoves.moves.map { it.san }
        assertEquals(20, authored.size, "the title says twenty")
        assertEquals(authored.size, authored.toSet().size, "a first move is listed twice")

        val start = Fen.parse(Fen.START)
        val legal = start.legalMoves().map { start.sanOf(it) }.toSet()
        assertEquals(legal, authored.toSet(), "the book and the rules of chess disagree")
    }

    @Test
    fun twentyFirstMovesStaysAVocabularyBookNotATheoryBook() {
        val tree = OpeningTree.resolve(twentyFirstMoves)
        assertEquals(20, tree.lines.size, "one line per first move")
        assertTrue(tree.lines.all { it.size <= 3 }, "three plies is plenty to name a move")
        // Best first, worst last: the ladder should start at e4 and end at the rim knights.
        assertEquals("e4", tree.lines.first().first().san)
        assertEquals("Nh3", tree.lines.last().first().san)
    }

    @Test
    fun theDeepBooksReachTheirNamedTabiyas() {
        val landmarks = mapOf(
            "ruy-lopez" to listOf("Closed Ruy, Main Tabiya", "Berlin Wall Endgame", "Exchange Variation, Main Line"),
            "open-game" to listOf("Scotch Game, Main Line", "King's Gambit Accepted", "Petrov's Defence"),
            "french" to listOf("Winawer Variation", "Advance, Tabiya", "Tarrasch Variation"),
            "caro-kann" to listOf("Classical Caro-Kann", "Panov-Botvinnik Attack"),
            "queens-gambit" to listOf("Slav Defence", "Queen's Gambit Accepted", "Catalan Opening"),
            "indian-defences" to listOf("Nimzo-Indian Defence", "King's Indian Defence", "Grünfeld Defence", "Benko Gambit"),
            "d4-systems" to listOf("London System", "Trompowsky Attack", "Colle System"),
            "english" to listOf("Reversed Sicilian", "Symmetrical English", "Mikenas-Carls Variation"),
            "reti" to listOf("Réti Opening", "King's Indian Attack"),
        )
        for ((id, names) in landmarks) {
            val reached = OpeningTree.resolve(SampleRepertoires.byId(id)).allNodes.map { it.name }.toSet()
            for (name in names) assertTrue(name in reached, "'$id' never reaches '$name'")
        }
    }
}
