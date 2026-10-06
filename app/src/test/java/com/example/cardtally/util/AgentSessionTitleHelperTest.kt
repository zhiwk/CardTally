package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class AgentSessionTitleHelperTest {

    @Test
    fun createDefaultTitle_formatsChineseDateWithZeroPadding() {
        val title = AgentSessionTitleHelper.createDefaultTitle(
            1775664000000L,
            TimeZone.getTimeZone("Asia/Shanghai")
        )

        assertEquals("新会话-2026年04月09日", title)
        assertEquals(
            "新会话-2026年04月08日",
            AgentSessionTitleHelper.createDefaultTitle(1775664000000L, TimeZone.getTimeZone("UTC"))
        )
    }

    @Test
    fun createDefaultTitle_keepsCalendarDayBoundariesStable() {
        val timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        val title = AgentSessionTitleHelper.createDefaultTitle(1735660799000L, timeZone)

        assertEquals("新会话-2024年12月31日", title)
        assertEquals(
            "新会话-2025年01月01日",
            AgentSessionTitleHelper.createDefaultTitle(1735660800000L, timeZone)
        )
    }
}
