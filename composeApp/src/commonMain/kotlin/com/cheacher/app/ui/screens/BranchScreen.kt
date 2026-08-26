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
import com.cheacher.app.training.MistakePolicy
import com.cheacher.app.ui.board.ChessBoardView
import com.cheacher.app.ui.theme.CheacherTheme
import com.cheacher.app.ui.theme.Motion
import com.cheacher.app.ui.tree.VariationTreeView

/**
 * Phase 2 — no names, no words, just the tree.
 *
 * The diagram under the board is the whole interface: play a line to its end and watch
 * its branch dim out, then find yourself snapped back to the last junction that still
 * has an open door. The green wash celebrates a banked branch; the red shake needs no
 * caption.
 */
@Composable
fun BranchScreen(
    viewModel: BranchViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shakes by viewModel.wrongShakes.collectAsStateWithLifecycle()
    val flashes by viewModel.closeFlashes.collectAsStateWithLifecycle()
    val unlock by viewModel.unlock.collectAsStateWithLifecycle()
    val perspective = state.tree.repertoire.perspective

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
                onAgain = viewModel::restartSession,
                onBack = onBack,
            )
        }

        Box(Modifier.fillMaxWidth()) {
            ChessBoardView(
                position = state.position,
                lastMove = state.cursor?.move,
                orientation = perspective,
                onMove = viewModel::onMove,
                enabled = !state.finished,
                shakeTrigger = shakes,
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

        Text(
            "THE TREE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VariationTreeView(
            tree = state.tree,
            statusOf = state::statusOf,
            cursorId = state.cursorId,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!state.finished && state.cursor != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::backToJunction) {
                    Text("Back to junction")
                }
            }
        }
    }
}
