package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsRankingModePreferencesTest {

    @Test
    fun fromToken_returnsCorrectMode() {
        assertEquals(StatisticsRankingMode.SECONDARY, StatisticsRankingMode.fromToken("secondary"))
        assertEquals(StatisticsRankingMode.PRIMARY, StatisticsRankingMode.fromToken("primary"))
        assertEquals(StatisticsRankingMode.SECONDARY, StatisticsRankingMode.fromToken(null))
        assertEquals(StatisticsRankingMode.SECONDARY, StatisticsRankingMode.fromToken("unknown"))
    }
}
