package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerAggregationHelperTest {

    @Test
    fun summarize_usesCompleteExpenseTotalWhenTopSlicesAreBounded() {
        val statistics = linkedMapOf(
            "Housing" to -50.0,
            "Food" to -40.0,
            "Transport" to -30.0,
            "Health" to -20.0,
            "Education" to -10.0
        )

        val summary = LedgerAggregationHelper.summarize(statistics, MAX_SLICES, OTHER_LABEL)

        assertEquals(150.0, summary.completeTotal, 0.0)
        assertEquals(listOf("Housing", "Food", "Transport", OTHER_LABEL), summary.slices.map { it.label })
        assertEquals(listOf(50.0, 40.0, 30.0, 30.0), summary.slices.map { it.amount })
    }

    @Test
    fun summarize_usesCompleteIncomeTotalWithoutChangingTopSliceLimit() {
        val statistics = linkedMapOf(
            "Salary" to 500.0,
            "Bonus" to 120.0,
            "Investment" to 80.0,
            "Freelance" to 60.0,
            "Gift" to 40.0
        )

        val summary = LedgerAggregationHelper.summarize(statistics, MAX_SLICES, OTHER_LABEL)

        assertEquals(800.0, summary.completeTotal, 0.0)
        assertEquals(MAX_SLICES, summary.slices.size)
        assertEquals(100.0, summary.slices.last().amount, 0.0)
    }

    @Test
    fun summarize_preservesFlatParentAndChildCategorySlices() {
        val statistics = linkedMapOf(
            "Food" to -90.0,
            "Food/Breakfast" to -20.0,
            "Food/Lunch" to -30.0
        )

        val summary = LedgerAggregationHelper.summarize(statistics, MAX_SLICES, OTHER_LABEL)

        assertEquals(140.0, summary.completeTotal, 0.0)
        assertEquals(setOf("Food", "Food/Breakfast", "Food/Lunch"), summary.slices.map { it.label }.toSet())
        assertEquals(listOf(90.0, 30.0, 20.0), summary.slices.map { it.amount })
    }

    companion object {
        private const val MAX_SLICES = 4
        private const val OTHER_LABEL = "Other"
    }
}
