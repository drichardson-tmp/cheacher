package com.roseau.opening.domain

import com.roseau.opening.chess.Color
import com.roseau.opening.chess.Fen
import com.roseau.opening.chess.Move
import com.roseau.opening.chess.Position
import com.roseau.opening.chess.moveFromSan
import com.roseau.opening.chess.sanOf

/**
 * A [Repertoire] resolved against real board states.
 *
 * Every node carries the [Position] *after* its move, so the trainers and the board UI
 * never have to replay from the root. Resolution happens once, at load; from then on
 * an illegal move in the data is impossible by construction.
 */
class OpeningTree private constructor(
    val repertoire: Repertoire,
    val root: Position,
    val rootChildren: List<TreeNode>,
) {
    /** Every node, in depth-first order. */
    val allNodes: List<TreeNode> by lazy {
        buildList { rootChildren.forEach { collectDepthFirst(it, this) } }
    }

    private val byId: Map<String, TreeNode> by lazy { allNodes.associateBy { it.id } }

    /** Every root-to-leaf path — one "line" of the repertoire. */
    val lines: List<List<TreeNode>> by lazy {
        buildList { rootChildren.forEach { collectLines(it, emptyList(), this) } }
    }

    fun node(id: String): TreeNode? = byId[id]

    /** Children available at [node], or the first moves when [node] is null (the root). */
    fun childrenOf(node: TreeNode?): List<TreeNode> = node?.children ?: rootChildren

    fun positionAt(node: TreeNode?): Position = node?.position ?: root

    /** Whose move it is at [node] — the side the learner must play next. */
    fun sideToMoveAt(node: TreeNode?): Color = positionAt(node).sideToMove

    companion object {
        /**
         * Resolves [repertoire] into a tree, or throws if a SAN string is not legal in the
         * position it was authored for. Repertoire data is content, and content can be wrong;
         * failing here means it can never be wrong at the board.
         */
        fun resolve(repertoire: Repertoire): OpeningTree {
            val root = Fen.parseOrNull(repertoire.startFen)
                ?: throw RepertoireFormatException("repertoire '${repertoire.id}' has an invalid start FEN")
            val children = repertoire.moves.mapIndexed { index, move ->
                resolveNode(repertoire, move, root, parentId = null, index = index, depth = 0)
            }
            return OpeningTree(repertoire, root, children)
        }

        private fun resolveNode(
            repertoire: Repertoire,
            authored: RepertoireMove,
            before: Position,
            parentId: String?,
            index: Int,
            depth: Int,
        ): TreeNode {
            val move = before.moveFromSan(authored.san)
                ?: throw RepertoireFormatException(
                    "repertoire '${repertoire.id}': '${authored.san}' is not legal in ${Fen.format(before)}",
                )
            val id = if (parentId == null) "$index" else "$parentId.$index"
            val after = before.applyUnchecked(move)
            val children = authored.children.mapIndexed { childIndex, child ->
                resolveNode(repertoire, child, after, parentId = id, index = childIndex, depth = depth + 1)
            }
            return TreeNode(
                id = id,
                parentId = parentId,
                move = move,
                san = before.sanOf(move),
                name = authored.name,
                idea = authored.idea,
                mover = before.sideToMove,
                positionBefore = before,
                position = after,
                depth = depth,
                children = children,
            )
        }

        private fun collectDepthFirst(node: TreeNode, into: MutableList<TreeNode>) {
            into += node
            node.children.forEach { collectDepthFirst(it, into) }
        }

        private fun collectLines(node: TreeNode, prefix: List<TreeNode>, into: MutableList<List<TreeNode>>) {
            val path = prefix + node
            if (node.children.isEmpty()) into += path else node.children.forEach { collectLines(it, path, into) }
        }
    }
}

/** One authored move, resolved. Immutable and safe to hold in Compose state. */
data class TreeNode(
    val id: String,
    val parentId: String?,
    val move: Move,
    /** SAN regenerated from the position, so it is canonical even if the data was sloppy. */
    val san: String,
    val name: String,
    val idea: String,
    /** The side that plays this move. */
    val mover: Color,
    val positionBefore: Position,
    val position: Position,
    val depth: Int,
    val children: List<TreeNode>,
) {
    val isLeaf: Boolean get() = children.isEmpty()

    /** `1.` / `1...` style prefix for display next to [san]. */
    val moveNumberLabel: String
        get() = "${positionBefore.fullmoveNumber}${if (mover == Color.WHITE) "." else "..."}"

    override fun toString(): String = "$moveNumberLabel$san"
}

class RepertoireFormatException(message: String) : IllegalArgumentException(message)
