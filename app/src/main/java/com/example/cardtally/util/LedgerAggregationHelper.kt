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
    /** One nonzero category per sector. Label budgeting belongs to the chart, not the data. */
    fun summarize(statistics: Map<String, Double>): LedgerAggregationSummary {
        val sortedEntries = statistics.entries.filter { it.value != 0.0 }
            .sortedByDescending { (_, amount) -> kotlin.math.abs(amount) }
        val slices = sortedEntries.map { entry ->
            LedgerCategorySlice(entry.key, kotlin.math.abs(entry.value), setOf(entry.key))
        }

        return LedgerAggregationSummary(
            completeTotal = sortedEntries.sumOf { kotlin.math.abs(it.value) },
            slices = slices
        )
    }
}
