package com.cheacher.app.ui.screens

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cheacher.app.training.DrillSummary
import com.cheacher.app.training.MoveDrillEvent
import com.cheacher.app.training.MoveDrillMode
import com.cheacher.app.training.MoveDrillState
import com.cheacher.app.ui.board.ChessBoardView

/**
 * The atomic vocabulary drill in both directions: name → move and move → name.
 * Every prompt is timed until its first correct answer; a miss does not stop the clock.
 */
@Composable
fun MoveDrillScreen(
    viewModel: MoveDrillViewModel,
    bankSize: Int,
    distinctNames: Int,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val attempts by viewModel.attempts.collectAsStateWithLifecycle()

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
                if (state.cards.isEmpty()) "No moves" else "${state.repNumber} of ${state.cards.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == MoveDrillMode.FIND_MOVE,
                onClick = { viewModel.setMode(MoveDrillMode.FIND_MOVE) },
                label = { Text("Find the move") },
            )
            FilterChip(
                selected = state.mode == MoveDrillMode.NAME_IT,
                onClick = { viewModel.setMode(MoveDrillMode.NAME_IT) },
                label = { Text("Name it") },
            )
        }

        LinearProgressIndicator(
            progress = { if (state.cards.isEmpty()) 0f else state.answered.size.toFloat() / state.cards.size },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary,
        )

        if (state.finished) {
            MoveDrillSummaryCard(state = state, onAgain = viewModel::again, onBack = onBack)
        } else {
            val card = state.card ?: return@Column
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (state.mode == MoveDrillMode.FIND_MOVE) "FIND THE MOVE" else "NAME THIS MOVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        if (state.mode == MoveDrillMode.FIND_MOVE) card.name else "${card.moveNumberLabel}${card.san}",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        if (state.mode == MoveDrillMode.FIND_MOVE) {
                            "The clock stops on the first correct board move."
                        } else {
                            "The highlighted move has one canonical name."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Random cards are unrelated positions. Re-keying prevents pieces from
            // spending the measured first second flying in from the previous puzzle.
            key(state.mode, card.id) {
                ChessBoardView(
                    position = if (state.mode == MoveDrillMode.FIND_MOVE) card.positionBefore else card.positionAfter,
                    lastMove = if (state.mode == MoveDrillMode.NAME_IT) card.move else null,
                    orientation = card.orientation,
                    onMove = viewModel::onMove,
                    enabled = state.mode == MoveDrillMode.FIND_MOVE,
                    shakeTrigger = attempts.takeIf {
                        state.lastEvent is MoveDrillEvent.WrongMove || state.lastEvent is MoveDrillEvent.WrongName
                    } ?: 0,
                    showCoordinates = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.mode == MoveDrillMode.NAME_IT) {
                NameAnswer(
                    query = query,
                    suggestions = suggestions,
                    bankSize = bankSize,
                    distinctNames = distinctNames,
                    wrong = state.lastEvent is MoveDrillEvent.WrongName,
                    onQueryChange = viewModel::onQueryChange,
                    onSuggestion = viewModel::submitName,
                    onCheck = viewModel::submitClosest,
                )
            }
        }
    }
}

@Composable
private fun NameAnswer(
    query: String,
    suggestions: List<String>,
    bankSize: Int,
    distinctNames: Int,
    wrong: Boolean,
    onQueryChange: (String) -> Unit,
    onSuggestion: (String) -> Unit,
    onCheck: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Opening name") },
            supportingText = {
                Text(
                    if (wrong) "Not that name — try again."
                    else "Fuzzy search across $distinctNames names on all $bankSize moves.",
                )
            },
            isError = wrong,
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onCheck() }),
        )
        suggestions.forEach { name ->
            OutlinedButton(
                onClick = { onSuggestion(name) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(name)
            }
        }
        Button(onClick = onCheck, enabled = query.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (suggestions.isEmpty()) "Check name" else "Use closest match")
        }
    }
}

@Composable
private fun MoveDrillSummaryCard(
    state: MoveDrillState,
    onAgain: () -> Unit,
    onBack: () -> Unit,
) {
    val summary: DrillSummary = state.summary
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${seconds(summary.medianMillis)} median", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${summary.cleanReps} of ${summary.reps} correct first try · " +
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

private fun seconds(millis: Long?): String {
    if (millis == null) return "—"
    val tenths = (millis + 50) / 100
    return "${tenths / 10}.${tenths % 10}s"
}
