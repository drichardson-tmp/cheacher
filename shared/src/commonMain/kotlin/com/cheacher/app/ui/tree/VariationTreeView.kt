package com.cheacher.app.ui.tree

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cheacher.app.domain.OpeningTree
import com.cheacher.app.domain.TreeNode
import com.cheacher.app.training.NodeStatus
import com.cheacher.app.ui.theme.CheacherTheme
import com.cheacher.app.ui.theme.Motion

/**
 * The mini DAG for branch recall: every move is a chip, every variation a row.
 *
 * The diagram *is* the scoreboard. Chips take their colour from [NodeStatus] — parchment
 * while open, green once the line is banked, red when it was lost — and closed branches
 * dim toward the paper so the eye lands on what is still live. The cursor gets a brass
 * ring. Colour and alpha both animate, so closing a branch reads as it fading into
 * history rather than a repaint.
 *
 * [showNames] widens the chips and puts the canonical name above the move — the
 * read-only form, for the end of a round. During recall the diagram stays off screen
 * entirely: it is a picture of the answer.
 */
@Composable
fun VariationTreeView(
    tree: OpeningTree,
    statusOf: (TreeNode) -> NodeStatus,
    cursorId: String?,
    modifier: Modifier = Modifier,
    showNames: Boolean = false,
) {
    val layout = remember(tree) { TreeLayout(tree) }
    val cellW = if (showNames) 132.dp else 54.dp
    val cellH = if (showNames) 40.dp else 26.dp
    val hGap = 14.dp
    val vGap = 8.dp

    val width = cellW * layout.columns + hGap * (layout.columns - 1).coerceAtLeast(0)
    val height = cellH * layout.rows + vGap * (layout.rows - 1).coerceAtLeast(0)

    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Box(Modifier.size(width = width, height = height)) {
            Connectors(tree, layout, statusOf, cellW, cellH, hGap, vGap)
            for (node in tree.allNodes) {
                val slot = layout.slotOf(node.id)
                key(node.id) {
                    NodeChip(
                        node = node,
                        status = statusOf(node),
                        isCursor = node.id == cursorId,
                        showName = showNames,
                        modifier = Modifier
                            .offsetIn(slot, cellW, cellH, hGap, vGap)
                            .size(cellW, cellH),
                    )
                }
            }
        }
    }
}

private fun Modifier.offsetIn(slot: Slot, cellW: Dp, cellH: Dp, hGap: Dp, vGap: Dp): Modifier =
    offset(x = (cellW + hGap) * slot.column, y = (cellH + vGap) * slot.row)

@Composable
private fun NodeChip(
    node: TreeNode,
    status: NodeStatus,
    isCursor: Boolean,
    showName: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = CheacherTheme.colors
    val fill by animateColorAsState(
        targetValue = when (status) {
            NodeStatus.UNVISITED -> colors.treeUnvisited
            NodeStatus.IN_PROGRESS -> colors.treeInProgress
            NodeStatus.COMPLETED -> colors.treeCompleted
            NodeStatus.FAILED -> colors.treeFailed
            NodeStatus.LOCKED -> colors.lockedGhost
        },
        animationSpec = Motion.fade(),
        label = "fill-${node.id}",
    )
    // Three depths of presence: live branches at full ink, closed ones dimmed into
    // history, locked ones barely on the paper at all — a promise, not a task.
    val alpha by animateFloatAsState(
        targetValue = when {
            status == NodeStatus.LOCKED -> 0.22f
            status.isClosed -> 0.45f
            else -> 1f
        },
        animationSpec = Motion.fade(),
        label = "alpha-${node.id}",
    )
    val onFill = when (status) {
        NodeStatus.COMPLETED, NodeStatus.FAILED -> colors.onVerdict
        else -> colors.treeOpenText
    }
    val label = when {
        status == NodeStatus.COMPLETED && node.isLeaf -> "${node.san} ✓"
        status == NodeStatus.FAILED && node.isLeaf -> "${node.san} ✗"
        else -> node.san
    }
    Box(
        modifier = modifier
            .alpha(alpha)
            .background(fill, RoundedCornerShape(7.dp))
            .then(
                if (isCursor) {
                    Modifier.border(2.dp, colors.streakAccent, RoundedCornerShape(7.dp))
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showName) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = onFill,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = onFill,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = onFill,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Connectors(
    tree: OpeningTree,
    layout: TreeLayout,
    statusOf: (TreeNode) -> NodeStatus,
    cellW: Dp,
    cellH: Dp,
    hGap: Dp,
    vGap: Dp,
) {
    val lineColor = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current
    Canvas(
        Modifier.size(
            width = cellW * layout.columns + hGap * (layout.columns - 1).coerceAtLeast(0),
            height = cellH * layout.rows + vGap * (layout.rows - 1).coerceAtLeast(0),
        ),
    ) {
        val cw = with(density) { cellW.toPx() }
        val ch = with(density) { cellH.toPx() }
        val hg = with(density) { hGap.toPx() }
        val vg = with(density) { vGap.toPx() }

        fun rightEdge(slot: Slot) = Offset(slot.column * (cw + hg) + cw, slot.row * (ch + vg) + ch / 2)
        fun leftEdge(slot: Slot) = Offset(slot.column * (cw + hg), slot.row * (ch + vg) + ch / 2)

        fun linkAlpha(child: TreeNode): Float = when {
            statusOf(child) == NodeStatus.LOCKED -> 0.12f
            statusOf(child).isClosed -> 0.3f
            else -> 0.8f
        }

        for (node in tree.allNodes) {
            for (child in node.children) {
                drawLink(
                    from = rightEdge(layout.slotOf(node.id)),
                    to = leftEdge(layout.slotOf(child.id)),
                    color = lineColor,
                    alpha = linkAlpha(child),
                )
            }
        }
        // Root fan-out: a shared stem for the first moves.
        if (tree.rootChildren.size > 1) {
            val first = leftEdge(layout.slotOf(tree.rootChildren.first().id))
            for (child in tree.rootChildren.drop(1)) {
                drawLink(
                    from = Offset(first.x - hg / 2, first.y),
                    to = leftEdge(layout.slotOf(child.id)),
                    color = lineColor,
                    alpha = linkAlpha(child),
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLink(
    from: Offset,
    to: Offset,
    color: Color,
    alpha: Float,
) {
    val path = Path().apply {
        moveTo(from.x, from.y)
        val midX = (from.x + to.x) / 2
        cubicTo(midX, from.y, midX, to.y, to.x, to.y)
    }
    drawPath(path, color = color.copy(alpha = alpha), style = Stroke(width = 2f))
}

// ------------------------------------------------------------------------ layout

private data class Slot(val column: Int, val row: Int)

/**
 * Column = depth; row = the variation the node belongs to. Each leaf claims the next
 * free row and interior nodes sit on their first child's row, so a line reads straight
 * left-to-right and branches drop below their parent — the shape of an opening book.
 */
private class TreeLayout(tree: OpeningTree) {
    private val slots = mutableMapOf<String, Slot>()
    var rows = 0
        private set
    val columns: Int = (tree.allNodes.maxOfOrNull { it.depth } ?: 0) + 1

    init {
        tree.rootChildren.forEach { place(it) }
        if (rows == 0) rows = 1
    }

    private fun place(node: TreeNode): Int {
        val row = if (node.isLeaf) {
            rows++
        } else {
            node.children.map { place(it) }.first()
        }
        slots[node.id] = Slot(column = node.depth, row = row)
        return row
    }

    fun slotOf(id: String): Slot = slots.getValue(id)
}
