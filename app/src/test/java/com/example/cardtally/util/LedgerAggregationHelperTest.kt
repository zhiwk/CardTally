package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerAggregationHelperTest {

    @Test
    fun summarize_keepsEveryExpenseCategoryWithoutOtherGrouping() {
        val statistics = linkedMapOf(
            "Housing" to -50.0,
            "Food" to -40.0,
            "Transport" to -30.0,
            "Health" to -20.0,
            "Education" to -10.0
        )

        val summary = LedgerAggregationHelper.summarize(statistics)

        assertEquals(150.0, summary.completeTotal, 0.0)
        assertEquals(statistics.keys.toList(), summary.slices.map { it.label })
        assertEquals(listOf(50.0, 40.0, 30.0, 20.0, 10.0), summary.slices.map { it.amount })
        summary.slices.forEach { assertEquals(setOf(it.label), it.sourceLabels) }
    }

    @Test
    fun summarize_keepsEveryIncomeCategoryAndCompleteTotal() {
        val statistics = linkedMapOf(
            "Salary" to 500.0,
            "Bonus" to 120.0,
            "Investment" to 80.0,
            "Freelance" to 60.0,
            "Gift" to 40.0
        )

        val summary = LedgerAggregationHelper.summarize(statistics)

        assertEquals(800.0, summary.completeTotal, 0.0)
        assertEquals(5, summary.slices.size)
        assertEquals(40.0, summary.slices.last().amount, 0.0)
        assertEquals(statistics.keys.toList(), summary.slices.map { it.label })
    }

    @Test
    fun summarize_preservesFlatParentAndChildCategorySlices() {
        val statistics = linkedMapOf(
            "Food" to -90.0,
            "Food/Breakfast" to -20.0,
            "Food/Lunch" to -30.0
        )

        val summary = LedgerAggregationHelper.summarize(statistics)

        assertEquals(140.0, summary.completeTotal, 0.0)
        assertEquals(setOf("Food", "Food/Breakfast", "Food/Lunch"), summary.slices.map { it.label }.toSet())
        assertEquals(listOf(90.0, 30.0, 20.0), summary.slices.map { it.amount })
    }

    @Test
    fun summarize_preservesRealOtherCategoryAndSkipsZeroAmounts() {
        val summary = LedgerAggregationHelper.summarize(linkedMapOf(
            "Food" to -50.0, "Other" to -12.0, "Transport" to -8.0,
            "Health" to -6.0, "Education" to -4.0, "Unused" to 0.0
        ))
        assertEquals(80.0, summary.completeTotal, 0.0)
        assertEquals(5, summary.slices.size)
        assertEquals(12.0, summary.slices.single { it.label == "Other" }.amount, 0.0)
        assertEquals(setOf("Other"), summary.slices.single { it.label == "Other" }.sourceLabels)
    }

    @Test
    fun summarize_supportsMoreCategoriesThanThePalette() {
        val statistics = (1..32).associate { "Category $it" to -it.toDouble() }
        val summary = LedgerAggregationHelper.summarize(statistics)
        assertEquals(32, summary.slices.size)
        assertEquals(528.0, summary.completeTotal, 0.0)
        assertEquals(statistics.keys, summary.slices.map { it.label }.toSet())
    }
}
