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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cheacher.app.chess.Color as ChessColor
import com.cheacher.app.domain.TreeNode
import com.cheacher.app.ui.board.ChessBoardView

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
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shakes by viewModel.wrongShakes.collectAsStateWithLifecycle()
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
            Text(
                "Line ${state.progress.lineNumber} of ${state.progress.lineCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.finished) {
            SessionCompleteCard(
                title = "Every name found.",
                subtitle = "You walked all ${state.progress.lineCount} lines of ${state.tree.repertoire.title}.",
                onAgain = viewModel::restartSession,
                onBack = onBack,
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

@Composable
fun SessionCompleteCard(
    title: String,
    subtitle: String,
    onAgain: () -> Unit,
    onBack: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAgain) { Text("Once more") }
                OutlinedButton(onClick = onBack) { Text("Back to shelf") }
            }
        }
    }
}
