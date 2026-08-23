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
import com.example.cardtally.state.ChartMode
import com.example.cardtally.state.FilterSurface
import com.example.cardtally.state.LedgerScreenState
import com.example.cardtally.state.LedgerViewState
import com.example.cardtally.state.PeriodPreset
import com.example.cardtally.state.StatisticsType
import com.example.cardtally.util.FloatingNavLayoutHelper
import com.example.cardtally.util.LedgerAggregationHelper
import com.example.cardtally.util.LedgerDateRange
import com.example.cardtally.util.LedgerDisplayHelper
import com.example.cardtally.util.LedgerPeriodHelper
import com.example.cardtally.util.LedgerPeriodPreset
import com.example.cardtally.util.LedgerUxPreferences
import com.example.cardtally.util.LedgerView
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.view.LedgerDonutChartView
import com.example.cardtally.view.LedgerLineChartView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.util.Calendar

class StatisticsFragment : Fragment() {
    private lateinit var togglePeriodPreset: MaterialButtonToggleGroup
    private lateinit var textEmpty: TextView
    private lateinit var textRangeValue: TextView
    private lateinit var textTypeExpense: MaterialButton
    private lateinit var textTypeIncome: MaterialButton
    private lateinit var indicatorTypeExpense: View
    private lateinit var indicatorTypeIncome: View
    private lateinit var textViewModeHint: TextView
    private lateinit var textSummaryExpense: TextView
    private lateinit var textSummaryIncome: TextView
    private lateinit var textSummaryBalance: TextView
    private lateinit var textChartSubtitle: TextView
    private lateinit var recyclerStatistics: RecyclerView
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var viewStatisticsChart: LedgerDonutChartView
    private lateinit var viewStatisticsLineChart: LedgerLineChartView
    private lateinit var toggleChartMode: ImageButton
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
    private lateinit var textChartTitle: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var statisticsAdapter: StatisticsAdapter
    private var recordsAdapter: DateGroupAdapter? = null

    private var currentViewMode = VIEW_MODE_STATISTICS
    private var currentStatsType = TYPE_EXPENSE
    private var currentPeriodPreset = LedgerPeriodPreset.WEEK
    private var currentRange = LedgerPeriodHelper.resolveRange(LedgerPeriodPreset.WEEK)
    private var currentChartMode = CHART_MODE_LINE
    private var openFilterSurface = FilterSurface.NONE
    private var restoredLedgerState: LedgerScreenState? = null

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
        textTypeExpense = view.findViewById(R.id.text_type_expense)
        textTypeIncome = view.findViewById(R.id.text_type_income)
        indicatorTypeExpense = view.findViewById(R.id.indicator_type_expense)
        indicatorTypeIncome = view.findViewById(R.id.indicator_type_income)
        textViewModeHint = view.findViewById(R.id.text_view_mode_hint)
        textSummaryExpense = view.findViewById(R.id.text_summary_expense)
        textSummaryIncome = view.findViewById(R.id.text_summary_income)
        textSummaryBalance = view.findViewById(R.id.text_summary_balance)
        textStatisticsSectionTitle = view.findViewById(R.id.text_statistics_section_title)
        textChartTitle = view.findViewById(R.id.text_chart_title)
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
        val btnRangePrevious = view.findViewById<ImageButton>(R.id.btn_range_previous)
        val btnRangeNext = view.findViewById<ImageButton>(R.id.btn_range_next)

