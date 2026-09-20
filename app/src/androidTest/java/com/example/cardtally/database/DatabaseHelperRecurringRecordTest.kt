package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Asset
import com.example.cardtally.model.RecurringRecord
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.util.Money
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperRecurringRecordTest {
    private lateinit var database: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
        database = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun processingDueTemplate_createsMissedDatesOnceAndAdvancesNextDate() {
        val assetId = database.addAsset(Asset(name = "Wallet", amount = 1_000.0))
        val category = database.getCategoryTreeByType(0).first { it.parentId != null && database.getCategoryTreeByType(0).none { child -> child.parentId == it.id } }
        database.saveRecurringRecord(
            RecurringRecord(
                type = 0,
                name = "Daily coffee",
                amountMinor = Money.toMinor(10.0)!!,
                categoryId = category.id,
                categoryName = category.name,
                assetId = assetId,
                assetSource = "Wallet",
                frequency = RecurringRecord.DAILY,
                startDate = "2026-09-15",
                nextDueDate = "2026-09-15"
            )
        )

        assertEquals(3, database.processDueRecurringRecords("2026-09-17"))
        assertEquals(0, database.processDueRecurringRecords("2026-09-17"))
        assertEquals(3, database.getAllRecords().size)
        assertEquals("2026-09-18", database.getRecurringRecords().single().nextDueDate)
    }

    @Test
    fun monthly31AndYearlyLeapDay_skipDatesThatDoNotExist() {
        val category = database.getCategoryTreeByType(0).first { it.parentId != null && database.getCategoryTreeByType(0).none { child -> child.parentId == it.id } }
        database.saveRecurringRecord(
            RecurringRecord(
                type = 0,
                name = "Month end day 31",
                amountMinor = 100,
                categoryId = category.id,
                categoryName = category.name,
                frequency = RecurringRecord.MONTHLY,
                monthlyDay = 31,
                startDate = "2026-01-31",
                nextDueDate = "2026-01-31"
            )
        )
        database.saveRecurringRecord(
            RecurringRecord(
                type = 0,
                name = "Leap day",
                amountMinor = 100,
                categoryId = category.id,
                categoryName = category.name,
                frequency = RecurringRecord.YEARLY,
                yearlyMonth = 2,
                yearlyDay = 29,
                startDate = "2028-02-29",
                nextDueDate = "2028-02-29"
            )
        )

        assertEquals(2, database.processDueRecurringRecords("2026-04-01"))
        assertEquals("2026-05-31", database.getRecurringRecords().first { it.name == "Month end day 31" }.nextDueDate)
        database.processDueRecurringRecords("2030-03-01")
        val records = database.getRecurringRecords()
        assertEquals("2030-03-31", records.first { it.name == "Month end day 31" }.nextDueDate)
        assertEquals("2032-02-29", records.first { it.name == "Leap day" }.nextDueDate)
    }

    @Test
    fun repairInvalidMonthEndDueDate_movesToTheCurrentMonthEnd() {
        val category = database.getCategoryTreeByType(0).first { it.parentId != null && database.getCategoryTreeByType(0).none { child -> child.parentId == it.id } }
        database.saveRecurringRecord(
            RecurringRecord(
                type = 0,
                name = "Stale month end",
                amountMinor = 100,
                categoryId = category.id,
                categoryName = category.name,
                frequency = RecurringRecord.MONTHLY,
                monthlyDay = 0,
                startDate = "2026-09-01",
                nextDueDate = "2026-09-21"
            )
        )

        assertEquals(1, database.repairInvalidRecurringNextDueDates())
        assertEquals("2026-09-30", database.getRecurringRecords().single().nextDueDate)
    }

    @Test
    fun transferTemplate_generatesRecordWithBothAssets() {
        val sourceId = database.addAsset(Asset(name = "Source", amount = 100.0))
        val destinationId = database.addAsset(Asset(name = "Destination", amount = 0.0))
        database.saveRecurringRecord(
            RecurringRecord(
                type = 2,
                name = "Move money",
                amountMinor = 1_000,
                assetId = sourceId,
                assetSource = "Source",
                destinationAssetId = destinationId,
                destinationAssetSource = "Destination",
                frequency = RecurringRecord.DAILY,
                startDate = "2026-09-19",
                nextDueDate = "2026-09-19"
            )
        )

        assertEquals(1, database.processDueRecurringRecords("2026-09-19"))
        val record = database.getAllRecords().single()
        assertEquals(2, record.type)
        assertEquals(sourceId, record.assetId)
        assertEquals(destinationId, record.destinationAssetId)
    }

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
    }
}
