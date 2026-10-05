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
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.adapter.LedgerCalendarAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.LedgerDateRange
import com.example.cardtally.util.LedgerPeriodHelper
import com.example.cardtally.util.Money
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var textMonth: TextView
    private lateinit var textSelectedDate: TextView
    private lateinit var textSelectedSummary: TextView
    private lateinit var textEmpty: TextView
    private lateinit var calendarAdapter: LedgerCalendarAdapter
    private lateinit var recordsRecycler: RecyclerView
    private lateinit var recordsCard: View
    private val dayRecords = mutableListOf<Record>()
    private var dayCursor: DatabaseHelper.RecordListCursor? = null
    private var dayLoading = false
    private var year = Calendar.getInstance().get(Calendar.YEAR)
    private var month = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialMonth = requireArguments().getString(ARG_MONTH)
            ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        year = initialMonth.substring(0, 4).toInt()
        month = initialMonth.substring(5, 7).toInt() - 1
        val today = Calendar.getInstance()
        val todayMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(today.time)
        selectedDate = if (todayMonth == initialMonth) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(today.time)
        } else {
            "$initialMonth-01"
        }
        savedInstanceState?.let {
            year = it.getInt(STATE_YEAR, year)
            month = it.getInt(STATE_MONTH, month)
            selectedDate = it.getString(STATE_DATE, selectedDate)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_YEAR, year)
        outState.putInt(STATE_MONTH, month)
        outState.putString(STATE_DATE, selectedDate)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        databaseHelper = DatabaseHelper(requireContext())
        textMonth = view.findViewById(R.id.text_calendar_month)
        textSelectedDate = view.findViewById(R.id.text_selected_date)
        textSelectedSummary = view.findViewById(R.id.text_selected_summary)
        textEmpty = view.findViewById(R.id.text_calendar_empty)
        recordsRecycler = view.findViewById(R.id.recycler_calendar_records)
        recordsCard = view.findViewById(R.id.card_calendar_records)

        view.findViewById<ImageButton>(R.id.button_calendar_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<ImageButton>(R.id.button_calendar_month_picker).setOnClickListener {
            showMonthPicker()
        }

        calendarAdapter = LedgerCalendarAdapter(singleSelection = true) { day ->
            day.isoDate?.let {
                selectedDate = it
                renderMonth()
            }
        }
        view.findViewById<RecyclerView>(R.id.recycler_calendar_days).apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
        }
        recordsRecycler.layoutManager = LinearLayoutManager(requireContext())
        val calendarScroll = view.findViewById<NestedScrollView>(R.id.scroll_calendar_content)
        calendarScroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val remaining = calendarScroll.getChildAt(0).height - scrollY - calendarScroll.height
            val preloadDistance = (300 * resources.displayMetrics.density).toInt()
            if (remaining < preloadDistance) loadNextDayPage()
        }
        renderMonth()
        return view
    }

    override fun onResume() {
        super.onResume()
        if (::calendarAdapter.isInitialized) renderMonth()
    }

    private fun renderMonth() {
        textMonth.text = String.format(Locale.US, "%04d年%02d月", year, month + 1)
        val monthKey = String.format(Locale.US, "%04d-%02d", year, month + 1)
        if (!selectedDate.startsWith(monthKey)) selectedDate = "$monthKey-01"
        val monthStart = String.format(Locale.US, "%04d-%02d-01", year, month + 1)
        val lastDay = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthEnd = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, lastDay)
        val totals = databaseHelper.getDailyTotals(monthStart, monthEnd)
        val cells = LedgerPeriodHelper.buildMonthCells(
            year,
            month,
            LedgerDateRange(selectedDate, selectedDate)
        ).map { day ->
            val total = day.isoDate?.let { totals[it] }
            day.copy(
                income = Money.toMajorDouble(total?.incomeMinor ?: 0L),
                expense = Money.toMajorDouble(total?.expenseMinor ?: 0L)
            )
        }
        calendarAdapter.submitList(cells)
        renderSelectedDay()
    }

    private fun renderSelectedDay() {
        val totals = databaseHelper.getDailyTotals(selectedDate, selectedDate)[selectedDate]
        val income = Money.toMajorDouble(totals?.incomeMinor ?: 0L)
        val expense = Money.toMajorDouble(totals?.expenseMinor ?: 0L)
        textSelectedDate.text = selectedDate
        textSelectedSummary.text = getString(R.string.calendar_day_summary, expense, income)
        dayRecords.clear()
        dayCursor = null
        dayLoading = false
        val listener =
            object : DateGroupAdapter.OnRecordActionListener {
                override fun onOpenDetails(record: Record) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, RecordDetailFragment.newInstance(record.id))
                        .addToBackStack(null)
                        .commit()
                }

                override fun onEdit(record: Record) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, EditRecordFragment.newInstance(record.id))
                        .addToBackStack(null)
                        .commit()
                }

                override fun onDelete(record: Record) {
                    databaseHelper.deleteRecord(record.id)
                    Toast.makeText(requireContext(), R.string.toast_delete_success, Toast.LENGTH_SHORT).show()
                    renderMonth()
                }

                override fun onMultiSelectChanged(selectedCount: Int) = Unit
                override fun onDeleteSelected(records: List<Record>) = Unit
                override fun onEnterMultiSelectMode(record: Record) = Unit
                override fun onToggleMultiSelect(record: Record) = Unit
            }
        recordsRecycler.adapter = CalendarRecordAdapter(dayRecords, listener)
        appendDayPage(databaseHelper.getRecordsPage(selectedDate, selectedDate))
    }

    private fun loadNextDayPage() {
        val cursor = dayCursor ?: return
        if (dayLoading) return
        dayLoading = true
        val page = databaseHelper.getRecordsPage(
            selectedDate, selectedDate, after = cursor
        )
        dayLoading = false
        appendDayPage(page)
    }

    private fun appendDayPage(page: DatabaseHelper.RecordListPage) {
        dayCursor = page.nextCursor
        dayRecords.addAll(page.records)
        (recordsRecycler.adapter as CalendarRecordAdapter).notifyDataSetChanged()
        textEmpty.visibility = if (dayRecords.isEmpty()) View.VISIBLE else View.GONE
        recordsCard.visibility = if (dayRecords.isEmpty()) View.GONE else View.VISIBLE
    }

    private class CalendarRecordAdapter(
        private val records: List<Record>,
        private val listener: DateGroupAdapter.OnRecordActionListener
    ) : RecyclerView.Adapter<DateGroupAdapter.RecordViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DateGroupAdapter.RecordViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_record, parent, false)
            return DateGroupAdapter.RecordViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: DateGroupAdapter.RecordViewHolder,
            position: Int
        ) {
            holder.bind(records[position], listener, false, false)
        }

        override fun getItemCount(): Int = records.size
    }

    private fun showMonthPicker() {
        val pickerView = layoutInflater.inflate(R.layout.dialog_month_picker, null)
        val yearPicker = pickerView.findViewById<NumberPicker>(R.id.picker_year)
        val monthPicker = pickerView.findViewById<NumberPicker>(R.id.picker_month)
        yearPicker.minValue = year - 20
        yearPicker.maxValue = year + 20
        yearPicker.value = year
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = month + 1

        AlertDialog.Builder(requireContext())
            .setView(pickerView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                year = yearPicker.value
                month = monthPicker.value - 1
                selectedDate = String.format(Locale.US, "%04d-%02d-01", year, month + 1)
                renderMonth()
            }
            .show()
    }

    companion object {
        private const val ARG_MONTH = "arg_month"
        private const val STATE_YEAR = "calendar_year"
        private const val STATE_MONTH = "calendar_month"
        private const val STATE_DATE = "calendar_selected_date"

        fun newInstance(month: String): CalendarFragment = CalendarFragment().apply {
            arguments = Bundle().apply { putString(ARG_MONTH, month) }
        }
    }
}
