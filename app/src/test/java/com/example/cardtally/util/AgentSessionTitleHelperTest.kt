package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentSessionTitleHelperTest {

    @Test
    fun createDefaultTitle_formatsChineseDateWithZeroPadding() {
        val title = AgentSessionTitleHelper.createDefaultTitle(1775664000000L)

        assertEquals("新会话-2026年04月09日", title)
    }

    @Test
    fun createDefaultTitle_keepsCalendarDayBoundariesStable() {
        val title = AgentSessionTitleHelper.createDefaultTitle(1735660799000L)

        assertEquals("新会话-2024年12月31日", title)
    }
}
