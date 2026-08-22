package com.example.cardtally

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SearchFragment : Fragment() {
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var editSearch: EditText
    private lateinit var layoutSummary: View
    private lateinit var textTotals: TextView
    private lateinit var textFilter: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: DateGroupAdapter? = null

    private var searchKeyword: String = ""
    private var rangeStart: String? = null
    private var rangeEnd: String? = null
    private var rangeLabel = ""

    companion object {
        fun newInstance(keyword: String): SearchFragment {
            val fragment = SearchFragment()
            val args = Bundle()
            args.putString("keyword", keyword)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            searchKeyword = it.getString("keyword", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerRecords = view.findViewById(R.id.recycler_records)
        textEmpty = view.findViewById(R.id.text_empty)
        editSearch = view.findViewById(R.id.edit_search)
        layoutSummary = view.findViewById(R.id.layout_search_summary)
        textTotals = view.findViewById(R.id.text_search_totals)
        textFilter = view.findViewById(R.id.text_search_filter)
        textFilter.setOnClickListener { showDateRangeDialog() }

        databaseHelper = DatabaseHelper(requireContext())

        view.findViewById<ImageButton>(R.id.button_search_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())

        editSearch.setText(searchKeyword)
        editSearch.setSelection(editSearch.text.length)
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchKeyword = s?.toString().orEmpty().trim()
                searchRecords()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                editSearch.clearFocus()
                val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.hideSoftInputFromWindow(editSearch.windowToken, 0)
                true
            } else {
                false
            }
        }
        view.findViewById<TextView>(R.id.button_search_cancel).setOnClickListener {
            editSearch.setText("")
            editSearch.requestFocus()
            editSearch.post {
                val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        searchRecords()

        editSearch.requestFocus()
        editSearch.post {
            val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        showBottomNav()
    }

    private fun searchRecords() {
        if (searchKeyword.isBlank()) {
            layoutSummary.visibility = View.GONE
            textEmpty.visibility = View.GONE
            recyclerRecords.visibility = View.GONE
            return
        }

        val allRecords = databaseHelper.getAllRecords()

        val filteredRecords = allRecords.filter { record ->
            val inRange = (rangeStart == null || record.date >= rangeStart!!) &&
                (rangeEnd == null || record.date <= rangeEnd!!)
            val matchesKeyword =
                (record.categoryPathSnapshot ?: record.category).contains(searchKeyword, ignoreCase = true) ||
                    record.description?.contains(searchKeyword, ignoreCase = true) == true ||
                    record.amount.toString().contains(searchKeyword) ||
                    String.format("%.2f", record.amount).contains(searchKeyword) ||
                    record.assetSource?.contains(searchKeyword, ignoreCase = true) == true ||
                    record.date.contains(searchKeyword)
            inRange && matchesKeyword
        }

        layoutSummary.visibility = View.VISIBLE
        val expense = filteredRecords.filter { it.type == 0 }.sumOf { it.amount }
        val income = filteredRecords.filter { it.type == 1 }.sumOf { it.amount }
        textTotals.text = getString(R.string.search_totals, expense, income)

        if (filteredRecords.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerRecords.visibility = View.GONE
        } else {
            textEmpty.visibility = View.GONE
            recyclerRecords.visibility = View.VISIBLE

            val dateGroups = groupRecordsByDate(filteredRecords)

            if (adapter == null) {
                adapter = DateGroupAdapter(dateGroups, object : DateGroupAdapter.OnRecordActionListener {
                    override fun onEdit(record: Record) {
                        val editFragment = EditRecordFragment.newInstance(record.id)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, editFragment)
                            .addToBackStack(null)
                            .commit()
                    }

                    override fun onDelete(record: Record) {
                        showDeleteDialog(record)
                    }

                    override fun onMultiSelectChanged(selectedCount: Int) {
                    }

                    override fun onDeleteSelected(records: List<Record>) {
                    }

                    override fun onEnterMultiSelectMode(record: Record) {
                    }

                    override fun onToggleMultiSelect(record: Record) {
                    }
                })
                recyclerRecords.adapter = adapter
            } else {
                adapter?.updateDateGroups(dateGroups)
                if (recyclerRecords.adapter == null) {
                    recyclerRecords.adapter = adapter
                }
            }
        }
    }

    private fun groupRecordsByDate(records: List<Record>): List<DateGroup> {
        val grouped = records.groupBy { it.date }
        return grouped.map { (date, records) ->
            DateGroup(date, records)
        }.sortedByDescending { it.date }
    }

    private fun showDateRangeDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val content = layoutInflater.inflate(R.layout.dialog_search_date_range, null)
        val startButton = content.findViewById<Button>(R.id.button_range_start)
        val endButton = content.findViewById<Button>(R.id.button_range_end)
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun applyRange(start: String?, end: String?, label: String) {
            rangeStart = start
            rangeEnd = end
            rangeLabel = label
            startButton.text = start ?: getString(R.string.search_start_date)
            endButton.text = end ?: getString(R.string.search_end_date)
        }

        fun rangeForPreset(preset: String) {
            val now = Calendar.getInstance()
            val end = now.clone() as Calendar
            val start = now.clone() as Calendar
            when (preset) {
                "all" -> applyRange(null, null, getString(R.string.search_all_time))
                "month" -> {
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                    applyRange(format.format(start.time), format.format(end.time), getString(R.string.search_range_this_month))
                }
                "lastMonth" -> {
                    start.add(Calendar.MONTH, -1)
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    end.time = start.time
                    end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                    applyRange(format.format(start.time), format.format(end.time), getString(R.string.search_range_last_month))
                }
                "week" -> {
                    val delta = (start.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                    start.add(Calendar.DAY_OF_MONTH, -delta)
                    end.time = start.time
                    end.add(Calendar.DAY_OF_MONTH, 6)
                    applyRange(format.format(start.time), format.format(end.time), getString(R.string.search_range_this_week))
                }
                "three", "six" -> {
                    start.add(Calendar.MONTH, if (preset == "three") -2 else -5)
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    applyRange(format.format(start.time), format.format(end.time), getString(if (preset == "three") R.string.search_range_three_months else R.string.search_range_six_months))
                }
                "year" -> {
                    start.set(Calendar.MONTH, Calendar.JANUARY)
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    end.set(Calendar.MONTH, Calendar.DECEMBER)
                    end.set(Calendar.DAY_OF_MONTH, 31)
                    applyRange(format.format(start.time), format.format(end.time), getString(R.string.search_range_this_year))
                }
                "lastYear" -> {
                    start.add(Calendar.YEAR, -1)
                    start.set(Calendar.MONTH, Calendar.JANUARY)
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    end.time = start.time
                    end.set(Calendar.MONTH, Calendar.DECEMBER)
                    end.set(Calendar.DAY_OF_MONTH, 31)
                    applyRange(format.format(start.time), format.format(end.time), getString(R.string.search_range_last_year))
                }
            }
        }

        val chips = mapOf(
            R.id.chip_range_all to "all", R.id.chip_range_this_month to "month",
            R.id.chip_range_last_month to "lastMonth", R.id.chip_range_this_week to "week",
            R.id.chip_range_three_months to "three", R.id.chip_range_six_months to "six",
            R.id.chip_range_this_year to "year", R.id.chip_range_last_year to "lastYear"
        )
        chips.forEach { (id, preset) -> content.findViewById<Button>(id).setOnClickListener { rangeForPreset(preset) } }

        fun pickDate(target: Boolean) {
            val selected = Calendar.getInstance()
            val current = if (target) rangeStart else rangeEnd
            if (current != null) selected.time = format.parse(current) ?: selected.time
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                val value = format.format(picked.time)
                if (target) rangeStart = value else rangeEnd = value
                rangeLabel = "$value - ${rangeEnd ?: value}"
                startButton.text = rangeStart
                endButton.text = rangeEnd ?: getString(R.string.search_end_date)
            }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show()
        }
        startButton.setOnClickListener { pickDate(true) }
        endButton.setOnClickListener { pickDate(false) }
        content.findViewById<TextView>(R.id.button_range_cancel).setOnClickListener { dialog.dismiss() }
        content.findViewById<TextView>(R.id.button_range_done).setOnClickListener {
            textFilter.text = if (rangeLabel.isBlank()) getString(R.string.search_all_time) else rangeLabel
            dialog.dismiss()
            searchRecords()
        }
        dialog.setContentView(content)
        dialog.show()
    }

    private fun showDeleteDialog(record: Record) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("确定") { _, _ ->
                databaseHelper.deleteRecord(record.id)
                Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                searchRecords()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun hideBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.GONE
    }

    private fun showBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.VISIBLE
    }
}
