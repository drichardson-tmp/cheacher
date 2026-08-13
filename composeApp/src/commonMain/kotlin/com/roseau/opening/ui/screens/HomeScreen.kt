package com.roseau.opening.ui.screens

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roseau.opening.chess.Color as ChessColor
import com.roseau.opening.domain.OpeningTree
import com.roseau.opening.progress.TrainingRecord
import com.roseau.opening.training.MistakePolicy

/**
 * The bookshelf: pick a repertoire, pick how you want to be tested.
 *
 * Recall settings (mistake policy, one-sided practice) live here rather than mid-session
 * because changing the rules of a round midway is how you stop trusting the score.
 */
@Composable
fun HomeScreen(
    trees: List<OpeningTree>,
    records: Map<String, TrainingRecord>,
    policy: MistakePolicy,
    oneSided: Boolean,
    onPolicyChange: (MistakePolicy) -> Unit,
    onOneSidedChange: (Boolean) -> Unit,
    onOpenGuided: (OpeningTree) -> Unit,
    onOpenBranch: (OpeningTree) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Roseau", style = MaterialTheme.typography.displaySmall)
        Text(
            "Openings, learned by name. Then pruned from memory.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("RECALL RULES", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = policy == MistakePolicy.STRICT,
                        onClick = { onPolicyChange(MistakePolicy.STRICT) },
                        label = { Text("Strict") },
                    )
                    FilterChip(
                        selected = policy == MistakePolicy.ONE_ALLOWANCE,
                        onClick = { onPolicyChange(MistakePolicy.ONE_ALLOWANCE) },
                        label = { Text("One allowance") },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Play one side only", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Roseau answers for the opponent",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = oneSided, onCheckedChange = onOneSidedChange)
                }
            }
        }

        for (tree in trees) {
            RepertoireCard(
                tree = tree,
                record = records[tree.repertoire.id],
                onOpenGuided = { onOpenGuided(tree) },
                onOpenBranch = { onOpenBranch(tree) },
            )
        }
    }
}

@Composable
private fun RepertoireCard(
    tree: OpeningTree,
    record: TrainingRecord?,
    onOpenGuided: () -> Unit,
    onOpenBranch: () -> Unit,
) {
    val repertoire = tree.repertoire
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(repertoire.title, style = MaterialTheme.typography.headlineSmall)
            if (repertoire.subtitle.isNotBlank()) {
                Text(
                    repertoire.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${tree.lines.size} lines · ${tree.allNodes.size} moves · " +
                    if (repertoire.perspective == ChessColor.WHITE) "as White" else "as Black",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            record?.takeIf { it.sessionStarts.isNotEmpty() }?.let { r ->
                Text(
                    text = buildString {
                        append("Finished ${r.sessionsCompleted}×")
                        if (r.branchCleanSweeps > 0) append(" · ${r.branchCleanSweeps} clean sweeps")
                        val worst = r.troubleSpots(limit = 1).firstOrNull()
                        if (worst != null) {
                            val node = tree.node(worst.first)
                            val label = node?.let { "${it.moveNumberLabel}${it.san}" } ?: "the first move"
                            append(" · trouble spot: $label (${worst.second} misses)")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenGuided,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Learn the names")
                }
                OutlinedButton(onClick = onOpenBranch) {
                    Text("Prune the tree")
                }
            }
        }
    }
}
