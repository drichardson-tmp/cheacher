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
import com.cheacher.app.training.GuidedEvent
import com.cheacher.app.training.StudyKind
import com.cheacher.app.ui.board.BoardResetHold
import com.cheacher.app.ui.board.ChessBoardView
import com.cheacher.app.ui.feedback.TrainingHaptic
import com.cheacher.app.ui.feedback.rememberTrainingHaptics
import com.cheacher.app.ui.theme.CheacherTheme
import com.cheacher.app.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Phase 1 — the name *is* the prompt.
 *
 * The canonical name sits above the board in big serif type, the one-sentence idea
 * unfolds beneath it only when earned (a miss) or asked for. Everything else on the
 * screen is quiet: this mode is about binding vocabulary to squares.
 *
 * The board sits with the side to move at the bottom, turning over between prompts —
 * you learn the black moves from black's chair.
 */
@Composable
fun GuidedScreen(
    viewModel: GuidedViewModel,
    hapticsEnabled: Boolean = true,
    onBack: () -> Unit,
    /** Deals the next session on the study plan — the "pop to the next book" moment. */
    onContinue: () -> Unit,
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
    val isReview = viewModel.kind == StudyKind.REVIEW
    val haptic = rememberTrainingHaptics(hapticsEnabled)

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
            // Which book is open, and whether this visit is retrieval practice, not news.
            Text(
                state.tree.repertoire.title + if (isReview) " · review" else "",
                style = MaterialTheme.typography.labelMedium,
                // Reviews are marked in the cool wash — retrieval practice, not news.
                color = if (isReview) CheacherTheme.colors.reviewTint else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        UnlockBannerCard(banner = unlock, onDismiss = viewModel::dismissUnlock)

        if (state.finished) {
            val dealt = state.deal.size
            // The optional fork: this book's final walked line, played out to mate
            // against the sparring engine. Continuing the plan stays the default.
            val finalLeafId = state.currentLine.lastOrNull()?.id
            val playOut = if (onPlayOut != null && finalLeafId != null) {
                PlayOutOffer(engineElo = sparringElo, onPlayOut = { onPlayOut(finalLeafId) })
            } else {
                null
            }
            if (isReview) {
                val percent = if (dealt == 0) 100 else (state.sessionScore / dealt * 100).roundToInt()
                SessionCompleteCard(
                    title = if (state.allClean) "Still yours." else "It slipped to $percent%.",
                    subtitle = if (state.allClean) {
                        "All $dealt lines of ${state.tree.repertoire.title}, unaided. " +
                            "It comes back later, and rarer."
                    } else {
                        "${formatHalfPoints(state.sessionScore)} of $dealt lines held. " +
                            "This book comes back sooner now."
                    },
                    primaryLabel = "Continue",
                    onPrimary = onContinue,
                    onBack = onBack,
                    playOut = playOut,
                )
            } else {
                SessionCompleteCard(
                    title = "Opening learned.",
                    subtitle = "Every line of ${state.tree.repertoire.title} found unaided. " +
                        "On to the next book.",
                    primaryLabel = "Continue",
                    onPrimary = onContinue,
                    onBack = onBack,
                    playOut = playOut,
                )
            }
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
                // The chair follows the prompt: black's moves are learned from black's side.
                orientation = state.prompt?.mover ?: state.tree.repertoire.perspective,
                onMove = { move ->
                    viewModel.onMove(move)
                    haptic(
                        when (viewModel.state.value.lastEvent) {
                            is GuidedEvent.Wrong -> TrainingHaptic.Wrong
                            is GuidedEvent.LineComplete, GuidedEvent.SessionComplete ->
                                TrainingHaptic.LineComplete
                            is GuidedEvent.Correct -> TrainingHaptic.Correct
                            null -> return@ChessBoardView
                        },
                    )
                },
                shakeTrigger = shakes,
                resetHold = (state.lastEvent as? GuidedEvent.LineComplete)
                    ?.line
                    ?.lastOrNull()
                    ?.let { BoardResetHold(position = it.position, lastMove = it.move) },
                onResetPulse = { haptic(TrainingHaptic.ResetPulse) },
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Line ${state.progress.lineNumber} of ${state.progress.lineCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    // The running score: a hint is half a point, a wrong move none — the
                    // deal is only learned when every line reads whole.
                    Text(
                        "${formatHalfPoints(state.sessionScore)} of ${state.deal.size} learned",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    AnimatedVisibility(
                        visible = state.lineMissed,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            "This line won’t count",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::restartLine) { Text("Restart line") }
            }
        }
    }
}

/** "3½", "4", "0" — credits read as points, and half points deserve the real glyph. */
internal fun formatHalfPoints(score: Double): String {
    val whole = score.toInt()
    val hasHalf = score - whole >= 0.25
    return when {
        whole == 0 && hasHalf -> "½"
        hasHalf -> "$whole½"
        else -> "$whole"
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
    onPrimary: () -> Unit,
    onBack: () -> Unit,
    primaryLabel: String = "Once more",
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
                Button(onClick = onPrimary) { Text(primaryLabel) }
                OutlinedButton(onClick = onBack) { Text("Shelf") }
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
