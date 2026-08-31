package com.cheacher.app.ui.screens

import com.cheacher.app.chess.Move
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.progress.ProgressStore
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.progress.currentEpochMillis
import com.cheacher.app.training.GuidedEvent
import com.cheacher.app.training.GuidedState
import com.cheacher.app.training.Progression
import com.cheacher.app.training.ProgressionAdvance
import com.cheacher.app.training.StudyKind
import com.cheacher.app.training.advanceFrom
import com.cheacher.app.training.trunkNodeIds
import com.cheacher.app.training.restartLine
import com.cheacher.app.training.revealIdea
import com.cheacher.app.training.submit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Appends one pure transform to the training history. Handed to session models by the
 * app root, which runs the write on its own long-lived scope — a session leaving the
 * screen must never cancel a journal entry mid-flight.
 */
typealias Journal = (transform: (TrainingRecord) -> TrainingRecord) -> Unit

/**
 * Thin shell around the pure [GuidedState] reducer: holds the value, forwards intents,
 * and journals what happened — misses per node, lines walked, sessions finished. The
 * reducer stays pure; history is a side effect that lives here.
 *
 * Deliberately *not* an androidx ViewModel: a session belongs to one visit to one
 * screen, so it is remembered in the composition and its [scope] (a
 * `rememberCoroutineScope`) dies with it. That makes stale-session reuse impossible by
 * construction — a new navigation is a new model — and leaves nothing watching flows
 * after the screen is gone. Only [Journal] writes outlive the visit, by design.
 *
 * [lineIndices] is the study-plan gate, decided at navigation time: null walks the
 * whole book, otherwise the session is exactly those lines (in practice, the deal).
 * [kind] decides the rules: LEARN sessions run the mastery loop and end only when every
 * dealt line has a clean unaided walk; REVIEW sessions are one pass, score as scored.
 *
 * [entryPly] is the earned road in ([com.cheacher.app.training.OpeningEntry]), also
 * decided at navigation time: every line opens that many shared plies deep.
 *
 * [wrongShakes] is a monotonic counter (not derived from state) because two identical
 * wrong attempts produce equal events — the UI needs a value that always changes to
 * replay the shake.
 */
class GuidedViewModel(
    private val tree: OpeningTree,
    progress: ProgressStore,
    lineIndices: List<Int>?,
    val kind: StudyKind,
    entryPly: Int = 0,
    priorCredits: Map<Int, Double> = emptyMap(),
    private val scope: CoroutineScope,
    private val journal: Journal,
) {
    private val _state =
        MutableStateFlow(
            GuidedState.start(
                tree,
                lineIndices,
                masteryLoop = kind == StudyKind.LEARN,
                entryPly = entryPly,
                priorCredits = priorCredits,
            ),
        )
    val state: StateFlow<GuidedState> = _state.asStateFlow()

    private val _wrongShakes = MutableStateFlow(0)
    val wrongShakes: StateFlow<Int> = _wrongShakes.asStateFlow()

    private val _unlock = MutableStateFlow<UnlockBanner?>(null)

    /** The "new branch unlocked" moment, when this session's completions move the frontier. */
    val unlock: StateFlow<UnlockBanner?> = _unlock.asStateFlow()

    init {
        journal { it.recordSessionStart(currentEpochMillis()) }
        // Everything dealt was already banked clean in an earlier visit — the book was
        // finished move by move, only the closing session never landed. Close it now
        // rather than making the learner walk a book they have already answered.
        if (_state.value.finished) journalSessionComplete()
        watchFrontier(scope, tree, tree.repertoire.id, progress, _unlock)
    }

    fun onMove(move: Move) {
        val current = _state.value
        val next = current.submit(move)
        _state.value = next
        // Arriving at the opening proper, unaided and unmissed, is the whole entry toll:
        // later sessions of this book start here instead of replaying the road in.
        if (next.roadInWalkedClean && !current.roadInWalkedClean) {
            journal { it.recordTrunkCleared() }
        }
        when (val event = next.lastEvent) {
            is GuidedEvent.Wrong -> {
                _wrongShakes.update { it + 1 }
                // The exact move comes back on its own clock. A guided miss must not
                // rewrite every line review that happens to share the position.
                journal { it.recordMiss(event.expected.id) }
                // Forgetting one *on the road in* also hands the entry back: sessions go
                // back to starting at move one until it is walked clean again.
                if (event.expected.id in tree.trunkNodeIds()) journal { it.recordTrunkFumbled() }
            }
            is GuidedEvent.LineComplete -> journal {
                it.recordLineCompleted(event.line.last().id)
                    .recordLineCredit(event.line.last().id, event.credit)
            }
            GuidedEvent.SessionComplete -> if (!current.finished) {
                val lastLeaf = current.currentLine.lastOrNull()
                // The final line's credit, banked by the same submit that ended the session.
                val credit = current.passLines.getOrNull(current.lineIndex)?.let { next.lineCredits[it] }
                journalSessionComplete { r ->
                    lastLeaf?.let {
                        r.recordLineCompleted(it.id).recordLineCredit(it.id, credit ?: 1.0)
                    } ?: r
                }
            }
            else -> Unit
        }
    }

    /**
     * Closes the session in the journal, after [walked] writes whatever the finishing
     * move itself earned. The opening's review clock rolls on the credits just written:
     * a fully accounted book starts (or grows) its streak, a slipped review resets it.
     */
    private fun journalSessionComplete(walked: (TrainingRecord) -> TrainingRecord = { it }) {
        val leafIds = tree.lines.map { it.last().id }
        val at = currentEpochMillis()
        journal { r ->
            walked(r).recordGuidedSessionCompleted().recordOpeningOutcome(leafIds, at)
        }
    }

    fun revealIdea() = _state.update { it.revealIdea() }

    fun restartLine() = _state.update { it.restartLine() }

    fun restartSession() {
        _state.update {
            GuidedState.start(it.tree, it.lineIndices, it.masteryLoop, it.entryPly, it.priorCredits)
        }
        journal { it.recordSessionStart(currentEpochMillis()) }
    }

    fun dismissUnlock() = _unlock.update { null }
}

/** One frontier move. [serial] exists because equal advances must still replay their moment. */
data class UnlockBanner(val advance: ProgressionAdvance, val serial: Int)

/**
 * Folds the record flow into consecutive [Progression]s and posts a banner whenever the
 * frontier advances. Shared by both session models because mastery can flip in either
 * mode — whichever half of "name it once, recall it once" lands last.
 */
internal fun watchFrontier(
    scope: CoroutineScope,
    tree: OpeningTree,
    repertoireId: String,
    progress: ProgressStore,
    into: MutableStateFlow<UnlockBanner?>,
) {
    scope.launch {
        var previous: Progression? = null
        progress.records
            .map { it[repertoireId] ?: TrainingRecord.empty(repertoireId) }
            .collect { record ->
                val next = Progression(tree, record)
                val before = previous
                previous = next
                val advance = before?.let { next.advanceFrom(it) } ?: return@collect
                into.update { UnlockBanner(advance, (it?.serial ?: 0) + 1) }
            }
    }
}
