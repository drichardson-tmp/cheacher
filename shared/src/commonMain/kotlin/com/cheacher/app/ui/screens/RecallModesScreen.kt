package com.cheacher.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cheacher.app.ui.board.ChessBoardView
import com.cheacher.app.ui.feedback.TrainingHaptic
import com.cheacher.app.ui.feedback.rememberTrainingHaptics

@Composable
fun BlitzScreen(viewModel: BlitzViewModel, hapticsEnabled: Boolean, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val count by viewModel.playCount.collectAsStateWithLifecycle()
    val attempts by viewModel.attempts.collectAsStateWithLifecycle()
    val haptic = rememberTrainingHaptics(hapticsEnabled)
    TrainingModeColumn(onBack, if (state.finished) "Complete" else "${state.index + 1} of ${state.cards.size}") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20).forEach { option ->
                FilterChip(selected = count == option, onClick = { viewModel.setPlayCount(option) }, label = { Text("$option plays") })
            }
        }
        LinearProgressIndicator(
            progress = { if (state.cards.isEmpty()) 0f else state.index.toFloat() / state.cards.size },
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.finished) {
            SummaryCard(
                title = "${state.summary.cleanReps} of ${state.summary.reps} first try",
                subtitle = "${state.summary.medianMillis?.let { "${it}ms median" } ?: "No timing yet"}",
                onAgain = viewModel::again,
            )
        } else {
            val card = state.card ?: return@TrainingModeColumn
            PromptCard("BLITZ · FIND THE MOVE", card.name, "The next answer deals a whole new position.")
            key(card.id) {
                ChessBoardView(
                    position = card.positionBefore,
                    lastMove = null,
                    orientation = card.orientation,
                    onMove = {
                        viewModel.onMove(it)
                        haptic(if (viewModel.state.value.lastCorrect == true) TrainingHaptic.Correct else TrainingHaptic.Wrong)
                    },
                    shakeTrigger = attempts.takeIf { state.lastCorrect == false } ?: 0,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun QuietScreen(viewModel: QuietViewModel, hapticsEnabled: Boolean, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val attempts by viewModel.attempts.collectAsStateWithLifecycle()
    val haptic = rememberTrainingHaptics(hapticsEnabled)
    TrainingModeColumn(onBack, if (state.finished) "Line complete" else "${state.index} moves found") {
        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        PromptCard(
            "QUIET RECALL · ${if (state.position.sideToMove == com.cheacher.app.chess.Color.WHITE) "WHITE" else "BLACK"} TO MOVE",
            state.card.targetName,
            state.hint ?: "Reach the named destination. No intermediary names.",
        )
        ChessBoardView(
            position = state.position,
            lastMove = state.card.line.getOrNull(state.index - 1)?.move,
            orientation = state.card.orientation,
            onMove = {
                viewModel.onMove(it)
                haptic(if (viewModel.state.value.lastCorrect == true) TrainingHaptic.Correct else TrainingHaptic.Wrong)
            },
            enabled = !state.finished,
            shakeTrigger = attempts.takeIf { state.lastCorrect == false } ?: 0,
            showCoordinates = false,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.finished) {
            Button(onClick = viewModel::nextLine, modifier = Modifier.fillMaxWidth()) { Text("Next line") }
        } else if (!state.hintRevealed) {
            OutlinedButton(onClick = viewModel::revealHint) { Text("Show current-move hint") }
        }
    }
}

@Composable
private fun TrainingModeColumn(onBack: () -> Unit, status: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Shelf") }
            Spacer(Modifier.weight(1f))
            Text(status, style = MaterialTheme.typography.labelMedium)
        }
        content()
    }
}

@Composable
private fun PromptCard(kicker: String, title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(kicker, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryCard(title: String, subtitle: String, onAgain: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle)
            Button(onClick = onAgain) { Text("Again") }
        }
    }
}
