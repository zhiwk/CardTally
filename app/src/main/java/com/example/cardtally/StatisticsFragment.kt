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
import com.example.cardtally.util.FloatingNavLayoutHelper
import java.util.Calendar

class StatisticsFragment : Fragment() {
    private lateinit var textEmpty: TextView
    private lateinit var recyclerStatistics: RecyclerView
    private lateinit var fabAdd: View
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var adapter: StatisticsAdapter

    private var currentTab = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        textEmpty = view.findViewById(R.id.text_empty)
        recyclerStatistics = view.findViewById(R.id.recycler_statistics)
        fabAdd = view.findViewById(R.id.fab_add)

        databaseHelper = DatabaseHelper(requireContext())

        recyclerStatistics.layoutManager = LinearLayoutManager(requireContext())
        adapter = StatisticsAdapter()
        recyclerStatistics.adapter = adapter

        loadStatistics(0)
        
        fabAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddRecordFragment())
                .addToBackStack(null)
                .commit()
        }

        requireActivity().findViewById<View>(R.id.nav_shell)?.let { navShell ->
            FloatingNavLayoutHelper.applyFabGapAboveBottomNav(fabAdd, navShell)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadStatistics(currentTab)
    }

    private fun loadStatistics(tabPosition: Int) {

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
