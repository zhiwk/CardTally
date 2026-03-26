package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.StatisticsAdapter
import com.example.cardtally.database.DatabaseHelper
import java.util.Calendar

class StatisticsFragment : Fragment() {
    private lateinit var cardTabCategory: CardView
    private lateinit var cardTabTime: CardView
    private lateinit var textExpenseTotal: TextView
    private lateinit var textIncomeTotal: TextView
    private lateinit var textEmpty: TextView
    private lateinit var recyclerStatistics: RecyclerView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var adapter: StatisticsAdapter

    private var currentTab = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        cardTabCategory = view.findViewById(R.id.card_tab_category)
        cardTabTime = view.findViewById(R.id.card_tab_time)
        textExpenseTotal = view.findViewById(R.id.text_expense_total)
        textIncomeTotal = view.findViewById(R.id.text_income_total)
        textEmpty = view.findViewById(R.id.text_empty)
        recyclerStatistics = view.findViewById(R.id.recycler_statistics)

        databaseHelper = DatabaseHelper(requireContext())

        recyclerStatistics.layoutManager = LinearLayoutManager(requireContext())
        adapter = StatisticsAdapter()
        recyclerStatistics.adapter = adapter

        loadStatistics(0)
        updateTabStyle()

        cardTabCategory.setOnClickListener {
            if (currentTab != 0) {
                currentTab = 0
                updateTabStyle()
                loadStatistics(0)
            }
        }

        cardTabTime.setOnClickListener {
            if (currentTab != 1) {
                currentTab = 1
                updateTabStyle()
                loadStatistics(1)
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadStatistics(currentTab)
        updateTabStyle()
    }

    private fun updateTabStyle() {
        if (currentTab == 0) {
            cardTabCategory.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.primary_light)
            )
            cardTabTime.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.editorial_surface_low)
            )
        } else {
            cardTabCategory.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.editorial_surface_low)
            )
            cardTabTime.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.primary_light)
            )
        }
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
