package com.example.cardtally.util

import com.example.cardtally.adapter.StatisticsAdapter
import com.example.cardtally.model.Category

/** Builds the parent/leaf ranking tree from category totals without touching views or SQLite. */
object StatisticsRankingBuilder {
    data class CategoryRow(
        val categoryId: Long,
        val label: String,
        val amount: Double,
        val entryCount: Int,
        val iconName: String?
    )

    fun buildById(
        rows: List<CategoryRow>,
        categoriesById: Map<Long, Category>
    ): Pair<List<StatisticsAdapter.StatisticsAdapterItem>, Double> {
        fun rootId(id: Long): Long {
            var current = categoriesById[id] ?: return id
            val visited = mutableSetOf<Long>()
            while (current.parentId != null && visited.add(current.id)) {
                current = categoriesById[current.parentId] ?: break
            }
            return current.id
        }
        val parents = rows.groupBy { rootId(it.categoryId) }.map { (rootId, children) ->
            val root = categoriesById[rootId]
            val rootRow = children.singleOrNull()?.takeIf { it.categoryId == rootId }
            val childItems = children.filter { it.categoryId != rootId }.map { row ->
                StatisticsAdapter.StatisticsAdapterItem(
                    type = StatisticsAdapter.ItemType.PRIMARY_CHILD,
                    label = categoriesById[row.categoryId]?.name ?: row.label,
                    amount = row.amount,
                    categoryId = row.categoryId,
                    entryCount = row.entryCount,
                    iconName = row.iconName,
                    parentLabel = root?.name
                )
            }.sortedByDescending { kotlin.math.abs(it.amount) }
            StatisticsAdapter.StatisticsAdapterItem(
                type = StatisticsAdapter.ItemType.PRIMARY_PARENT,
                label = root?.name ?: rootRow?.label ?: children.first().label,
                amount = children.sumOf { it.amount },
                categoryId = rootId,
                entryCount = children.sumOf { it.entryCount },
                iconName = root?.icon ?: rootRow?.iconName,
                children = childItems
            )
        }.sortedByDescending { kotlin.math.abs(it.amount) }
        return parents to rows.sumOf { kotlin.math.abs(it.amount) }
    }

    fun build(
        normalizedStats: Map<String, Double>,
        entryCounts: Map<String, Int>,
        categoryIcons: Map<String, String>,
        categoriesById: Map<Long, Category>,
        categoriesByName: Map<String, Category>
    ): Pair<List<StatisticsAdapter.StatisticsAdapterItem>, Double> {
        data class ChildData(val label: String, val amount: Double, val count: Int, val icon: String?)
        data class ParentData(
            val label: String,
            var amount: Double = 0.0,
            var count: Int = 0,
            var icon: String? = null,
            val children: MutableMap<String, ChildData> = mutableMapOf()
        )

        fun rootOf(category: Category): Category {
            var current = category
            val visited = mutableSetOf<Long>()
            while (current.parentId != null && visited.add(current.id)) {
                current = categoriesById[current.parentId] ?: break
            }
            return current
        }

        val parents = mutableMapOf<String, ParentData>()
        normalizedStats.forEach { (rawLabel, amount) ->
            val label = normalizeStatisticsCategoryLabel(rawLabel)
            val count = entryCounts[label] ?: 0
            val icon = categoryIcons[label]
            val category = categoriesByName[label]
            val root = category?.let(::rootOf)
            val rootLabel = root?.name ?: label
            val rootIcon = root?.icon?.takeIf(String::isNotBlank) ?: categoryIcons[rootLabel]
            val parent = parents.getOrPut(rootLabel) { ParentData(rootLabel, icon = rootIcon) }
            parent.amount += amount
            parent.count += count
            if (parent.icon.isNullOrBlank() && !rootIcon.isNullOrBlank()) parent.icon = rootIcon
            if (label != rootLabel || category?.parentId != null) {
                val old = parent.children[label]
                parent.children[label] = ChildData(
                    label,
                    amount + (old?.amount ?: 0.0),
                    count + (old?.count ?: 0),
                    icon ?: old?.icon
                )
            }
        }
        val total = parents.values.sumOf { kotlin.math.abs(it.amount) }
        val items = parents.values.sortedByDescending { kotlin.math.abs(it.amount) }.map { parent ->
            val children = parent.children.values.sortedByDescending { kotlin.math.abs(it.amount) }.map { child ->
                StatisticsAdapter.StatisticsAdapterItem(
                    type = StatisticsAdapter.ItemType.PRIMARY_CHILD,
                    label = child.label,
                    amount = child.amount,
                    entryCount = child.count,
                    iconName = child.icon,
                    parentLabel = parent.label
                )
            }
            StatisticsAdapter.StatisticsAdapterItem(
                type = StatisticsAdapter.ItemType.PRIMARY_PARENT,
                label = parent.label,
                amount = parent.amount,
                entryCount = parent.count,
                iconName = parent.icon,
                children = children
            )
        }
        return items to total
    }
}
