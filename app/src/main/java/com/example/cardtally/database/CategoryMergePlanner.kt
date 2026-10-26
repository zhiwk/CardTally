package com.example.cardtally.database

import com.example.cardtally.model.Category

class CategoryMergeException(val reason: Reason) : IllegalArgumentException(reason.name) {
    enum class Reason { INVALID_TARGET, NAME_CONFLICT, DEPTH, STALE, INVALID_REFERENCES }
}

/** Pure tree planning, shared by target filtering, preview and transaction-time validation. */
object CategoryMergePlanner {
    data class Plan(
        val sourceId: Long, val targetId: Long, val original: List<Category>, val result: List<Category>,
        val aliases: Map<Long, Long>, val affectedLeaves: Set<Long>, val moved: Map<Long, Long>, val sourceNodes: Set<Long>
    )

    fun path(rows: List<Category>, id: Long): String {
        val byId = rows.associateBy { it.id }
        val seen = mutableSetOf<Long>()
        val parts = mutableListOf<String>()
        var row = byId[id]
        while (row != null && seen.add(row.id)) { parts += row.name; row = row.parentId?.let(byId::get) }
        return parts.reversed().joinToString(" / ")
    }

    fun prepare(rows: List<Category>, sourceId: Long, targetId: Long, maxDepth: Int): Plan {
        fun invalid(): Nothing = throw CategoryMergeException(CategoryMergeException.Reason.INVALID_TARGET)
        val original = rows.sortedBy { it.id }.map { it.copy() }
        val work = original.associateBy { it.id }.toMutableMap()
        val source = work[sourceId] ?: invalid()
        val target = work[targetId] ?: invalid()
        if (sourceId == targetId || source.type != target.type || source.type !in 0..1) invalid()
        fun ancestors(id: Long): Set<Long> {
            val seen = mutableSetOf<Long>()
            var parent = work[id]?.parentId
            while (parent != null) {
                if (!seen.add(parent)) invalid()
                parent = work[parent]?.parentId
            }
            return seen
        }
        if (sourceId in ancestors(targetId) || targetId in ancestors(sourceId)) invalid()
        fun children(id: Long) = work.values.filter { it.parentId == id }.sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.id })
        fun subtree(id: Long): Set<Long> {
            val found = mutableSetOf<Long>()
            fun visit(node: Long) { if (!found.add(node)) invalid(); children(node).forEach { visit(it.id) } }
            visit(id)
            return found
        }
        val sourceIds = subtree(sourceId)
        if (sourceIds.any { work.getValue(it).type != source.type }) invalid()
        if (subtree(targetId).any { work.getValue(it).type != target.type }) invalid()
        val affected = sourceIds.filter { children(it).isEmpty() }.toSet()
        val aliases = mutableMapOf<Long, Long>()
        val moved = mutableMapOf<Long, Long>()
        fun merge(from: Long, into: Long) {
            val fromChildren = children(from)
            val intoChildren = children(into)
            if (fromChildren.isEmpty() != intoChildren.isEmpty())
                throw CategoryMergeException(CategoryMergeException.Reason.NAME_CONFLICT)
            for (child in fromChildren) {
                val matches = children(into).filter { it.name == child.name }
                if (matches.size > 1) throw CategoryMergeException(CategoryMergeException.Reason.NAME_CONFLICT)
                if (matches.isNotEmpty()) merge(child.id, matches.single().id)
                else {
                    val previous = children(into).maxOfOrNull { it.sortOrder } ?: -1
                    if (previous == Int.MAX_VALUE) invalid()
                    val order = previous + 1
                    work[child.id] = child.copy(parentId = into, sortOrder = order)
                    moved[child.id] = into
                }
            }
            aliases[from] = into
            work.remove(from)
        }
        merge(sourceId, targetId)
        // Check the entire moved subtree, not just its first assigned parent.
        for (id in (sourceIds - aliases.keys) + aliases.values) {
            val depth = ancestors(id).size + 1
            if (depth > maxDepth) throw CategoryMergeException(CategoryMergeException.Reason.DEPTH)
        }
        return Plan(sourceId, targetId, original, work.values.sortedBy { it.id }, aliases, affected, moved, sourceIds)
    }
}
