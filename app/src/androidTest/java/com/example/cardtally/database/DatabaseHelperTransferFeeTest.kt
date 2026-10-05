package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Record
import com.example.cardtally.testing.IsolatedTestGuard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperTransferFeeTest {

    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
        databaseHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseHelper.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun transferWithFee_debitsSourceByAmountPlusFeeAndCreditsDestinationByAmount() {
        val sourceId = databaseHelper.addAsset(Asset(name = "Source", amount = 1_000.0))
        val destinationId = databaseHelper.addAsset(Asset(name = "Destination", amount = 500.0))

        val recordId = databaseHelper.addRecord(
            Record(
                date = "2026-08-15",
                amount = 100.0,
                category = "资产转资产",
                type = TRANSFER_TYPE,
                fee = 5.0,
                assetId = sourceId,
                destinationAssetId = destinationId,
                assetSource = "Source",
                destinationAssetSource = "Destination"
            )
        )

        assertEquals(895.0, activeAssetAmount(sourceId), 0.0)
        assertEquals(600.0, activeAssetAmount(destinationId), 0.0)
        val stored = databaseHelper.getRecordById(recordId)
        assertNotNull(stored)
        assertEquals(5.0, stored!!.fee, 0.0)
    }

    @Test
    fun deletingTransfer_reversesAmountAndFeeExactly() {
        val sourceId = databaseHelper.addAsset(Asset(name = "Source", amount = 1_000.0))
        val destinationId = databaseHelper.addAsset(Asset(name = "Destination", amount = 500.0))
        val recordId = databaseHelper.addRecord(
            Record(
                date = "2026-08-15",
                amount = 100.0,
                category = "资产转资产",
                type = TRANSFER_TYPE,
                fee = 5.0,
                assetId = sourceId,
                destinationAssetId = destinationId,
                assetSource = "Source",
                destinationAssetSource = "Destination"
            )
        )

        databaseHelper.deleteRecord(recordId)

        assertEquals(1_000.0, activeAssetAmount(sourceId), 0.0)
        assertEquals(500.0, activeAssetAmount(destinationId), 0.0)
    }

    @Test
    fun expenseAndIncome_neverPersistFee() {
        val assetId = databaseHelper.addAsset(Asset(name = "Wallet", amount = 1_000.0))
        val expenseId = databaseHelper.addRecord(
            Record(date = "2026-08-15", amount = 10.0, category = "Food", type = EXPENSE_TYPE, fee = 9.0, assetId = assetId)
        )
        val incomeId = databaseHelper.addRecord(
            Record(date = "2026-08-15", amount = 10.0, category = "Salary", type = INCOME_TYPE, fee = 9.0, assetId = assetId)
        )

        assertEquals(0.0, databaseHelper.getRecordById(expenseId)!!.fee, 0.0)
        assertEquals(0.0, databaseHelper.getRecordById(incomeId)!!.fee, 0.0)
    }

    @Test
    fun expenseTotals_includeTransferFeesButCategoryStatsDoNot() {
        val sourceId = databaseHelper.addAsset(Asset(name = "Source", amount = 1_000.0))
        val destinationId = databaseHelper.addAsset(Asset(name = "Destination", amount = 500.0))
        databaseHelper.addRecord(
            Record(date = "2026-08-15", amount = 10.0, category = "Food", type = EXPENSE_TYPE, assetId = sourceId)
        )
        databaseHelper.addRecord(
            Record(
                date = "2026-08-15",
                amount = 100.0,
                category = "资产转资产",
                type = TRANSFER_TYPE,
                fee = 5.0,
                assetId = sourceId,
                destinationAssetId = destinationId,
                assetSource = "Source",
                destinationAssetSource = "Destination"
            )
        )

        assertEquals("expense total must add the transfer fee", 15.0, databaseHelper.getTotalByType(EXPENSE_TYPE), 0.0)
        assertEquals(
            "range expense total must add the transfer fee",
            15.0,
            databaseHelper.getTotalByTypeAndDateRange(EXPENSE_TYPE, "2026-08-01", "2026-08-31"),
            0.0
        )
        assertEquals(
            "income total is unaffected",
            0.0,
            databaseHelper.getTotalByType(INCOME_TYPE),
            0.0
        )
        assertEquals(
            "transfer fee has no category and must not appear in category stats",
            mapOf("Food" to 10.0),
            databaseHelper.getCategoryStatistics(EXPENSE_TYPE)
        )
        val monthly = databaseHelper.getMonthlyStatistics(EXPENSE_TYPE, 2026)
        assertEquals(15.0, monthly["2026-08"] ?: 0.0, 0.0)
        val daily = databaseHelper.getDailyTotals("2026-08-15", "2026-08-15")["2026-08-15"]
        assertEquals(1500L, daily?.expenseMinor)
        assertEquals(1500L, databaseHelper.getTrendByDate(
            EXPENSE_TYPE, "2026-08-15", "2026-08-15", byMonth = false
        )["2026-08-15"])
        assertEquals(500L, databaseHelper.getSearchTotals(null, null, "Source", null).expenseMinor)
    }

    private fun activeAssetAmount(assetId: Long): Double {
        return databaseHelper.getAllAssets().first { it.id == assetId }.amount
    }

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val EXPENSE_TYPE = 0
        private const val INCOME_TYPE = 1
        private const val TRANSFER_TYPE = 2
    }
}
