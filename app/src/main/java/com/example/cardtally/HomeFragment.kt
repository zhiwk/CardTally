package com.example.cardtally

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.RecordDragCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class HomeFragment : Fragment() {
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnPrevMonth: Button
    private lateinit var btnNextMonth: Button
    private lateinit var textMonth: TextView
    private lateinit var btnSearch: ImageButton
    private lateinit var textIncome: TextView
    private lateinit var textExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: DateGroupAdapter? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    private var currentYear: Int = 0
    private var currentMonth: Int = 0

    private var isBottomNavVisible = true
    private var isFabVisible = true

    private var allRecords: List<Record> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerRecords = view.findViewById(R.id.recycler_records)
        textEmpty = view.findViewById(R.id.text_empty)
        fabAdd = view.findViewById(R.id.fab_add)
        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)
        textMonth = view.findViewById(R.id.text_month)
        btnSearch = view.findViewById(R.id.btn_search)
        textIncome = view.findViewById(R.id.text_income)
        textExpense = view.findViewById(R.id.text_expense)
        textBalance = view.findViewById(R.id.text_balance)

        databaseHelper = DatabaseHelper(requireContext())

        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())

        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH) + 1

        updateMonthDisplay()

        btnPrevMonth.setOnClickListener {
            currentMonth--
            if (currentMonth < 1) {
                currentMonth = 12
                currentYear--
            }
            updateMonthDisplay()
            loadRecords()
        }

        btnNextMonth.setOnClickListener {
            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
            updateMonthDisplay()
            loadRecords()
        }

        textMonth.setOnClickListener {
            showMonthPicker()
        }

        btnSearch.setOnClickListener {
            showSearchDialog()
        }

        fabAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddRecordFragment())
                .addToBackStack(null)
                .commit()
        }

        recyclerRecords.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                if (dy > 0) {
                    hideBottomNav()
                    hideFab()
                } else if (dy < 0) {
                    showBottomNav()
                    showFab()
                }
            }
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
        showBottomNav()
        showFab()
    }

    private fun updateMonthDisplay() {
        textMonth.text = String.format("%d年%02d月", currentYear, currentMonth)
    }

    private fun showMonthPicker() {
        val calendar = Calendar.getInstance()
        val year = currentYear
        val month = currentMonth - 1

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, _ ->
                currentYear = selectedYear
                currentMonth = selectedMonth + 1
                updateMonthDisplay()
                loadRecords()
            },
            year,
            month,
            1
        )
        
        datePickerDialog.datePicker.findViewById<View>(
            resources.getIdentifier("day", "id", "android")
        )?.visibility = View.GONE
        
        datePickerDialog.show()
    }

    private fun showSearchDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("搜索记录")

        val input = EditText(requireContext())
        input.hint = "输入分类、描述或金额"
        builder.setView(input)

        builder.setPositiveButton("搜索") { _, _ ->
            val keyword = input.text.toString().trim()
            if (keyword.isNotEmpty()) {
                val searchFragment = SearchFragment.newInstance(keyword)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, searchFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        builder.setNegativeButton("取消", null)

        builder.show()
    }

    private fun loadRecords() {
        val startDate = String.format("%04d-%02d-01", currentYear, currentMonth)
        val lastDay = when (currentMonth) {
            2 -> if (currentYear % 4 == 0 && (currentYear % 100 != 0 || currentYear % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        val endDate = String.format("%04d-%02d-%02d", currentYear, currentMonth, lastDay)

        allRecords = databaseHelper.getRecordsByDateRange(startDate, endDate)

        val income = databaseHelper.getTotalByTypeAndDateRange(1, startDate, endDate)
        val expense = databaseHelper.getTotalByTypeAndDateRange(0, startDate, endDate)
        val balance = income - expense

        textIncome.text = String.format("¥%.2f", income)
        textExpense.text = String.format("¥%.2f", expense)
        textBalance.text = String.format("¥%.2f", balance)

        if (allRecords.isEmpty()) {
            textEmpty.text = "暂无记录，点击右下角按钮添加记录"
            textEmpty.visibility = View.VISIBLE
            recyclerRecords.visibility = View.GONE
        } else {
            textEmpty.visibility = View.GONE
            recyclerRecords.visibility = View.VISIBLE

            val dateGroups = groupRecordsByDate(allRecords)

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
                        if (selectedCount > 0) {
                            fabAdd.setImageResource(R.drawable.ic_delete)
                            fabAdd.setOnClickListener {
                                showDeleteSelectedDialog()
                            }
                        } else {
                            fabAdd.setImageResource(R.drawable.ic_add)
                            fabAdd.setOnClickListener {
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, AddRecordFragment())
                                    .addToBackStack(null)
                                    .commit()
                            }
                        }
                    }

                    override fun onDeleteSelected(records: List<Record>) {
                        showDeleteSelectedDialog()
                    }

                    override fun onEnterMultiSelectMode(record: Record) {
                        adapter?.enterMultiSelectMode(record)
                    }

                    override fun onToggleMultiSelect(record: Record) {
                        adapter?.toggleMultiSelect(record)
                    }
                })
                recyclerRecords.adapter = adapter
                
                val callback = RecordDragCallback(
                    adapter!!,
                    onRecordMoved = { fromPosition, toPosition ->
                        saveSortOrder()
                    },
                    onEnterMultiSelectMode = { position ->
                        val record = adapter?.getRecordAtPosition(position)
                        if (record != null) {
                            adapter?.enterMultiSelectMode(record)
                        }
                    }
                )
                itemTouchHelper = ItemTouchHelper(callback)
                itemTouchHelper?.attachToRecyclerView(recyclerRecords)
            } else {
                adapter?.updateDateGroups(dateGroups)
                if (recyclerRecords.adapter == null) {
                    recyclerRecords.adapter = adapter
                }
            }
        }
    }

    private fun saveSortOrder() {
        val dateGroups = adapter?.getDateGroups() ?: return
        val records = mutableListOf<Record>()
        for (dateGroup in dateGroups) {
            records.addAll(dateGroup.records)
        }
        databaseHelper.updateRecordsSortOrder(records)
    }

    private fun groupRecordsByDate(records: List<Record>): List<DateGroup> {
        val grouped = records.groupBy { it.date }
        return grouped.map { (date, records) ->
            DateGroup(date, records)
        }.sortedByDescending { it.date }
    }

    private fun showDeleteDialog(record: Record) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("确定") { _, _ ->
                databaseHelper.deleteRecord(record.id)
                Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                loadRecords()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteSelectedDialog() {
        val selectedRecords = adapter?.getSelectedRecords() ?: return
        if (selectedRecords.isEmpty()) return
        
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除选中的 ${selectedRecords.size} 条记录吗？")
            .setPositiveButton("确定") { _, _ ->
                for (record in selectedRecords) {
                    databaseHelper.deleteRecord(record.id)
                }
                Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                adapter?.exitMultiSelectMode()
                fabAdd.setImageResource(R.drawable.ic_add)
                fabAdd.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AddRecordFragment())
                        .addToBackStack(null)
                        .commit()
                }
                loadRecords()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun hideBottomNav() {
        if (isBottomNavVisible) {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.animate()
                ?.alpha(0f)
                ?.translationY(bottomNav.height.toFloat())
                ?.setDuration(200)
                ?.withEndAction {
                    bottomNav.visibility = View.GONE
                }
                ?.start()
            isBottomNavVisible = false
        }
    }

    private fun showBottomNav() {
        if (!isBottomNavVisible) {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav.visibility = View.VISIBLE
            bottomNav.alpha = 0f
            bottomNav.translationY = bottomNav.height.toFloat()
            bottomNav.animate()
                ?.alpha(1f)
                ?.translationY(0f)
                ?.setDuration(200)
                ?.start()
            isBottomNavVisible = true
        }
    }

    private fun hideFab() {
        if (isFabVisible) {
            fabAdd.animate()
                .alpha(0f)
                .translationX(fabAdd.width.toFloat() * 2)
                .setDuration(200)
                .withEndAction {
                    fabAdd.visibility = View.GONE
                }
                .start()
            isFabVisible = false
        }
    }

    private fun showFab() {
        if (!isFabVisible) {
            fabAdd.visibility = View.VISIBLE
            fabAdd.alpha = 0f
            fabAdd.translationX = fabAdd.width.toFloat() * 2
            fabAdd.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(200)
                .start()
            isFabVisible = true
        }
    }
}
