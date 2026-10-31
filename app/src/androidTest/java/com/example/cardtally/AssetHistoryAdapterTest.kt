package com.example.cardtally

import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.adapter.AssetHistoryAdapter
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.model.AssetBalanceEvent
import com.example.cardtally.model.AssetHistoryEntry
import com.example.cardtally.model.Record
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetHistoryAdapterTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun balanceRowsShowSignedDeltasAndAreReadOnlyIncludingAfterRebinding() {
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_CardTally_Light)
            val parent = FrameLayout(context)
            val adapter = AssetHistoryAdapter(listener { fail("A balance event must not open a bill") })
            val initial = balance(AssetBalanceEvent.INITIAL, 0, 50_000)
            val adjustment = balance(AssetBalanceEvent.ADJUSTMENT, 50_000, 60_000)
            adapter.submit(listOf(adjustment, initial), emptyMap(), emptyMap(), emptyMap())
            val row = adapter.onCreateViewHolder(parent, adapter.getItemViewType(1))
            adapter.onBindViewHolder(row, 1)
            assertEquals(context.getString(R.string.asset_balance_adjustment), row.itemView.findViewById<TextView>(R.id.text_category).text)
            assertEquals("+¥100.00", row.itemView.findViewById<TextView>(R.id.text_amount).text)
            assertEquals(context.getString(R.string.asset_balance_change, "500.00", "600.00"),
                row.itemView.findViewById<TextView>(R.id.text_description).text)
            assertFalse(row.itemView.findViewById<View>(R.id.card_content).isClickable)
            assertEquals(View.GONE, row.itemView.findViewById<View>(R.id.layout_actions).visibility)
            adapter.onBindViewHolder(row, 2)
            assertEquals(context.getString(R.string.asset_balance_initial), row.itemView.findViewById<TextView>(R.id.text_category).text)
            assertEquals("+¥500.00", row.itemView.findViewById<TextView>(R.id.text_amount).text)
            adapter.submit(listOf(balance(AssetBalanceEvent.ADJUSTMENT, 60_000, 40_000)), emptyMap(), emptyMap(), emptyMap())
            adapter.onBindViewHolder(row, 1)
            assertEquals("-¥200.00", row.itemView.findViewById<TextView>(R.id.text_amount).text)
        }
    }

    @Test fun mixedHistoryKeepsBillNavigationAndExcludesBalanceEventsFromDailyTotals() {
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_CardTally_Light)
            val parent = FrameLayout(context)
            var opened = 0L
            val adapter = AssetHistoryAdapter(listener { opened = it.id })
            adapter.submit(listOf(balance(AssetBalanceEvent.ADJUSTMENT, 50_000, 60_000),
                AssetHistoryEntry.Bill(Record(id = 42, date = "2026-11-01", category = "餐饮", amount = 20.0,
                    ledgerName = "Fixture ledger"))), emptyMap(), emptyMap(), emptyMap())
            val header = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0))
            adapter.onBindViewHolder(header, 0)
            assertEquals(context.getString(R.string.ledger_day_income_expense, 0.0, 20.0),
                header.itemView.findViewById<TextView>(R.id.text_date_total).text)
            val bill = adapter.onCreateViewHolder(parent, adapter.getItemViewType(2))
            adapter.onBindViewHolder(bill, 2)
            assertEquals("Fixture ledger", bill.itemView.findViewById<TextView>(R.id.text_ledger).text)
            bill.itemView.findViewById<View>(R.id.card_content).performClick()
            assertEquals(42L, opened)
        }
    }

    private fun balance(kind: Int, before: Long, after: Long) = AssetHistoryEntry.Balance(AssetBalanceEvent(
        assetId = 1, kind = kind, date = "2026-11-01", createdAt = 1,
        balanceBeforeMinor = before, balanceAfterMinor = after))

    private fun listener(open: (Record) -> Unit) = object : DateGroupAdapter.OnRecordActionListener {
        override fun onOpenDetails(record: Record) = open(record)
        override fun onEdit(record: Record) = Unit
        override fun onDelete(record: Record) = Unit
        override fun onMultiSelectChanged(selectedCount: Int) = Unit
        override fun onDeleteSelected(records: List<Record>) = Unit
        override fun onEnterMultiSelectMode(record: Record) = Unit
        override fun onToggleMultiSelect(record: Record) = Unit
    }
}
