package com.example.cardtally.util

data class LedgerCategorySlice(
    val label: String,
    val amount: Double,
    val sourceLabels: Set<String>
)

data class LedgerAggregationSummary(
    val completeTotal: Double,
    val slices: List<LedgerCategorySlice>
)

object LedgerAggregationHelper {
    fun summarize(
        statistics: Map<String, Double>,
        maxSlices: Int,
        otherLabel: String
    ): LedgerAggregationSummary {
        val sortedEntries = statistics.entries.sortedByDescending { (_, amount) -> kotlin.math.abs(amount) }
        val visibleCategoryCount = if (sortedEntries.size > maxSlices) maxSlices - 1 else maxSlices
        val visibleEntries = sortedEntries.take(visibleCategoryCount)
        val hiddenEntries = sortedEntries.drop(visibleCategoryCount)
        val slices = visibleEntries.map { entry ->
            LedgerCategorySlice(entry.key, kotlin.math.abs(entry.value), setOf(entry.key))
        }.toMutableList()

        if (hiddenEntries.isNotEmpty()) {
            slices += LedgerCategorySlice(
                label = otherLabel,
                amount = hiddenEntries.sumOf { kotlin.math.abs(it.value) },
                sourceLabels = hiddenEntries.mapTo(linkedSetOf<String>()) { it.key }
            )
        }

        return LedgerAggregationSummary(
            completeTotal = sortedEntries.sumOf { kotlin.math.abs(it.value) },
            slices = slices
        )
    }
}
