package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.model.RecurringRecord
import com.example.cardtally.adapter.RecordCategoryTreeAdapter
import com.example.cardtally.adapter.RecordCategoryGroupAdapter
import com.example.cardtally.adapter.CategorySelectorAdapter
import com.example.cardtally.util.LedgerSession
import com.example.cardtally.util.Money
import com.example.cardtally.util.RecurringRecordScheduler
import com.example.cardtally.util.RecurringScheduleCalculator
import com.example.cardtally.util.DateSelectionSheet
import com.example.cardtally.util.AmountKeypadController
import com.example.cardtally.util.AmountKeypadCompletionMode
import com.example.cardtally.util.TablerIconCatalog
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class RecurringRecordEditFragment : Fragment() {
    private lateinit var database: DatabaseHelper
    private var recurringId: Long? = null
    private var type = 0
    private var categoryId: Long? = null
    private var categoryName = ""
    private var categoryPath: String? = null
    private var assetId: Long? = null
    private var assetSource: String? = null
    private var destinationAssetId: Long? = null
    private var destinationAssetSource: String? = null
    private var ledgerId: Long? = null
    private var ledgerName = ""
    private var frequency = RecurringRecord.DAILY
    private var weeklyDay: Int? = null
    private var monthlyDay: Int? = null
    private var yearlyMonth: Int? = null
    private var yearlyDay: Int? = null
    private var intervalDays: Int? = null
    private var quickCategoryMode = false
    private var startDate = today()
    private var endDate: String? = null
    private lateinit var switchEnabled: Switch
    private lateinit var typeSegments: List<TextView>
    private lateinit var textLedger: View
    private lateinit var textCategory: View
    private lateinit var textAsset: View
    private lateinit var textDestinationAsset: View
    private lateinit var textFrequency: View
    private lateinit var textStart: View
    private lateinit var textEnd: View
    private lateinit var valueLedger: TextView
    private lateinit var valueCategory: TextView
    private lateinit var valueAsset: TextView
    private lateinit var valueDestinationAsset: TextView
    private lateinit var valueFrequency: TextView
    private lateinit var valueStart: TextView
    private lateinit var valueEnd: TextView
    private lateinit var editName: EditText
    private lateinit var editAmount: EditText
    private lateinit var editNote: EditText
    private lateinit var amountKeypadController: AmountKeypadController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recurringId = arguments?.getLong(ARG_ID)?.takeIf { it > 0L }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_recurring_record_edit, container, false)
        database = DatabaseHelper(requireContext())
        switchEnabled = view.findViewById(R.id.switch_recurring_enabled)
        typeSegments = listOf(
            view.findViewById(R.id.recurring_type_expense),
            view.findViewById(R.id.recurring_type_income),
            view.findViewById(R.id.recurring_type_transfer)
        )
        textLedger = view.findViewById(R.id.text_recurring_ledger)
        textCategory = view.findViewById(R.id.text_recurring_category)
        textAsset = view.findViewById(R.id.text_recurring_asset)
        textDestinationAsset = view.findViewById(R.id.text_recurring_destination_asset)
        textFrequency = view.findViewById(R.id.text_recurring_frequency)
        textStart = view.findViewById(R.id.text_recurring_start_date)
        textEnd = view.findViewById(R.id.text_recurring_end_date)
        valueLedger = view.findViewById(R.id.value_recurring_ledger)
        valueCategory = view.findViewById(R.id.value_recurring_category)
        valueAsset = view.findViewById(R.id.value_recurring_asset)
        valueDestinationAsset = view.findViewById(R.id.value_recurring_destination_asset)
        valueFrequency = view.findViewById(R.id.value_recurring_frequency)
        valueStart = view.findViewById(R.id.value_recurring_start_date)
        valueEnd = view.findViewById(R.id.value_recurring_end_date)
        editName = view.findViewById(R.id.edit_recurring_name)
        editAmount = view.findViewById(R.id.edit_recurring_amount)
        editNote = view.findViewById(R.id.edit_recurring_note)
        editName.layoutParams = editName.layoutParams.apply { width = ViewGroup.LayoutParams.MATCH_PARENT }
        amountKeypadController = AmountKeypadController(
            requireContext(),
            editAmount,
            view.findViewById(R.id.layout_amount_keypad),
            normalActions = null,
            onConfirm = { },
            completionMode = AmountKeypadCompletionMode.DISMISS_KEYPAD
        ).also { it.bind() }
        val title = view.findViewById<TextView>(R.id.text_recurring_edit_title)
        val delete = view.findViewById<View>(R.id.btn_recurring_delete)

        recurringId?.let { database.getRecurringRecordById(it) }?.let(::bind)
        if (ledgerId == null) {
            database.getCurrentLedger()?.let { ledgerId = it.id; ledgerName = it.name }
        }
        if (recurringId == null) switchEnabled.isChecked = true
        title.setText(if (recurringId == null) R.string.recurring_add_title else R.string.recurring_edit_title)
        delete.visibility = if (recurringId == null) View.GONE else View.VISIBLE
        bindLabels()

        view.findViewById<View>(R.id.btn_recurring_edit_back).setOnClickListener { parentFragmentManager.popBackStack() }
        typeSegments.forEachIndexed { index, segment -> segment.setOnClickListener { selectType(index) } }
        textLedger.setOnClickListener { prepareForSelector(); chooseLedger() }
        textCategory.setOnClickListener { prepareForSelector(); chooseCategory() }
        textAsset.setOnClickListener { prepareForSelector(); chooseAsset() }
        textDestinationAsset.setOnClickListener { prepareForSelector(); chooseAsset(selectDestination = true) }
        textFrequency.setOnClickListener { prepareForSelector(); chooseFrequency() }
        textStart.setOnClickListener { prepareForSelector(); chooseDate(false) }
        textEnd.setOnClickListener { prepareForSelector(); chooseDate(true) }
        view.findViewById<View>(R.id.btn_recurring_save).setOnClickListener { save() }
        delete.setOnClickListener { confirmDelete() }
        editName.setOnEditorActionListener { _, _, _ ->
            editNote.requestFocus()
            true
        }
        listOf(editName, editNote).forEach { input ->
            input.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && ::amountKeypadController.isInitialized) amountKeypadController.hide()
            }
        }
        editNote.setOnEditorActionListener { _, _, _ ->
            prepareForSelector()
            true
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            RecordAssetPickerBottomSheetFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, result ->
            val asset = database.getAllAssets(ledgerId).firstOrNull {
                it.id == result.getLong(RecordAssetPickerBottomSheetFragment.RESULT_ASSET_ID)
            } ?: return@setFragmentResultListener
            onAssetPickerSelected(
                asset.id,
                asset.name,
                result.getBoolean(RecordAssetPickerBottomSheetFragment.RESULT_SELECT_DESTINATION)
            )
        }
    }

    override fun onDestroyView() {
        if (::database.isInitialized) database.close()
        super.onDestroyView()
    }

    private fun bind(item: RecurringRecord) {
        type = item.type
        categoryId = item.categoryId
        categoryName = item.categoryName
        categoryPath = item.categoryPath
        assetId = item.assetId
        assetSource = item.assetSource
        destinationAssetId = item.destinationAssetId
        destinationAssetSource = item.destinationAssetSource
        ledgerId = item.ledgerId
        ledgerName = database.getLedgers().firstOrNull { it.id == item.ledgerId }?.name.orEmpty()
        frequency = item.frequency
        weeklyDay = item.weeklyDay
        monthlyDay = item.monthlyDay
        yearlyMonth = item.yearlyMonth
        yearlyDay = item.yearlyDay
        intervalDays = item.intervalDays
        startDate = item.startDate
        endDate = item.endDate
        switchEnabled.isChecked = item.enabled
        editName.setText(item.name)
        editAmount.setText(if (item.amountMinor > 0L) Money.formatYuan(item.amountMinor) else "")
        editNote.setText(item.note.orEmpty())
    }

    private fun bindLabels() {
        valueLedger.text = ledgerName.ifBlank { database.getCurrentLedger()?.name ?: getString(R.string.recurring_select) }
        valueCategory.text = categoryName.ifBlank { getString(R.string.recurring_select) }
        valueAsset.text = assetSource ?: getString(R.string.recurring_select)
        valueDestinationAsset.text = destinationAssetSource ?: getString(R.string.recurring_select)
        textCategory.visibility = if (type == 2) View.GONE else View.VISIBLE
        view?.findViewById<View>(R.id.divider_recurring_category)?.visibility = if (type == 2) View.GONE else View.VISIBLE
        textAsset.visibility = View.VISIBLE
        textDestinationAsset.visibility = if (type == 2) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.divider_recurring_destination_asset)?.visibility = if (type == 2) View.VISIBLE else View.GONE
        view?.findViewById<TextView>(R.id.label_recurring_asset)?.text = if (type == 2) getString(R.string.recurring_source_asset) else getString(R.string.recurring_asset)
        valueFrequency.text = frequencyLabel()
        valueStart.text = startDate
        valueEnd.text = endDate ?: getString(R.string.recurring_no_end)
        typeSegments.forEachIndexed { index, segment -> segment.isActivated = index == type }
    }

    private fun selectType(which: Int) {
        type = which
        categoryId = null
        categoryName = ""
        categoryPath = null
        if (type != 2) {
            destinationAssetId = null
            destinationAssetSource = null
        }
        bindLabels()
    }

    private fun chooseLedger() {
        val dialog = BottomSheetDialog(requireContext())
        val scroll = ScrollView(requireContext())
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        content.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(4)).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL; bottomMargin = dp(8) }
            setBackgroundResource(R.drawable.bg_bottom_sheet_handle)
        })
        content.addView(LinearLayout(requireContext()).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(TextView(requireContext()).apply {
                text = getString(R.string.recurring_select_ledger)
                textSize = 22f
                setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
            })
            addView(ImageButton(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                setBackgroundResource(android.R.color.transparent)
                setImageResource(R.drawable.ic_close)
                contentDescription = getString(R.string.dialog_cancel)
                setOnClickListener { dialog.dismiss() }
            })
        })
        database.getLedgers().forEach { ledger ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                minimumHeight = dp(64)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface))
                setOnClickListener {
                    ledgerId = ledger.id
                    ledgerName = ledger.name
                    categoryId = null; categoryName = ""; categoryPath = null
                    assetId = null; assetSource = null; destinationAssetId = null; destinationAssetSource = null
                    dialog.dismiss(); bindLabels()
                }
            }
            val icon = ImageView(requireContext()).apply {
                setImageResource(TablerIconCatalog.resourceId(ledger.iconName).takeIf { it != 0 } ?: R.drawable.tabler_book)
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            }
            row.addView(icon)
            row.addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(TextView(requireContext()).apply { text = ledger.name; textSize = 16f; setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface)) })
                addView(TextView(requireContext()).apply { text = ledger.subtitle; textSize = 12f; setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)) })
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            content.addView(row)
        }
        scroll.addView(content)
        dialog.setContentView(scroll)
        showStandardSheet(dialog)
    }

    private fun chooseCategory() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.bottom_sheet_record_category, null)
        val categories = database.getCategoryTreeByType(type)
        val recycler = sheet.findViewById<RecyclerView>(R.id.recycler_categories)
        val modeButton = sheet.findViewById<ImageButton>(R.id.btn_category_mode)
        sheet.findViewById<TextView>(R.id.text_category_sheet_title).text = getString(R.string.recurring_select_category)
        val select = { category: Category ->
            categoryId = category.id
            categoryName = category.name
            categoryPath = database.buildCategoryPathLabel(category.id)
            dialog.dismiss()
            bindLabels()
        }
        fun renderCategoryPanel() {
            if (quickCategoryMode) {
                recycler.layoutManager = GridLayoutManager(requireContext(), 3)
                recycler.adapter = CategorySelectorAdapter(
                    categories,
                    categoryId,
                    pathLabelProvider = { database.buildCategoryPathLabel(it.id) ?: it.name },
                    onCategorySelected = select
                )
            } else {
                recycler.layoutManager = LinearLayoutManager(requireContext())
                recycler.adapter = RecordCategoryGroupAdapter(categories, categoryId, select)
            }
            updateCategoryModeButton(modeButton)
        }
        renderCategoryPanel()
        modeButton.setOnClickListener {
            quickCategoryMode = !quickCategoryMode
            renderCategoryPanel()
        }
        sheet.findViewById<View>(R.id.btn_close_sheet).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(sheet)
        showStandardSheet(dialog)
    }

    private fun updateCategoryModeButton(button: ImageButton) {
        button.setImageResource(if (quickCategoryMode) R.drawable.tabler_layout_grid else R.drawable.tabler_layout_list)
        button.contentDescription = getString(R.string.recurring_category_mode_accessibility)
    }

    private fun chooseAsset(selectDestination: Boolean = false) {
        RecordAssetPickerBottomSheetFragment.newInstance(
            selectDestination,
            if (selectDestination) assetId else destinationAssetId,
            if (selectDestination) destinationAssetId else assetId,
            ledgerId
        )
            .show(parentFragmentManager, RecordAssetPickerBottomSheetFragment.TAG)
    }

    private fun showSimpleSelectionSheet(title: String, options: List<String>, selected: Int, onSelected: (Int) -> Unit) {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.bottom_sheet_recurring_frequency, null)
        sheet.background = recurringSheetBackground()
        sheet.findViewById<TextView>(R.id.text_recurring_sheet_title).text = title
        val tabs = sheet.findViewById<ViewGroup>(R.id.layout_recurring_sheet_tabs)
        sheet.findViewById<View>(R.id.layout_recurring_sheet_options).visibility = View.GONE
        var draft = selected
        lateinit var tabViews: List<TextView>
        tabViews = options.mapIndexed { index, label ->
            TextView(requireContext()).apply {
                text = label
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
                setOnClickListener {
                    draft = index
                    updateFrequencyTabStyles(tabViews, draft)
                }
            }
        }
        tabViews.forEach(tabs::addView)
        updateFrequencyTabStyles(tabViews, draft)
        sheet.findViewById<View>(R.id.btn_recurring_sheet_cancel).setOnClickListener { dialog.dismiss() }
        sheet.findViewById<View>(R.id.btn_recurring_sheet_confirm).setOnClickListener { dialog.dismiss(); onSelected(draft) }
        dialog.setContentView(sheet)
        showStandardSheet(dialog)
    }

    private fun onAssetPickerSelected(assetId: Long, assetName: String, selectDestination: Boolean) {
        if (selectDestination) {
            destinationAssetId = assetId
            destinationAssetSource = assetName
        } else {
            this.assetId = assetId
            assetSource = assetName
        }
        bindLabels()
    }

    private fun chooseFrequency() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.bottom_sheet_recurring_frequency, null)
        sheet.background = recurringSheetBackground()
        sheet.findViewById<TextView>(R.id.text_recurring_sheet_title).text = getString(R.string.recurring_select_frequency)
        val tabs = sheet.findViewById<ViewGroup>(R.id.layout_recurring_sheet_tabs)
        val content = sheet.findViewById<ViewGroup>(R.id.layout_recurring_sheet_options)
        val values = listOf("每天", "每周", "每月", "每年", "每间隔")
        var draftFrequency = frequency
        var draftWeeklyDay = weeklyDay ?: isoDay(parseDate(startDate))
        var draftMonthlyDay = monthlyDay ?: parseDate(startDate).get(Calendar.DAY_OF_MONTH)
        var draftYearlyMonth = yearlyMonth ?: parseDate(startDate).get(Calendar.MONTH) + 1
        var draftYearlyDay = yearlyDay ?: parseDate(startDate).get(Calendar.DAY_OF_MONTH)
        var draftIntervalDays = intervalDays ?: 1
        lateinit var render: () -> Unit
        lateinit var tabViews: List<TextView>
        tabViews = values.mapIndexed { index, label ->
            TextView(requireContext()).apply {
                text = label
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
                setOnClickListener {
                    draftFrequency = when (index) {
                        1 -> RecurringRecord.WEEKLY
                        2 -> RecurringRecord.MONTHLY
                        3 -> RecurringRecord.YEARLY
                        4 -> RecurringRecord.INTERVAL
                        else -> RecurringRecord.DAILY
                    }
                    render()
                    updateFrequencyTabStyles(tabViews, index)
                }
            }
        }
        tabViews.forEach(tabs::addView)
        val initialIndex = when (draftFrequency) {
            RecurringRecord.WEEKLY -> 1
            RecurringRecord.MONTHLY -> 2
            RecurringRecord.YEARLY -> 3
            RecurringRecord.INTERVAL -> 4
            else -> 0
        }
        render = {
            renderFrequencySheetContent(content, draftFrequency, {
                draftWeeklyDay = it
                render()
            }, {
                draftMonthlyDay = it
                render()
            }, { month, day ->
                draftYearlyMonth = month
                draftYearlyDay = day
            }, {
                draftIntervalDays = it
                render()
            }, draftWeeklyDay, draftMonthlyDay, draftYearlyMonth, draftYearlyDay, draftIntervalDays)
        }
        render()
        updateFrequencyTabStyles(tabViews, initialIndex)
        sheet.findViewById<View>(R.id.btn_recurring_sheet_cancel).setOnClickListener { dialog.dismiss() }
        sheet.findViewById<View>(R.id.btn_recurring_sheet_confirm).setOnClickListener {
            frequency = draftFrequency
            weeklyDay = draftWeeklyDay
            monthlyDay = draftMonthlyDay
            yearlyMonth = draftYearlyMonth
            yearlyDay = draftYearlyDay
            intervalDays = draftIntervalDays
            dialog.dismiss()
            bindLabels()
        }
        dialog.setContentView(sheet)
        showStandardSheet(dialog)
    }

    private fun renderFrequencySheetContent(
        content: ViewGroup,
        selectedFrequency: String,
        onWeekly: (Int) -> Unit,
        onMonthly: (Int) -> Unit,
        onYearly: (Int, Int) -> Unit,
        onInterval: (Int) -> Unit,
        weekly: Int,
        monthly: Int,
        yearMonth: Int,
        yearDay: Int,
        interval: Int
    ) {
        content.removeAllViews()
        when (selectedFrequency) {
            RecurringRecord.WEEKLY -> {
                val grid = GridLayout(requireContext()).apply { columnCount = 4; rowCount = 2 }
                listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").forEachIndexed { index, label ->
                    grid.addView(optionCell(label, index + 1 == weekly, 52) { onWeekly(index + 1) }, GridLayout.LayoutParams().apply {
                        width = 0; height = dp(52); columnSpec = GridLayout.spec(index % 4, 1f); rowSpec = GridLayout.spec(index / 4)
                    })
                }
                content.addView(grid)
            }
            RecurringRecord.MONTHLY -> {
                val grid = GridLayout(requireContext()).apply { columnCount = 7; rowCount = 5 }
                (1..31).map { it.toString() }.plus("月末").forEachIndexed { index, label ->
                    grid.addView(optionCell(label, (monthly == index + 1) || (monthly == 0 && index == 31), 48) {
                        onMonthly(if (index == 31) 0 else index + 1)
                    }, GridLayout.LayoutParams().apply {
                        width = 0; height = dp(48); columnSpec = GridLayout.spec(index % 7, 1f); rowSpec = GridLayout.spec(index / 7)
                    })
                }
                content.addView(grid)
            }
            RecurringRecord.YEARLY -> {
                val row = LinearLayout(requireContext()).apply {
                    gravity = android.view.Gravity.CENTER
                    orientation = LinearLayout.HORIZONTAL
                }
                val monthPicker = numberPicker(1, 12, yearMonth)
                val dayPicker = numberPicker(1, 31, yearDay)
                row.addView(monthPicker, LinearLayout.LayoutParams(dp(90), dp(160)))
                row.addView(TextView(requireContext()).apply { text = "月"; textSize = 18f; setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface)) })
                row.addView(dayPicker, LinearLayout.LayoutParams(dp(90), dp(160)))
                row.addView(TextView(requireContext()).apply { text = "日"; textSize = 18f; setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface)) })
                monthPicker.setOnValueChangedListener { _, _, value -> onYearly(value, dayPicker.value) }
                dayPicker.setOnValueChangedListener { _, _, value -> onYearly(monthPicker.value, value) }
                content.addView(row)
            }
            RecurringRecord.INTERVAL -> {
                val row = LinearLayout(requireContext()).apply {
                    gravity = android.view.Gravity.CENTER
                    orientation = LinearLayout.HORIZONTAL
                }
                row.addView(TextView(requireContext()).apply { text = "每"; textSize = 18f; setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface)) })
                val picker = numberPicker(1, 365, interval)
                row.addView(picker, LinearLayout.LayoutParams(dp(90), dp(160)))
                row.addView(TextView(requireContext()).apply { text = "天"; textSize = 18f; setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface)) })
                picker.setOnValueChangedListener { _, _, value -> onInterval(value) }
                content.addView(row)
            }
        }
        val warning = when {
            selectedFrequency == RecurringRecord.MONTHLY && monthly == 31 -> getString(R.string.recurring_monthly_31_notice)
            selectedFrequency == RecurringRecord.YEARLY && yearMonth == 2 && yearDay == 29 -> getString(R.string.recurring_yearly_leap_notice)
            else -> null
        }
        warning?.let {
            content.addView(TextView(requireContext()).apply {
                text = it
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.warning_primary))
                setPadding(dp(12), dp(12), dp(12), 0)
            })
        }
    }

    private fun optionCell(label: String, selected: Boolean, height: Int, action: () -> Unit): TextView = TextView(requireContext()).apply {
        text = label
        textSize = 16f
        gravity = android.view.Gravity.CENTER
        setTextColor(if (selected) themeColor(com.google.android.material.R.attr.colorSurface) else themeColor(com.google.android.material.R.attr.colorOnSurface))
        if (selected) background = GradientDrawable().apply { setColor(themeColor(com.google.android.material.R.attr.colorOnSurface)); cornerRadius = dp(6).toFloat() }
        setOnClickListener { action() }
    }

    private fun numberPicker(min: Int, max: Int, value: Int): NumberPicker = NumberPicker(requireContext()).apply {
        minValue = min
        maxValue = max
        this.value = value.coerceIn(min, max)
        wrapSelectorWheel = false
        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        setNumberPickerTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
    }

    private fun updateFrequencyTabStyles(tabs: List<TextView>, selected: Int) {
        tabs.forEachIndexed { index, tab ->
            tab.setTextColor(if (index == selected) themeColor(com.google.android.material.R.attr.colorSurface) else themeColor(com.google.android.material.R.attr.colorOnSurface))
            tab.background = if (index == selected) GradientDrawable().apply { setColor(themeColor(com.google.android.material.R.attr.colorOnSurface)); cornerRadius = dp(6).toFloat() } else null
        }
    }

    private fun isoDay(calendar: Calendar): Int =
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun themeColor(attribute: Int): Int {
        val value = TypedValue()
        requireContext().theme.resolveAttribute(attribute, value, true)
        return if (value.resourceId != 0) requireContext().getColor(value.resourceId) else value.data
    }

    private fun NumberPicker.setNumberPickerTextColor(color: Int) {
        val id = resources.getIdentifier("numberpicker_input", "id", "android")
        findViewById<EditText>(id)?.setTextColor(color)
    }

    private fun recurringSheetBackground() = GradientDrawable().apply {
        setColor(themeColor(com.google.android.material.R.attr.colorSurface))
        cornerRadii = floatArrayOf(
            dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat(), dp(16).toFloat(),
            0f, 0f, 0f, 0f
        )
    }

    private fun prepareForSelector() {
        if (::amountKeypadController.isInitialized) amountKeypadController.hide()
        view?.clearFocus()
        activity?.currentFocus?.let { focused ->
            (requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(focused.windowToken, 0)
            focused.clearFocus()
        }
    }

    private fun showStandardSheet(dialog: BottomSheetDialog) {
        dialog.show()
        dialog.behavior.isFitToContents = true
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.8f).roundToInt()
    }

    private fun showRuleNotice(title: String, message: String) {
        val sheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_recurring_frequency, null)
        view.background = recurringSheetBackground()
        view.findViewById<TextView>(R.id.text_recurring_sheet_title).text = title
        view.findViewById<View>(R.id.layout_recurring_sheet_tabs).visibility = View.GONE
        view.findViewById<View>(R.id.btn_recurring_sheet_cancel).setOnClickListener { sheet.dismiss() }
        view.findViewById<View>(R.id.btn_recurring_sheet_confirm).setOnClickListener { sheet.dismiss() }
        view.findViewById<android.view.ViewGroup>(R.id.layout_recurring_sheet_options).addView(
            TextView(requireContext()).apply {
                text = message
                textSize = 16f
                setTextColor(requireContext().getColor(R.color.onSurface_light))
                setPadding(0, 4, 0, 8)
            }
        )
        sheet.setContentView(view)
        sheet.show()
    }

    private fun chooseDate(end: Boolean) {
        val value = (if (end) endDate else startDate) ?: today()
        DateSelectionSheet.create(requireContext(), value, commitOnSelection = true) { selected ->
            if (end) {
                endDate = selected
            } else {
                startDate = selected
            }
            bindLabels()
        }.show()
    }

    private fun save() {
        val amount = Money.parseYuan(editAmount.text.toString())
        if (editName.text.toString().isBlank() || amount == null || amount <= 0L) {
            Toast.makeText(requireContext(), R.string.recurring_amount_hint, Toast.LENGTH_SHORT).show()
            return
        }
        if (type != 2 && categoryName.isBlank()) {
            Toast.makeText(requireContext(), R.string.recurring_category, Toast.LENGTH_SHORT).show()
            return
        }
        val existing = recurringId?.let(database::getRecurringRecordById)
        val item = RecurringRecord(
            id = recurringId ?: 0L,
            ledgerId = ledgerId ?: database.getCurrentLedger()?.id ?: 0L,
            type = type,
            name = editName.text.toString(),
            amountMinor = amount,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryPath = categoryPath,
            assetId = assetId,
            assetSource = assetSource,
            destinationAssetId = destinationAssetId,
            destinationAssetSource = destinationAssetSource,
            note = editNote.text.toString().ifBlank { null },
            frequency = frequency,
            weeklyDay = weeklyDay,
            monthlyDay = monthlyDay,
            yearlyMonth = yearlyMonth,
            yearlyDay = yearlyDay,
            intervalDays = intervalDays,
            startDate = startDate,
            endDate = endDate,
            enabled = switchEnabled.isChecked,
            nextDueDate = if (existing == null || scheduleChanged(existing)) initialDueDate() else existing.nextDueDate
        )
        runCatching { database.saveRecurringRecord(item) }
            .onSuccess {
                database.processDueRecurringRecordsForAllLedgers()
                RecurringRecordScheduler.schedule(requireContext())
                Toast.makeText(requireContext(), R.string.recurring_save_success, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .onFailure { Toast.makeText(requireContext(), it.message ?: getString(R.string.recurring_save), Toast.LENGTH_SHORT).show() }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext()).setMessage(R.string.recurring_delete_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.recurring_delete) { _, _ ->
                recurringId?.let(database::deleteRecurringRecord)
                parentFragmentManager.popBackStack()
            }.show()
    }

    private fun frequencyLabel(value: String = frequency) = when (value) {
        RecurringRecord.WEEKLY -> "每周 · 周${weeklyDayLabel(weeklyDay ?: isoDay(parseDate(startDate)))}"
        RecurringRecord.MONTHLY -> if (monthlyDay == 0) "每月月末" else "每月 ${monthlyDay ?: ""}日"
        RecurringRecord.YEARLY -> "每年 ${yearlyMonth ?: ""}月${yearlyDay ?: ""}日"
        RecurringRecord.INTERVAL -> "每 ${intervalDays ?: 1} 天"
        else -> getString(R.string.recurring_daily)
    }

    private fun weeklyDayLabel(day: Int): String =
        listOf("一", "二", "三", "四", "五", "六", "日")[day.coerceIn(1, 7) - 1]

    private fun scheduleChanged(existing: RecurringRecord): Boolean =
        existing.frequency != frequency ||
            existing.weeklyDay != weeklyDay ||
            existing.monthlyDay != monthlyDay ||
            existing.yearlyMonth != yearlyMonth ||
            existing.yearlyDay != yearlyDay ||
            existing.intervalDays != intervalDays ||
            existing.destinationAssetId != destinationAssetId ||
            existing.startDate != startDate

    private fun initialDueDate(): String {
        return RecurringScheduleCalculator.initialDueDate(
            startDate,
            RecurringRecord(
                frequency = frequency,
                weeklyDay = weeklyDay,
                monthlyDay = monthlyDay,
                yearlyMonth = yearlyMonth,
                yearlyDay = yearlyDay,
                intervalDays = intervalDays
            )
        )
    }

    companion object {
        private const val ARG_ID = "recurring_id"
        fun newInstance(id: Long?) = RecurringRecordEditFragment().apply {
            arguments = Bundle().apply { id?.let { putLong(ARG_ID, it) } }
        }
        private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        private fun parseDate(value: String): Calendar = Calendar.getInstance().apply {
            runCatching { time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)!! }
        }
    }
}
