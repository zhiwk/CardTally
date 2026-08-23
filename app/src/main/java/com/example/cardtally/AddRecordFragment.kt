package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.AssetSheetItem
import com.example.cardtally.adapter.RecordAssetSheetAdapter
import com.example.cardtally.adapter.RecordCategoryTreeAdapter
import com.example.cardtally.adapter.IconPickerAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.example.cardtally.state.RecordFormState
import com.example.cardtally.state.RecordSheet
import com.example.cardtally.state.RecordType
import com.example.cardtally.state.StableIdResolver
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.MaterialSymbolCatalog
import com.example.cardtally.util.AmountKeypadController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.Locale

open class AddRecordFragment : Fragment() {
    companion object {
        private const val KEY_RECORD_ID = "record_id"
    }

    private lateinit var textDate: TextView
    private lateinit var textAssetValue: TextView
    private lateinit var textCategoryValue: TextView
    private lateinit var imageCategoryIcon: ImageView
    private lateinit var editAmount: EditText
    private lateinit var editDescription: EditText
    private lateinit var btnExpense: Button
    private lateinit var btnIncome: Button
    private lateinit var btnClose: View
    private lateinit var btnCancel: View
    private lateinit var btnSave: View
    private lateinit var btnSaveAndAdd: View
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
    private var editingRecordId: Long? = null
    private var editingRecord: Record? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingRecordId = when {
            savedInstanceState?.containsKey(KEY_RECORD_ID) == true -> savedInstanceState.getLong(KEY_RECORD_ID)
            arguments?.containsKey(KEY_RECORD_ID) == true -> arguments?.getLong(KEY_RECORD_ID)
            else -> null
        }?.takeIf { it > 0L }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_record, container, false)

        textDate = view.findViewById(R.id.text_date)
        textAssetValue = view.findViewById(R.id.text_asset_value)
        textCategoryValue = view.findViewById(R.id.text_category_value)
        imageCategoryIcon = view.findViewById(R.id.image_category_icon)
        editAmount = view.findViewById(R.id.edit_amount)
        editDescription = view.findViewById(R.id.edit_description)
        btnExpense = view.findViewById(R.id.btn_expense)
        btnIncome = view.findViewById(R.id.btn_income)
        btnClose = view.findViewById(R.id.btn_close)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSave = view.findViewById(R.id.btn_save)
        btnSaveAndAdd = view.findViewById(R.id.btn_save_and_add)
        val amountKeypad = view.findViewById<View>(R.id.layout_amount_keypad)
        AmountKeypadController(requireContext(), editAmount, amountKeypad, view.findViewById(R.id.layout_buttons)) {
            editAmount.clearFocus()
        }.also { it.bind() }
        rowDate = view.findViewById(R.id.row_date)
        rowAsset = view.findViewById(R.id.row_asset)
        rowCategory = view.findViewById(R.id.row_category)

        databaseHelper = DatabaseHelper(requireContext())

        editingRecord = editingRecordId?.let(databaseHelper::getRecordById)
        val defaultState = editingRecord?.let { record ->
            RecordFormState(
                amountBuffer = String.format(Locale.US, "%.2f", record.amount),
                recordType = if (record.type == 1) RecordType.INCOME else RecordType.EXPENSE,
                selectedDate = record.date,
                selectedAssetId = databaseHelper.getAllAssets().firstOrNull { it.name == record.assetSource }?.id,
                selectedCategoryId = record.categoryId,
                description = record.description.orEmpty(),
                openSheet = RecordSheet.NONE,
                pendingCategoryId = null
            )
        } ?: RecordFormState.DEFAULT.copy(selectedDate = databaseHelper.getCurrentDate())
        val restoredState = RecordFormState.readFrom(savedInstanceState, defaultState)
        currentType = if (restoredState.recordType == RecordType.INCOME) 1 else 0
        selectedDate = restoredState.selectedDate
        openSheet = restoredState.openSheet
        pendingCategoryId = restoredState.pendingCategoryId
        editAmount.setText(restoredState.amountBuffer)
        editDescription.setText(restoredState.description)

        loadCategories(currentType, restoredState.selectedCategoryId)
        loadAssets(restoredState.selectedAssetId)
        if (savedInstanceState != null) {
            restoreSelections(restoredState)
        }
        updateDisplayedDate()
        view.findViewById<TextView>(R.id.text_title).text = getString(
            if (isEditing()) R.string.record_title_edit else R.string.record_title_new
        )
        if (isEditing()) {
            view.findViewById<TextView>(R.id.text_save_label).text = getString(R.string.record_update)
        }
        updateTypeStyle()
        setupBackNavigation()

        rowDate.setOnClickListener { showDateSheet() }
        rowAsset.setOnClickListener { showAssetSheet() }
        rowCategory.setOnClickListener { showCategorySheet() }

        btnExpense.setOnClickListener {
            if (currentType != 0) {
                currentType = 0
                updateTypeStyle()
                loadCategories(0, null)
            }
        }

        btnIncome.setOnClickListener {
            if (currentType != 1) {
                currentType = 1
                updateTypeStyle()
                loadCategories(1, null)
            }
        }

        btnClose.setOnClickListener { navigateBack() }
        btnCancel.setOnClickListener { navigateBack() }
        btnSave.setOnClickListener { saveRecord(true) }
        btnSaveAndAdd.setOnClickListener { saveRecord(false, true) }

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
        editingRecordId?.let { outState.putLong(KEY_RECORD_ID, it) }
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
            btnExpense.setBackgroundResource(R.drawable.bg_record_type_tab_selected)
            btnExpense.setTextColor(ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnSurface))
            btnExpense.isSelected = true
            btnIncome.setBackgroundResource(android.R.color.transparent)
            btnIncome.setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.editorial_text_muted))
            btnIncome.isSelected = false
        } else {
            btnExpense.setBackgroundResource(android.R.color.transparent)
            btnExpense.setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.editorial_text_muted))
            btnExpense.isSelected = false
            btnIncome.setBackgroundResource(R.drawable.bg_record_type_tab_selected)
            btnIncome.setTextColor(ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnSurface))
            btnIncome.isSelected = true
        }
    }

    private fun loadCategories(type: Int, selectedCategoryId: Long? = null) {
        currentCategories = databaseHelper.getCategoryTreeByType(type).toMutableList()
        selectedCategory = selectedCategoryId?.let { id ->
            currentCategories.firstOrNull { it.id == id && isLeafCategory(it) }
        } ?: currentCategories.firstOrNull { isLeafCategory(it) }
        updateCategorySummary()
    }

    private fun loadAssets(selectedAssetId: Long? = null) {
        currentAssets = databaseHelper.getAllAssets().toMutableList()
        selectedAsset = selectedAssetId?.let { id -> currentAssets.firstOrNull { it.id == id } }
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

        var pendingCategory = pendingCategoryId?.let { id -> currentCategories.firstOrNull { it.id == id } }
            ?: selectedCategory
        pendingCategoryId = pendingCategory?.id
        categoryAdapter = RecordCategoryTreeAdapter(
            currentCategories,
            pendingCategoryId,
            onCategorySelected = { category ->
                pendingCategory = category
                pendingCategoryId = category.id
                textBreadcrumb.text = category.id.let(databaseHelper::buildCategoryPathLabel)
                    ?: getString(R.string.record_category_sheet_breadcrumb_empty)
                selectedCategory = category
                updateCategorySummary()
                dialog.dismiss()
            },
            onAddChild = { parent ->
                showAddChildCategoryDialog(parent) { created ->
                    currentCategories = databaseHelper.getCategoryTreeByType(currentType).toMutableList()
                    selectedCategory = created
                    pendingCategoryId = created.id
                    updateCategorySummary()
                    dialog.dismiss()
                }
            }
        )

        recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        recyclerCategories.adapter = categoryAdapter
        textBreadcrumb.text = selectedCategory?.id?.let(databaseHelper::buildCategoryPathLabel)
            ?: getString(R.string.record_category_sheet_breadcrumb_empty)

        dialog.setOnDismissListener {
            openSheet = RecordSheet.NONE
            pendingCategoryId = null
        }
        btnCloseSheet.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showAddChildCategoryDialog(parent: Category, onCreated: (Category) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val editName = dialogView.findViewById<EditText>(R.id.edit_category_name)
        val imageIcon = dialogView.findViewById<ImageView>(R.id.image_category_icon)
        var selectedIcon: String? = null
        bindCategoryIcon(imageIcon, selectedIcon)
        imageIcon.setOnClickListener {
            showIconPickerDialog(selectedIcon) { icon ->
                selectedIcon = icon
                bindCategoryIcon(imageIcon, selectedIcon)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.category_add_title)
            .setView(dialogView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = editName.text.toString().trim()
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(requireContext(), R.string.category_error_empty_name, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val category = Category(
                    name = name,
                    type = currentType,
                    icon = selectedIcon,
                    parentId = parent.id
                )
                try {
                    val id = databaseHelper.addCategory(category)
                    if (id == -1L) {
                        Toast.makeText(requireContext(), R.string.category_add_failed, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    category.id = id
                    onCreated(category)
                    dialog.dismiss()
                } catch (exception: DatabaseHelper.CategoryOperationException) {
                    Toast.makeText(requireContext(), exception.message ?: getString(R.string.category_add_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun bindCategoryIcon(imageView: ImageView, icon: String?) {
        val resourceId = icon?.let { name ->
            MaterialSymbolCatalog.resourceId(name).takeIf { it != 0 }
                ?: requireContext().resources.getIdentifier(name, "drawable", requireContext().packageName)
        }?.takeIf { it != 0 } ?: R.drawable.ic_category_other
        imageView.setImageResource(resourceId)
    }

    private fun showIconPickerDialog(selectedIcon: String?, onIconSelected: (String?) -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_icon_picker, null)
        val recyclerIcons = view.findViewById<RecyclerView>(R.id.recycler_icons)
        val iconAdapter = IconPickerAdapter(MaterialSymbolCatalog.icons, selectedIcon) { icon ->
            onIconSelected(icon)
        }
        recyclerIcons.adapter = iconAdapter
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.category_icon_picker_title)
            .setView(view)
            .create()
        iconAdapter.setOnIconSelected { icon ->
            onIconSelected(icon)
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
        bindCategoryIcon(imageCategoryIcon, selectedCategory?.icon)
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

    private fun saveRecord(shouldReturn: Boolean, openNewEntry: Boolean = false) {
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
        val success = if (isEditing()) {
            val existing = editingRecord ?: return
            record.id = existing.id
            record.sortOrder = existing.sortOrder
            databaseHelper.updateRecord(record) > 0
        } else {
            databaseHelper.addRecord(record) != -1L
        }

        if (success) {
            Toast.makeText(
                requireContext(),
                getString(if (isEditing()) R.string.toast_update_success else R.string.toast_save_success),
                Toast.LENGTH_SHORT
            ).show()
            if (openNewEntry) {
                openFreshRecord()
            } else if (shouldReturn) {
                navigateBack()
            } else {
                clearAmountAndDescription()
            }
        } else {
            Toast.makeText(
                requireContext(),
                getString(if (isEditing()) R.string.toast_update_failed else R.string.toast_save_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun isEditing(): Boolean = editingRecordId != null && editingRecord != null

    private fun clearAmountAndDescription() {
        editAmount.setText(getString(R.string.amount_default))
        editDescription.setText("")
    }

    private fun setupBackNavigation() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private fun openFreshRecord() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AddRecordFragment())
            .commit()
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
