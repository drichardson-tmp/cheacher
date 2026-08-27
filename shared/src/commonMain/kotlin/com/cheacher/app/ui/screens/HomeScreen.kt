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
import com.cheacher.app.progress.DrillRecord
import com.cheacher.app.progress.MoveDrillRecord
import com.cheacher.app.progress.TrainingRecord
import com.cheacher.app.training.MistakePolicy
import com.cheacher.app.training.OpeningStanding
import kotlinx.datetime.TimeZone

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
    timeZone: TimeZone,
    policy: MistakePolicy,
    oneSided: Boolean,
    fullTree: Boolean,
    hapticsEnabled: Boolean,
    onPolicyChange: (MistakePolicy) -> Unit,
    onOneSidedChange: (Boolean) -> Unit,
    onFullTreeChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit,
    onOpenGuided: (OpeningTree) -> Unit,
    onOpenBranch: (OpeningTree) -> Unit,
    onOpenSquareDrill: () -> Unit = {},
    onOpenMoveDrill: () -> Unit = {},
    onOpenBlitz: () -> Unit = {},
    onOpenQuiet: () -> Unit = {},
    /** The drill's history, if any has been banked. Null reads as "never drilled". */
    drill: DrillRecord? = null,
    moveDrill: MoveDrillRecord? = null,
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
            "Openings, learned by name. Then recalled from nothing.",
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
                Text("FEEDBACK", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Vibration feedback", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Subtle cues for moves and completed lines",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = hapticsEnabled, onCheckedChange = onHapticsEnabledChange)
                }
            }
        }

        SquareDrillCard(drill = drill, onOpen = onOpenSquareDrill)
        MoveDrillCard(
            record = moveDrill,
            moveCount = trees.sumOf { it.allNodes.size },
            onOpen = onOpenMoveDrill,
        )
        RecallLabCard(
            moveCount = trees.sumOf { it.allNodes.size },
            lineCount = trees.sumOf { it.lines.size },
            blitzRecord = moveDrill?.blitz,
            onOpenBlitz = onOpenBlitz,
            onOpenQuiet = onOpenQuiet,
        )

        for (tree in trees) {
            RepertoireCard(
                tree = tree,
                record = records?.get(tree.repertoire.id),
                recordsLoaded = records != null,
                nowEpochMillis = nowEpochMillis,
                timeZone = timeZone,
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
    timeZone: TimeZone,
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
                            "Learned" +
                                if (standing.dueAtEpochMillis <= nowEpochMillis) " · review ready" else " · resting"
                        standing.learned ->
                            "Slipped to ${standing.percent}% · review ready"
                        else ->
                            "${formatHalfPoints(standing.creditTotal)} of ${tree.lines.size} lines learned"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                streakLabel(tree, record, nowEpochMillis, timeZone)?.let { streaks ->
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
                            append(" · ${r.errorSeverityOf(worst.first).label}: $label (${worst.second} pressure)")
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
                    Text("Recall the lines")
                }
            }
        }
    }
}

/**
 * The bragging line: day streaks and the proudest clean-recall streak. Only appears once
 * there is something worth saying — a streak of one is just Tuesday.
 */
private fun streakLabel(
    tree: OpeningTree,
    record: TrainingRecord?,
    nowEpochMillis: Long,
    timeZone: TimeZone,
): String? {
    if (record == null) return null
    val parts = buildList {
        val days = record.dayStreak(nowEpochMillis, timeZone)
        if (days >= 2) add("$days days in a row")
        val best = tree.lines
            .map { line -> line.last() to record.reviewStreakOf(line.last().id) }
            .filter { (_, streak) -> streak >= 2 }
            .maxByOrNull { (_, streak) -> streak }
        if (best != null) add("${best.first.name}: ${best.second} clean recalls")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/**
 * The warm-up, sitting above the shelf rather than on it: the drill trains board geometry,
 * which belongs to no opening and unlocks nothing. Its line is the median, because that is
 * the number that moves as the grid becomes automatic.
 */
@Composable
private fun SquareDrillCard(drill: DrillRecord?, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Square drill", style = MaterialTheme.typography.titleMedium)
            Text(
                text = drill?.let { record ->
                    val median = record.lastMedianMillis
                    val best = record.bestMedianMillis
                    buildString {
                        append("Last ${tenths(median)} a square")
                        if (best != null && best != median) append(" · best ${tenths(best)}")
                        append(" · ${record.rounds} rounds")
                    }
                } ?: "Find the square, no labels, both sides of the board. Ninety seconds.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Row {
                OutlinedButton(onClick = onOpen) { Text("Drill squares") }
            }
        }
    }
}

/** The shelf-wide vocabulary lab: every authored move, tested in both directions. */
@Composable
private fun MoveDrillCard(record: MoveDrillRecord?, moveCount: Int, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Move drill", style = MaterialTheme.typography.titleMedium)
            Text(
                text = record?.let {
                    val move = it.findMove.lastMedianMillis?.let(::tenths) ?: "—"
                    val name = it.nameIt.lastMedianMillis?.let(::tenths) ?: "—"
                    "Find the move $move · name it $name · $moveCount moves in the bank"
                } ?: "Name → move, then move → name. Fuzzy search across all $moveCount shelf moves.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Row {
                OutlinedButton(onClick = onOpen, enabled = moveCount > 0) { Text("Drill moves") }
            }
        }
    }
}

/** Two deliberately different retrieval modes: formation speed and whole-line silence. */
@Composable
private fun RecallLabCard(
    moveCount: Int,
    lineCount: Int,
    blitzRecord: DrillRecord?,
    onOpenBlitz: () -> Unit,
    onOpenQuiet: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Recall lab", style = MaterialTheme.typography.titleMedium)
            Text(
                "Blitz swaps formations after every move" +
                    (blitzRecord?.lastMedianMillis?.let { " · last ${tenths(it)}" } ?: "") +
                    ". Quiet asks for one of $lineCount complete lines with no intermediary names.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenBlitz, enabled = moveCount > 0) { Text("Blitz") }
                OutlinedButton(onClick = onOpenQuiet, enabled = lineCount > 0) { Text("Quiet") }
            }
        }
    }
}

private fun tenths(millis: Long?): String {
    if (millis == null) return "—"
    val t = (millis + 50) / 100
    return "${t / 10}.${t % 10}s"
}
