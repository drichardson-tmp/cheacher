package com.cheacher.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cheacher.app.data.SampleRepertoires
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.progress.ProgressStore
import com.cheacher.app.progress.ProgressStoreProvider
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.training.MistakePolicy
import com.cheacher.app.ui.screens.BranchScreen
import com.cheacher.app.ui.screens.BranchViewModel
import com.cheacher.app.ui.screens.GuidedScreen
import com.cheacher.app.ui.screens.GuidedViewModel
import com.cheacher.app.ui.screens.HomeScreen
import com.cheacher.app.ui.theme.CheacherTheme
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/** Where the learner is. A sealed value instead of a nav library: three screens, one enum-with-data. */
sealed interface Screen {
    data object Home : Screen

    data class Guided(val repertoireId: String) : Screen

    data class Branch(
        val repertoireId: String,
        val policy: MistakePolicy,
        val oneSided: Boolean,
    ) : Screen
}

/**
 * Owns navigation and the resolved trees. Resolution happens once, here, at startup —
 * if a sample repertoire had an illegal move the app would refuse to launch, which is
 * exactly the contract [OpeningTree.resolve] promises.
 */
class RootViewModel(val progress: ProgressStore) : ViewModel() {
    val trees: List<OpeningTree> = SampleRepertoires.all.map(OpeningTree::resolve)

    /** Live view of everything remembered, for the shelf's stats lines. */
    val records: StateFlow<Map<String, TrainingRecord>> =
        progress.records.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _policy = MutableStateFlow(MistakePolicy.STRICT)
    val policy: StateFlow<MistakePolicy> = _policy.asStateFlow()

    private val _oneSided = MutableStateFlow(false)
    val oneSided: StateFlow<Boolean> = _oneSided.asStateFlow()

    fun tree(id: String): OpeningTree = trees.first { it.repertoire.id == id }

    fun setPolicy(policy: MistakePolicy) {
        _policy.value = policy
    }

    fun setOneSided(oneSided: Boolean) {
        _oneSided.value = oneSided
    }

    fun openGuided(tree: OpeningTree) {
        _screen.value = Screen.Guided(tree.repertoire.id)
    }

    fun openBranch(tree: OpeningTree) {
        _screen.value = Screen.Branch(tree.repertoire.id, _policy.value, _oneSided.value)
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
        val records by root.records.collectAsStateWithLifecycle()

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
                is Screen.Home -> HomeScreen(
                    trees = root.trees,
                    records = records,
                    policy = policy,
                    oneSided = oneSided,
                    onPolicyChange = root::setPolicy,
                    onOneSidedChange = root::setOneSided,
                    onOpenGuided = root::openGuided,
                    onOpenBranch = root::openBranch,
                )

                is Screen.Guided -> {
                    val tree = root.tree(current.repertoireId)
                    val vm: GuidedViewModel = viewModel(key = "guided-${current.repertoireId}") {
                        GuidedViewModel(tree, root.progress)
                    }
                    GuidedScreen(viewModel = vm, onBack = root::home)
                }

                is Screen.Branch -> {
                    val tree = root.tree(current.repertoireId)
                    val autoReplyFor = if (current.oneSided) tree.repertoire.perspective.opposite else null
                    val vm: BranchViewModel = viewModel(
                        key = "branch-${current.repertoireId}-${current.policy}-${current.oneSided}",
                    ) {
                        BranchViewModel(tree, current.policy, autoReplyFor, root.progress)
                    }
                    BranchScreen(viewModel = vm, onBack = root::home)
                }
            }
        }
    }
}
