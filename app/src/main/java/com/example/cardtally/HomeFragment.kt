package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.HomeRecentRecordAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Record
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.example.cardtally.util.FloatingNavLayoutHelper
import com.example.cardtally.util.LanguageHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class HomeFragment : Fragment() {
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var cardAgentNote: View
    private lateinit var textViewLedger: View

    private lateinit var textDate: TextView
    private lateinit var textIncome: TextView
    private lateinit var textExpense: TextView
    private lateinit var textBalance: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: HomeRecentRecordAdapter? = null

    private var currentYear: Int = 0
    private var currentMonth: Int = 0
    private var currentPeriod: String = "month" // fixed to monthly view matching new UI

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
        cardAgentNote = view.findViewById(R.id.card_agent_note)
        textViewLedger = view.findViewById(R.id.text_view_ledger)

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
        val sdf = SimpleDateFormat(
            getString(R.string.home_date_pattern),
            LanguageHelper.getCurrentLocale(requireContext())
        )
        textDate.text = sdf.format(calendar.time)
        updateAiAssistantVisibility()

        cardAgentNote.setOnClickListener {
            if (!AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())) {
                return@setOnClickListener
            }

            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_agent
        }

        textViewLedger.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_statistics
        }

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
        updateAiAssistantVisibility()
        loadRecords()
    }

    private fun updateAiAssistantVisibility() {
        cardAgentNote.visibility = if (AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }



    private fun loadRecords() {
        val (startDate, endDate) = getDateRangeByPeriod()

        allRecords = databaseHelper.getLatestRecordDayRecords()

        val income = databaseHelper.getTotalByTypeAndDateRange(1, startDate, endDate)
        val expense = databaseHelper.getTotalByTypeAndDateRange(0, startDate, endDate)
        val balance = income - expense
        val daysElapsed = max(Calendar.getInstance().get(Calendar.DAY_OF_MONTH), 1)
        val dailyAverage = expense / daysElapsed

        textIncome.text = getString(R.string.currency_amount, expense)
        textExpense.text = getString(R.string.currency_amount, dailyAverage)
        textBalance.text = String.format(Locale.getDefault(), "%,.2f", balance)

        if (allRecords.isEmpty()) {
            textEmpty.text = getString(R.string.home_empty_records_hint)
            textEmpty.visibility = View.VISIBLE
            recyclerRecords.visibility = View.GONE
        } else {
            textEmpty.visibility = View.GONE
            recyclerRecords.visibility = View.VISIBLE

            if (adapter == null) {
                adapter = HomeRecentRecordAdapter(allRecords, object : HomeRecentRecordAdapter.Listener {
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
                })
                recyclerRecords.adapter = adapter
            } else {
                adapter?.updateRecords(allRecords)
                if (recyclerRecords.adapter == null) {
                    recyclerRecords.adapter = adapter
                }
            }
        }
    }

    private fun showDeleteDialog(record: Record) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_record_title)
            .setMessage(R.string.delete_record_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                databaseHelper.deleteRecord(record.id)
                Toast.makeText(requireContext(), getString(R.string.toast_delete_success), Toast.LENGTH_SHORT).show()
                loadRecords()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
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

}
