package com.example.cardtally

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.net.Uri
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.AssetSheetItem
import com.example.cardtally.adapter.RecordAssetSheetAdapter
import com.example.cardtally.adapter.RecordCategoryTreeAdapter
import com.example.cardtally.adapter.IconPickerAdapter
import com.example.cardtally.adapter.LedgerCalendarAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.example.cardtally.state.RecordFormState
import com.example.cardtally.state.RecordSheet
import com.example.cardtally.state.RecordType
import com.example.cardtally.state.StableIdResolver
import com.example.cardtally.util.LedgerDateRange
import com.example.cardtally.util.LedgerPeriodHelper
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.MaterialSymbolCatalog
import com.example.cardtally.util.AmountKeypadController
import com.example.cardtally.util.RecordPhotoSettingsHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.Locale
import java.io.File
import kotlin.math.roundToInt

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
    private lateinit var btnTakePhoto: ImageButton
    private lateinit var cardPhotoPreview: View
    private lateinit var layoutPhotoThumbnails: LinearLayout
    private lateinit var btnExpense: Button
    private lateinit var btnIncome: Button
    private lateinit var btnTransfer: Button
    private lateinit var btnClose: View
    private lateinit var btnCancel: View
    private lateinit var btnSave: View
    private lateinit var btnSaveAndAdd: View
    private lateinit var rowDate: View
    private lateinit var rowAsset: View
    private lateinit var rowDestinationAsset: View
    private lateinit var dividerDestinationAsset: View
    private lateinit var dividerBeforeDate: View
    private lateinit var dividerAfterCategory: View
    private lateinit var textAssetLabel: TextView
    private lateinit var rowCategory: View
    private lateinit var textDestinationAssetValue: TextView
    private lateinit var databaseHelper: DatabaseHelper

    private var categoryAdapter: RecordCategoryTreeAdapter? = null
    private var currentCategories = mutableListOf<Category>()
    private var currentAssets = mutableListOf<Asset>()
    private var currentType = 0
    private var selectedDate: String = ""
    private var selectedCategory: Category? = null
    private var selectedAsset: Asset? = null
    private var selectedDestinationAsset: Asset? = null
    private var openSheet = RecordSheet.NONE
    private var pendingCategoryId: Long? = null
    private lateinit var backPressedCallback: OnBackPressedCallback
    private var editingRecordId: Long? = null
    private var editingRecord: Record? = null
    private var photoUris = mutableListOf<String>()
    private var savedPhotoUris = emptyList<String>()
    private var pendingPhotoUri: Uri? = null

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null) {
            photoUris.add(uri.toString())
            showPhotoPreview()
        } else if (uri != null) {
            requireContext().contentResolver.delete(uri, null, null)
        }
        pendingPhotoUri = null
    }

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
        btnTakePhoto = view.findViewById(R.id.btn_take_photo)
        cardPhotoPreview = view.findViewById(R.id.card_photo_preview)
        layoutPhotoThumbnails = view.findViewById(R.id.layout_photo_thumbnails)
        btnExpense = view.findViewById(R.id.btn_expense)
        btnIncome = view.findViewById(R.id.btn_income)
        btnTransfer = view.findViewById(R.id.btn_transfer)
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
        rowDestinationAsset = view.findViewById(R.id.row_destination_asset)
        dividerDestinationAsset = view.findViewById(R.id.divider_destination_asset)
        dividerBeforeDate = view.findViewById(R.id.divider_before_date)
        dividerAfterCategory = view.findViewById(R.id.divider_after_category)
        textAssetLabel = view.findViewById(R.id.text_asset_label)
        rowCategory = view.findViewById(R.id.row_category)
        textDestinationAssetValue = view.findViewById(R.id.text_destination_asset_value)

        databaseHelper = DatabaseHelper(requireContext())

        editingRecord = editingRecordId?.let(databaseHelper::getRecordById)
        val defaultState = editingRecord?.let { record ->
            RecordFormState(
                amountBuffer = String.format(Locale.US, "%.2f", record.amount),
                recordType = when (record.type) {
                    1 -> RecordType.INCOME
                    2 -> RecordType.TRANSFER
                    else -> RecordType.EXPENSE
                },
                selectedDate = record.date,
                selectedAssetId = record.assetId,
                selectedDestinationAssetId = record.destinationAssetId,
                selectedCategoryId = record.categoryId,
                description = record.description.orEmpty(),
                photoUri = record.photoUri,
                photoUris = record.photoUris,
                openSheet = RecordSheet.NONE,
                pendingCategoryId = null
            )
        } ?: RecordFormState.DEFAULT.copy(selectedDate = databaseHelper.getCurrentDate())
        val restoredState = RecordFormState.readFrom(savedInstanceState, defaultState)
        currentType = when (restoredState.recordType) {
            RecordType.INCOME -> 1
            RecordType.TRANSFER -> 2
            else -> 0
        }
        selectedDate = restoredState.selectedDate
        openSheet = restoredState.openSheet
        pendingCategoryId = restoredState.pendingCategoryId
        editAmount.setText(restoredState.amountBuffer)
        editDescription.setText(restoredState.description)
        photoUris = restoredState.photoUris.toMutableList().ifEmpty {
            restoredState.photoUri?.let { mutableListOf(it) } ?: mutableListOf()
        }
        savedPhotoUris = editingRecord?.photoUris ?: emptyList()
        showPhotoPreview()

        loadCategories(currentType, restoredState.selectedCategoryId)
        loadAssets(restoredState.selectedAssetId, restoredState.selectedDestinationAssetId)
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
        rowDestinationAsset.setOnClickListener { showAssetSheet(selectDestination = true) }
        rowCategory.setOnClickListener { showCategorySheet() }
        btnTakePhoto.setOnClickListener { takePhoto() }

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
            recordType = when (currentType) {
                1 -> RecordType.INCOME
                2 -> RecordType.TRANSFER
                else -> RecordType.EXPENSE
            },
            selectedDate = selectedDate,
            selectedAssetId = selectedAsset?.id,
            selectedDestinationAssetId = selectedDestinationAsset?.id,
            selectedCategoryId = selectedCategory?.id,
            description = editDescription.text.toString(),
            photoUri = photoUris.firstOrNull(),
            photoUris = photoUris,
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
        val buttons = listOf(btnExpense, btnIncome, btnTransfer)
        buttons.forEach { button ->
            button.setBackgroundResource(android.R.color.transparent)
            button.setTextColor(ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.editorial_text_muted))
            button.isSelected = false
        }

        btnTransfer.setOnClickListener {
            if (currentType != 2) {
                currentType = 2
                selectedCategory = null
                updateTypeStyle()
            }
        }
        val selectedButton = when (currentType) {
            1 -> btnIncome
            2 -> btnTransfer
            else -> btnExpense
        }
        selectedButton.setBackgroundResource(R.drawable.bg_record_type_tab_selected)
        selectedButton.setTextColor(ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnSurface))
        selectedButton.isSelected = true
        updateTransferRows()
        if (currentType == 2) {
            rowCategory.visibility = View.GONE
        } else {
            rowCategory.visibility = View.VISIBLE
        }
    }

    private fun updateTransferRows() {
        val visible = currentType == 2
        val fields = rowAsset.parent as ViewGroup
        fields.removeView(rowAsset)
        fields.removeView(dividerBeforeDate)
        if (visible) {
            val destinationIndex = fields.indexOfChild(rowDestinationAsset)
            fields.addView(rowAsset, (destinationIndex + 2).coerceAtMost(fields.childCount))
            fields.addView(dividerBeforeDate, (fields.indexOfChild(rowAsset) + 1).coerceAtMost(fields.childCount))
        } else {
            val dateIndex = fields.indexOfChild(rowDate)
            fields.addView(dividerBeforeDate, (dateIndex + 1).coerceAtMost(fields.childCount))
            fields.addView(rowAsset, (dateIndex + 2).coerceAtMost(fields.childCount))
        }
        rowDestinationAsset.visibility = if (visible) View.VISIBLE else View.GONE
        dividerDestinationAsset.visibility = if (visible) View.VISIBLE else View.GONE
        dividerAfterCategory.visibility = if (visible) View.GONE else View.VISIBLE
        textAssetLabel.setText(if (visible) R.string.record_transfer_from_asset else R.string.record_asset_label)
        textDestinationAssetValue.text = selectedDestinationAsset?.name ?: "请选择"
    }

    private fun loadCategories(type: Int, selectedCategoryId: Long? = null) {
        if (type == 2) {
            currentCategories = mutableListOf()
            selectedCategory = null
            updateCategorySummary()
            return
        }
        currentCategories = databaseHelper.getCategoryTreeByType(type).toMutableList()
        selectedCategory = selectedCategoryId?.let { id ->
            currentCategories.firstOrNull { it.id == id && isLeafCategory(it) }
        } ?: currentCategories.firstOrNull { isLeafCategory(it) }
        updateCategorySummary()
    }

    private fun loadAssets(selectedAssetId: Long? = null, selectedDestinationAssetId: Long? = null) {
        currentAssets = databaseHelper.getAllAssets().toMutableList()
        selectedAsset = selectedAssetId?.let { id -> currentAssets.firstOrNull { it.id == id } }
        selectedDestinationAsset = selectedDestinationAssetId?.let { id -> currentAssets.firstOrNull { it.id == id } }
        updateAssetSummary()
        updateTransferRows()
    }

    private fun showDateSheet() {
        openSheet = RecordSheet.DATE
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_record_date, null)
        dialog.setContentView(sheetView)

        val btnCloseSheet = sheetView.findViewById<ImageButton>(R.id.btn_close_sheet)
        val btnConfirmDate = sheetView.findViewById<View>(R.id.btn_confirm_date)
        val textSelectToday = sheetView.findViewById<TextView>(R.id.text_select_today)
        val monthTitle = sheetView.findViewById<TextView>(R.id.text_date_month_title)
        val selectionValue = sheetView.findViewById<TextView>(R.id.text_date_selection_value)
        val recyclerCalendar = sheetView.findViewById<RecyclerView>(R.id.recycler_date_calendar)
        val btnPrevMonth = sheetView.findViewById<ImageButton>(R.id.btn_date_prev_month)
        val btnNextMonth = sheetView.findViewById<ImageButton>(R.id.btn_date_next_month)

        var localDate = selectedDate
        var displayYear = localDate.substring(0, 4).toInt()
        var displayMonth = localDate.substring(5, 7).toInt() - 1
        lateinit var adapter: LedgerCalendarAdapter

        fun renderCalendar() {
            monthTitle.text = LedgerPeriodHelper.formatMonthTitle(displayYear, displayMonth)
            selectionValue.text = formatDisplayDate(localDate)
            adapter.submitList(
                LedgerPeriodHelper.buildMonthCells(
                    displayYear,
                    displayMonth,
                    LedgerDateRange(localDate, localDate)
                )
            )
        }

        adapter = LedgerCalendarAdapter(singleSelection = true) { day ->
            val picked = day.isoDate ?: return@LedgerCalendarAdapter
            localDate = picked
            displayYear = picked.substring(0, 4).toInt()
            displayMonth = picked.substring(5, 7).toInt() - 1
            renderCalendar()
        }
        recyclerCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        recyclerCalendar.adapter = adapter
        renderCalendar()

        dialog.setOnDismissListener { openSheet = RecordSheet.NONE }
        btnCloseSheet.setOnClickListener { dialog.dismiss() }
        textSelectToday.setOnClickListener {
            val calendar = Calendar.getInstance()
            localDate = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            displayYear = calendar.get(Calendar.YEAR)
            displayMonth = calendar.get(Calendar.MONTH)
            renderCalendar()
        }
        btnPrevMonth.setOnClickListener {
            val shifted = LedgerPeriodHelper.shiftMonth(displayYear, displayMonth, -1)
            displayYear = shifted.first
            displayMonth = shifted.second
            renderCalendar()
        }
        btnNextMonth.setOnClickListener {
            val shifted = LedgerPeriodHelper.shiftMonth(displayYear, displayMonth, 1)
            displayYear = shifted.first
            displayMonth = shifted.second
            renderCalendar()
        }
        btnConfirmDate.setOnClickListener {
            selectedDate = localDate
            updateDisplayedDate()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showAssetSheet(selectDestination: Boolean = false) {
        openSheet = RecordSheet.ASSET
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_record_assets, null)
        dialog.setContentView(sheetView)

        val btnCloseSheet = sheetView.findViewById<ImageButton>(R.id.btn_close_sheet)
        val recyclerAssets = sheetView.findViewById<RecyclerView>(R.id.recycler_assets)
        val selectedId = if (selectDestination) selectedDestinationAsset?.id else selectedAsset?.id
        val adapter = RecordAssetSheetAdapter(buildAssetSheetItems(), selectedId) { item ->
            if (selectDestination) {
                selectedDestinationAsset = item.asset
            } else {
                selectedAsset = item.asset
            }
            updateAssetSummary()
            updateTransferRows()
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
        val destinationAssetId = StableIdResolver.resolve(state.selectedDestinationAssetId, currentAssets.mapTo(mutableSetOf()) { it.id })
        selectedDestinationAsset = destinationAssetId?.let { id -> currentAssets.firstOrNull { it.id == id } }
        val categoryId = StableIdResolver.resolve(state.selectedCategoryId, currentCategories.mapTo(mutableSetOf()) { it.id })
        selectedCategory = categoryId?.let { id -> currentCategories.firstOrNull { it.id == id && isLeafCategory(it) } }
        pendingCategoryId = state.pendingCategoryId?.takeIf { id -> currentCategories.any { it.id == id && isLeafCategory(it) } }
        updateAssetSummary()
        updateTransferRows()
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
        val category = if (currentType == 2) "资产转资产" else selectedCategory?.name
        val description = editDescription.text.toString().trim()

        if (selectedDate.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.validation_select_date), Toast.LENGTH_SHORT).show()
            return
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.validation_enter_amount), Toast.LENGTH_SHORT).show()
            return
        }

        val amount = evaluateAmountExpression(amountStr)
        if (amount == null) {
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
            assetId = selectedAsset?.id,
            destinationAssetId = selectedDestinationAsset?.id,
            assetSource = selectedAsset?.name,
            destinationAssetSource = selectedDestinationAsset?.name,
            photoUri = photoUris.firstOrNull(),
            photoUris = photoUris
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
            val oldPhotoUris = editingRecord?.photoUris.orEmpty()
            if (isEditing()) {
                deletePhotoUris(oldPhotoUris - photoUris.toSet())
            }
            savedPhotoUris = photoUris.toList()
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

    /** Supports simple left-to-right expressions such as 12+3-1.5. */
    private fun evaluateAmountExpression(expression: String): Double? {
        val normalized = expression.replace(" ", "")
        if (normalized.isEmpty() || !normalized.matches(Regex("\\d+(\\.\\d+)?([+-]\\d+(\\.\\d+)?)*"))) {
            return null
        }
        val tokens = normalized.split(Regex("(?=[+-])|(?<=[+-])"))
        var result = tokens.firstOrNull()?.toDoubleOrNull() ?: return null
        var index = 1
        while (index + 1 < tokens.size) {
            val operand = tokens[index + 1].toDoubleOrNull() ?: return null
            result = when (tokens[index]) {
                "+" -> result + operand
                "-" -> result - operand
                else -> return null
            }
            index += 2
        }
        return result
    }

    private fun clearAmountAndDescription() {
        editAmount.setText(getString(R.string.amount_default))
        editDescription.setText("")
        savedPhotoUris = photoUris.toList()
        photoUris.clear()
        cardPhotoPreview.visibility = View.GONE
        layoutPhotoThumbnails.removeAllViews()
    }

    private fun takePhoto() {
        val maxPhotos = RecordPhotoSettingsHelper.getMaxPhotos(requireContext())
        if (photoUris.size >= maxPhotos) {
            Toast.makeText(requireContext(), "当前记录最多添加${maxPhotos}张图片", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentType == 2) {
            if (selectedAsset == null || selectedDestinationAsset == null) {
                Toast.makeText(requireContext(), "请选择转出和转入资产", Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedAsset?.id == selectedDestinationAsset?.id) {
                Toast.makeText(requireContext(), "转出和转入资产不能相同", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val directory = File(requireContext().filesDir, "record_photos").apply { mkdirs() }
        val photoFile = File(directory, "record_${System.currentTimeMillis()}.jpg")
        pendingPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePhotoLauncher.launch(pendingPhotoUri)
    }

    private fun showPhotoPreview() {
        layoutPhotoThumbnails.removeAllViews()
        if (photoUris.isEmpty()) {
            cardPhotoPreview.visibility = View.GONE
            return
        }
        photoUris.forEachIndexed { index, uriString ->
            val frame = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(88.dp(), 88.dp()).apply {
                    if (index > 0) marginStart = 8.dp()
                }
            }
            val image = ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(-1, -1)
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "查看第${index + 1}张照片"
                setImageURI(Uri.parse(uriString))
                setOnClickListener { showPhotoFullScreen(uriString) }
            }
            val remove = ImageButton(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(24.dp(), 24.dp(), Gravity.TOP or Gravity.END)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_primary_container)
                setImageResource(R.drawable.ic_close)
                imageTintList = ColorStateList.valueOf(
                    ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.error_primary)
                )
                contentDescription = "删除第${index + 1}张照片"
                setPadding(4.dp(), 4.dp(), 4.dp(), 4.dp())
                setOnClickListener {
                    photoUris.removeAt(index)
                    showPhotoPreview()
                }
            }
            frame.addView(image)
            frame.addView(remove)
            layoutPhotoThumbnails.addView(frame)
        }
        cardPhotoPreview.visibility = View.VISIBLE
    }

    private fun showPhotoFullScreen(uriString: String) {
        val uri = Uri.parse(uriString)
        val dialog = Dialog(requireContext())
        val imageView = ImageView(requireContext()).apply {
            setBackgroundColor(Color.BLACK)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageURI(uri)
            setOnClickListener { dialog.dismiss() }
            contentDescription = "全屏查看照片，点击关闭"
        }
        dialog.setContentView(imageView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).roundToInt()

    private fun deletePhotoUris(uris: Collection<String>) {
        uris.forEach { uriString ->
            runCatching {
                requireContext().contentResolver.delete(Uri.parse(uriString), null, null)
            }
        }
    }

    override fun onDestroyView() {
        deletePhotoUris(photoUris.filterNot { it in savedPhotoUris })
        super.onDestroyView()
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
