package com.example.cardtally.adapter

import org.junit.Assert.assertEquals
import org.junit.Test

class FeaturedIconLabelsTest {

    @Test
    fun `parse maps key equals label entries`() {
        val labels = FeaturedIconLabels.parse(listOf("tabler_car=汽车", "tabler_home=家", "malformed"))
        assertEquals("汽车", labels["tabler_car"])
        assertEquals("家", labels["tabler_home"])
        assertEquals(2, labels.size)
    }

    @Test
    fun `parse ignores blank labels and no-separator entries`() {
        val labels = FeaturedIconLabels.parse(listOf("tabler_car=汽车", "tabler_x=", "no_separator", "= 空key"))
        assertEquals("汽车", labels["tabler_car"])
        assertEquals(1, labels.size)
    }

    @Test
    fun `humanize never leaves a raw catalog key`() {
        assertEquals("Cash banknote plus", FeaturedIconLabels.humanize("tabler_cash_banknote_plus"))
        assertEquals("Category", FeaturedIconLabels.humanize("tabler_category"))
    }

    @Test
    fun `describe prefers a localized label and falls back to humanized name`() {
        val labels = mapOf("tabler_car" to "汽车")
        assertEquals("汽车", FeaturedIconLabels.describe("tabler_car", labels))
        assertEquals("Cash banknote plus", FeaturedIconLabels.describe("tabler_cash_banknote_plus", labels))
    }
}
