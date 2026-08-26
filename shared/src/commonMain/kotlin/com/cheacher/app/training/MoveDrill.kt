package com.cheacher.app.training

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Position
import com.cheacher.app.domain.OpeningTree
import kotlin.random.Random

/** The two directions of the opening-vocabulary drill. */
enum class MoveDrillMode {
    /** Read the canonical name, then find its move on the board. */
    FIND_MOVE,

    /** See the move, then retrieve its canonical name. */
    NAME_IT,
}

/**
 * One authored move from the shelf, flattened into a portable drill card.
 *
 * Cards intentionally are not deduplicated: the same SAN can mean something different
 * after a different path, and "every move" means every authored node, not every distinct
 * string that happened to be used for one.
 */
data class MoveDrillCard(
    val id: String,
    val name: String,
    val san: String,
    val moveNumberLabel: String,
    val move: Move,
    val positionBefore: Position,
    val positionAfter: Position,
    val orientation: Color,
)

/** A completed prompt. [clean] means the first submitted answer was correct. */
data class MoveDrillRep(
    val cardId: String,
    val millis: Long,
    val clean: Boolean,
)

sealed interface MoveDrillEvent {
    data class Correct(val card: MoveDrillCard, val millis: Long) : MoveDrillEvent
    data class WrongMove(val played: Move) : MoveDrillEvent
    data class WrongName(val answer: String) : MoveDrillEvent
}

/**
 * One timed round in one direction. Wrong answers leave the prompt and its clock running,
 * so the recorded number is genuinely time-to-first-correct-answer.
 */
data class MoveDrillState(
    val cards: List<MoveDrillCard>,
    val mode: MoveDrillMode,
    val index: Int = 0,
    val answered: List<MoveDrillRep> = emptyList(),
    val wrongAttempts: Int = 0,
    val promptShownAt: Long,
    val lastEvent: MoveDrillEvent? = null,
) {
    val card: MoveDrillCard? get() = cards.getOrNull(index)
    val finished: Boolean get() = index >= cards.size
    val repNumber: Int get() = (index + 1).coerceAtMost(cards.size)

    val summary: DrillSummary
        get() = DrillSummary.ofTimes(
            times = answered.map { it.millis },
            cleanReps = answered.count { it.clean },
        )

    companion object {
        const val ROUND_LENGTH = 20

        fun start(
            cards: List<MoveDrillCard>,
            mode: MoveDrillMode,
            startedAt: Long,
        ): MoveDrillState = MoveDrillState(cards = cards, mode = mode, promptShownAt = startedAt)
    }
}

fun MoveDrillState.submitMove(move: Move, nowMillis: Long): MoveDrillState {
    if (mode != MoveDrillMode.FIND_MOVE) return this
    val current = card ?: return this
    if (move != current.move) {
        return copy(wrongAttempts = wrongAttempts + 1, lastEvent = MoveDrillEvent.WrongMove(move))
    }
    return advance(current, nowMillis)
}

fun MoveDrillState.submitName(answer: String, nowMillis: Long): MoveDrillState {
    if (mode != MoveDrillMode.NAME_IT) return this
    val current = card ?: return this
    if (normalizeOpeningName(answer) != normalizeOpeningName(current.name)) {
        return copy(wrongAttempts = wrongAttempts + 1, lastEvent = MoveDrillEvent.WrongName(answer))
    }
    return advance(current, nowMillis)
}

private fun MoveDrillState.advance(current: MoveDrillCard, nowMillis: Long): MoveDrillState {
    val elapsed = (nowMillis - promptShownAt).coerceAtLeast(0)
    return copy(
        index = index + 1,
        answered = answered + MoveDrillRep(current.id, elapsed, clean = wrongAttempts == 0),
        wrongAttempts = 0,
        promptShownAt = nowMillis,
        lastEvent = MoveDrillEvent.Correct(current, elapsed),
    )
}

/** Every authored node on every shelf tree, ready to deal in either direction. */
fun moveDrillBank(trees: List<OpeningTree>): List<MoveDrillCard> = trees.flatMap { tree ->
    tree.allNodes.map { node ->
        MoveDrillCard(
            id = "${tree.repertoire.id}:${node.id}",
            name = node.name,
            san = node.san,
            moveNumberLabel = node.moveNumberLabel,
            move = node.move,
            positionBefore = node.positionBefore,
            positionAfter = node.position,
            orientation = tree.repertoire.perspective,
        )
    }
}

/** A fresh round sampled without replacement from the full bank. */
fun dealMoveDrill(
    bank: List<MoveDrillCard>,
    count: Int = MoveDrillState.ROUND_LENGTH,
    random: Random = Random.Default,
): List<MoveDrillCard> = bank.shuffled(random).take(count.coerceAtLeast(0))

/**
 * Ranks canonical names for autocomplete. Exact and prefix matches win first, then a
 * Sørensen–Dice trigram score catches omissions, transpositions, and near spellings.
 */
fun fuzzyOpeningNames(
    query: String,
    names: Collection<String>,
    limit: Int = 5,
): List<String> {
    val needle = normalizeOpeningName(query)
    if (needle.isBlank() || limit <= 0) return emptyList()

    return names.distinct()
        .map { candidate -> candidate to nameMatchScore(needle, normalizeOpeningName(candidate)) }
        .filter { (_, score) -> score > 0 }
        .sortedWith(
            compareByDescending<Pair<String, Int>> { it.second }
                .thenBy { it.first.length }
                .thenBy { it.first },
        )
        .take(limit)
        .map { it.first }
}

private fun nameMatchScore(needle: String, candidate: String): Int {
    if (needle == candidate) return 100_000

    var score = 0
    if (candidate.startsWith(needle)) score += 20_000
    if (candidate.contains(needle)) score += 10_000

    val needleWords = needle.split(' ').filter { it.isNotBlank() }
    val candidateWords = candidate.split(' ').filter { it.isNotBlank() }
    score += needleWords.sumOf { word ->
        when {
            candidateWords.any { it == word } -> 3_000
            candidateWords.any { it.startsWith(word) } -> 1_500
            else -> 0
        }
    }

    val a = trigrams(needle)
    val b = trigrams(candidate)
    if (a.isNotEmpty() && b.isNotEmpty()) {
        val shared = a.intersect(b).size
        score += (10_000 * 2 * shared) / (a.size + b.size)
    }
    return score
}

private fun trigrams(value: String): Set<String> {
    val compact = value.replace(" ", "")
    if (compact.length < 3) return emptySet()
    return (0..compact.length - 3).mapTo(mutableSetOf()) { compact.substring(it, it + 3) }
}

/** Search normalization is deliberately more forgiving than answer display. */
fun normalizeOpeningName(value: String): String = buildString {
    for (character in value.lowercase()) {
        append(
            when (character) {
                'á', 'à', 'â', 'ä', 'ã', 'å' -> 'a'
                'ç' -> 'c'
                'é', 'è', 'ê', 'ë' -> 'e'
                'í', 'ì', 'î', 'ï' -> 'i'
                'ñ' -> 'n'
                'ó', 'ò', 'ô', 'ö', 'õ' -> 'o'
                'ú', 'ù', 'û', 'ü' -> 'u'
                'ý', 'ÿ' -> 'y'
                '’', '‘', '`' -> '\''
                else -> character
            },
        )
    }
}.map { if (it.isLetterOrDigit()) it else ' ' }
    .joinToString("")
    .trim()
    .replace(Regex("\\s+"), " ")
