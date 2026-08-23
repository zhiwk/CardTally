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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class CalendarFragment : Fragment() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var textMonth: TextView
    private lateinit var textSelectedDate: TextView
    private lateinit var textSelectedSummary: TextView
    private lateinit var textEmpty: TextView
    private lateinit var calendarAdapter: LedgerCalendarAdapter
    private lateinit var recordsRecycler: RecyclerView
    private var monthRecords: List<Record> = emptyList()
    private var year = Calendar.getInstance().get(Calendar.YEAR)
    private var month = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialMonth = requireArguments().getString(ARG_MONTH)
            ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        year = initialMonth.substring(0, 4).toInt()
        month = initialMonth.substring(5, 7).toInt() - 1
        selectedDate = "$initialMonth-01"
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
        renderMonth()
        return view
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
        monthRecords = databaseHelper.getRecordsByDateRange(monthStart, monthEnd)
        val totals = monthRecords.groupBy { it.date }
            .mapValues { (_, records) ->
                records.fold(0.0 to 0.0) { totals, record ->
                    if (record.type == 1) totals.first + abs(record.amount) to totals.second
                    else totals.first to totals.second + abs(record.amount)
                }
            }
        val cells = LedgerPeriodHelper.buildMonthCells(
            year,
            month,
            LedgerDateRange(selectedDate, selectedDate)
        ).map { day ->
            val total = day.isoDate?.let { totals[it] }
            day.copy(income = total?.first ?: 0.0, expense = total?.second ?: 0.0)
        }
        calendarAdapter.submitList(cells)
        renderSelectedDay()
    }

    private fun renderSelectedDay() {
        val records = databaseHelper.getRecordsByDateRange(selectedDate, selectedDate)
            .sortedByDescending { it.sortOrder }
        val income = records.filter { it.type == 1 }.sumOf { it.amount }
        val expense = records.filter { it.type == 0 }.sumOf { it.amount }
        textSelectedDate.text = selectedDate
        textSelectedSummary.text = getString(R.string.calendar_day_summary, expense, income)
        textEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        recordsRecycler.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
        if (records.isEmpty()) return

        recordsRecycler.adapter = CalendarRecordAdapter(
            records,
            object : DateGroupAdapter.OnRecordActionListener {
                override fun onEdit(record: Record) {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, EditRecordFragment.newInstance(record.id))
                        .addToBackStack(null)
                        .commit()
                }

                override fun onDelete(record: Record) {
                    databaseHelper.deleteRecord(record.id)
                    Toast.makeText(requireContext(), R.string.toast_delete_success, Toast.LENGTH_SHORT).show()
                    renderSelectedDay()
                }

                override fun onMultiSelectChanged(selectedCount: Int) = Unit
                override fun onDeleteSelected(records: List<Record>) = Unit
                override fun onEnterMultiSelectMode(record: Record) = Unit
                override fun onToggleMultiSelect(record: Record) = Unit
            }
        )
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

        fun newInstance(month: String): CalendarFragment = CalendarFragment().apply {
            arguments = Bundle().apply { putString(ARG_MONTH, month) }
        }
    }
}
