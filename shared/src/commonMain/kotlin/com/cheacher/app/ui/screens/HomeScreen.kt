package com.cheacher.app.ui.screens

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
import androidx.compose.runtime.remember
import com.cheacher.app.chess.Color as ChessColor
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.progress.StoreHealth
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.training.MistakePolicy
import com.cheacher.app.training.OpeningStanding

/**
 * The bookshelf: pick a repertoire, pick how you want to be tested.
 *
 * Recall settings (mistake policy, one-sided practice) live here rather than mid-session
 * because changing the rules of a round midway is how you stop trusting the score.
 *
 * [records] is null until the store's first read lands; the shelf shows its cards but
 * holds progress claims and session buttons until it knows the truth — a card promising
 * "your first line" to a learner with a year of history is a lie worth a beat of delay.
 */
@Composable
fun HomeScreen(
    trees: List<OpeningTree>,
    records: Map<String, TrainingRecord>?,
    health: StoreHealth,
    nowEpochMillis: Long,
    policy: MistakePolicy,
    oneSided: Boolean,
    fullTree: Boolean,
    onPolicyChange: (MistakePolicy) -> Unit,
    onOneSidedChange: (Boolean) -> Unit,
    onFullTreeChange: (Boolean) -> Unit,
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
        Text("Cheacher", style = MaterialTheme.typography.displaySmall)
        Text(
            "Openings, learned by name. Then pruned from memory.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The store never fails silently: if history couldn't be read or saved, say so.
        if (!health.isHealthy) {
            Text(
                text = if (health.lastWriteFailed) {
                    "Some progress couldn't be saved — sessions still count, but recent history may be missing."
                } else {
                    "Some saved progress couldn't be read. It has been set aside, not deleted."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("STUDY PLAN", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !fullTree,
                        onClick = { onFullTreeChange(false) },
                        label = { Text("Coach's plan") },
                    )
                    FilterChip(
                        selected = fullTree,
                        onClick = { onFullTreeChange(true) },
                        label = { Text("Full tree") },
                    )
                }
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
                            "Cheacher answers for the opponent",
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
                record = records?.get(tree.repertoire.id),
                recordsLoaded = records != null,
                nowEpochMillis = nowEpochMillis,
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
    recordsLoaded: Boolean,
    nowEpochMillis: Long,
    onOpenGuided: () -> Unit,
    onOpenBranch: () -> Unit,
) {
    val repertoire = tree.repertoire
    // An authored-but-empty book gets shelved honestly: no progress claims, no doors.
    val hasLines = tree.lines.isNotEmpty()
    val sessionsEnabled = recordsLoaded && hasLines
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
            if (!hasLines) {
                Text(
                    "Nothing authored here yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Progress claims wait for both content and the store's first read: an
            // unloaded record must never masquerade as a fresh learner.
            if (hasLines && recordsLoaded) {
                val standing = remember(tree, record) {
                    OpeningStanding(tree, record ?: TrainingRecord.empty(repertoire.id))
                }
                // The credit ledger: found unaided is a point, with the hint half a point.
                Text(
                    text = when {
                        standing.learned && standing.percent >= 100 ->
                            "Accounted for" +
                                if (standing.dueAtEpochMillis <= nowEpochMillis) " · review ready" else " · resting"
                        standing.learned ->
                            "Slipped to ${standing.percent}% · review ready"
                        else ->
                            "${formatHalfPoints(standing.creditTotal)} of ${tree.lines.size} lines accounted"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                streakLabel(tree, record, nowEpochMillis)?.let { streaks ->
                    Text(
                        text = streaks,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
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
                    enabled = sessionsEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Learn the names")
                }
                OutlinedButton(onClick = onOpenBranch, enabled = sessionsEnabled) {
                    Text("Prune the tree")
                }
            }
        }
    }
}

/**
 * The bragging line: day streaks and the proudest clean-recall streak. Only appears once
 * there is something worth saying — a streak of one is just Tuesday.
 */
private fun streakLabel(tree: OpeningTree, record: TrainingRecord?, nowEpochMillis: Long): String? {
    if (record == null) return null
    val parts = buildList {
        val days = record.dayStreak(nowEpochMillis)
        if (days >= 2) add("$days days in a row")
        val best = tree.lines
            .map { line -> line.last() to record.reviewStreakOf(line.last().id) }
            .filter { (_, streak) -> streak >= 2 }
            .maxByOrNull { (_, streak) -> streak }
        if (best != null) add("${best.first.name}: ${best.second} clean recalls")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
