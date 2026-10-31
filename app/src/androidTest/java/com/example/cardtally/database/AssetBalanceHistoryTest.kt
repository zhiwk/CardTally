package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Asset
import com.example.cardtally.model.AssetBalanceEvent
import com.example.cardtally.model.AssetHistoryEntry
import com.example.cardtally.model.Record
import com.example.cardtally.testing.IsolatedTestGuard
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetBalanceHistoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var helper: DatabaseHelper

    @Before fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context.deleteDatabase("CardTally.db")
        helper = DatabaseHelper(context)
    }

    @After fun tearDown() {
        if (::helper.isInitialized) helper.close()
        if (IsolatedTestGuard.isIsolatedBuild) context.deleteDatabase("CardTally.db")
    }

    @Test fun initializationIncludesZeroAndNegativeButNeverCreatesIncomeOrExpense() {
        for (amount in listOf(0.0, -125.50, 500.0)) {
            val id = helper.addAsset(Asset(name = "Wallet $amount", amount = amount))
            val event = events(id).single()
            assertEquals(AssetBalanceEvent.INITIAL, event.kind)
            assertEquals(0L, event.balanceBeforeMinor)
            assertEquals(requireNotNull(com.example.cardtally.util.Money.toMinor(amount)), event.balanceAfterMinor)
            assertEquals(amount, helper.getAllAssets().single { it.id == id }.amount, 0.0)
        }
        assertTrue(helper.getAllRecords().isEmpty())
        assertEquals(0L, helper.getTotalByTypeMinor(0))
        assertEquals(0L, helper.getTotalByTypeMinor(1))
    }

    @Test fun editingTheSameMutableAssetRecordsPersistedBeforeBalanceAndSkipsMetadataChanges() {
        val id = helper.addAsset(Asset(name = "Wallet", amount = 500.0))
        val asset = helper.getAllAssets().single()
        asset.amount = 600.0
        assertEquals(1, helper.updateAsset(asset))
        asset.amount = 400.0
        assertEquals(1, helper.updateAsset(asset))
        asset.name = "Renamed"
        asset.includeInTotal = false
        assertEquals(1, helper.updateAsset(asset))
        assertEquals(listOf(-20_000L, 10_000L, 50_000L), events(id).map { it.deltaMinor })
        assertEquals(60_000L, events(id).first().balanceBeforeMinor)
        assertEquals(400.0, helper.getAllAssets().single().amount, 0.0)
        assertTrue(helper.getAllRecords().isEmpty())
    }

    @Test fun billAddEditDeleteAndTransferDoNotProduceManualAdjustments() {
        val first = helper.addAsset(Asset(name = "First", amount = 500.0))
        val second = helper.addAsset(Asset(name = "Second", amount = 100.0))
        val id = helper.addRecord(Record(date = helper.getCurrentDate(), amount = 10.0,
            category = "餐饮", assetId = first, assetSource = "First"))
        val record = helper.getRecordById(id)!!
        assertEquals(1, helper.updateRecord(record.copy(amount = 20.0)))
        helper.deleteRecord(id)
        val transfer = helper.addRecord(Record(date = helper.getCurrentDate(), type = 2, amount = 30.0,
            assetId = first, destinationAssetId = second, fee = 0.5))
        assertTrue(transfer > 0)
        assertEquals(1, events(first).size)
        assertEquals(1, events(second).size)
        assertEquals(50L, helper.getTotalByTypeMinor(0))
        helper.deleteRecord(transfer)
        assertEquals(1, events(first).size)
        assertEquals(500.0, helper.getAllAssets().single { it.id == first }.amount, 0.0)
    }

    @Test fun eventInsertFailureRollsBackBothAssetCreationAndBalanceUpdate() {
        val id = helper.addAsset(Asset(name = "Wallet", amount = 500.0))
        helper.writableDatabase.execSQL("CREATE TRIGGER fail_balance_event BEFORE INSERT ON asset_balance_events " +
            "BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertEquals(-1L, helper.addAsset(Asset(name = "Must not persist", amount = 200.0)))
        val asset = helper.getAllAssets().single()
        assertEquals(0, helper.updateAsset(asset.copy(amount = 600.0)))
        assertEquals(500.0, helper.getAllAssets().single().amount, 0.0)
        assertEquals(1, events(id).size)
    }

    @Test fun mixedPagingHasNoGapsDuplicatesOrLedgerScopeLoss() {
        val id = helper.addAsset(Asset(name = "Wallet", amount = 500.0))
        val second = helper.addAsset(Asset(name = "Second", amount = 100.0))
        val today = helper.getCurrentDate()
        helper.addRecord(Record(date = today, type = 1, amount = 20.0, category = "工资", assetId = id))
        helper.addRecord(Record(date = "2020-01-01", amount = 1.0, category = "餐饮", assetId = id))
        val shared = helper.createLedgerInAssetGroup("Shared", helper.getMasterLedgerId())!!
        DatabaseHelper(context, shared).use { scoped ->
            scoped.addRecord(Record(date = today, amount = 10.0, category = "餐饮", assetId = id))
            scoped.addRecord(Record(date = today, type = 2, amount = 30.0,
                assetId = second, destinationAssetId = id))
            // Non-owners cannot adjust a shared asset through the metadata write path.
            assertEquals(0, scoped.updateAsset(scoped.getAllAssets().single { it.id == id }.copy(amount = 900.0)))
        }
        assertEquals(1, helper.updateAsset(helper.getAllAssets().single { it.id == id }.copy(amount = 600.0)))
        val complete = helper.getAssetHistoryPage(id, "Wallet").entries
        assertEquals(6, complete.size)
        val paged = mutableListOf<AssetHistoryEntry>()
        var cursor: DatabaseHelper.AssetHistoryCursor? = null
        do {
            val page = helper.getAssetHistoryPage(id, "Wallet", cursor, limit = 2)
            paged.addAll(page.entries)
            cursor = page.nextCursor
        } while (cursor != null)
        assertEquals(complete, paged)
        assertEquals(6, paged.map { when (it) {
            is AssetHistoryEntry.Bill -> "bill:${it.record.id}"
            is AssetHistoryEntry.Balance -> "balance:${it.event.id}"
        } }.distinct().size)
        DatabaseHelper(context, shared).use { assertEquals(complete, it.getAssetHistoryPage(id, "Wallet").entries) }
    }

    @Test fun legacyAssetHasNoInventedInitializationAndStartsRecordingItsNextAdjustment() {
        helper.writableDatabase.execSQL("INSERT INTO assets(id, name, amount, type, ledger_id) VALUES (900, 'Legacy', 50000, 0, ?)",
            arrayOf(helper.getMasterLedgerId()))
        // Recreating the additive v44 schema must not infer past dates or amounts.
        helper.writableDatabase.execSQL("DROP TABLE asset_balance_events")
        helper.writableDatabase.execSQL(AssetHistoryRepository.CREATE_TABLE_SQL)
        helper.writableDatabase.execSQL(AssetHistoryRepository.CREATE_INDEX_SQL)
        helper.close()
        helper = DatabaseHelper(context)
        assertTrue(events(900).isEmpty())
        assertEquals(500.0, helper.getAllAssets().single().amount, 0.0)
        assertEquals(1, helper.updateAsset(helper.getAllAssets().single().copy(amount = 600.0)))
        assertEquals(AssetBalanceEvent.ADJUSTMENT, events(900).single().kind)
        assertEquals(10_000L, events(900).single().deltaMinor)
    }

    @Test fun archiveKeepsHistoryAndDeletingAnUnusedAssetCascadesItsEvents() {
        val id = helper.addAsset(Asset(name = "Wallet", amount = 500.0))
        helper.archiveAsset(id)
        assertEquals(1, events(id).size)
        assertTrue(helper.deleteArchivedAsset(id))
        assertTrue(events(id).isEmpty())
    }

    @Test fun copiedAssetGetsItsOwnInitialBalanceInsteadOfTheSourceHistory() {
        val id = helper.addAsset(Asset(name = "Wallet", amount = 500.0))
        helper.updateAsset(helper.getAllAssets().single().copy(amount = 600.0))
        val target = helper.createLedgerInAssetGroup("Independent", null)!!
        val copy = helper.copyAssetToLedger(id, target)
        assertTrue(copy > 0 && copy != id)
        val event = events(copy).single()
        assertEquals(AssetBalanceEvent.INITIAL, event.kind)
        assertEquals(60_000L, event.deltaMinor)
        assertEquals(2, events(id).size)
    }

    private fun events(assetId: Long): List<AssetBalanceEvent> = helper.getAssetHistoryPage(assetId, null).entries
        .filterIsInstance<AssetHistoryEntry.Balance>().map { it.event }
}
