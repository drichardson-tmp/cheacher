package com.cheacher.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cheacher.app.chess.Color as ChessColor
import com.cheacher.app.engine.SparringElo
import com.cheacher.app.training.EndReason
import com.cheacher.app.training.GameResult
import com.cheacher.app.training.PlayOutOutcome
import com.cheacher.app.training.PlayOutState
import com.cheacher.app.training.PlayedMove
import com.cheacher.app.ui.board.ChessBoardView

/**
 * The epilogue screen: the book position stays on the board and the game simply keeps
 * going, against an engine playing at the learner's sparring rating. Quiet by design —
 * no names, no ideas, no ladder. Just chess, until somebody's king has had enough.
 */
@Composable
fun PlayOutScreen(
    viewModel: PlayOutViewModel,
    openingTitle: String,
    onBack: () -> Unit,
    /** Deals the next session on the study plan — the default path home from a game. */
    onContinue: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val thinking by viewModel.thinking.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.dispose() }
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
                "Sparring at ~${viewModel.engineElo}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val outcome = state.outcome
        if (outcome != null) {
            PlayOutResultCard(
                outcome = outcome,
                learnerSide = state.learnerSide,
                eloBefore = viewModel.engineElo,
                onContinue = onContinue,
                onRematch = viewModel::rematch,
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "OFF BOOK · CLOSE OUT THE GAME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(openingTitle, style = MaterialTheme.typography.headlineMedium)
                    AnimatedVisibility(visible = thinking, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            "The engine is thinking…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        ChessBoardView(
            position = state.position,
            lastMove = state.lastMove,
            orientation = state.learnerSide,
            onMove = viewModel::onMove,
            enabled = state.isLearnerTurn,
            modifier = Modifier.fillMaxWidth(),
        )

        PlayOutMoveStrip(bookMoves = state.bookMoves, freshMoves = state.freshMoves)

        if (outcome == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::resign) { Text("Resign") }
            }
        }
    }
}

/** Book moves in the quiet wash, fresh ones in full ink — memory, then battle. */
@Composable
private fun PlayOutMoveStrip(
    bookMoves: List<PlayedMove>,
    freshMoves: List<PlayedMove>,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((fromBook, move) in bookMoves.map { true to it } + freshMoves.map { false to it }) {
            Text(
                text = if (move.mover == ChessColor.WHITE) "${move.moveNumberLabel}${move.san}" else move.san,
                style = MaterialTheme.typography.labelMedium,
                color = if (fromBook) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun PlayOutResultCard(
    outcome: PlayOutOutcome,
    learnerSide: ChessColor,
    eloBefore: Int,
    onContinue: () -> Unit,
    onRematch: () -> Unit,
) {
    val eloAfter = SparringElo.updated(eloBefore, outcome.result.score)
    val title = when (outcome.result) {
        GameResult.LEARNER_WIN -> "Checkmate. Your game."
        GameResult.ENGINE_WIN ->
            if (outcome.reason == EndReason.RESIGNATION) "Resigned. Fair enough." else "The engine takes it."
        GameResult.DRAW -> "A draw."
    }
    val reason = when (outcome.reason) {
        EndReason.CHECKMATE ->
            if (outcome.result == GameResult.LEARNER_WIN) {
                "You closed out the whole game as ${learnerSide.label()}."
            } else {
                "Mated — but every one of these makes the next one closer."
            }
        EndReason.STALEMATE -> "Stalemate: nobody may move, nobody wins."
        EndReason.THREEFOLD_REPETITION -> "The same position three times — the game calls it."
        EndReason.FIFTY_MOVE_RULE -> "Fifty quiet moves — the game calls it."
        EndReason.DEAD_POSITION -> "Not enough wood left to mate with."
        EndReason.RESIGNATION -> "The openings are still there whenever you are."
    }
    val eloLine = when {
        eloAfter > eloBefore -> "The engine climbs: $eloBefore → $eloAfter."
        eloAfter < eloBefore -> "The engine eases off: $eloBefore → $eloAfter."
        else -> "The engine holds at $eloBefore."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(reason, style = MaterialTheme.typography.bodyLarge)
            Text(
                eloLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onContinue) { Text("Back to the openings") }
                OutlinedButton(onClick = onRematch) { Text("Rematch") }
            }
        }
    }
}

private fun ChessColor.label(): String = if (this == ChessColor.WHITE) "White" else "Black"