        databaseHelper = DatabaseHelper(requireContext())
        restoredLedgerState = LedgerScreenState.readFrom(savedInstanceState, resolveStartupView())
        applyLedgerState(restoredLedgerState!!)
        currentViewMode = VIEW_MODE_STATISTICS
        layoutModeSelector.visibility = View.GONE
        view.findViewById<View>(R.id.layout_statistics_legacy_header).visibility = View.GONE
        view.findViewById<ImageButton>(R.id.btn_ledger_menu).visibility = View.GONE
        textTypeExpense.setOnClickListener {
            if (currentStatsType != TYPE_EXPENSE) {
                currentStatsType = TYPE_EXPENSE
                renderCurrentView()
            }
        }
        textTypeIncome.setOnClickListener {
            if (currentStatsType != TYPE_INCOME) {
                currentStatsType = TYPE_INCOME
                renderCurrentView()
            }
        }
        btnRangePrevious.setOnClickListener { shiftCurrentRange(-1) }
        btnRangeNext.setOnClickListener { shiftCurrentRange(1) }
        recyclerStatistics.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())
        statisticsAdapter = StatisticsAdapter { category ->
            val searchFragment = SearchFragment.newInstance(
                keyword = category,
                rangeStart = currentQueryRange()?.startDate,
                rangeEnd = currentQueryRange()?.endDate
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, searchFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerStatistics.adapter = statisticsAdapter

        view.findViewById<ImageButton>(R.id.btn_ledger_menu).isEnabled = false
        toggleChartMode.setOnClickListener {
            currentChartMode = if (currentChartMode == CHART_MODE_LINE) CHART_MODE_PIE else CHART_MODE_LINE
            updateChartModeUi()
        }

        togglePeriodPreset.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == R.id.btn_period_all) {
                if (currentPeriodPreset == LedgerPeriodPreset.CUSTOM) return@addOnButtonCheckedListener
                clearTopPeriodToggleSelection()
                currentPeriodPreset = LedgerPeriodPreset.CUSTOM
                currentRange = LedgerDateRange(null, null)
                currentChartMode = CHART_MODE_PIE
                renderCurrentView()
                return@addOnButtonCheckedListener
            }
            currentPeriodPreset = when (checkedId) {
                R.id.btn_period_week -> LedgerPeriodPreset.WEEK
                R.id.btn_period_year -> LedgerPeriodPreset.YEAR
                else -> LedgerPeriodPreset.MONTH
            }
            currentRange = resolveRangeFromPreset(currentPeriodPreset)
            currentChartMode = CHART_MODE_LINE
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

        checkPeriodToggle(togglePeriodPreset, currentPeriodPreset, false)
        if (currentPeriodPreset == LedgerPeriodPreset.CUSTOM) {
            clearTopPeriodToggleSelection()
        }

        if (currentPeriodPreset == LedgerPeriodPreset.CUSTOM || currentPeriodPreset == LedgerPeriodPreset.ALL) {
            toggleChartMode.visibility = View.GONE
        }

        view.post {
            restoreScrollPositions()
            if (openFilterSurface == FilterSurface.PERIOD) showCustomPeriodSheet()
        }
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentLedgerState().writeTo(outState)
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
        textChartTitle.text = getString(
            if (currentStatsType == TYPE_EXPENSE) R.string.ledger_chart_title_expense
            else R.string.ledger_chart_title_income
        )
        updateToggleVisuals()
        updateChartModeUi()
        updateTypeTabs()
    }

    private fun updateTypeTabs() {
        val active = ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnSurface)
        val inactive = ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.editorial_text_muted)
        val background = ThemeColorHelper.resolveThemeAwareResource(
            requireContext(),
            R.color.surface_light
        )
        textTypeExpense.setTextColor(if (currentStatsType == TYPE_EXPENSE) active else inactive)
        textTypeIncome.setTextColor(if (currentStatsType == TYPE_INCOME) active else inactive)
        textTypeExpense.backgroundTintList = android.content.res.ColorStateList.valueOf(
            background
        )
        textTypeIncome.backgroundTintList = android.content.res.ColorStateList.valueOf(
            background
        )
        indicatorTypeExpense.visibility = if (currentStatsType == TYPE_EXPENSE) View.VISIBLE else View.GONE
        indicatorTypeIncome.visibility = if (currentStatsType == TYPE_INCOME) View.VISIBLE else View.GONE
        textTypeExpense.paint.isFakeBoldText = currentStatsType == TYPE_EXPENSE
        textTypeIncome.paint.isFakeBoldText = currentStatsType == TYPE_INCOME
    }

    private fun shiftCurrentRange(direction: Int) {
        if (currentPeriodPreset == LedgerPeriodPreset.ALL || currentPeriodPreset == LedgerPeriodPreset.CUSTOM) {
            showCustomPeriodSheet()
            return
        }
        val start = currentRange.startDate ?: return
        val baseMillis = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .parse(start)?.time ?: return
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = baseMillis
            when (currentPeriodPreset) {
                LedgerPeriodPreset.MONTH -> add(java.util.Calendar.MONTH, direction)
                LedgerPeriodPreset.YEAR -> add(java.util.Calendar.YEAR, direction)
                LedgerPeriodPreset.WEEK -> add(java.util.Calendar.DAY_OF_YEAR, direction * 7)
                else -> Unit
            }
        }
        currentRange = LedgerPeriodHelper.resolveRange(currentPeriodPreset, calendar.timeInMillis)
        renderCurrentView()
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
        // Keep aliases for legacy records: current records use `category`, while
        // older records may only have the category name snapshot populated.
        // The statistics query groups by `category`, so either value must resolve
        // to the same visible category when the count is rendered.
        val entryCounts = mutableMapOf<String, Int>()
        categoryRecords.forEach { record ->
            setOfNotNull(
                record.category.takeIf { it.isNotBlank() },
                record.categoryNameSnapshot?.takeIf { it.isNotBlank() }
            ).forEach { categoryName ->
                entryCounts[categoryName] = (entryCounts[categoryName] ?: 0) + 1
            }
        }
        val categoryIcons = databaseHelper.getAllCategories()
            .filter { !it.icon.isNullOrBlank() }
            .associate { it.name to it.icon!! }

        val normalizedStats = stats.mapKeys { (label, _) ->
            if (currentStatsType == TYPE_EXPENSE) {
                getString(R.string.statistics_expense_prefix, label)
            } else {
                getString(R.string.statistics_income_prefix, label)
            }
        }.mapValues { (_, amount) ->
            if (currentStatsType == TYPE_EXPENSE) -amount else amount
        }

        textSummaryExpense.text = getString(R.string.currency_amount, expenseTotal)
        textSummaryIncome.text = getString(R.string.currency_amount, incomeTotal)
        textSummaryBalance.text = getString(
            R.string.currency_amount,
            incomeTotal - expenseTotal
        )
        textStatisticsSectionTitle.text = if (currentStatsType == TYPE_EXPENSE) {
            getString(R.string.ledger_statistics_section_title)
        } else {
            getString(R.string.ledger_statistics_section_title)
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
            statisticsAdapter.updateData(normalizedStats, entryCounts, categoryIcons)
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
        val summary = LedgerAggregationHelper.summarize(
            statistics = stats,
            maxSlices = palette.size,
            otherLabel = getString(R.string.ledger_chart_other)
        )
        val slices = summary.slices.mapIndexed { index, slice ->
            LedgerDonutChartView.Slice(
                value = slice.amount.toFloat(),
                color = palette[index % palette.size],
                label = slice.label.substringAfter(": ", slice.label).substringAfter("· ", slice.label)
            )
        }

        viewStatisticsChart.submitData(
            slices = slices,
            totalLabel = getString(R.string.ledger_chart_total_label),
            totalValue = getString(R.string.currency_amount, summary.completeTotal)
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
        summary.slices.forEachIndexed { index, slice ->
            val legendView = layoutInflater.inflate(R.layout.item_ledger_chart_legend, layoutChartLegend, false)
            val dot = legendView.findViewById<View>(R.id.view_legend_dot)
            val label = legendView.findViewById<TextView>(R.id.text_legend_label)
            val value = legendView.findViewById<TextView>(R.id.text_legend_value)
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(palette[index % palette.size])

            val entryCount = slice.sourceLabels.sumOf { sourceLabel ->
                val normalizedLabel = sourceLabel.substringAfter(": ", sourceLabel).substringAfter("· ", sourceLabel)
                entryCounts[normalizedLabel] ?: 0
            }
            val displayLabel = slice.label.substringAfter(": ", slice.label).substringAfter("· ", slice.label)
            label.text = displayLabel.uppercase() + "  ·  " + LedgerDisplayHelper.formatEntriesMeta(entryCount)
            value.text = getString(R.string.currency_amount, slice.amount)
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
        layoutChartLegend.visibility = View.GONE
        viewStatisticsLineChart.visibility = if (!isPie && currentViewMode == VIEW_MODE_STATISTICS && showLineChart) View.VISIBLE else View.GONE
        layoutLineAxis.visibility = if (!isPie && currentViewMode == VIEW_MODE_STATISTICS && showLineChart) View.VISIBLE else View.GONE
        textChartSubtitle.text = if (isPie) {
            getString(R.string.ledger_chart_subtitle_pie)
        } else {
            getString(R.string.ledger_chart_subtitle_line)
        }

        toggleChartMode.setImageResource(
            if (isPie) R.drawable.ms_rounded_pie_chart else R.drawable.ms_rounded_multiline_chart
        )
        val showChartToggle = currentViewMode == VIEW_MODE_STATISTICS &&
                currentPeriodPreset != LedgerPeriodPreset.CUSTOM &&
                currentPeriodPreset != LedgerPeriodPreset.ALL
        toggleChartMode.visibility = if (showChartToggle) View.VISIBLE else View.INVISIBLE
        toggleChartMode.isEnabled = showChartToggle
    }

    private fun bindLineAxis(dates: List<String>) {
        layoutLineAxis.removeAllViews()
        if (dates.isEmpty()) {
            return
        }

        val labels = when (currentPeriodPreset) {
            LedgerPeriodPreset.WEEK -> dates.map(::formatWeekAxisLabel)
            LedgerPeriodPreset.MONTH -> {
                val lastDay = dates.size
                listOf(1, 5, 10, 15, 20, 25, lastDay)
                    .distinct()
                    .filter { it in 1..lastDay }
                    .map(Int::toString)
            }
            LedgerPeriodPreset.YEAR -> dates.map { "${it.substring(5, 7).toInt()}月" }
            else -> emptyList()
        }

        labels.forEach { labelText ->
            val label = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
                text = labelText
                setTextColor(ThemeColorHelper.resolveThemeAwareResource(this@StatisticsFragment.requireContext(), R.color.editorial_text_muted))
                textSize = 9f
                maxLines = 1
            }
            layoutLineAxis.addView(label)
        }
    }

    private fun formatWeekAxisLabel(date: String): String {
        val calendar = Calendar.getInstance().apply {
            time = LedgerPeriodHelper.parseIsoDate(date)
        }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> "周日"
        }
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
            LedgerUxPreferences.saveLastView(requireContext(), currentLedgerViewPreference())
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
        openFilterSurface = FilterSurface.PERIOD
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

        dialog.setOnDismissListener { openFilterSurface = FilterSurface.NONE }
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
                checkPeriodToggle(togglePeriodPreset, LedgerPeriodPreset.CUSTOM, false)
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
        dialog.setOnShowListener {
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
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
        val checkedId = when (preset) {
            LedgerPeriodPreset.WEEK -> if (isSheet) -1 else R.id.btn_period_week
            LedgerPeriodPreset.MONTH -> if (isSheet) -1 else R.id.btn_period_month
            LedgerPeriodPreset.YEAR -> if (isSheet) -1 else R.id.btn_period_year
            LedgerPeriodPreset.ALL -> -1
            LedgerPeriodPreset.CUSTOM -> if (isSheet) -1 else R.id.btn_period_all
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
        val background = ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_light)
        val textColor = if (selected) {
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer)
        } else {
            ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_text_muted)
        }
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(background)
        button.setTextColor(textColor)
        button.isSelected = selected
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

    private fun resolveStartupView(): LedgerViewState {
        return when (LedgerUxPreferences.resolveStartupView(requireContext())) {
            LedgerView.DETAILS -> LedgerViewState.DETAILS
            LedgerView.STATISTICS_EXPENSE -> LedgerViewState.STATISTICS_EXPENSE
            LedgerView.STATISTICS_INCOME -> LedgerViewState.STATISTICS_INCOME
        }
    }

    private fun applyLedgerState(state: LedgerScreenState) {
        currentViewMode = if (state.view == LedgerViewState.DETAILS) VIEW_MODE_RECORDS else VIEW_MODE_STATISTICS
        currentStatsType = if (state.statisticsType == StatisticsType.INCOME) TYPE_INCOME else TYPE_EXPENSE
        currentPeriodPreset = when (state.periodPreset) {
            PeriodPreset.WEEK -> LedgerPeriodPreset.WEEK
            PeriodPreset.MONTH -> LedgerPeriodPreset.MONTH
            PeriodPreset.YEAR -> LedgerPeriodPreset.YEAR
            PeriodPreset.ALL -> LedgerPeriodPreset.ALL
            PeriodPreset.CUSTOM -> LedgerPeriodPreset.CUSTOM
        }
        currentRange = when (state.periodPreset) {
            PeriodPreset.ALL -> resolveAllRange()
            PeriodPreset.CUSTOM -> LedgerDateRange(state.customStartDate, state.customEndDate)
            else -> LedgerPeriodHelper.resolveRange(currentPeriodPreset)
        }
        currentChartMode = if (currentPeriodPreset == LedgerPeriodPreset.CUSTOM || currentPeriodPreset == LedgerPeriodPreset.ALL) {
            CHART_MODE_PIE
        } else if (state.chartMode == ChartMode.LINE) {
            CHART_MODE_LINE
        } else {
            CHART_MODE_PIE
        }
        openFilterSurface = state.openFilterSurface
    }

    private fun currentLedgerState(): LedgerScreenState {
        val previous = restoredLedgerState ?: LedgerScreenState.defaults(resolveStartupView())
        val detailsPosition = recyclerScrollState(recyclerRecords)
        val statisticsPosition = recyclerScrollState(recyclerStatistics)
        return LedgerScreenState(
            view = when {
                currentViewMode == VIEW_MODE_RECORDS -> LedgerViewState.DETAILS
                currentStatsType == TYPE_INCOME -> LedgerViewState.STATISTICS_INCOME
                else -> LedgerViewState.STATISTICS_EXPENSE
            },
            statisticsType = if (currentStatsType == TYPE_INCOME) StatisticsType.INCOME else StatisticsType.EXPENSE,
            periodPreset = when (currentPeriodPreset) {
                LedgerPeriodPreset.WEEK -> PeriodPreset.WEEK
                LedgerPeriodPreset.MONTH -> PeriodPreset.MONTH
                LedgerPeriodPreset.YEAR -> PeriodPreset.YEAR
                LedgerPeriodPreset.ALL -> PeriodPreset.ALL
                LedgerPeriodPreset.CUSTOM -> PeriodPreset.CUSTOM
            },
            customStartDate = if (currentPeriodPreset == LedgerPeriodPreset.CUSTOM) currentRange.startDate else null,
            customEndDate = if (currentPeriodPreset == LedgerPeriodPreset.CUSTOM) currentRange.endDate else null,
            chartMode = if (currentChartMode == CHART_MODE_LINE) ChartMode.LINE else ChartMode.PIE,
            detailsScrollPosition = detailsPosition?.first ?: previous.detailsScrollPosition,
            detailsScrollOffset = detailsPosition?.second ?: previous.detailsScrollOffset,
            statisticsScrollPosition = statisticsPosition?.first ?: previous.statisticsScrollPosition,
            statisticsScrollOffset = statisticsPosition?.second ?: previous.statisticsScrollOffset,
            openFilterSurface = openFilterSurface
        )
    }

    private fun currentLedgerViewPreference(): LedgerView {
        return when {
            currentViewMode == VIEW_MODE_RECORDS -> LedgerView.DETAILS
            currentStatsType == TYPE_INCOME -> LedgerView.STATISTICS_INCOME
            else -> LedgerView.STATISTICS_EXPENSE
        }
    }

    private fun recyclerScrollState(recyclerView: RecyclerView): Pair<Int, Int>? {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return null
        val position = layoutManager.findFirstVisibleItemPosition()
        if (position == RecyclerView.NO_POSITION) return null
        val offset = layoutManager.findViewByPosition(position)?.top ?: 0
        return position to offset
    }

    private fun restoreScrollPositions() {
        val state = restoredLedgerState ?: return
        (recyclerRecords.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(state.detailsScrollPosition, state.detailsScrollOffset)
        (recyclerStatistics.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(state.statisticsScrollPosition, state.statisticsScrollOffset)
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
            val selected = group.checkedButtonId == buttonId ||
                    (buttonId == R.id.btn_period_all && currentPeriodPreset == LedgerPeriodPreset.CUSTOM)
            updateToggleButtonState(group.findViewById(buttonId), selected)
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
