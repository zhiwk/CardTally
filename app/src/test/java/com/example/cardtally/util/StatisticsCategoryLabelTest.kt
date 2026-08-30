package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsCategoryLabelTest {
    @Test
    fun removesPresentationPrefixesWithoutChangingBareNames() {
        assertEquals("住房", normalizeStatisticsCategoryLabel("支出: 住房"))
        assertEquals("服饰", normalizeStatisticsCategoryLabel("支出 · 服饰"))
        assertEquals("家电", normalizeStatisticsCategoryLabel("家电"))
    }
}
