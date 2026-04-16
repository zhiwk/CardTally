package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Record
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperLatestRecordDayTest {

    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
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
    fun getLatestRecordDayRecords_returnsOnlyRecordsFromMostRecentRecordedDate() {
        databaseHelper.addRecord(
            Record(
                date = "2026-04-11",
                amount = 18.0,
                category = "早餐",
                type = EXPENSE_TYPE,
                sortOrder = 1
            )
        )
        databaseHelper.addRecord(
            Record(
                date = "2026-04-14",
                amount = 35.0,
                category = "午餐",
                type = EXPENSE_TYPE,
                sortOrder = 1
            )
        )
        databaseHelper.addRecord(
            Record(
                date = "2026-04-14",
                amount = 12.0,
                category = "咖啡",
                type = EXPENSE_TYPE,
                sortOrder = 2
            )
        )

        val records = databaseHelper.getLatestRecordDayRecords()

        assertEquals(2, records.size)
        assertEquals(listOf("2026-04-14", "2026-04-14"), records.map { it.date })
        assertEquals(listOf("午餐", "咖啡"), records.map { it.category })
    }

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val EXPENSE_TYPE = 0
    }
}
