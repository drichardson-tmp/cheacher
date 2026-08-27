package com.cheacher.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cheacher.app.data.SampleRepertoires
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.engine.SparringElo
import com.cheacher.app.progress.AppSettings
import com.cheacher.app.progress.ProgressStore
import com.cheacher.app.progress.ProgressStoreProvider
import com.cheacher.app.progress.StoreHealth
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.progress.currentEpochMillis
import com.cheacher.app.progress.withStableNodeIds
import com.cheacher.app.training.MistakePolicy
import com.cheacher.app.training.OpeningEntry
import com.cheacher.app.training.OpeningStanding
import com.cheacher.app.training.Progression
import com.cheacher.app.training.StudyKind
import com.cheacher.app.training.moveDrillBank
import com.cheacher.app.training.studyPlan
import com.cheacher.app.training.syllabusAt
import com.cheacher.app.ui.screens.BranchScreen
import com.cheacher.app.ui.screens.BranchViewModel
import com.cheacher.app.ui.screens.GuidedScreen
import com.cheacher.app.ui.screens.GuidedViewModel
import com.cheacher.app.ui.screens.HomeScreen
import com.cheacher.app.ui.screens.MoveDrillScreen
import com.cheacher.app.ui.screens.MoveDrillViewModel
import com.cheacher.app.ui.screens.SquareDrillScreen
import com.cheacher.app.ui.screens.SquareDrillViewModel
import com.cheacher.app.ui.screens.Journal
import com.cheacher.app.ui.screens.PlayOutScreen
import com.cheacher.app.ui.screens.PlayOutViewModel
import com.cheacher.app.ui.theme.CheacherTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * Where the learner is. A sealed value instead of a nav library: a splash beat, the
 * shelf, and two session screens. Session screens carry their study-plan gate
 * ([Guided.lineIndices], [Branch.allowedNodeIds]) as a *snapshot taken at navigation
 * time* — a session's deal must not shift because a record update landed mid-round, and
 * a moved frontier simply produces a new screen value (and a fresh session model) next
 * visit.
 */
sealed interface Screen {
    /** The launch beat: records are loading and the first session is being dealt. */
    data object Dealing : Screen

    data object Home : Screen

    data class Guided(
        val repertoireId: String,
        /** Null walks the whole book; otherwise exactly these lines. */
        val lineIndices: List<Int>?,
        /** Whether this session is study or retrieval — it sets the rules and the copy. */
        val kind: StudyKind,
        /** Shared opening plies already behind the learner — see [OpeningEntry]. */
        val entryPly: Int = 0,
        /** Distinguishes consecutive deals of the same opening, so Continue always restarts. */
        val serial: Int = 0,
    ) : Screen

    /** The square drill: no repertoire, no gate, no snapshot — it belongs to no opening. */
    data object SquareDrill : Screen

    /** Shelf-wide opening vocabulary in both directions, independent of progression. */
    data object MoveDrill : Screen

    data class Branch(
        val repertoireId: String,
        val policy: MistakePolicy,
        val oneSided: Boolean,
        /** Null opens the whole tree; otherwise nodes outside this set start locked. */
        val allowedNodeIds: Set<String>?,
        /** The earned trunk end the round opens on, or null to start at move one. */
        val entryNodeId: String? = null,
    ) : Screen

    /**
     * The optional epilogue: play the game out from the end of one book line against
     * the sparring engine. [engineElo] is the learner's sparring rating, snapshotted at
     * navigation like every other gate — mid-game record updates must not retune a
     * running opponent.
     */
    data class PlayOut(
        val repertoireId: String,
        /** Leaf node whose line's final position the game continues from. */
        val leafNodeId: String,
        val engineElo: Int,
    ) : Screen
}

/**
 * Owns navigation, the resolved trees, and the journal. Resolution happens once, here,
 * at startup — if a sample repertoire had an illegal move the app would refuse to
 * launch, which is exactly the contract [OpeningTree.resolve] promises.
 *
 * This is the one androidx ViewModel in the app, because it is the one thing with
 * app-scoped lifetime. Session models are composition-scoped values (see
 * [GuidedViewModel]); their journal writes ride this ViewModel's scope via [journalFor]
 * so a session leaving the screen never cancels a write mid-flight.
 */
