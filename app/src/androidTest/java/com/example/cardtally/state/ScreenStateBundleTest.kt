package com.example.cardtally.state

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenStateBundleTest {
    @Test
    fun recordForms_roundTripEveryApprovedFieldAcrossProcessStyleBundle() {
        // Given: complete Add/Edit Record drafts, including an open category sheet
        val addState = RecordFormState.DEFAULT.copy(
            amountBuffer = "12.30", recordType = RecordType.INCOME,
            selectedDate = "2026-08-15", selectedAssetId = 7L,
            selectedCategoryId = 9L, description = "draft",
            feeBuffer = "3.50",
            openSheet = RecordSheet.CATEGORY, pendingCategoryId = 10L
        )
        val editState = EditRecordState(42L, addState)
        val bundle = Bundle()

        // When: state is written and reconstructed from a separate Bundle
        addState.writeTo(bundle)
        editState.writeTo(bundle)
        val restoredBundle = Bundle(bundle)

        // Then: stable IDs and draft text restore exactly
        assertEquals(addState, RecordFormState.readFrom(restoredBundle, RecordFormState.DEFAULT))
        assertEquals(editState, EditRecordState.readFrom(restoredBundle, RecordFormState.DEFAULT))
    }

    @Test
    fun malformedRecordState_usesExactEnumFallbacksAndClearsMissingEntities() {
        // Given: interrupted recreation with malformed tokens and deleted references
        val bundle = Bundle().apply {
            putString("state_record_type", "future_type")
            putString("state_open_sheet", "future_sheet")
            putLong("state_selected_asset_id", 7L)
            putLong("state_selected_category_id", 9L)
        }

        // When: state is sanitized and IDs are resolved against current entities
        val restored = RecordFormState.readFrom(bundle, RecordFormState.DEFAULT)

        // Then: enums fall back and missing IDs remain cleared rather than name-matched
        assertEquals(RecordType.EXPENSE, restored.recordType)
        assertEquals(RecordSheet.NONE, restored.openSheet)
        assertNull(StableIdResolver.resolve(restored.selectedAssetId, emptySet()))
        assertNull(StableIdResolver.resolve(restored.selectedCategoryId, emptySet()))
    }

    @Test
    fun assetForms_roundTripDraftsAndSanitizeUnknownType() {
        // Given: complete Add/Edit Asset drafts
        val addState = AssetFormState("Wallet", "88.20", AssetType.WECHAT)
        val editState = EditAssetState(12L, addState)
        val bundle = Bundle()

        // When: state is restored after process-style Bundle copying
        addState.writeTo(bundle)
        editState.writeTo(bundle)

        // Then: drafts override loaded values and retain stable asset ID
        assertEquals(addState, AssetFormState.readFrom(Bundle(bundle), AssetFormState.DEFAULT))
        assertEquals(editState, EditAssetState.readFrom(Bundle(bundle), AssetFormState.DEFAULT))

        bundle.putString("state_asset_type", "crypto")
        assertEquals(AssetType.CASH, AssetFormState.readFrom(bundle, AssetFormState.DEFAULT).assetType)
    }

    @Test
    fun ledger_roundTripsEveryFieldAndRepairsInvalidCustomRange() {
        // Given: Ledger details/statistics positions, custom range, chart and open filter state
        val state = LedgerScreenState(
            view = LedgerViewState.STATISTICS_INCOME,
            statisticsType = StatisticsType.INCOME,
            periodPreset = PeriodPreset.CUSTOM,
            customStartDate = "2026-08-01",
            customEndDate = "2026-08-15",
            chartMode = ChartMode.LINE,
            detailsScrollPosition = 4,
            detailsScrollOffset = 11,
            statisticsScrollPosition = 3,
            statisticsScrollOffset = 9,
            openFilterSurface = FilterSurface.PERIOD
        )
        val bundle = Bundle()

        // When: every approved field is restored
        state.writeTo(bundle)

        // Then: recreation is exact
        assertEquals(state, LedgerScreenState.readFrom(Bundle(bundle), LedgerViewState.DETAILS))

        // Given: an inverted custom range
        bundle.putString("state_custom_start_date", "2026-08-20")
        bundle.putString("state_custom_end_date", "2026-08-10")

        // When: malformed custom state is parsed
        val repaired = LedgerScreenState.readFrom(bundle, LedgerViewState.DETAILS)

        // Then: the range falls back to ALL
        assertEquals(PeriodPreset.ALL, repaired.periodPreset)
        assertNull(repaired.customStartDate)
        assertNull(repaired.customEndDate)
    }

    @Test
    fun agent_roundTripsComposerSessionDrawerAndBothScrollPositions() {
        // Given: configured Agent state with local draft text
        val state = AgentScreenState(8L, "unsent draft", true, 12, 5, 2, 7)
        val bundle = Bundle()

        // When: process-style state is restored
        state.writeTo(bundle)

        // Then: only approved fields are present and exact
        assertEquals(state, AgentScreenState.readFrom(Bundle(bundle)))
        assertFalse(bundle.keySet().any { it.contains("api", ignoreCase = true) || it.contains("model", ignoreCase = true) || it.contains("url", ignoreCase = true) })
    }

    @Test
    fun inFlightAiLifecycle_cancelsAndVisiblyResetsWithoutRestoringRequest() {
        // Given: an active Agent request lifecycle bound to a cancellable handle
        var agentCancelled = false
        val lifecycle = InFlightAiLifecycle()
        val handle = com.example.cardtally.network.MiniMaxRequestHandle(
            cancelAction = { agentCancelled = true }
        )
        lifecycle.markStarted(handle)

        // When: recreation destroys the request owner
        lifecycle.cancelAndReset()

        // Then: transport cancellation occurs and loading state is visibly reset
        assertTrue(agentCancelled)
        assertFalse(lifecycle.isLoading)
    }
}
