package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
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
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.adapter.LedgerDateGroupAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.FloatingNavLayoutHelper
import com.example.cardtally.util.ScrollTopFabHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LedgerFragment : Fragment() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var textMonth: TextView
    private lateinit var textIncome: TextView
    private lateinit var textExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var textMonthSummaryTitle: TextView
    private lateinit var textEmpty: TextView
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var recordsAdapter: LedgerDateGroupAdapter
    private lateinit var fabScrollTop: FloatingActionButton
    private var currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ledger, container, false)
        databaseHelper = DatabaseHelper(requireContext())
        textEmpty = view.findViewById(R.id.text_empty)
        recyclerRecords = view.findViewById(R.id.recycler_records)
        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())
        recordsAdapter = LedgerDateGroupAdapter(emptyList(), object : DateGroupAdapter.OnRecordActionListener {
            override fun onEdit(record: Record) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, EditRecordFragment.newInstance(record.id))
                    .addToBackStack(null)
                    .commit()
            }

            override fun onDelete(record: Record) = showDeleteDialog(record)
            override fun onMultiSelectChanged(selectedCount: Int) = Unit
            override fun onDeleteSelected(records: List<Record>) = Unit
            override fun onEnterMultiSelectMode(record: Record) = Unit
            override fun onToggleMultiSelect(record: Record) = Unit
        }, ::bindHeader)
        recyclerRecords.adapter = recordsAdapter
        recyclerRecords.post { if (isAdded) render() }
        fabScrollTop = view.findViewById(R.id.fab_scroll_top)
        fabScrollTop.setOnClickListener { recyclerRecords.smoothScrollToPosition(0) }
        recyclerRecords.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollTopFab(recyclerView.canScrollVertically(-1))
            }
        })
        view.findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddRecordFragment())
                .addToBackStack(null)
                .commit()
        }
        view.findViewById<FloatingActionButton>(R.id.fab_add).let { fab ->
            view.findViewById<View>(R.id.ledger_content).post {
                FloatingNavLayoutHelper.applyFabGapAboveBottomNav(
                    fab,
                    requireActivity().findViewById(R.id.nav_shell)
                )
            }
        }
        val addFab = view.findViewById<FloatingActionButton>(R.id.fab_add)
        requireActivity().findViewById<View>(R.id.nav_shell).post {
            val addParams = addFab.layoutParams as? ViewGroup.MarginLayoutParams
            val scrollParams = fabScrollTop.layoutParams as? ViewGroup.MarginLayoutParams
            if (addParams != null && scrollParams != null) {
                scrollParams.bottomMargin = addParams.bottomMargin + addFab.height + 16.dp
                fabScrollTop.layoutParams = scrollParams
            }
        }
        updateScrollTopFab(false)
        return view
    }

    override fun onResume() {
        super.onResume()
        render()
        if (::recyclerRecords.isInitialized) updateScrollTopFab(recyclerRecords.canScrollVertically(-1))
    }

    private fun updateScrollTopFab(canScrollUp: Boolean) {
        if (!::fabScrollTop.isInitialized) return
        val shouldShow = ScrollTopFabHelper.isEnabled(requireContext()) && canScrollUp
        if (shouldShow && fabScrollTop.visibility != View.VISIBLE) {
            fabScrollTop.bringToFront()
            fabScrollTop.alpha = 0f
            fabScrollTop.scaleX = 0.8f
            fabScrollTop.scaleY = 0.8f
            fabScrollTop.visibility = View.VISIBLE
            fabScrollTop.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160L).start()
        } else if (shouldShow) {
            fabScrollTop.bringToFront()
        } else if (!shouldShow && fabScrollTop.visibility == View.VISIBLE) {
            fabScrollTop.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(120L)
                .withEndAction { fabScrollTop.visibility = View.GONE }
                .start()
        }
    }

    private fun render() {
        if (!::textMonth.isInitialized || !::textIncome.isInitialized || !::textMonthSummaryTitle.isInitialized) {
            if (::recyclerRecords.isInitialized) {
                recyclerRecords.post { if (isAdded) render() }
            }
            return
        }
        val range = monthRange(currentMonth)
        val records = databaseHelper.getRecordsByDateRange(range.first, range.second)
        val income = records.filter { it.type == 1 }.sumOf { it.amount }
        val expense = records.filter { it.type == 0 }.sumOf { it.amount }
        textMonth.text = getString(R.string.ledger_book_name)
        textMonthSummaryTitle.text = monthSummaryTitle(currentMonth)
        textIncome.text = getString(R.string.currency_amount, income)
        textExpense.text = getString(R.string.currency_amount, expense)
        textBalance.text = getString(R.string.currency_amount, income - expense)

        val groups = records.groupBy { it.date }
            .toSortedMap(compareByDescending { it })
            .map { (date, items) -> DateGroup(date, items.sortedByDescending { it.sortOrder }) }
        recordsAdapter.updateDateGroups(groups)
        textEmpty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        recyclerRecords.visibility = View.VISIBLE
    }

    private fun monthRange(month: String): Pair<String, String> {
        val start = "$month-01"
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, month.substring(0, 4).toInt())
            set(Calendar.MONTH, month.substring(5, 7).toInt() - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val end = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        return start to end
    }

    private fun monthSummaryTitle(month: String): String {
        val year = month.substring(0, 4).toInt()
        val monthNumber = month.substring(5, 7).toInt()
        val now = Calendar.getInstance()
        return when {
            year == now.get(Calendar.YEAR) && monthNumber == now.get(Calendar.MONTH) + 1 ->
                getString(R.string.ledger_month_summary)
            year == now.get(Calendar.YEAR) ->
                getString(R.string.ledger_month_summary_same_year, monthNumber)
            else ->
                getString(R.string.ledger_month_summary_other_year, year, monthNumber)
        }
    }

    private fun showMonthPicker() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentMonth.substring(0, 4).toInt())
            set(Calendar.MONTH, currentMonth.substring(5, 7).toInt() - 1)
        }
        val pickerView = layoutInflater.inflate(R.layout.dialog_month_picker, null)
        val yearPicker = pickerView.findViewById<NumberPicker>(R.id.picker_year)
        val monthPicker = pickerView.findViewById<NumberPicker>(R.id.picker_month)
        val selectedYear = calendar.get(Calendar.YEAR)
        val minYear = selectedYear - 20
        val maxYear = selectedYear + 20

        yearPicker.minValue = minYear
        yearPicker.maxValue = maxYear
        yearPicker.value = selectedYear
        yearPicker.wrapSelectorWheel = true

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = calendar.get(Calendar.MONTH) + 1
        monthPicker.wrapSelectorWheel = true

        AlertDialog.Builder(requireContext())
            .setView(pickerView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                currentMonth = String.format(
                    Locale.US,
                    "%04d-%02d",
                    yearPicker.value,
                    monthPicker.value
                )
                render()
            }
            .show()
    }

    private fun switchMonth(offset: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentMonth.substring(0, 4).toInt())
            set(Calendar.MONTH, currentMonth.substring(5, 7).toInt() - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, offset)
        }
        currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
        recyclerRecords.scrollToPosition(0)
        render()
        val contentView = if (recyclerRecords.visibility == View.VISIBLE) {
            recyclerRecords
        } else {
            textEmpty
        }
        contentView.translationY = if (offset < 0) -8.dp.toFloat() else 8.dp.toFloat()
        contentView.alpha = 0.7f
        contentView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(160L)
            .start()
        updateScrollTopFab(false)
    }

    private fun bindHeader(header: View) {
        textMonth = header.findViewById(R.id.text_month)
        textIncome = header.findViewById(R.id.text_month_income)
        textExpense = header.findViewById(R.id.text_month_expense)
        textBalance = header.findViewById(R.id.text_month_balance)
        textMonthSummaryTitle = header.findViewById(R.id.text_month_summary_title)
        listOf(textIncome, textExpense, textBalance).forEach { it.isSelected = true }

        header.findViewById<View>(R.id.card_month_summary).setOnClickListener { showMonthPicker() }
        header.findViewById<ImageButton>(R.id.button_month_previous).setOnClickListener { switchMonth(-1) }
        header.findViewById<ImageButton>(R.id.button_month_next).setOnClickListener { switchMonth(1) }
        header.findViewById<ImageButton>(R.id.button_ledger_switch).setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.ledger_book_current), Toast.LENGTH_SHORT).show()
        }
        header.findViewById<ImageButton>(R.id.button_ledger_search).setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragment_container, SearchFragment()).addToBackStack(null).commit()
        }
        header.findViewById<ImageButton>(R.id.button_ledger_calendar).setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragment_container, CalendarFragment.newInstance(currentMonth)).addToBackStack(null).commit()
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun showDeleteDialog(record: Record) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_record_title)
            .setMessage(R.string.delete_record_message)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                databaseHelper.deleteRecord(record.id)
                Toast.makeText(requireContext(), R.string.toast_delete_success, Toast.LENGTH_SHORT).show()
                render()
            }
            .show()
    }
}
