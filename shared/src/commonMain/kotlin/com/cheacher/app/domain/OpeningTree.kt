package com.cheacher.app.domain

import com.cheacher.app.chess.Color
import com.cheacher.app.chess.Fen
import com.cheacher.app.chess.Move
import com.cheacher.app.chess.Position
import com.cheacher.app.chess.moveFromSan
import com.cheacher.app.chess.sanOf

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
            val children = repertoire.moves.map { move ->
                resolveNode(repertoire, move, root, parentId = null, depth = 0)
            }
            requireUniqueIds(repertoire, children)
            return OpeningTree(repertoire, root, children)
        }

        private fun resolveNode(
            repertoire: Repertoire,
            authored: RepertoireMove,
            before: Position,
            parentId: String?,
            depth: Int,
        ): TreeNode {
            val move = before.moveFromSan(authored.san)
                ?: throw RepertoireFormatException(
                    "repertoire '${repertoire.id}': '${authored.san}' is not legal in ${Fen.format(before)}",
                )
            // A move path names the chess that happened, not where the author happened
            // to place it in a list. Editing a sibling must never move persisted history.
            val id = listOfNotNull(parentId, move.uci).joinToString(NODE_ID_SEPARATOR)
            val after = before.applyUnchecked(move)
            val children = authored.children.map { child ->
                resolveNode(repertoire, child, after, parentId = id, depth = depth + 1)
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

        private fun requireUniqueIds(repertoire: Repertoire, roots: List<TreeNode>) {
            val seen = mutableSetOf<String>()

            fun visit(node: TreeNode) {
                if (!seen.add(node.id)) {
                    throw RepertoireFormatException(
                        "repertoire '${repertoire.id}' authors the move path '${node.id}' more than once",
                    )
                }
                node.children.forEach(::visit)
            }

            roots.forEach(::visit)
        }

        private const val NODE_ID_SEPARATOR = "/"

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
    /** Stable UCI move path from the repertoire root, independent of authored sibling order. */
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
