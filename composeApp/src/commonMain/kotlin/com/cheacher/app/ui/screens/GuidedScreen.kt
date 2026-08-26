package com.cheacher.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cheacher.app.chess.Color as ChessColor
import com.cheacher.app.domain.TreeNode
import com.cheacher.app.ui.board.ChessBoardView
import com.cheacher.app.ui.theme.CheacherTheme
import com.cheacher.app.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * Phase 1 — the name *is* the prompt.
 *
 * The canonical name sits above the board in big serif type, the one-sentence idea
 * unfolds beneath it only when earned (a miss) or asked for. Everything else on the
 * screen is quiet: this mode is about binding vocabulary to squares.
 */
@Composable
fun GuidedScreen(
    viewModel: GuidedViewModel,
    onBack: () -> Unit,
    /** Absolute line indices that are reviews on today's syllabus — marked, never blocked. */
    reviewLineIndices: Set<Int> = emptySet(),
    /** The sparring engine's current level, shown on the optional play-out branch. */
    sparringElo: Int = 700,
    /**
     * The optional branch at the end of the book: play the last line's final position
     * out against the engine. Handed the leaf node id. Null hides the offer — the
     * default path is, and stays, the opening progression.
     */
    onPlayOut: ((leafNodeId: String) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shakes by viewModel.wrongShakes.collectAsStateWithLifecycle()
    val unlock by viewModel.unlock.collectAsStateWithLifecycle()
    val perspective = state.tree.repertoire.perspective

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Shelf") }
            Spacer(Modifier.weight(1f))
            // The one-word marker that this line is retrieval practice, not news.
            val absoluteLine = state.lineIndices?.getOrNull(state.lineIndex)
            val isReview = absoluteLine != null && absoluteLine in reviewLineIndices
            Text(
                "Line ${state.progress.lineNumber} of ${state.progress.lineCount}" +
                    if (isReview) " · review" else "",
                style = MaterialTheme.typography.labelMedium,
                // Review lines are marked in the cool wash — retrieval practice, not news.
                color = if (isReview) CheacherTheme.colors.reviewTint else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        UnlockBannerCard(banner = unlock, onDismiss = viewModel::dismissUnlock)

        if (state.finished) {
            val finalLeafId = state.currentLine.lastOrNull()?.id
            SessionCompleteCard(
                title = "Every name found.",
                subtitle = "You walked all ${state.progress.lineCount} lines of ${state.tree.repertoire.title}.",
                onAgain = viewModel::restartSession,
                onBack = onBack,
                playOut = if (onPlayOut != null && finalLeafId != null) {
                    PlayOutOffer(engineElo = sparringElo, onPlayOut = { onPlayOut(finalLeafId) })
                } else {
                    null
                },
            )
        } else {
            state.prompt?.let { prompt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${prompt.moveNumberLabel} · " +
                                if (prompt.mover == ChessColor.WHITE) "WHITE PLAYS" else "BLACK PLAYS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        AnimatedContent(
                            targetState = prompt.name,
                            transitionSpec = {
                                (slideInVertically { it / 3 } + fadeIn()) togetherWith fadeOut()
                            },
                            label = "prompt-name",
                        ) { name ->
                            Text(name, style = MaterialTheme.typography.headlineMedium)
                        }
                        AnimatedVisibility(
                            visible = prompt.idea != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Text(
                                prompt.idea.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (prompt.idea == null) {
                            TextButton(
                                onClick = viewModel::revealIdea,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            ) {
                                Text("Why this move?")
                            }
                        }
                    }
                }
            }

            ChessBoardView(
                position = state.position,
                lastMove = state.played.lastOrNull()?.move,
                orientation = perspective,
                onMove = viewModel::onMove,
                shakeTrigger = shakes,
                modifier = Modifier.fillMaxWidth(),
            )

            MoveStrip(played = state.played)

            LinearProgressIndicator(
                progress = {
                    if (state.progress.plyCount == 0) 0f
                    else state.progress.plyNumber.toFloat() / state.progress.plyCount
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiary,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::restartLine) { Text("Restart line") }
            }
        }
    }
}

/** The played moves of the current line, `1.e4 c5 2.Nf3` style, monospace ink. */
@Composable
fun MoveStrip(played: List<TreeNode>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (played.isEmpty()) {
            Text(
                "—",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        for (node in played) {
            Text(
                text = if (node.mover == ChessColor.WHITE) "${node.moveNumberLabel}${node.san}" else node.san,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * The "new branch unlocked" moment: a brass card that settles in when a session's
 * completions move the progression frontier, names the fork it opened, and slips away
 * on its own. Time-boxed here in the UI — the reducers and watchers stay clockless.
 */
@Composable
fun UnlockBannerCard(banner: UnlockBanner?, onDismiss: () -> Unit) {
    // Keep the last real banner so the card can animate out after dismissal.
    var latest by remember { mutableStateOf(banner) }
    if (banner != null) latest = banner

    LaunchedEffect(banner?.serial) {
        if (banner != null) {
            delay(4_000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = banner != null,
        enter = expandVertically(animationSpec = Motion.settle()) + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val advance = latest?.advance ?: return@AnimatedVisibility
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (advance.repertoireMastered) "REPERTOIRE MASTERED" else "NEW BRANCH UNLOCKED",
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    advance.unlockedLine?.name ?: "Every branch is open.",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}

/** The optional fork at the end of the book: a full game against the sparring engine. */
data class PlayOutOffer(val engineElo: Int, val onPlayOut: () -> Unit)

@Composable
fun SessionCompleteCard(
    title: String,
    subtitle: String,
    onAgain: () -> Unit,
    onBack: () -> Unit,
    playOut: PlayOutOffer? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            // The default path stays the opening progression — the filled button leads
            // back to the ladder; playing the game out is a quieter, optional fork.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAgain) { Text("Once more") }
                OutlinedButton(onClick = onBack) { Text("Back to shelf") }
            }
            if (playOut != null) {
                TextButton(
                    onClick = playOut.onPlayOut,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text("Or play this one out · engine at ~${playOut.engineElo}")
                }
            }
        }
    }
}