class RootViewModel(val progress: ProgressStore) : ViewModel() {
    val trees: List<OpeningTree> = SampleRepertoires.all.map(OpeningTree::resolve)

    /**
     * Live view of everything remembered, for the shelf's stats lines. Null until the
     * store's first emission: "not read yet" and "empty" are different truths, and
     * navigation must not draw a syllabus from the wrong one.
     */
    val records: StateFlow<Map<String, TrainingRecord>?> =
        progress.records
            .map<Map<String, TrainingRecord>, Map<String, TrainingRecord>?> { stored ->
                stored.mapValues { (repertoireId, record) ->
                    trees.firstOrNull { it.repertoire.id == repertoireId }
                        ?.let(record::withStableNodeIds)
                        ?: record
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The store's condition — the shelf mentions it when something can't be read or saved. */
    val storeHealth: StateFlow<StoreHealth> =
        progress.health.stateIn(viewModelScope, SharingStarted.Eagerly, StoreHealth())

    /** App-wide preferences, emitted by the same durable store as training history. */
    val settings: StateFlow<AppSettings?> =
        progress.settings
            .map<AppSettings, AppSettings?> { it }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _screen = MutableStateFlow<Screen>(Screen.Dealing)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    /** Monotonic deal counter — consecutive deals of the same opening must still be new screens. */
    private var dealSerial = 0

    private val _policy = MutableStateFlow(MistakePolicy.STRICT)
    val policy: StateFlow<MistakePolicy> = _policy.asStateFlow()

    private val _oneSided = MutableStateFlow(false)
    val oneSided: StateFlow<Boolean> = _oneSided.asStateFlow()

    /**
     * True opens every branch at once; false (the default) follows the coach's plan.
     * Never a lock, always a preference: the full tree is one tap away.
     */
    private val _fullTree = MutableStateFlow(false)
    val fullTree: StateFlow<Boolean> = _fullTree.asStateFlow()

    /** Journal writes still in flight — navigation waits for zero so syllabi never lag truth. */
    private val pendingJournalWrites = MutableStateFlow(0)

    init {
        // Straight to the board: the app opens mid-study, never on a menu. This block
        // sits below every property deal() touches — viewModelScope.launch on the
        // immediate main dispatcher runs the coroutine body during construction, so an
        // earlier init would read pendingJournalWrites before it exists.
        deal()
    }

    fun tree(id: String): OpeningTree = trees.first { it.repertoire.id == id }

    fun setPolicy(policy: MistakePolicy) {
        _policy.value = policy
    }

    fun setOneSided(oneSided: Boolean) {
        _oneSided.value = oneSided
    }

    fun setFullTree(fullTree: Boolean) {
        _fullTree.value = fullTree
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            progress.updateSettings { it.copy(hapticsEnabled = enabled) }
        }
    }

    /** A [Journal] for one repertoire, writing on this app-scoped ViewModel's lifetime. */
    fun journalFor(tree: OpeningTree): Journal = journalForId(tree.repertoire.id) { record ->
        record.withStableNodeIds(tree)
    }

    /** The drill saves under a reserved id rather than an opening's — see [TrainingRecord.DRILL_RECORD_ID]. */
    fun journalForId(repertoireId: String): Journal = journalForId(repertoireId) { it }

    private fun journalForId(
        repertoireId: String,
        prepare: (TrainingRecord) -> TrainingRecord,
    ): Journal {
        return { transform ->
            pendingJournalWrites.update { it + 1 }
            viewModelScope.launch {
                try {
                    progress.update(repertoireId) { record -> transform(prepare(record)) }
                } finally {
                    pendingJournalWrites.update { it - 1 }
                }
            }
        }
    }

    /**
     * The committed record for [tree]: waits for in-flight journal writes to land and for
     * the store's first emission, so a snapshot drawn from it is never stale and never
     * mistakes "not loaded" for "new learner".
     */
    private suspend fun settledRecordFor(tree: OpeningTree): TrainingRecord {
        pendingJournalWrites.first { it == 0 }
        val all = records.filterNotNull().first()
        return all[tree.repertoire.id] ?: TrainingRecord.empty(tree.repertoire.id)
    }

    /**
     * Deals the next session from the shelf-wide study plan: due opening reviews first,
     * then the first unlearned opening's remaining lines. Called at launch and by every
     * "Continue" — the learner rides the plan without ever choosing from a list.
     */
    fun deal() {
        viewModelScope.launch {
            pendingJournalWrites.first { it == 0 }
            val all = records.filterNotNull().first()
            val task = studyPlan(trees, all, currentEpochMillis()).firstOrNull()
            _screen.value = task?.let {
                val record = all[it.tree.repertoire.id] ?: TrainingRecord.empty(it.tree.repertoire.id)
                Screen.Guided(
                    repertoireId = it.tree.repertoire.id,
                    lineIndices = it.lineIndices,
                    kind = it.kind,
                    entryPly = entryPlyFor(it.tree, record, it.kind),
                    serial = ++dealSerial,
                )
            } ?: Screen.Home
        }
    }

    fun openGuided(tree: OpeningTree) {
        viewModelScope.launch {
            val record = settledRecordFor(tree)
            val standing = OpeningStanding(tree, record)
            val kind = if (standing.learned) StudyKind.REVIEW else StudyKind.LEARN
            val lineIndices = when {
                _fullTree.value -> null
                kind == StudyKind.LEARN -> standing.learnDeal
                else -> null
            }
            _screen.value = Screen.Guided(
                repertoireId = tree.repertoire.id,
                lineIndices = lineIndices,
                kind = kind,
                entryPly = entryPlyFor(tree, record, kind),
                serial = ++dealSerial,
            )
        }
    }

    /**
     * How deep a session opens into [tree]. LEARN sessions ride the earned entry — the
     * road in was proven the first time it was walked perfectly, and replaying it before
     * each of the next ten lines is toll, not practice. Reviews always start at move one:
     * a review asks whether the whole thing still holds, and getting to the opening is
     * part of the whole thing.
     *
     * "Show the full tree" opts out of every gate, this one included.
     */
    private fun entryPlyFor(tree: OpeningTree, record: TrainingRecord, kind: StudyKind): Int =
        if (_fullTree.value || kind == StudyKind.REVIEW) 0 else OpeningEntry(tree, record).entryPly

    fun openBranch(tree: OpeningTree) {
        viewModelScope.launch {
            val record = settledRecordFor(tree)
            val allowed = if (_fullTree.value) {
                null
            } else {
                Progression(tree, record).syllabusAt(currentEpochMillis()).branchAllowedNodeIds
            }
            val entry = if (_fullTree.value) null else OpeningEntry(tree, record).entryNode?.id
            _screen.value =
                Screen.Branch(tree.repertoire.id, _policy.value, _oneSided.value, allowed, entry)
        }
    }

    fun openPlayOut(tree: OpeningTree, leafNodeId: String) {
        viewModelScope.launch {
            val record = settledRecordFor(tree)
            _screen.value = Screen.PlayOut(tree.repertoire.id, leafNodeId, record.sparring.rating)
        }
    }

    fun openSquareDrill() {
        _screen.value = Screen.SquareDrill
    }

    fun openMoveDrill() {
        _screen.value = Screen.MoveDrill
    }

    fun home() {
        _screen.value = Screen.Home
    }
}

@Composable
fun App() {
    CheacherTheme {
        val root: RootViewModel = viewModel { RootViewModel(ProgressStoreProvider.store) }
        val screen by root.screen.collectAsStateWithLifecycle()
        val policy by root.policy.collectAsStateWithLifecycle()
        val oneSided by root.oneSided.collectAsStateWithLifecycle()
        val fullTree by root.fullTree.collectAsStateWithLifecycle()
        val records by root.records.collectAsStateWithLifecycle()
        val health by root.storeHealth.collectAsStateWithLifecycle()
        val settings by root.settings.collectAsStateWithLifecycle()
        // A persisted opt-out must win even during the store's first read. Feedback is
        // therefore quiet until settings arrive; a fresh install emits the default on.
        val hapticsEnabled = settings?.hapticsEnabled == true

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                if (targetState == Screen.Home) {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                }
            },
            label = "screen",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
        ) { current ->
            when (current) {
                // The launch beat: just the wordmark on parchment while the first deal lands.
                is Screen.Dealing -> DealingScreen()

                is Screen.Home -> HomeScreen(
                    trees = root.trees,
                    records = records,
                    health = health,
                    nowEpochMillis = currentEpochMillis(),
                    timeZone = TimeZone.currentSystemDefault(),
                    policy = policy,
                    oneSided = oneSided,
                    fullTree = fullTree,
                    hapticsEnabled = hapticsEnabled,
                    onPolicyChange = root::setPolicy,
                    onOneSidedChange = root::setOneSided,
                    onFullTreeChange = root::setFullTree,
                    onHapticsEnabledChange = root::setHapticsEnabled,
                    onOpenGuided = root::openGuided,
                    onOpenBranch = root::openBranch,
                    onOpenSquareDrill = root::openSquareDrill,
                    onOpenMoveDrill = root::openMoveDrill,
                    drill = records?.get(TrainingRecord.DRILL_RECORD_ID)?.squareDrill,
                    moveDrill = records?.get(TrainingRecord.DRILL_RECORD_ID)?.moveDrill,
                )

                is Screen.SquareDrill -> {
                    val vm = remember(current) {
                        SquareDrillViewModel(journal = root.journalForId(TrainingRecord.DRILL_RECORD_ID))
                    }
                    SquareDrillScreen(
                        viewModel = vm,
                        hapticsEnabled = hapticsEnabled,
                        onBack = root::home,
                    )
                }

                is Screen.MoveDrill -> {
                    val bank = remember(root.trees) { moveDrillBank(root.trees) }
                    val vm = remember(current) {
                        MoveDrillViewModel(
                            bank = bank,
                            journal = root.journalForId(TrainingRecord.DRILL_RECORD_ID),
                        )
                    }
                    MoveDrillScreen(
                        viewModel = vm,
                        bankSize = bank.size,
                        distinctNames = bank.map { it.name }.distinct().size,
                        hapticsEnabled = hapticsEnabled,
                        onBack = root::home,
                    )
                }

                is Screen.Guided -> {
                    val tree = root.tree(current.repertoireId)
                    // One visit, one model: remembered in this content's composition and
                    // gone with it, so a later visit can never resume a finished session.
                    val sessionScope = rememberCoroutineScope()
                    val vm = remember(current) {
                        GuidedViewModel(
                            tree = tree,
                            progress = root.progress,
                            lineIndices = current.lineIndices,
                            kind = current.kind,
                            entryPly = current.entryPly,
                            scope = sessionScope,
                            journal = root.journalFor(tree),
                        )
                    }
                    GuidedScreen(
                        viewModel = vm,
                        hapticsEnabled = hapticsEnabled,
                        onContinue = root::deal,
                        onBack = root::home,
                        sparringElo = records?.get(current.repertoireId)?.sparring?.rating
                            ?: SparringElo.START,
                        onPlayOut = { leafId -> root.openPlayOut(tree, leafId) },
                    )
                }

                is Screen.Branch -> {
                    val tree = root.tree(current.repertoireId)
                    val autoReplyFor = if (current.oneSided) tree.repertoire.perspective.opposite else null
                    val sessionScope = rememberCoroutineScope()
                    val vm = remember(current) {
                        BranchViewModel(
                            tree = tree,
                            policy = current.policy,
                            autoReplyFor = autoReplyFor,
                            progress = root.progress,
                            allowedNodeIds = current.allowedNodeIds,
                            entryNodeId = current.entryNodeId,
                            scope = sessionScope,
                            journal = root.journalFor(tree),
                        )
                    }
                    BranchScreen(
                        viewModel = vm,
                        hapticsEnabled = hapticsEnabled,
                        onBack = root::home,
                    )
                }

                is Screen.PlayOut -> {
                    val tree = root.tree(current.repertoireId)
                    val sessionScope = rememberCoroutineScope()
                    val vm = remember(current) {
                        PlayOutViewModel(
                            tree = tree,
                            leafId = current.leafNodeId,
                            engineElo = current.engineElo,
                            scope = sessionScope,
                            journal = root.journalFor(tree),
                        )
                    }
                    PlayOutScreen(
                        viewModel = vm,
                        openingTitle = tree.repertoire.title,
                        onBack = root::home,
                        onContinue = root::deal,
                    )
                }
            }
        }
    }
}

/**
 * What shows for the beat between launch and the first dealt board: the wordmark alone.
 * Deliberately not a spinner — the store read is fast, and a flash of quiet parchment
 * reads as intent, not latency.
 */
@Composable
private fun DealingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Cheacher", style = MaterialTheme.typography.displaySmall)
    }
}
