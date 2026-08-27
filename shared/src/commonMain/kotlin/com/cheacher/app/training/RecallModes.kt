package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Position
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.TreeNode
import kotlin.random.Random

/** Rapid-fire formation recognition: every correct move deals a completely unrelated board. */
data class BlitzState(
    val cards: List<MoveDrillCard>,
    val index: Int = 0,
    val answered: List<MoveDrillRep> = emptyList(),
    val wrongAttempts: Int = 0,
    val promptShownAt: Long,
    val lastCorrect: Boolean? = null,
) {
    val card: MoveDrillCard? get() = cards.getOrNull(index)
    val finished: Boolean get() = index >= cards.size
    val summary: DrillSummary
        get() = DrillSummary.ofTimes(answered.map { it.millis }, answered.count { it.clean })

    companion object {
        const val DEFAULT_PLAYS = 10
        fun start(cards: List<MoveDrillCard>, startedAt: Long) = BlitzState(cards = cards, promptShownAt = startedAt)
    }
}

fun BlitzState.submit(move: Move, nowMillis: Long): BlitzState {
    val target = card ?: return this
    if (move != target.move) return copy(wrongAttempts = wrongAttempts + 1, lastCorrect = false)
    return copy(
        index = index + 1,
        answered = answered + MoveDrillRep(
            target.id,
            (nowMillis - promptShownAt).coerceAtLeast(0),
            clean = wrongAttempts == 0,
        ),
        wrongAttempts = 0,
        promptShownAt = nowMillis,
        lastCorrect = true,
    )
}

fun dealBlitz(bank: List<MoveDrillCard>, count: Int, random: Random = Random.Default): List<MoveDrillCard> =
    bank.shuffled(random).take(count.coerceIn(1, bank.size.coerceAtLeast(1)))

/** One authored destination with its exact root-to-leaf sequence. */
data class QuietCard(
    val id: String,
    val targetName: String,
    val line: List<TreeNode>,
    val orientation: Color,
)

data class QuietState(
    val card: QuietCard,
    val index: Int = 0,
    val wrongAttempts: Int = 0,
    val hintRevealed: Boolean = false,
    val lastCorrect: Boolean? = null,
) {
    val expected: TreeNode? get() = card.line.getOrNull(index)
    val position: Position get() = card.line.getOrNull(index - 1)?.position ?: card.line.first().positionBefore
    val finished: Boolean get() = index >= card.line.size
    val progress: Float get() = if (card.line.isEmpty()) 1f else index.toFloat() / card.line.size
    /** Recovery names only the current move; no future move or intermediary opening name leaks. */
    val hint: String?
        get() = expected?.takeIf { hintRevealed }?.let { "${it.moveNumberLabel}${it.san} — ${it.idea}" }
}

fun QuietState.submit(move: Move): QuietState {
    val target = expected ?: return this
    if (move != target.move) {
        return copy(wrongAttempts = wrongAttempts + 1, hintRevealed = true, lastCorrect = false)
    }
    return copy(index = index + 1, hintRevealed = false, lastCorrect = true)
}

fun QuietState.revealHint(): QuietState = if (finished) this else copy(hintRevealed = true)

fun quietBank(trees: List<OpeningTree>): List<QuietCard> = trees.flatMap { tree ->
    tree.lines.map { line -> QuietCard("${tree.repertoire.id}:${line.last().id}", line.last().name, line, tree.repertoire.perspective) }
}
