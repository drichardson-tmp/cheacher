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
import com.cheacher.app.progress.ProgressStore
import com.cheacher.app.progress.ProgressStoreProvider
import com.cheacher.app.progress.StoreHealth
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.progress.currentEpochMillis
import com.cheacher.app.training.MistakePolicy
import com.cheacher.app.training.OpeningStanding
import com.cheacher.app.training.Progression
import com.cheacher.app.training.StudyKind
import com.cheacher.app.training.studyPlan
import com.cheacher.app.training.syllabusAt
import com.cheacher.app.ui.screens.BranchScreen
import com.cheacher.app.ui.screens.BranchViewModel
import com.cheacher.app.ui.screens.GuidedScreen
import com.cheacher.app.ui.screens.GuidedViewModel
import com.cheacher.app.ui.screens.HomeScreen
import com.cheacher.app.ui.screens.Journal
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
        /** Distinguishes consecutive deals of the same opening, so Continue always restarts. */
        val serial: Int = 0,
    ) : Screen

    data class Branch(
        val repertoireId: String,
        val policy: MistakePolicy,
        val oneSided: Boolean,
        /** Null opens the whole tree; otherwise nodes outside this set start locked. */
        val allowedNodeIds: Set<String>?,
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
            .map<Map<String, TrainingRecord>, Map<String, TrainingRecord>?> { it }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The store's condition — the shelf mentions it when something can't be read or saved. */
    val storeHealth: StateFlow<StoreHealth> =
        progress.health.stateIn(viewModelScope, SharingStarted.Eagerly, StoreHealth())

    private val _screen = MutableStateFlow<Screen>(Screen.Dealing)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    /** Monotonic deal counter — consecutive deals of the same opening must still be new screens. */
    private var dealSerial = 0

    init {
        // Straight to the board: the app opens mid-study, never on a menu.
        deal()
    }

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

    /** A [Journal] for one repertoire, writing on this app-scoped ViewModel's lifetime. */
    fun journalFor(tree: OpeningTree): Journal {
        val repertoireId = tree.repertoire.id
        return { transform ->
            pendingJournalWrites.update { it + 1 }
            viewModelScope.launch {
                try {
                    progress.update(repertoireId, transform)
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
                Screen.Guided(it.tree.repertoire.id, it.lineIndices, it.kind, serial = ++dealSerial)
            } ?: Screen.Home
        }
    }

    fun openGuided(tree: OpeningTree) {
        viewModelScope.launch {
            val standing = OpeningStanding(tree, settledRecordFor(tree))
            val kind = if (standing.learned) StudyKind.REVIEW else StudyKind.LEARN
            val lineIndices = when {
                _fullTree.value -> null
                kind == StudyKind.LEARN -> standing.learnDeal
                else -> null
            }
            _screen.value = Screen.Guided(tree.repertoire.id, lineIndices, kind, serial = ++dealSerial)
        }
    }

    fun openBranch(tree: OpeningTree) {
        viewModelScope.launch {
            val allowed = if (_fullTree.value) {
                null
            } else {
                Progression(tree, settledRecordFor(tree)).syllabusAt(currentEpochMillis()).branchAllowedNodeIds
            }
            _screen.value = Screen.Branch(tree.repertoire.id, _policy.value, _oneSided.value, allowed)
        }
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
                    policy = policy,
                    oneSided = oneSided,
                    fullTree = fullTree,
                    onPolicyChange = root::setPolicy,
                    onOneSidedChange = root::setOneSided,
                    onFullTreeChange = root::setFullTree,
                    onOpenGuided = root::openGuided,
                    onOpenBranch = root::openBranch,
                )

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
                            scope = sessionScope,
                            journal = root.journalFor(tree),
                        )
                    }
                    GuidedScreen(
                        viewModel = vm,
                        onContinue = root::deal,
                        onBack = root::home,
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
                            scope = sessionScope,
                            journal = root.journalFor(tree),
                        )
                    }
                    BranchScreen(viewModel = vm, onBack = root::home)
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
