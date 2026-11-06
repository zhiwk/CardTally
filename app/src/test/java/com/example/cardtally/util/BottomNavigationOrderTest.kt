package com.example.cardtally.util

import com.example.cardtally.util.BottomNavigationDestination.*
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavigationOrderTest {
    @Test
    fun missingInvalidOrUnknownValuesUseTheOriginalOrder() {
        listOf(null, true, 12, "", "unknown").forEach { value ->
            assertEquals(listOf(LEDGER, STATISTICS, ASSETS, AGENT, SETTINGS), BottomNavigationOrder.decode(value))
        }
    }

    @Test
    fun duplicatesAndUnknownKeysAreDroppedAndMissingDestinationsAreAppended() {
        assertEquals(listOf(SETTINGS, LEDGER, STATISTICS, ASSETS, AGENT),
            BottomNavigationOrder.decode("settings,unknown,ledger,settings"))
    }

    @Test
    fun persistedKeysRoundTripWithoutUsingResourceIdsOrTranslatedLabels() {
        val order = listOf(AGENT, SETTINGS, ASSETS, STATISTICS, LEDGER)
        assertEquals("agent,settings,assets,statistics,ledger", BottomNavigationOrder.encode(order))
        assertEquals(order, BottomNavigationOrder.decode(BottomNavigationOrder.encode(order)))
    }

    @Test
    fun reorderingAllDestinationsUsesTheRequestedOrder() {
        val reversed = BottomNavigationOrder.defaultOrder.reversed()
        assertEquals(reversed, BottomNavigationOrder.mergeVisible(BottomNavigationOrder.defaultOrder, reversed))
    }

    @Test
    fun hiddenAssetAndIndependentAiKeepTheirSavedSlots() {
        assertEquals(listOf(SETTINGS, LEDGER, ASSETS, AGENT, STATISTICS),
            BottomNavigationOrder.mergeVisible(BottomNavigationOrder.defaultOrder, listOf(SETTINGS, LEDGER, STATISTICS)))
    }

    @Test
    fun emptyAndDuplicateVisibleListsCannotLoseOrDuplicateDestinations() {
        assertEquals(BottomNavigationOrder.defaultOrder,
            BottomNavigationOrder.mergeVisible(BottomNavigationOrder.defaultOrder, emptyList()))
        assertEquals(listOf(SETTINGS, STATISTICS, ASSETS, AGENT, LEDGER),
            BottomNavigationOrder.mergeVisible(BottomNavigationOrder.defaultOrder, listOf(SETTINGS, SETTINGS, LEDGER)))
    }
}
