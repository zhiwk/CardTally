package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.adapter.LedgerDateGroupAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.IncomeExpenseColorScheme
import com.example.cardtally.util.LedgerMonthIndex
import com.example.cardtally.util.Money
import com.example.cardtally.util.ScrollTopFabHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LedgerFragment : Fragment() {
    private lateinit var databaseHelper: DatabaseHelper
    private var pager: ViewPager2? = null
    private var pages: MonthAdapter? = null
    private var fabScrollTop: FloatingActionButton? = null
    private var currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
    private var ledgerId: Long? = null
    private val states = mutableMapOf<Int, MonthState>()
    private var categoryIcons = emptyMap<Long, String>()

    private class MonthState {
        val records = mutableListOf<Record>()
        var cursor: DatabaseHelper.RecordListCursor? = null
        var loaded = false
        var income = 0L
        var expense = 0L
        var scroll: Parcelable? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentMonth = savedInstanceState?.getString("ledger_month") ?: currentMonth
        ledgerId = savedInstanceState?.getLong("ledger_id")?.takeIf { it > 0 }
        savedInstanceState?.getParcelable<Parcelable>("ledger_scroll")?.let {
            states[LedgerMonthIndex.position(currentMonth)] = MonthState().apply { scroll = it }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saveScrollPositions()
        outState.putString("ledger_month", currentMonth)
        ledgerId?.let { outState.putLong("ledger_id", it) }
        outState.putParcelable("ledger_scroll", states[LedgerMonthIndex.position(currentMonth)]?.scroll)
        outState.putInt("ledger_loaded_count", states[LedgerMonthIndex.position(currentMonth)]?.records?.size ?: 0)
        super.onSaveInstanceState(outState)
    }

    private var restoredRecordCount = 0
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_ledger, container, false)
        databaseHelper = DatabaseHelper(requireContext())
        restoredRecordCount = savedInstanceState?.getInt("ledger_loaded_count") ?: 0
        pager = root.findViewById<ViewPager2>(R.id.pager_months).apply {
            // Explicit LTR keeps chronological swipe direction independent of locale.
            offscreenPageLimit = 1
            pages = MonthAdapter()
            adapter = pages
            // Detached pages must release their data rather than retaining a second month cache.
            (getChildAt(0) as RecyclerView).setItemViewCacheSize(0)
            setCurrentItem(LedgerMonthIndex.position(currentMonth), false)
            registerOnPageChangeCallback(pageCallback)
        }
        root.findViewById<View>(R.id.button_ledger_switch).setOnClickListener {
            open(LedgerManagementFragment.newLedgerSelectorInstance())
        }
        root.findViewById<View>(R.id.button_ledger_search).setOnClickListener { open(SearchFragment()) }
        root.findViewById<View>(R.id.button_ledger_calendar).setOnClickListener {
            open(CalendarFragment.newInstance(currentMonth))
        }
        root.findViewById<View>(R.id.fab_add).setOnClickListener { open(AddRecordFragment()) }
        fabScrollTop = root.findViewById<FloatingActionButton>(R.id.fab_scroll_top).apply {
            setOnClickListener { activeHolder()?.list?.smoothScrollToPosition(0) }
        }
        return root
    }

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentMonth = LedgerMonthIndex.month(position)
            pager?.post {
                if (view != null && pager?.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                    trimMonthStates()
                    updateScrollTopFab()
                }
            }
            updateScrollTopFab()
        }
        override fun onPageScrollStateChanged(state: Int) {
            if (state == ViewPager2.SCROLL_STATE_IDLE) {
                trimMonthStates()
                updateScrollTopFab()
            } else fabScrollTop?.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        saveScrollPositions()
        val selectedLedger = databaseHelper.getCurrentLedger()?.id
        if (ledgerId != selectedLedger) {
            states.clear()
            pages?.holders?.forEach { it.monthPosition = -1 }
            restoredRecordCount = 0
        }
        ledgerId = selectedLedger
        view?.findViewById<TextView>(R.id.text_month)?.text =
            databaseHelper.getCurrentLedger()?.name ?: getString(R.string.ledger_book_name)
        categoryIcons = databaseHelper.getAllCategories().filter { !it.icon.isNullOrEmpty() }
            .associate { it.id to it.icon.orEmpty() }
        states.values.forEach { it.loaded = false }
        pages?.notifyDataSetChanged()
        pager?.post { if (view != null) updateScrollTopFab() }
    }

    private fun open(fragment: Fragment) {
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .addToBackStack(null).commit()
    }

    private val recordListener = object : DateGroupAdapter.OnRecordActionListener {
        override fun onOpenDetails(record: Record) = open(RecordDetailFragment.newInstance(record.id))
        override fun onEdit(record: Record) = open(EditRecordFragment.newInstance(record.id))
        override fun onDelete(record: Record) {
            AlertDialog.Builder(requireContext()).setTitle(R.string.delete_record_title)
                .setMessage(R.string.delete_record_message).setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                    databaseHelper.deleteRecord(record.id)
                    Toast.makeText(requireContext(), R.string.toast_delete_success, Toast.LENGTH_SHORT).show()
                    states.values.forEach { it.loaded = false }
                    pages?.notifyDataSetChanged()
                }.show()
        }
        override fun onMultiSelectChanged(selectedCount: Int) = Unit
        override fun onDeleteSelected(records: List<Record>) = Unit
        override fun onEnterMultiSelectMode(record: Record) = Unit
        override fun onToggleMultiSelect(record: Record) = Unit
    }

    private inner class MonthAdapter : RecyclerView.Adapter<MonthHolder>() {
        val holders = mutableSetOf<MonthHolder>()
        init { setHasStableIds(true) }
        override fun getItemCount() = LedgerMonthIndex.COUNT
        override fun getItemId(position: Int) = position.toLong()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MonthHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_ledger_month_page, parent, false)
        ).also { holders.add(it) }
        override fun onBindViewHolder(holder: MonthHolder, position: Int) {
            holders.add(holder)
            holder.bind(position)
        }
        override fun onViewRecycled(holder: MonthHolder) {
            holder.saveScroll()
            holder.release()
            holders.remove(holder)
        }
    }

    private inner class MonthHolder(root: View) : RecyclerView.ViewHolder(root) {
        val list: RecyclerView = root.findViewById(R.id.recycler_records)
        private val empty: TextView = root.findViewById(R.id.text_empty)
        var monthPosition = -1
        private lateinit var rows: LedgerDateGroupAdapter
        private var restoring = false
        init {
            list.layoutManager = LinearLayoutManager(root.context)
            list.itemAnimator = null
            list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (monthPosition < 0 || restoring) return
                    if (monthPosition == LedgerMonthIndex.position(currentMonth)) updateScrollTopFab()
                    val manager = list.layoutManager as LinearLayoutManager
                    if (dy > 0 && manager.findLastVisibleItemPosition() >= rows.itemCount - 5) {
                        val state = states[monthPosition] ?: return
                        val cursor = state.cursor ?: return
                        val range = monthRange(LedgerMonthIndex.month(monthPosition))
                        append(state, databaseHelper.getRecordsPage(range.first, range.second, after = cursor))
                        updateRows(state)
                    }
                }
            })
        }
        fun saveScroll() {
            if (monthPosition >= 0 && list.isLaidOut) states[monthPosition]?.scroll = list.layoutManager?.onSaveInstanceState()
        }
        fun release() {
            list.stopScroll()
            monthPosition = -1
            list.adapter = null
            rows = LedgerDateGroupAdapter(emptyList(), recordListener, {}, emptyMap())
        }
        fun bind(newPosition: Int) {
            saveScroll()
            monthPosition = newPosition
            val state = states.getOrPut(monthPosition) { MonthState() }
            val month = LedgerMonthIndex.month(monthPosition)
            if (!state.loaded) {
                val count = maxOf(state.records.size, if (month == currentMonth) restoredRecordCount else 0)
                val range = monthRange(month)
                state.income = databaseHelper.getTotalByTypeAndDateRangeMinor(1, range.first, range.second)
                state.expense = databaseHelper.getTotalByTypeAndDateRangeMinor(0, range.first, range.second)
                state.records.clear()
                append(state, databaseHelper.getRecordsPage(range.first, range.second))
                while (state.records.size < count) {
                    val cursor = state.cursor ?: break
                    append(state, databaseHelper.getRecordsPage(range.first, range.second, after = cursor))
                }
                state.loaded = true
                if (month == currentMonth) restoredRecordCount = 0
            }
            rows = LedgerDateGroupAdapter(emptyList(), recordListener, { header ->
                bindSummary(header, month, state)
            }, categoryIcons)
            restoring = true
            list.adapter = rows
            updateRows(state)
            state.scroll?.let { list.layoutManager?.onRestoreInstanceState(it) }
                ?: list.scrollToPosition(0)
            restoring = false
            list.post { if (monthPosition == LedgerMonthIndex.position(currentMonth)) updateScrollTopFab() }
        }
        private fun updateRows(state: MonthState) {
            val groups = state.records.groupBy { it.date }.toSortedMap(compareByDescending { it })
                .map { (date, records) -> DateGroup(date, records.sortedByDescending { it.sortOrder }) }
            rows.updateDateGroups(groups)
            empty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun append(state: MonthState, page: DatabaseHelper.RecordListPage) {
        state.records.addAll(page.records)
        state.cursor = page.nextCursor
    }
    private fun trimMonthStates() {
        saveScrollPositions()
        val position = LedgerMonthIndex.position(currentMonth)
        states.keys.removeAll { kotlin.math.abs(it - position) > 1 }
    }
    private fun saveScrollPositions() { pages?.holders?.forEach { it.saveScroll() } }
    private fun activeHolder() = pages?.holders?.firstOrNull { it.monthPosition == LedgerMonthIndex.position(currentMonth) }
    private fun updateScrollTopFab() {
        fabScrollTop?.visibility = if (isAdded && ScrollTopFabHelper.isEnabled(requireContext()) &&
            pager?.scrollState == ViewPager2.SCROLL_STATE_IDLE && activeHolder()?.list?.canScrollVertically(-1) == true)
            View.VISIBLE else View.GONE
    }

    private fun bindSummary(header: View, month: String, state: MonthState) {
        header.findViewById<TextView>(R.id.text_month_summary_title).text = monthSummaryTitle(month)
        header.findViewById<TextView>(R.id.text_month_income).apply {
            text = "¥${Money.formatYuan(state.income)}"; isSelected = true
            setTextColor(IncomeExpenseColorScheme.incomePrimary(context))
        }
        header.findViewById<TextView>(R.id.text_month_expense).apply {
            text = "¥${Money.formatYuan(state.expense)}"; isSelected = true
            setTextColor(IncomeExpenseColorScheme.expensePrimary(context))
        }
        header.findViewById<TextView>(R.id.text_month_balance).apply {
            text = "¥${Money.formatYuan(state.income - state.expense)}"; isSelected = true
        }
        header.findViewById<View>(R.id.card_month_summary).setOnClickListener { showMonthPicker() }
        header.findViewById<ImageButton>(R.id.button_month_previous).apply {
            isEnabled = LedgerMonthIndex.position(month) > 0
            alpha = if (isEnabled) 1f else 0.4f
            setOnClickListener { switchMonth(-1) }
        }
        header.findViewById<ImageButton>(R.id.button_month_next).apply {
            isEnabled = LedgerMonthIndex.position(month) < LedgerMonthIndex.COUNT - 1
            alpha = if (isEnabled) 1f else 0.4f
            setOnClickListener { switchMonth(1) }
        }
    }
    private fun switchMonth(offset: Int) {
        pager?.let { it.setCurrentItem((it.currentItem + offset).coerceIn(0, LedgerMonthIndex.COUNT - 1), true) }
    }
    private fun showMonthPicker() {
        val selected = LedgerMonthIndex.position(currentMonth)
        val picker = layoutInflater.inflate(R.layout.dialog_month_picker, null)
        val year = picker.findViewById<NumberPicker>(R.id.picker_year).apply {
            minValue = 1
            maxValue = 9999
            value = selected / 12 + 1
            wrapSelectorWheel = false
        }
        val month = picker.findViewById<NumberPicker>(R.id.picker_month).apply {
            minValue = 1; maxValue = 12; value = selected % 12 + 1; wrapSelectorWheel = true
        }
        AlertDialog.Builder(requireContext()).setView(picker).setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                pager?.setCurrentItem((year.value - 1) * 12 + month.value - 1, false)
            }.show()
    }
    private fun monthRange(month: String): Pair<String, String> {
        val year = month.substring(0, 4).toInt()
        val number = month.substring(5, 7).toInt()
        val days = when (number) {
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return "$month-01" to String.format(Locale.US, "%s-%02d", month, days)
    }
    private fun monthSummaryTitle(month: String): String {
        val year = month.substring(0, 4).toInt()
        val number = month.substring(5, 7).toInt()
        val now = Calendar.getInstance()
        return when {
            year == now.get(Calendar.YEAR) && number == now.get(Calendar.MONTH) + 1 -> getString(R.string.ledger_month_summary)
            year == now.get(Calendar.YEAR) -> getString(R.string.ledger_month_summary_same_year, number)
            else -> getString(R.string.ledger_month_summary_other_year, year, number)
        }
    }
    override fun onDestroyView() {
        saveScrollPositions()
        pages?.holders?.forEach { it.list.stopScroll() }
        pager?.unregisterOnPageChangeCallback(pageCallback)
        pager?.adapter = null
        pager = null
        pages = null
        fabScrollTop = null
        databaseHelper.close()
        super.onDestroyView()
    }
}
