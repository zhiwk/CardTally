package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.StatisticsAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.util.ThemeHelper
import com.google.android.material.tabs.TabLayout
import java.util.Calendar

class StatisticsFragment : Fragment() {
    private lateinit var tabLayout: TabLayout
    private lateinit var textExpenseTotal: TextView
    private lateinit var textIncomeTotal: TextView
    private lateinit var textEmpty: TextView
    private lateinit var recyclerStatistics: RecyclerView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var adapter: StatisticsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        tabLayout = view.findViewById(R.id.tab_layout)
        textExpenseTotal = view.findViewById(R.id.text_expense_total)
        textIncomeTotal = view.findViewById(R.id.text_income_total)
        textEmpty = view.findViewById(R.id.text_empty)
        recyclerStatistics = view.findViewById(R.id.recycler_statistics)

        databaseHelper = DatabaseHelper(requireContext())

        recyclerStatistics.layoutManager = LinearLayoutManager(requireContext())
        adapter = StatisticsAdapter()
        recyclerStatistics.adapter = adapter

        loadStatistics(0)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { loadStatistics(it.position) }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        loadStatistics(tabLayout.selectedTabPosition)
    }

    private fun loadStatistics(tabPosition: Int) {
        val expenseTotal = databaseHelper.getTotalByType(0)
        val incomeTotal = databaseHelper.getTotalByType(1)

        textExpenseTotal.text = String.format("¥%.2f", expenseTotal)
        textIncomeTotal.text = String.format("¥%.2f", incomeTotal)

        if (tabPosition == 0) {
            loadCategoryStatistics()
        } else {
            loadTimeStatistics()
        }
    }

    private fun loadCategoryStatistics() {
        val expenseStats = databaseHelper.getCategoryStatistics(0)
        val incomeStats = databaseHelper.getCategoryStatistics(1)

        val allStats = mutableMapOf<String, Double>()
        allStats.putAll(expenseStats)
        allStats.putAll(incomeStats)

        if (allStats.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerStatistics.visibility = View.GONE
        } else {
            textEmpty.visibility = View.GONE
            recyclerStatistics.visibility = View.VISIBLE
            adapter.updateData(allStats)
        }
    }

    private fun loadTimeStatistics() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val expenseStats = databaseHelper.getMonthlyStatistics(0, currentYear)
        val incomeStats = databaseHelper.getMonthlyStatistics(1, currentYear)

        val allStats = mutableMapOf<String, Double>()
        allStats.putAll(expenseStats)
        allStats.putAll(incomeStats)

        if (allStats.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerStatistics.visibility = View.GONE
        } else {
            textEmpty.visibility = View.GONE
            recyclerStatistics.visibility = View.VISIBLE
            adapter.updateData(allStats)
        }
    }
}
