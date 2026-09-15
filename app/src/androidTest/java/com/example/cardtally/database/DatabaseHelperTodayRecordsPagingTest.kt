package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.model.Record
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class DatabaseHelperTodayRecordsPagingTest {

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
    fun getTodayRecordsPage_returnsTodayAndExcludesYesterday() {
        databaseHelper.addRecord(record(TODAY, "today-first"))
        databaseHelper.addRecord(record(YESTERDAY, "yesterday"))
        databaseHelper.addRecord(record(TODAY, "today-second"))

        val page = databaseHelper.getTodayRecordsPage(TODAY)

        assertEquals(listOf("today-first", "today-second"), page.records.map { it.category })
        assertTrue(page.records.all { it.date == TODAY })
        assertNull(page.nextCursor)
    }

    @Test
    fun getTodayRecordsPage_returnsEmptyWhenOnlyHistoricalRecordsExist() {
        databaseHelper.addRecord(record(YESTERDAY, "historical"))

        val page = databaseHelper.getTodayRecordsPage(TODAY)

        assertTrue(page.records.isEmpty())
        assertNull(page.nextCursor)
    }

    @Test
    fun getTodayRecordsPage_returnsEmptyWhenLedgerHasNoRecords() {
        val page = databaseHelper.getTodayRecordsPage(TODAY)

        assertTrue(page.records.isEmpty())
        assertNull(page.nextCursor)
    }

    @Test
    fun getTodayRecordsPage_reachesAllRowsAcrossBoundedPagesWithoutDuplicates() {
        val todayRecordIds = (0 until TOTAL_TODAY_RECORDS).map { index ->
            databaseHelper.addRecord(record(TODAY, "today-$index"))
        }
        databaseHelper.addRecord(record(YESTERDAY, "historical"))
        todayRecordIds.forEach { recordId -> databaseHelper.updateRecordSortOrder(recordId, 1) }

        val pages = mutableListOf<List<Record>>()
        var cursor: DatabaseHelper.RecordPageCursor? = null
        do {
            val page = databaseHelper.getTodayRecordsPage(TODAY, cursor)
            pages += page.records
            cursor = page.nextCursor
        } while (cursor != null)

        val records = pages.flatten()
        assertTrue(pages.all { it.size <= DatabaseHelper.MAX_RECORD_QUERY_LIMIT })
        assertEquals(TOTAL_TODAY_RECORDS, records.size)
        assertEquals(TOTAL_TODAY_RECORDS, records.map { it.id }.distinct().size)
        assertEquals((0 until TOTAL_TODAY_RECORDS).map { "today-$it" }, records.map { it.category })
    }

    @Test
    fun getCurrentDate_usesDeterministicStoredIsoDateFormat() {
        val originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val today = try {
            databaseHelper.getCurrentDate(NOW_MILLIS)
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }

        assertEquals(TODAY, today)
    }

    private fun record(date: String, category: String): Record {
        return Record(date = date, amount = 1.0, category = category, type = EXPENSE_TYPE)
    }

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val EXPENSE_TYPE = 0
        private const val TOTAL_TODAY_RECORDS = 205
        private const val TODAY = "2026-08-15"
        private const val YESTERDAY = "2026-08-14"
        private const val NOW_MILLIS = 1786752000000L
    }
}
