package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.AssetSheetItem
import com.example.cardtally.adapter.RecordAssetSheetAdapter
import com.example.cardtally.adapter.RecordCategoryTreeAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.example.cardtally.state.RecordFormState
import com.example.cardtally.state.RecordSheet
import com.example.cardtally.state.RecordType
import com.example.cardtally.state.StableIdResolver
import com.example.cardtally.util.ThemeColorHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.Locale

class AddRecordFragment : Fragment() {
    private lateinit var textDate: TextView
    private lateinit var textAssetValue: TextView
    private lateinit var textCategoryValue: TextView
    private lateinit var editAmount: EditText
    private lateinit var editDescription: EditText
    private lateinit var btnExpense: Button
    private lateinit var btnIncome: Button
    private lateinit var btnClose: View
    private lateinit var btnCancel: View
    private lateinit var btnSave: View
    private lateinit var rowDate: View
    private lateinit var rowAsset: View
    private lateinit var rowCategory: View
    private lateinit var databaseHelper: DatabaseHelper

    private var categoryAdapter: RecordCategoryTreeAdapter? = null
    private var currentCategories = mutableListOf<Category>()
    private var currentAssets = mutableListOf<Asset>()
    private var currentType = 0
    private var selectedDate: String = ""
    private var selectedCategory: Category? = null
    private var selectedAsset: Asset? = null
    private var openSheet = RecordSheet.NONE
    private var pendingCategoryId: Long? = null
    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_record, container, false)

        textDate = view.findViewById(R.id.text_date)
        textAssetValue = view.findViewById(R.id.text_asset_value)
        textCategoryValue = view.findViewById(R.id.text_category_value)
        editAmount = view.findViewById(R.id.edit_amount)
        editDescription = view.findViewById(R.id.edit_description)
        btnExpense = view.findViewById(R.id.btn_expense)
        btnIncome = view.findViewById(R.id.btn_income)
        btnClose = view.findViewById(R.id.btn_close)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSave = view.findViewById(R.id.btn_save)
        rowDate = view.findViewById(R.id.row_date)
        rowAsset = view.findViewById(R.id.row_asset)
        rowCategory = view.findViewById(R.id.row_category)

        databaseHelper = DatabaseHelper(requireContext())

        val defaultState = RecordFormState.DEFAULT.copy(selectedDate = databaseHelper.getCurrentDate())
        val restoredState = RecordFormState.readFrom(savedInstanceState, defaultState)
        currentType = if (restoredState.recordType == RecordType.INCOME) 1 else 0
        selectedDate = restoredState.selectedDate
        openSheet = restoredState.openSheet
        pendingCategoryId = restoredState.pendingCategoryId
        editAmount.setText(restoredState.amountBuffer)
        editDescription.setText(restoredState.description)
        setupAmountInputBehavior()

        loadCategories(currentType)
        loadAssets()
        if (savedInstanceState != null) {
            restoreSelections(restoredState)
        }
        updateDisplayedDate()
        updateTypeStyle()
        setupBackNavigation()

        rowDate.setOnClickListener { showDateSheet() }
        rowAsset.setOnClickListener { showAssetSheet() }
        rowCategory.setOnClickListener { showCategorySheet() }

        btnExpense.setOnClickListener {
            if (currentType != 0) {
                currentType = 0
                updateTypeStyle()
                loadCategories(0)
            }
        }

        btnIncome.setOnClickListener {
            if (currentType != 1) {
                currentType = 1
                updateTypeStyle()
                loadCategories(1)
            }
        }

        btnClose.setOnClickListener { navigateBack() }
        btnCancel.setOnClickListener { navigateBack() }
        btnSave.setOnClickListener { saveRecord(true) }

        view.post { restoreOpenSheet() }
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        RecordFormState(
            amountBuffer = editAmount.text.toString(),
            recordType = if (currentType == 1) RecordType.INCOME else RecordType.EXPENSE,
            selectedDate = selectedDate,
            selectedAssetId = selectedAsset?.id,
            selectedCategoryId = selectedCategory?.id,
            description = editDescription.text.toString(),
            openSheet = openSheet,
            pendingCategoryId = pendingCategoryId
        ).writeTo(outState)
    }

    override fun onResume() {
        super.onResume()
        hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        showBottomNav()
    }

    private fun updateTypeStyle() {
        if (currentType == 0) {
            btnExpense.setBackgroundResource(R.drawable.shape_button_primary)
            btnExpense.setTextColor(
                ThemeColorHelper.resolveColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorOnPrimary
                )
            )

            btnIncome.setBackgroundResource(android.R.color.transparent)
            btnIncome.setTextColor(
                ThemeColorHelper.resolveColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
        } else {
            btnExpense.setBackgroundResource(android.R.color.transparent)
            btnExpense.setTextColor(
                ThemeColorHelper.resolveColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )

            btnIncome.setBackgroundResource(R.drawable.shape_button_primary)
            btnIncome.setTextColor(
                ThemeColorHelper.resolveColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorOnPrimary
                )
            )
        }
    }

    private fun loadCategories(type: Int) {
        currentCategories = databaseHelper.getCategoryTreeByType(type).toMutableList()
        selectedCategory = currentCategories.firstOrNull { isLeafCategory(it) }
        updateCategorySummary()
    }

    private fun loadAssets() {
        currentAssets = databaseHelper.getAllAssets().toMutableList()
        selectedAsset = null
        updateAssetSummary()
    }

    private fun showDateSheet() {
        openSheet = RecordSheet.DATE
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_record_date, null)
        dialog.setContentView(sheetView)

        val datePicker = sheetView.findViewById<DatePicker>(R.id.date_picker)
        val btnCloseSheet = sheetView.findViewById<ImageButton>(R.id.btn_close_sheet)
        val btnConfirmDate = sheetView.findViewById<View>(R.id.btn_confirm_date)
        val textSelectToday = sheetView.findViewById<TextView>(R.id.text_select_today)

        val parts = selectedDate.split("-")
        datePicker.updateDate(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())

        dialog.setOnDismissListener { openSheet = RecordSheet.NONE }
        btnCloseSheet.setOnClickListener { dialog.dismiss() }
        textSelectToday.setOnClickListener {
            val calendar = Calendar.getInstance()
            datePicker.updateDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        btnConfirmDate.setOnClickListener {
            selectedDate = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                datePicker.year,
                datePicker.month + 1,
                datePicker.dayOfMonth
            )
            updateDisplayedDate()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showAssetSheet() {
        openSheet = RecordSheet.ASSET
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_record_assets, null)
        dialog.setContentView(sheetView)

        val btnCloseSheet = sheetView.findViewById<ImageButton>(R.id.btn_close_sheet)
        val recyclerAssets = sheetView.findViewById<RecyclerView>(R.id.recycler_assets)
        val adapter = RecordAssetSheetAdapter(buildAssetSheetItems(), selectedAsset?.id) { item ->
            selectedAsset = item.asset
            updateAssetSummary()
            dialog.dismiss()
        }

        dialog.setOnDismissListener { openSheet = RecordSheet.NONE }
        recyclerAssets.layoutManager = LinearLayoutManager(requireContext())
        recyclerAssets.adapter = adapter
        btnCloseSheet.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showCategorySheet() {
        openSheet = RecordSheet.CATEGORY
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_record_category, null)
        dialog.setContentView(sheetView)

        val btnCloseSheet = sheetView.findViewById<ImageButton>(R.id.btn_close_sheet)
        val recyclerCategories = sheetView.findViewById<RecyclerView>(R.id.recycler_categories)
        val textBreadcrumb = sheetView.findViewById<TextView>(R.id.text_category_breadcrumb)
        val btnConfirm = sheetView.findViewById<View>(R.id.btn_confirm_category)

        var pendingCategory = pendingCategoryId?.let { id -> currentCategories.firstOrNull { it.id == id } }
            ?: selectedCategory
        pendingCategoryId = pendingCategory?.id
        categoryAdapter = RecordCategoryTreeAdapter(currentCategories, pendingCategoryId) { category ->
            pendingCategory = category
            pendingCategoryId = category.id
            textBreadcrumb.text = category.id.let(databaseHelper::buildCategoryPathLabel)
                ?: getString(R.string.record_category_sheet_breadcrumb_empty)
        }

        recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        recyclerCategories.adapter = categoryAdapter
        textBreadcrumb.text = selectedCategory?.id?.let(databaseHelper::buildCategoryPathLabel)
            ?: getString(R.string.record_category_sheet_breadcrumb_empty)

        dialog.setOnDismissListener {
            openSheet = RecordSheet.NONE
            pendingCategoryId = null
        }
        btnCloseSheet.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            val category = pendingCategory
            if (category == null || !isLeafCategory(category)) {
                Toast.makeText(requireContext(), getString(R.string.validation_select_category), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            selectedCategory = category
            updateCategorySummary()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateDisplayedDate() {
        textDate.text = formatDisplayDate(selectedDate)
    }

    private fun formatDisplayDate(rawDate: String): String {
        val parts = rawDate.split("-")
        return if (parts.size == 3) {
            getString(R.string.record_date_display, parts[1].toInt(), parts[2].toInt())
        } else {
            rawDate
        }
    }

    private fun updateAssetSummary() {
        textAssetValue.text = selectedAsset?.name ?: getString(R.string.record_asset_none)
    }

    private fun updateCategorySummary() {
        textCategoryValue.text = selectedCategory?.id?.let(databaseHelper::buildCategoryPathLabel)
            ?: getString(R.string.record_category_unselected)
    }

    private fun buildAssetSheetItems(): List<AssetSheetItem> {
        val noneItem = AssetSheetItem(
            id = null,
            asset = null,
            title = getString(R.string.record_asset_none),
            subtitle = getString(R.string.record_asset_sheet_none_subtitle),
            amountLabel = getString(R.string.record_asset_sheet_none_amount)
        )
        return buildList {
            add(noneItem)
            currentAssets.forEach { asset ->
                add(
                    AssetSheetItem(
                        id = asset.id,
                        asset = asset,
                        title = asset.name,
                        subtitle = getAssetTypeLabel(asset.type),
                        amountLabel = getString(R.string.currency_amount, asset.amount)
                    )
                )
            }
        }
    }

    private fun getAssetTypeLabel(type: Int): String {
        return when (type) {
            0 -> getString(R.string.asset_type_cash)
            1 -> getString(R.string.asset_type_bank)
            2 -> getString(R.string.asset_type_alipay)
            3 -> getString(R.string.asset_type_wechat)
            else -> getString(R.string.record_asset_none)
        }
    }

    private fun isLeafCategory(category: Category): Boolean {
        return currentCategories.none { it.parentId == category.id }
    }

    private fun restoreSelections(state: RecordFormState) {
        val assetId = StableIdResolver.resolve(state.selectedAssetId, currentAssets.mapTo(mutableSetOf()) { it.id })
        selectedAsset = assetId?.let { id -> currentAssets.firstOrNull { it.id == id } }
        val categoryId = StableIdResolver.resolve(state.selectedCategoryId, currentCategories.mapTo(mutableSetOf()) { it.id })
        selectedCategory = categoryId?.let { id -> currentCategories.firstOrNull { it.id == id && isLeafCategory(it) } }
        pendingCategoryId = state.pendingCategoryId?.takeIf { id -> currentCategories.any { it.id == id && isLeafCategory(it) } }
        updateAssetSummary()
        updateCategorySummary()
    }

    private fun restoreOpenSheet() {
        when (openSheet) {
            RecordSheet.NONE -> Unit
            RecordSheet.DATE -> showDateSheet()
            RecordSheet.ASSET -> showAssetSheet()
            RecordSheet.CATEGORY -> showCategorySheet()
        }
    }

    private fun saveRecord(shouldReturn: Boolean) {
        val amountStr = editAmount.text.toString().trim()
        val category = selectedCategory?.name
        val description = editDescription.text.toString().trim()

        if (selectedDate.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.validation_select_date), Toast.LENGTH_SHORT).show()
            return
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.validation_enter_amount), Toast.LENGTH_SHORT).show()
            return
        }

        val amount = try {
            amountStr.toDouble()
        } catch (_: NumberFormatException) {
            Toast.makeText(requireContext(), getString(R.string.validation_enter_valid_amount), Toast.LENGTH_SHORT).show()
            return
        }

        if (amount == 0.0) {
            Toast.makeText(requireContext(), getString(R.string.validation_zero_amount), Toast.LENGTH_SHORT).show()
            return
        }

        if (category == null) {
            Toast.makeText(requireContext(), getString(R.string.validation_select_category), Toast.LENGTH_SHORT).show()
            return
        }

        val record = Record(
            date = selectedDate,
            amount = amount,
            category = category,
            categoryId = selectedCategory?.id,
            categoryNameSnapshot = selectedCategory?.name,
            categoryPathSnapshot = selectedCategory?.id?.let(databaseHelper::buildCategoryPathLabel) ?: category,
            type = currentType,
            description = description,
            assetSource = selectedAsset?.name
        )
        val id = databaseHelper.addRecord(record)

        if (id != -1L) {
            Toast.makeText(requireContext(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show()
            if (shouldReturn) {
                navigateBack()
            } else {
                clearAmountAndDescription()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.toast_save_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAmountAndDescription() {
        editAmount.setText(getString(R.string.amount_default))
        editDescription.setText("")
    }

    private fun setupAmountInputBehavior() {
        editAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                selectAmountIfStillDefault()
            }
        }
        editAmount.setOnClickListener { selectAmountIfStillDefault() }
    }

    private fun selectAmountIfStillDefault() {
        if (editAmount.text.toString() != getString(R.string.amount_default)) {
            return
        }
        editAmount.post { editAmount.selectAll() }
    }

    private fun setupBackNavigation() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private fun navigateBack() {
        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
            return
        }

        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_ledger
    }

    private fun hideBottomNav() {
        requireActivity().findViewById<View>(R.id.nav_shell)?.visibility = View.GONE
    }

    private fun showBottomNav() {
        requireActivity().findViewById<View>(R.id.nav_shell)?.visibility = View.VISIBLE
    }
}
