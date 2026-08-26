package com.cheacher.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.cheacher.app.chess.Color as ChessColor
import com.cheacher.app.training.BranchEvent
import com.cheacher.app.training.MistakePolicy
import com.cheacher.app.ui.board.ChessBoardView
import com.cheacher.app.ui.feedback.TrainingHaptic
import com.cheacher.app.ui.feedback.rememberTrainingHaptics
import com.cheacher.app.ui.theme.CheacherTheme
import com.cheacher.app.ui.theme.Motion
import com.cheacher.app.ui.tree.VariationTreeView

/**
 * Phase 2 — the name, and nothing else.
 *
 * One line is named above the board and you have to produce all of it: no per-move
 * prompt, no idea sentence, no coordinates, and — while you are playing — no diagram,
 * because a diagram of the tree is a picture of the answer. The green wash celebrates a
 * banked line; the red shake needs no caption. The tree only comes out at the end, as a
 * map of what you just walked.
 */
@Composable
fun BranchScreen(
    viewModel: BranchViewModel,
    hapticsEnabled: Boolean = true,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shakes by viewModel.wrongShakes.collectAsStateWithLifecycle()
    val flashes by viewModel.closeFlashes.collectAsStateWithLifecycle()
    val unlock by viewModel.unlock.collectAsStateWithLifecycle()
    val haptic = rememberTrainingHaptics(hapticsEnabled)

    // One-sided practice keeps the learner's chair fixed; both-sides recall turns the
    // board over with the move, exactly as the guided walkthrough does.
    val perspective = state.autoReplyFor?.opposite ?: state.position.sideToMove

    // Green wash when a branch closes out.
    val flash = remember { Animatable(0f) }
    LaunchedEffect(flashes) {
        if (flashes > 0) {
            flash.snapTo(0.35f)
            flash.animateTo(0f, tween(durationMillis = 650))
        }
    }

    val progressFraction by animateFloatAsState(
        targetValue = state.progress.fraction,
        animationSpec = Motion.settle(),
        label = "branch-progress",
    )

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
            Text(
                "${state.progress.closedLines} of ${state.progress.totalLines} branches" +
                    if (state.policy == MistakePolicy.ONE_ALLOWANCE && state.strikes > 0) " · 1 miss spent" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier.fillMaxWidth(),
            color = if (state.progress.failedLines > 0) CheacherTheme.colors.streakAccent else MaterialTheme.colorScheme.tertiary,
        )

        UnlockBannerCard(banner = unlock, onDismiss = viewModel::dismissUnlock)

        if (state.finished) {
            val failed = state.progress.failedLines
            SessionCompleteCard(
                title = if (failed == 0) "Clean sweep." else "Tree closed.",
                subtitle = if (failed == 0) {
                    "All ${state.progress.totalLines} branches recalled without a slip."
                } else {
                    "${state.progress.totalLines - failed} recalled, $failed lost. Run it back."
                },
                onPrimary = viewModel::restartSession,
                onBack = onBack,
            )
        }

        state.targetLeaf?.takeIf { !state.finished }?.let { target ->
            TargetCard(name = target.name, mover = state.tree.sideToMoveAt(state.cursor))
        }

        Box(Modifier.fillMaxWidth()) {
            ChessBoardView(
                position = state.position,
                lastMove = state.cursor?.move,
                orientation = perspective,
                onMove = { move ->
                    viewModel.onMove(move)
                    haptic(
                        when (viewModel.state.value.lastEvent) {
                            is BranchEvent.Advanced -> TrainingHaptic.Correct
                            is BranchEvent.BranchClosed, BranchEvent.SessionComplete ->
                                TrainingHaptic.LineComplete
                            is BranchEvent.Missed,
                            is BranchEvent.BranchFailed,
                            is BranchEvent.AlreadyClosed,
                            is BranchEvent.Locked -> TrainingHaptic.Wrong
                            null -> return@ChessBoardView
                        },
                    )
                },
                enabled = !state.finished,
                shakeTrigger = shakes,
                holdBeforeReset = state.lastEvent is BranchEvent.BranchClosed ||
                    state.lastEvent is BranchEvent.SessionComplete,
                // Recall is unaided: the grid comes off the board once the names are learned.
                showCoordinates = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .alpha(flash.value)
                    .background(CheacherTheme.colors.verdictCorrect, RoundedCornerShape(10.dp)),
            )
        }

        MoveStrip(played = state.path)

        // The map, once the walking is done: every line by name, green for banked and
        // red for lost. Reading it mid-round would just be reading the answers.
        if (state.finished) {
            Text(
                "THE TREE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VariationTreeView(
                tree = state.tree,
                statusOf = state::statusOf,
                cursorId = null,
                showNames = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!state.finished && state.cursor != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::backToJunction) {
                    Text("Back to junction")
                }
            }
        }
    }
}

/**
 * The whole prompt: one name, in the same serif weight guided mode gives it, plus whose
 * move it is. No move number — knowing you are eight plies in is a hint about how much
 * is left, and recall should not come with a progress bar inside the line.
 */
@Composable
private fun TargetCard(name: String, mover: ChessColor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "PLAY THE LINE · " +
                    if (mover == ChessColor.WHITE) "WHITE TO MOVE" else "BLACK TO MOVE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(name, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
