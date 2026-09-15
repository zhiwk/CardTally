package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.model.Record
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperLedgerAggregationContractTest {

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
    fun allPeriod_keepsParentAndChildExpenseStatisticsFlat() {
        addRecord(EARLIER_DATE, 90.0, "Food", EXPENSE_TYPE)
        addRecord(CUSTOM_DATE, 20.0, "Food/Breakfast", EXPENSE_TYPE)
        addRecord(CUSTOM_DATE, 30.0, "Food/Lunch", EXPENSE_TYPE)
        addRecord(CUSTOM_DATE, 500.0, "Salary", INCOME_TYPE)

        val statistics = databaseHelper.getCategoryStatistics(EXPENSE_TYPE)
        val total = databaseHelper.getTotalByType(EXPENSE_TYPE)

        assertEquals(140.0, total, 0.0)
        assertEquals(90.0, statistics["Food"] ?: 0.0, 0.0)
        assertEquals(20.0, statistics["Food/Breakfast"] ?: 0.0, 0.0)
        assertEquals(30.0, statistics["Food/Lunch"] ?: 0.0, 0.0)
        assertFalse(statistics.values.any { it == 140.0 })
    }

    @Test
    fun allPeriod_separatesIncomeFromExpenseTotals() {
        addRecord(CUSTOM_DATE, 20.0, "Food/Breakfast", EXPENSE_TYPE)
        addRecord(CUSTOM_DATE, 500.0, "Salary", INCOME_TYPE)
        addRecord(CUSTOM_DATE, 120.0, "Bonus", INCOME_TYPE)

        val statistics = databaseHelper.getCategoryStatistics(INCOME_TYPE)
        val total = databaseHelper.getTotalByType(INCOME_TYPE)

        assertEquals(620.0, total, 0.0)
        assertEquals(setOf("Salary", "Bonus"), statistics.keys)
    }

    @Test
    fun customPeriod_filtersExpenseAndIncomeUsingInclusiveIsoBoundaries() {
        addRecord(EARLIER_DATE, 90.0, "Historical expense", EXPENSE_TYPE)
        addRecord(CUSTOM_DATE, 20.0, "Custom expense", EXPENSE_TYPE)
        addRecord(CUSTOM_DATE, 500.0, "Custom income", INCOME_TYPE)
        addRecord(LATER_DATE, 120.0, "Later income", INCOME_TYPE)

        val expenseStatistics = databaseHelper.getCategoryStatisticsByDateRange(
            EXPENSE_TYPE,
            CUSTOM_DATE,
            CUSTOM_DATE
        )
        val incomeStatistics = databaseHelper.getCategoryStatisticsByDateRange(
            INCOME_TYPE,
            CUSTOM_DATE,
            CUSTOM_DATE
        )
        val expenseTotal = databaseHelper.getTotalByTypeAndDateRange(EXPENSE_TYPE, CUSTOM_DATE, CUSTOM_DATE)
        val incomeTotal = databaseHelper.getTotalByTypeAndDateRange(INCOME_TYPE, CUSTOM_DATE, CUSTOM_DATE)

        assertEquals(mapOf("Custom expense" to 20.0), expenseStatistics)
        assertEquals(mapOf("Custom income" to 500.0), incomeStatistics)
        assertEquals(20.0, expenseTotal, 0.0)
        assertEquals(500.0, incomeTotal, 0.0)
    }

    private fun addRecord(date: String, amount: Double, category: String, type: Int) {
        databaseHelper.addRecord(Record(date = date, amount = amount, category = category, type = type))
    }

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val EXPENSE_TYPE = 0
        private const val INCOME_TYPE = 1
        private const val EARLIER_DATE = "2026-07-31"
        private const val CUSTOM_DATE = "2026-08-15"
        private const val LATER_DATE = "2026-08-16"
    }
}
