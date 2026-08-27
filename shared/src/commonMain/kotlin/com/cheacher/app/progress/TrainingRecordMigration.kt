package com.cheacher.app.progress

import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.TreeNode

/**
 * Re-keys records written when tree nodes were addressed by authored list position.
 *
 * Old ids (`0.1.0`) can only be interpreted while the corresponding authored shape is
 * still available. Migrating at the app boundary makes that interpretation once, before
 * new UCI-path keys are read or written; the next journal write persists the stable keys.
 */
internal fun TrainingRecord.withStableNodeIds(tree: OpeningTree): TrainingRecord {
    val legacyIds = buildMap {
        fun visit(nodes: List<TreeNode>, parentId: String?) {
            nodes.forEachIndexed { index, node ->
                val legacyId = listOfNotNull(parentId, index.toString()).joinToString(".")
                put(legacyId, node.id)
                visit(node.children, legacyId)
            }
        }

        visit(tree.rootChildren, null)
    }

    val hasLegacyKeys = sequenceOf(
        missCounts.keys,
        errorScores.keys,
        lineCompletions.keys,
        branchLineCompletions.keys,
        lineReviews.keys,
        nodeReviews.keys,
        lineCredits.keys,
    ).flatten().any(legacyIds::containsKey)
    if (!hasLegacyKeys) return this

    return copy(
        missCounts = missCounts.rekeyCounts(legacyIds),
        errorScores = errorScores.rekeyCounts(legacyIds),
        lineCompletions = lineCompletions.rekeyCounts(legacyIds),
        branchLineCompletions = branchLineCompletions.rekeyCounts(legacyIds),
        lineReviews = lineReviews.rekeyLatest(legacyIds),
        nodeReviews = nodeReviews.rekeyLatest(legacyIds),
        lineCredits = lineCredits.rekeyLatest(legacyIds),
    )
}

/** Counts are additive if a record contains both an old key and its stable replacement. */
private fun Map<String, Int>.rekeyCounts(legacyIds: Map<String, String>): Map<String, Int> =
    entries.fold(emptyMap()) { result, (id, count) ->
        val stableId = legacyIds[id] ?: id
        result + (stableId to (result[stableId] ?: 0) + count)
    }

/** A stable entry is newer than its legacy counterpart and therefore wins on collision. */
private fun <Value> Map<String, Value>.rekeyLatest(legacyIds: Map<String, String>): Map<String, Value> =
    buildMap {
        this@rekeyLatest.forEach { (id, value) ->
            if (id in legacyIds) put(legacyIds.getValue(id), value)
        }
        this@rekeyLatest.forEach { (id, value) ->
            if (id !in legacyIds) put(id, value)
        }
    }
