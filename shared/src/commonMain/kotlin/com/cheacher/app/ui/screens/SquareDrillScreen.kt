package com.cheacher.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.cheacher.app.chess.Fen
import com.cheacher.app.training.DrillEvent
import com.cheacher.app.training.DrillSummary
import com.cheacher.app.ui.board.ChessBoardView
import com.cheacher.app.ui.board.Spotlight
import com.cheacher.app.ui.feedback.TrainingHaptic
import com.cheacher.app.ui.feedback.rememberTrainingHaptics
import kotlinx.coroutines.delay

/** An empty board: the drill is about the grid, and pieces would only be scenery. */
private val emptyBoard = Fen.parse("8/8/8/8/8/8/8/8 w - - 0 1")

/**
 * The square drill — a coordinate, a bare board, and a stopwatch.
 *
 * Deliberately the plainest screen in the app: one name in large type, sixty-four empty
 * squares, and a progress bar. No coordinates on the edges (finding the square *is* the
 * exercise) and the board turns at the halfway mark, so the second half is drilled from
 * Black's side.
 */
@Composable
fun SquareDrillScreen(
    viewModel: SquareDrillViewModel,
    hapticsEnabled: Boolean = true,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val taps by viewModel.taps.collectAsStateWithLifecycle()
    val haptic = rememberTrainingHaptics(hapticsEnabled)

    // The verdict wash, cleared on a timer so a stale green never sits under a new prompt.
    var spotlight by remember { mutableStateOf<Spotlight?>(null) }
    LaunchedEffect(taps) {
        spotlight = when (val event = state.lastEvent) {
            is DrillEvent.Found -> Spotlight(event.square, correct = true)
            is DrillEvent.Missed -> Spotlight(event.square, correct = false)
            null -> null
        }
        if (spotlight != null) {
            delay(260)
            spotlight = null
        }
    }

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
                "${state.repNumber} of ${state.prompts.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.finished) {
            DrillSummaryCard(
                summary = state.summary,
                onAgain = viewModel::again,
                onBack = onBack,
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "FIND THE SQUARE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    AnimatedContent(
                        targetState = state.targetName.orEmpty(),
                        transitionSpec = {
                            (slideInVertically { it / 3 } + fadeIn()) togetherWith fadeOut()
                        },
                        label = "drill-prompt",
                    ) { name ->
                        Text(name, style = MaterialTheme.typography.displaySmall)
                    }
                }
            }
        }

        ChessBoardView(
            position = emptyBoard,
            lastMove = null,
            orientation = state.orientation,
            onMove = {},
            enabled = !state.finished,
            // The grid has to live in your head, so it does not live on the board.
            showCoordinates = false,
            onSquareTap = { square ->
                viewModel.onSquareTap(square)
                haptic(
                    when (viewModel.state.value.lastEvent) {
                        is DrillEvent.Found -> if (viewModel.state.value.finished) {
                            TrainingHaptic.LineComplete
                        } else {
                            TrainingHaptic.Correct
                        }
                        is DrillEvent.Missed -> TrainingHaptic.Wrong
                        null -> return@ChessBoardView
                    },
                )
            },
            spotlight = spotlight,
            modifier = Modifier.fillMaxWidth(),
        )

        LinearProgressIndicator(
            progress = { state.answered.size.toFloat() / state.prompts.size },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

/** Median first, because it is the number that moves as the grid becomes automatic. */
@Composable
private fun DrillSummaryCard(
    summary: DrillSummary,
    onAgain: () -> Unit,
    onBack: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${seconds(summary.medianMillis)} a square", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${summary.cleanReps} of ${summary.reps} found first time · " +
                    "best ${seconds(summary.bestMillis)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAgain) { Text("Again") }
                OutlinedButton(onClick = onBack) { Text("Back to shelf") }
            }
        }
    }
}

/** "1.4s" — one decimal, because tenths are the unit the learner actually feels. */
private fun seconds(millis: Long?): String {
    if (millis == null) return "—"
    val tenths = (millis + 50) / 100
    return "${tenths / 10}.${tenths % 10}s"
}
