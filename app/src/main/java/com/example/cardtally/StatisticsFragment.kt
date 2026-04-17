package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.adapter.LedgerCalendarAdapter
import com.example.cardtally.adapter.StatisticsAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.FloatingNavLayoutHelper
import com.example.cardtally.util.LedgerDateRange
import com.example.cardtally.util.LedgerDisplayHelper
import com.example.cardtally.util.LedgerPeriodHelper
import com.example.cardtally.util.LedgerPeriodPreset
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.view.LedgerDonutChartView
import com.example.cardtally.view.LedgerLineChartView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class StatisticsFragment : Fragment() {
    private lateinit var togglePeriodPreset: MaterialButtonToggleGroup
    private lateinit var textEmpty: TextView
    private lateinit var textRangeValue: TextView
    private lateinit var textViewModeHint: TextView
    private lateinit var textSummaryLabel: TextView
    private lateinit var textSummaryAmount: TextView
    private lateinit var textSummarySupporting: TextView
    private lateinit var textSummaryBadge: TextView
    private lateinit var textSummaryIncome: TextView
    private lateinit var textSummaryExpense: TextView
    private lateinit var textStatisticsInsight: TextView
    private lateinit var textChartSubtitle: TextView
    private lateinit var recyclerStatistics: RecyclerView
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var viewStatisticsChart: LedgerDonutChartView
    private lateinit var viewStatisticsLineChart: LedgerLineChartView
    private lateinit var toggleChartMode: MaterialButtonToggleGroup
    private lateinit var layoutChartLegend: LinearLayout
    private lateinit var layoutLineAxis: LinearLayout
    private lateinit var textAxisStart: TextView
    private lateinit var textAxisMid: TextView
    private lateinit var textAxisEnd: TextView
    private lateinit var fabAdd: View
    private lateinit var layoutModeSelector: View
    private lateinit var layoutStatisticsContent: View
    private lateinit var layoutRecordsContent: View
    private lateinit var layoutRangeSelector: View
    private lateinit var textStatisticsSectionTitle: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var statisticsAdapter: StatisticsAdapter
    private var recordsAdapter: DateGroupAdapter? = null

    private var currentViewMode = VIEW_MODE_STATISTICS
    private var currentStatsType = TYPE_EXPENSE
    private var currentPeriodPreset = LedgerPeriodPreset.MONTH
    private var currentRange = LedgerPeriodHelper.resolveRange(LedgerPeriodPreset.MONTH)
    private var currentChartMode = CHART_MODE_PIE

    companion object {
        private const val VIEW_MODE_STATISTICS = 0
        private const val VIEW_MODE_RECORDS = 1
        private const val TYPE_EXPENSE = 0
        private const val TYPE_INCOME = 1
        private const val CHART_MODE_LINE = 0
        private const val CHART_MODE_PIE = 1
        private const val MENU_MODE_STATS_EXPENSE = 1
        private const val MENU_MODE_STATS_INCOME = 2
        private const val MENU_MODE_RECORDS = 3
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        togglePeriodPreset = view.findViewById(R.id.toggle_period_preset)
        textEmpty = view.findViewById(R.id.text_empty)
        textRangeValue = view.findViewById(R.id.text_range_value)
        textViewModeHint = view.findViewById(R.id.text_view_mode_hint)
        textSummaryLabel = view.findViewById(R.id.text_summary_label)
        textSummaryAmount = view.findViewById(R.id.text_summary_amount)
        textSummarySupporting = view.findViewById(R.id.text_summary_supporting)
        textSummaryBadge = view.findViewById(R.id.text_summary_badge)
        textSummaryIncome = view.findViewById(R.id.text_summary_income)
        textSummaryExpense = view.findViewById(R.id.text_summary_expense)
        textStatisticsSectionTitle = view.findViewById(R.id.text_statistics_section_title)
        textStatisticsInsight = view.findViewById(R.id.text_statistics_insight)
        textChartSubtitle = view.findViewById(R.id.text_chart_subtitle)
        recyclerStatistics = view.findViewById(R.id.recycler_statistics)
        recyclerRecords = view.findViewById(R.id.recycler_records)
        viewStatisticsChart = view.findViewById(R.id.view_statistics_chart)
        viewStatisticsLineChart = view.findViewById(R.id.view_statistics_line_chart)
        toggleChartMode = view.findViewById(R.id.toggle_chart_mode)
        layoutChartLegend = view.findViewById(R.id.layout_chart_legend)
        layoutLineAxis = view.findViewById(R.id.layout_line_axis)
        textAxisStart = view.findViewById(R.id.text_axis_start)
        textAxisMid = view.findViewById(R.id.text_axis_mid)
        textAxisEnd = view.findViewById(R.id.text_axis_end)
        fabAdd = view.findViewById(R.id.fab_add)
        layoutModeSelector = view.findViewById(R.id.layout_mode_selector)
        layoutStatisticsContent = view.findViewById(R.id.layout_statistics_content)
        layoutRecordsContent = view.findViewById(R.id.layout_records_content)
        layoutRangeSelector = view.findViewById(R.id.layout_range_selector)

        databaseHelper = DatabaseHelper(requireContext())
        recyclerStatistics.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())
        statisticsAdapter = StatisticsAdapter()
        recyclerStatistics.adapter = statisticsAdapter

        view.findViewById<ImageButton>(R.id.btn_ledger_menu).isEnabled = false
        toggleChartMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentChartMode = if (checkedId == R.id.btn_chart_line) CHART_MODE_LINE else CHART_MODE_PIE
            updateChartModeUi()
        }

        togglePeriodPreset.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentPeriodPreset = when (checkedId) {
                R.id.btn_period_week -> LedgerPeriodPreset.WEEK
                R.id.btn_period_year -> LedgerPeriodPreset.YEAR
                R.id.btn_period_all -> LedgerPeriodPreset.ALL
                else -> LedgerPeriodPreset.MONTH
            }
            currentRange = resolveRangeFromPreset(currentPeriodPreset)
            renderCurrentView()
        }

        layoutModeSelector.setOnClickListener { showModeMenu() }
        layoutRangeSelector.setOnClickListener { showCustomPeriodSheet() }
        fabAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddRecordFragment())
                .addToBackStack(null)
                .commit()
        }

        requireActivity().findViewById<View>(R.id.nav_shell)?.let { navShell ->
            FloatingNavLayoutHelper.applyFabGapAboveBottomNav(fabAdd, navShell)
        }

        togglePeriodPreset.check(R.id.btn_period_month)
        toggleChartMode.check(R.id.btn_chart_pie)

        if (currentPeriodPreset == LedgerPeriodPreset.CUSTOM) {
            clearTopPeriodToggleSelection()
        }

        if (currentPeriodPreset == LedgerPeriodPreset.CUSTOM || currentPeriodPreset == LedgerPeriodPreset.ALL) {
            toggleChartMode.visibility = View.GONE
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        renderCurrentView()
    }

    private fun renderCurrentView() {
        updateAllRangeIfNeeded()
        updateChrome()
        if (currentViewMode == VIEW_MODE_RECORDS) {
            loadRecordDetails()
        } else {
            loadCategoryStatistics()
        }
    }

    private fun updateChrome() {
        layoutStatisticsContent.visibility = if (currentViewMode == VIEW_MODE_STATISTICS) View.VISIBLE else View.GONE
        layoutRecordsContent.visibility = if (currentViewMode == VIEW_MODE_RECORDS) View.VISIBLE else View.GONE
        textViewModeHint.text = if (currentViewMode == VIEW_MODE_STATISTICS) {
            if (currentStatsType == TYPE_EXPENSE) getString(R.string.ledger_mode_statistics_expense)
            else getString(R.string.ledger_mode_statistics_income)
        } else {
            getString(R.string.ledger_view_records)
        }
        textRangeValue.text = when {
            currentPeriodPreset == LedgerPeriodPreset.ALL -> getString(R.string.ledger_period_all_range)
            currentRange.startDate.isNullOrBlank() || currentRange.endDate.isNullOrBlank() -> getString(R.string.ledger_period_all_range)
            else -> LedgerPeriodHelper.formatRangeLabel(currentRange)
        }
        updateToggleVisuals()
        updateChartModeUi()
    }

    private fun loadCategoryStatistics() {
        val range = currentQueryRange()
        val expenseTotal = if (range == null) {
            databaseHelper.getTotalByType(TYPE_EXPENSE)
        } else {
            databaseHelper.getTotalByTypeAndDateRange(TYPE_EXPENSE, range.startDate!!, range.endDate!!)
        }
        val incomeTotal = if (range == null) {
            databaseHelper.getTotalByType(TYPE_INCOME)
        } else {
            databaseHelper.getTotalByTypeAndDateRange(TYPE_INCOME, range.startDate!!, range.endDate!!)
        }
        val stats = if (range == null) {
            databaseHelper.getCategoryStatistics(currentStatsType)
        } else {
            databaseHelper.getCategoryStatisticsByDateRange(currentStatsType, range.startDate!!, range.endDate!!)
        }
        val categoryRecords = filteredRecordsForType(range, currentStatsType)
        val entryCounts = categoryRecords.groupingBy { it.category }.eachCount()

        val normalizedStats = stats.mapKeys { (label, _) ->
            if (currentStatsType == TYPE_EXPENSE) {
                getString(R.string.statistics_expense_prefix, label)
            } else {
                getString(R.string.statistics_income_prefix, label)
            }
        }.mapValues { (_, amount) ->
            if (currentStatsType == TYPE_EXPENSE) -amount else amount
        }

        textSummaryLabel.text = if (currentStatsType == TYPE_EXPENSE) {
            getString(R.string.ledger_summary_total_expense)
        } else {
            getString(R.string.ledger_summary_total_income)
        }
        textSummaryAmount.text = getString(
            R.string.currency_amount,
            if (currentStatsType == TYPE_EXPENSE) expenseTotal else incomeTotal
        )
        textSummarySupporting.text = getString(R.string.ledger_summary_supporting_statistics)
        textSummaryBadge.text = getString(R.string.ledger_summary_badge_count, normalizedStats.size)
        textSummaryIncome.text = getString(R.string.currency_amount, incomeTotal)
        textSummaryExpense.text = getString(R.string.currency_amount, expenseTotal)
        textStatisticsSectionTitle.text = if (currentStatsType == TYPE_EXPENSE) {
            getString(R.string.ledger_statistics_section_title)
        } else {
            getString(R.string.record_type_income) + " · " + getString(R.string.ledger_statistics_section_title)
        }
        textStatisticsInsight.text = if (currentStatsType == TYPE_EXPENSE) {
            getString(R.string.ledger_statistics_insight_expense)
        } else {
            getString(R.string.ledger_statistics_insight_income)
        }

        if (normalizedStats.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerStatistics.visibility = View.GONE
            textEmpty.text = getString(R.string.ledger_empty_statistics)
            layoutChartLegend.removeAllViews()
            viewStatisticsChart.submitData(
                emptyList(),
                getString(R.string.ledger_chart_total_label),
                getString(R.string.currency_amount, 0.0)
            )
            viewStatisticsLineChart.submitData(emptyList())
            bindLineAxis(emptyList())
        } else {
            textEmpty.visibility = View.GONE
            recyclerStatistics.visibility = View.VISIBLE
            statisticsAdapter.updateData(normalizedStats, entryCounts)
            bindChart(normalizedStats, entryCounts, categoryRecords)
        }
    }

    private fun loadRecordDetails() {
        val range = currentQueryRange()
        val records = if (range == null) {
            databaseHelper.getAllRecords()
        } else {
            databaseHelper.getRecordsByDateRange(range.startDate!!, range.endDate!!)
        }

        if (records.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerRecords.visibility = View.GONE
            textEmpty.text = getString(R.string.ledger_empty_records)
        } else {
            textEmpty.visibility = View.GONE
            recyclerRecords.visibility = View.VISIBLE
            val dateGroups = groupRecordsByDate(records)
            if (recordsAdapter == null) {
                recordsAdapter = DateGroupAdapter(dateGroups, object : DateGroupAdapter.OnRecordActionListener {
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

                    override fun onMultiSelectChanged(selectedCount: Int) {}
                    override fun onDeleteSelected(records: List<Record>) {}
                    override fun onEnterMultiSelectMode(record: Record) {}
                    override fun onToggleMultiSelect(record: Record) {}
                })
            } else {
                recordsAdapter?.updateDateGroups(dateGroups)
            }
            recyclerRecords.adapter = recordsAdapter
        }
    }

    private fun currentQueryRange(): LedgerDateRange? {
        return if (currentPeriodPreset == LedgerPeriodPreset.ALL || currentRange.startDate == null || currentRange.endDate == null) {
            null
        } else {
            currentRange
        }
    }

    private fun updateAllRangeIfNeeded() {
        if (currentPeriodPreset != LedgerPeriodPreset.ALL) {
            return
        }
        val records = databaseHelper.getAllRecords()
        currentRange = if (records.isEmpty()) {
            LedgerDateRange(null, null)
        } else {
            LedgerDateRange(records.last().date, records.first().date)
        }
    }

    private fun filteredRecordsForType(range: LedgerDateRange?, type: Int): List<Record> {
        val records = if (range == null) {
            databaseHelper.getAllRecords()
        } else {
            databaseHelper.getRecordsByDateRange(range.startDate!!, range.endDate!!)
        }
        return records.filter { it.type == type }
    }

    private fun bindChart(stats: Map<String, Double>, entryCounts: Map<String, Int>, records: List<Record>) {
        val context = requireContext()
        val palette = listOf(
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary),
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSecondary),
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorTertiary),
            ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_outline)
        )
        val topItems = stats.entries
            .sortedByDescending { (_, amount) -> kotlin.math.abs(amount) }
            .take(4)
        val total = topItems.sumOf { kotlin.math.abs(it.value) }
        val slices = topItems.mapIndexed { index, entry ->
            LedgerDonutChartView.Slice(
                value = kotlin.math.abs(entry.value).toFloat(),
                color = palette[index % palette.size]
            )
        }

        viewStatisticsChart.submitData(
            slices = slices,
            totalLabel = getString(R.string.ledger_chart_total_label),
            totalValue = getString(R.string.currency_amount, total)
        )

        val showLineChart = currentPeriodPreset == LedgerPeriodPreset.WEEK ||
                currentPeriodPreset == LedgerPeriodPreset.MONTH ||
                currentPeriodPreset == LedgerPeriodPreset.YEAR

        if (showLineChart) {
            val range = currentRange
            val filteredRecords = records.filter { record ->
                !range.startDate.isNullOrBlank() && !range.endDate.isNullOrBlank() &&
                        record.date >= range.startDate && record.date <= range.endDate
            }

            val dateLabelToAmount: Map<String, Float>
            if (currentPeriodPreset == LedgerPeriodPreset.YEAR) {
                val grouped = filteredRecords
                    .groupBy { it.date.substring(0, 7) }
                    .mapValues { (_, recs) -> recs.sumOf { kotlin.math.abs(it.amount) }.toFloat() }

                val startYear = range.startDate?.substring(0, 4)?.toIntOrNull() ?: return
                val endYear = range.endDate?.substring(0, 4)?.toIntOrNull() ?: startYear

                dateLabelToAmount = (1..12).associate { month ->
                    val monthKey = String.format("%04d-%02d", startYear, month)
                    monthKey to (grouped[monthKey] ?: 0f)
                }
            } else {
                val grouped = filteredRecords
                    .groupBy { it.date }
                    .mapValues { (_, recs) -> recs.sumOf { kotlin.math.abs(it.amount) }.toFloat() }

                val startCal = LedgerPeriodHelper.parseIsoDate(range.startDate!!)
                val endCal = LedgerPeriodHelper.parseIsoDate(range.endDate!!)
                val calendar = java.util.Calendar.getInstance().apply { time = startCal }

                val dateLabelToAmountBuilder = mutableMapOf<String, Float>()
                while (!calendar.time.after(endCal)) {
                    val isoDate = LedgerPeriodHelper.formatIsoDateForExternal(calendar.time)
                    dateLabelToAmountBuilder[isoDate] = grouped[isoDate] ?: 0f
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
                dateLabelToAmount = dateLabelToAmountBuilder
            }

            val linePoints = dateLabelToAmount.values.map { LedgerLineChartView.Point(it) }
            viewStatisticsLineChart.submitData(linePoints)
            bindLineAxis(dateLabelToAmount.keys.toList())
        } else {
            viewStatisticsLineChart.submitData(emptyList())
            bindLineAxis(emptyList())
        }

        layoutChartLegend.removeAllViews()
        topItems.forEachIndexed { index, entry ->
            val legendView = layoutInflater.inflate(R.layout.item_ledger_chart_legend, layoutChartLegend, false)
            val dot = legendView.findViewById<View>(R.id.view_legend_dot)
            val label = legendView.findViewById<TextView>(R.id.text_legend_label)
            val value = legendView.findViewById<TextView>(R.id.text_legend_value)
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(palette[index % palette.size])

            val normalizedLabel = entry.key.substringAfter(": ", entry.key).substringAfter("· ", entry.key)
            label.text = normalizedLabel.uppercase() + "  ·  " + LedgerDisplayHelper.formatEntriesMeta(entryCounts[normalizedLabel] ?: 0)
            value.text = getString(R.string.currency_amount, kotlin.math.abs(entry.value))
            layoutChartLegend.addView(legendView)
        }

        updateChartModeUi()
    }

    private fun updateChartModeUi() {
        val isPie = currentChartMode == CHART_MODE_PIE
        val showLineChart = currentPeriodPreset == LedgerPeriodPreset.WEEK ||
                currentPeriodPreset == LedgerPeriodPreset.MONTH ||
                currentPeriodPreset == LedgerPeriodPreset.YEAR

        viewStatisticsChart.visibility = if (isPie && currentViewMode == VIEW_MODE_STATISTICS) View.VISIBLE else View.GONE
        layoutChartLegend.visibility = if (isPie && currentViewMode == VIEW_MODE_STATISTICS) View.VISIBLE else View.GONE
        viewStatisticsLineChart.visibility = if (!isPie && currentViewMode == VIEW_MODE_STATISTICS && showLineChart) View.VISIBLE else View.GONE
        layoutLineAxis.visibility = if (!isPie && currentViewMode == VIEW_MODE_STATISTICS && showLineChart) View.VISIBLE else View.GONE
        textChartSubtitle.text = if (isPie) {
            getString(R.string.ledger_chart_subtitle_pie)
        } else {
            getString(R.string.ledger_chart_subtitle_line)
        }

        updateToggleButtonState(toggleChartMode.findViewById(R.id.btn_chart_line), currentChartMode == CHART_MODE_LINE)
        updateToggleButtonState(toggleChartMode.findViewById(R.id.btn_chart_pie), currentChartMode == CHART_MODE_PIE)
        val showChartToggle = currentViewMode == VIEW_MODE_STATISTICS &&
                currentPeriodPreset != LedgerPeriodPreset.CUSTOM &&
                currentPeriodPreset != LedgerPeriodPreset.ALL
        toggleChartMode.visibility = if (showChartToggle) View.VISIBLE else View.INVISIBLE
        toggleChartMode.isEnabled = showChartToggle
        toggleChartMode.findViewById<MaterialButton>(R.id.btn_chart_line).isEnabled = showChartToggle
        toggleChartMode.findViewById<MaterialButton>(R.id.btn_chart_pie).isEnabled = showChartToggle
    }

    private fun bindLineAxis(dates: List<String>) {
        if (dates.isEmpty()) {
            textAxisStart.text = ""
            textAxisMid.text = ""
            textAxisEnd.text = ""
            return
        }
        val middleIndex = dates.size / 2
        val formattedStart = if (currentPeriodPreset == LedgerPeriodPreset.YEAR) {
            LedgerDisplayHelper.formatMonthLabel(dates.first())
        } else {
            LedgerDisplayHelper.formatDayOnlyLabel(dates.first())
        }
        val formattedMid = if (currentPeriodPreset == LedgerPeriodPreset.YEAR) {
            LedgerDisplayHelper.formatMonthLabel(dates[middleIndex])
        } else {
            LedgerDisplayHelper.formatDayOnlyLabel(dates[middleIndex])
        }
        val formattedEnd = if (currentPeriodPreset == LedgerPeriodPreset.YEAR) {
            LedgerDisplayHelper.formatMonthLabel(dates.last())
        } else {
            LedgerDisplayHelper.formatDayOnlyLabel(dates.last())
        }
        textAxisStart.text = formattedStart
        textAxisMid.text = formattedMid
        textAxisEnd.text = formattedEnd
    }

    private fun showModeMenu() {
        val popupMenu = PopupMenu(requireContext(), layoutModeSelector)
        popupMenu.menu.add(0, MENU_MODE_STATS_EXPENSE, 0, getString(R.string.ledger_mode_statistics_expense))
        popupMenu.menu.add(0, MENU_MODE_STATS_INCOME, 1, getString(R.string.ledger_mode_statistics_income))
        popupMenu.menu.add(0, MENU_MODE_RECORDS, 2, getString(R.string.ledger_view_records))
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_MODE_STATS_EXPENSE -> {
                    currentViewMode = VIEW_MODE_STATISTICS
                    currentStatsType = TYPE_EXPENSE
                }

                MENU_MODE_STATS_INCOME -> {
                    currentViewMode = VIEW_MODE_STATISTICS
                    currentStatsType = TYPE_INCOME
                }

                MENU_MODE_RECORDS -> {
                    currentViewMode = VIEW_MODE_RECORDS
                }

                else -> return@setOnMenuItemClickListener false
            }
            renderCurrentView()
            true
        }
        popupMenu.show()
    }

    private fun resolveRangeFromPreset(preset: LedgerPeriodPreset): LedgerDateRange {
        return if (preset == LedgerPeriodPreset.ALL) {
            resolveAllRange()
        } else {
            LedgerPeriodHelper.resolveRange(preset)
        }
    }

    private fun showCustomPeriodSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_ledger_period, null)
        dialog.setContentView(sheetView)

        val textSelectionLabel = sheetView.findViewById<TextView>(R.id.text_period_selection_label)
        val textSheetHint = sheetView.findViewById<TextView>(R.id.text_period_sheet_hint)
        val layoutCalendarSection = sheetView.findViewById<View>(R.id.layout_period_calendar_section)
        val monthTitle = sheetView.findViewById<TextView>(R.id.text_period_month_title)
        val selectionValue = sheetView.findViewById<TextView>(R.id.text_period_selection_value)
        val recyclerCalendar = sheetView.findViewById<RecyclerView>(R.id.recycler_period_calendar)
        val btnPrevMonth = sheetView.findViewById<ImageButton>(R.id.btn_period_prev_month)
        val btnNextMonth = sheetView.findViewById<ImageButton>(R.id.btn_period_next_month)
        val btnApply = sheetView.findViewById<MaterialButton>(R.id.btn_period_apply)
        val btnClear = sheetView.findViewById<MaterialButton>(R.id.btn_period_clear)

        var localPreset = LedgerPeriodPreset.CUSTOM
        var localRange = currentRange
        val baseDate = localRange.startDate ?: databaseHelper.getCurrentDate()
        var displayYear = baseDate.substring(0, 4).toInt()
        var displayMonth = baseDate.substring(5, 7).toInt() - 1
        var tempStart = localRange.startDate
        var tempEnd = localRange.endDate

        lateinit var adapter: LedgerCalendarAdapter
        adapter = LedgerCalendarAdapter { day ->
            val selectedDate = day.isoDate ?: return@LedgerCalendarAdapter
            localPreset = LedgerPeriodPreset.CUSTOM
            if (tempStart == null || tempEnd != null) {
                tempStart = selectedDate
                tempEnd = null
            } else {
                val normalized = LedgerPeriodHelper.normalizeRange(tempStart, selectedDate)
                tempStart = normalized.startDate
                tempEnd = normalized.endDate
            }
            localRange = LedgerDateRange(tempStart, tempEnd)
            renderPeriodSheet(
                selectionLabel = textSelectionLabel,
                sheetHint = textSheetHint,
                calendarSection = layoutCalendarSection,
                monthTitle = monthTitle,
                selectionValue = selectionValue,
                adapter = adapter,
                displayYear = displayYear,
                displayMonth = displayMonth,
                selectedRange = localRange,
                preset = localPreset,
                canApply = canApplyPeriodSelection(localPreset, localRange),
                btnApply = btnApply
            )
        }

        recyclerCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        recyclerCalendar.adapter = adapter

        renderPeriodSheet(
            selectionLabel = textSelectionLabel,
            sheetHint = textSheetHint,
            calendarSection = layoutCalendarSection,
            monthTitle = monthTitle,
            selectionValue = selectionValue,
            adapter = adapter,
            displayYear = displayYear,
            displayMonth = displayMonth,
            selectedRange = localRange,
            preset = localPreset,
            canApply = canApplyPeriodSelection(localPreset, localRange),
            btnApply = btnApply
        )

        sheetView.findViewById<ImageButton>(R.id.btn_close_period_sheet).setOnClickListener {
            dialog.dismiss()
        }
        btnPrevMonth.setOnClickListener {
            val shifted = LedgerPeriodHelper.shiftMonth(displayYear, displayMonth, -1)
            displayYear = shifted.first
            displayMonth = shifted.second
            renderPeriodSheet(
                selectionLabel = textSelectionLabel,
                sheetHint = textSheetHint,
                calendarSection = layoutCalendarSection,
                monthTitle,
                selectionValue,
                adapter,
                displayYear,
                displayMonth,
                localRange,
                localPreset,
                canApplyPeriodSelection(localPreset, localRange),
                btnApply
            )
        }
        btnNextMonth.setOnClickListener {
            val shifted = LedgerPeriodHelper.shiftMonth(displayYear, displayMonth, 1)
            displayYear = shifted.first
            displayMonth = shifted.second
            renderPeriodSheet(
                selectionLabel = textSelectionLabel,
                sheetHint = textSheetHint,
                calendarSection = layoutCalendarSection,
                monthTitle,
                selectionValue,
                adapter,
                displayYear,
                displayMonth,
                localRange,
                localPreset,
                canApplyPeriodSelection(localPreset, localRange),
                btnApply
            )
        }
        btnClear.setOnClickListener {
            localPreset = LedgerPeriodPreset.CUSTOM
            tempStart = null
            tempEnd = null
            localRange = LedgerDateRange(null, null)
            renderPeriodSheet(
                selectionLabel = textSelectionLabel,
                sheetHint = textSheetHint,
                calendarSection = layoutCalendarSection,
                monthTitle,
                selectionValue,
                adapter,
                displayYear,
                displayMonth,
                localRange,
                localPreset,
                false,
                btnApply
            )
        }
        btnApply.setOnClickListener {
            if (!canApplyPeriodSelection(localPreset, localRange)) return@setOnClickListener
            currentPeriodPreset = localPreset
            currentRange = if (localPreset == LedgerPeriodPreset.CUSTOM) {
                LedgerPeriodHelper.normalizeRange(tempStart, tempEnd)
            } else {
                localRange
            }
            if (localPreset == LedgerPeriodPreset.CUSTOM || localPreset == LedgerPeriodPreset.ALL) {
                currentChartMode = CHART_MODE_PIE
                toggleChartMode.visibility = View.GONE
            } else {
                toggleChartMode.visibility = View.VISIBLE
            }
            if (localPreset == LedgerPeriodPreset.CUSTOM) {
                clearTopPeriodToggleSelection()
            } else {
                checkPeriodToggle(togglePeriodPreset, localPreset, false)
            }
            dialog.dismiss()
            renderCurrentView()
        }

        renderPeriodSheet(
            selectionLabel = textSelectionLabel,
            sheetHint = textSheetHint,
            calendarSection = layoutCalendarSection,
            monthTitle,
            selectionValue,
            adapter,
            displayYear,
            displayMonth,
            localRange,
            localPreset,
            canApplyPeriodSelection(localPreset, localRange),
            btnApply
        )
        dialog.show()
    }

    private fun renderPeriodSheet(
        selectionLabel: TextView,
        sheetHint: TextView,
        calendarSection: View,
        monthTitle: TextView,
        selectionValue: TextView,
        adapter: LedgerCalendarAdapter,
        displayYear: Int,
        displayMonth: Int,
        selectedRange: LedgerDateRange,
        preset: LedgerPeriodPreset,
        canApply: Boolean,
        btnApply: MaterialButton
    ) {
        val isCustom = preset == LedgerPeriodPreset.CUSTOM
        selectionLabel.text = getPeriodPresetLabel(preset)
        sheetHint.visibility = if (isCustom) View.VISIBLE else View.GONE
        calendarSection.visibility = if (isCustom) View.VISIBLE else View.GONE
        monthTitle.text = LedgerPeriodHelper.formatMonthTitle(displayYear, displayMonth)
        selectionValue.text = when {
            preset == LedgerPeriodPreset.ALL -> getString(R.string.ledger_period_all_range)
            selectedRange.startDate == null -> getString(R.string.ledger_period_all_range)
            selectedRange.endDate == null -> LedgerPeriodHelper.formatSingleDateLabel(selectedRange.startDate)
            else -> LedgerPeriodHelper.formatRangeLabel(selectedRange)
        }
        adapter.submitList(
            LedgerPeriodHelper.buildMonthCells(
                year = displayYear,
                month = displayMonth,
                selectedRange = selectedRange
            )
        )
        btnApply.isEnabled = canApply
        btnApply.alpha = if (canApply) 1f else 0.5f
    }

    private fun canApplyPeriodSelection(preset: LedgerPeriodPreset, range: LedgerDateRange): Boolean {
        return when (preset) {
            LedgerPeriodPreset.CUSTOM -> !range.startDate.isNullOrBlank() && !range.endDate.isNullOrBlank()
            else -> true
        }
    }

    private fun getPeriodPresetLabel(preset: LedgerPeriodPreset): String {
        return when (preset) {
            LedgerPeriodPreset.WEEK -> getString(R.string.ledger_period_label_week)
            LedgerPeriodPreset.MONTH -> getString(R.string.ledger_period_label_month)
            LedgerPeriodPreset.YEAR -> getString(R.string.ledger_period_label_year)
            LedgerPeriodPreset.ALL -> getString(R.string.ledger_period_label_all)
            LedgerPeriodPreset.CUSTOM -> getString(R.string.ledger_period_label_custom)
        }
    }

    private fun checkPeriodToggle(
        group: MaterialButtonToggleGroup,
        preset: LedgerPeriodPreset,
        isSheet: Boolean
    ) {
        if (!isSheet && preset == LedgerPeriodPreset.CUSTOM) {
            clearTopPeriodToggleSelection()
            return
        }
        val checkedId = when (preset) {
            LedgerPeriodPreset.WEEK -> if (isSheet) -1 else R.id.btn_period_week
            LedgerPeriodPreset.MONTH -> if (isSheet) -1 else R.id.btn_period_month
            LedgerPeriodPreset.YEAR -> if (isSheet) -1 else R.id.btn_period_year
            LedgerPeriodPreset.ALL -> if (isSheet) -1 else R.id.btn_period_all
            LedgerPeriodPreset.CUSTOM -> -1
        }
        if (checkedId != -1) {
            group.check(checkedId)
        }
    }

    private fun clearTopPeriodToggleSelection() {
        togglePeriodPreset.uncheck(R.id.btn_period_week)
        togglePeriodPreset.uncheck(R.id.btn_period_month)
        togglePeriodPreset.uncheck(R.id.btn_period_year)
        togglePeriodPreset.uncheck(R.id.btn_period_all)
    }

    private fun updateToggleVisuals() {
        updatePeriodToggleStyles(togglePeriodPreset, false)
    }

    private fun updateToggleButtonState(button: MaterialButton, selected: Boolean) {
        val context = button.context
        val background = if (selected) {
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSecondaryContainer)
        } else {
            ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_container_high)
        }
        val textColor = if (selected) {
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer)
        } else {
            ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_text_muted)
        }
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(background)
        button.setTextColor(textColor)
    }

    private fun groupRecordsByDate(records: List<Record>): List<DateGroup> {
        return records.groupBy { it.date }
            .map { (date, groupedRecords) -> DateGroup(date, groupedRecords) }
            .sortedByDescending { it.date }
    }

    private fun resolveAllRange(): LedgerDateRange {
        val records = databaseHelper.getAllRecords()
        return if (records.isEmpty()) {
            LedgerDateRange(null, null)
        } else {
            LedgerDateRange(records.last().date, records.first().date)
        }
    }

    private fun updatePeriodToggleStyles(group: MaterialButtonToggleGroup, isSheet: Boolean) {
        if (isSheet) return
        val buttonIds = listOf(
            R.id.btn_period_week,
            R.id.btn_period_month,
            R.id.btn_period_year,
            R.id.btn_period_all
        )
        buttonIds.forEach { buttonId ->
            updateToggleButtonState(group.findViewById(buttonId), group.checkedButtonId == buttonId)
        }
    }

    private fun showDeleteDialog(record: Record) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_record_title)
            .setMessage(R.string.delete_record_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                databaseHelper.deleteRecord(record.id)
                Toast.makeText(requireContext(), R.string.toast_delete_success, Toast.LENGTH_SHORT).show()
                renderCurrentView()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
