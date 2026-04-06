package com.example.cardtally

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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

    private lateinit var textDate: TextView
    private lateinit var textIncome: TextView
    private lateinit var textExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: DateGroupAdapter? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    private var currentYear: Int = 0
    private var currentMonth: Int = 0
    private var currentPeriod: String = "month" // fixed to monthly view matching new UI

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

        textDate = view.findViewById(R.id.text_date)
        textIncome = view.findViewById(R.id.text_income)
        textExpense = view.findViewById(R.id.text_expense)
        textBalance = view.findViewById(R.id.text_balance)

        databaseHelper = DatabaseHelper(requireContext())

        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())

        val calendar = Calendar.getInstance()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH) + 1

        // Date header logic
        val sdf = java.text.SimpleDateFormat("M月dd日 E", java.util.Locale.CHINESE)
        textDate.text = sdf.format(calendar.time)

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

        recyclerRecords.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            private var initialY = 0f
            private val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
            
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = e.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = e.rawY - initialY
                        if (deltaY > touchSlop) {
                            showBottomNav()
                            showFab()
                        } else if (deltaY < -touchSlop) {
                            hideBottomNav()
                            hideFab()
                        }
                    }
                }
                return false
            }
            
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
        showBottomNav()
        showFab()
    }



    private fun showFilterDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("筛选记录")

        // 这里可以添加筛选选项，如分类、金额范围等
        val categories = databaseHelper.getAllCategories()
        val categoryNames = categories.map { it.name }.toTypedArray()
        val checkedItems = BooleanArray(categoryNames.size) { false }

        builder.setMultiChoiceItems(categoryNames, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }

        builder.setPositiveButton("确定") { _, _ ->
            // 处理筛选逻辑
            val selectedCategories = mutableListOf<String>()
            for (i in checkedItems.indices) {
                if (checkedItems[i]) {
                    selectedCategories.add(categoryNames[i])
                }
            }
            // 根据筛选条件加载记录
            loadRecords(selectedCategories)
        }

        builder.setNegativeButton("取消", null)

        builder.show()
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

    private fun loadRecords(selectedCategories: List<String> = emptyList()) {
        val (startDate, endDate) = getDateRangeByPeriod()

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

    private fun getDateRangeByPeriod(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val year = currentYear
        val month = currentMonth - 1
        
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        
        return when (currentPeriod) {
            "week" -> {
                // 计算本周的开始和结束日期
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysToMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
                calendar.add(Calendar.DAY_OF_YEAR, -daysToMonday)
                val startDate = String.format("%04d-%02d-%02d", 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH) + 1, 
                    calendar.get(Calendar.DAY_OF_MONTH))
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                val endDate = String.format("%04d-%02d-%02d", 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH) + 1, 
                    calendar.get(Calendar.DAY_OF_MONTH))
                Pair(startDate, endDate)
            }
            "month" -> {
                // 计算本月的开始和结束日期
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = String.format("%04d-%02d-%02d", 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH) + 1, 
                    calendar.get(Calendar.DAY_OF_MONTH))
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val endDate = String.format("%04d-%02d-%02d", 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH) + 1, 
                    calendar.get(Calendar.DAY_OF_MONTH))
                Pair(startDate, endDate)
            }
            "year" -> {
                // 计算本年的开始和结束日期
                calendar.set(Calendar.MONTH, 0)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = String.format("%04d-%02d-%02d", 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH) + 1, 
                    calendar.get(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.MONTH, 11)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                val endDate = String.format("%04d-%02d-%02d", 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH) + 1, 
                    calendar.get(Calendar.DAY_OF_MONTH))
                Pair(startDate, endDate)
            }
            else -> {
                // 默认返回本月
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = String.format("%04d-%02d-%02d", 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH) + 1, 
                    calendar.get(Calendar.DAY_OF_MONTH))
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val endDate = String.format("%04d-%02d-%02d", 
                    calendar.get(Calendar.YEAR), 
                    calendar.get(Calendar.MONTH) + 1, 
                    calendar.get(Calendar.DAY_OF_MONTH))
                Pair(startDate, endDate)
            }
        }
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
