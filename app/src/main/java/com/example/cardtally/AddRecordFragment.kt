package com.example.cardtally

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.RecordCategoryTreeAdapter
import com.example.cardtally.adapter.RecordCategoryGroupAdapter
import com.example.cardtally.adapter.CategorySelectorAdapter
import com.example.cardtally.adapter.IconPickerDialog
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
import com.example.cardtally.util.TablerIconCatalog
import com.example.cardtally.util.AmountKeypadController
import com.example.cardtally.util.EntryAmountLayoutController
import com.example.cardtally.util.Money
import com.example.cardtally.util.RecordEntryMode
import com.example.cardtally.util.RecordEntryModePreferences
import com.example.cardtally.util.RecordCategoryOrderPreferences
import com.example.cardtally.util.DefaultRecordAssetPreferences
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
    private var textCategoryValue: TextView? = null
    private var imageCategoryIcon: ImageView? = null
    private lateinit var editAmount: EditText
    private lateinit var editDescription: EditText
    private lateinit var btnTakePhoto: View
    private lateinit var cardPhotoPreview: View
    private lateinit var layoutPhotoThumbnails: LinearLayout
    private lateinit var btnExpense: Button
    private lateinit var btnIncome: Button
    private lateinit var btnTransfer: Button
    private lateinit var btnClose: View
    private lateinit var btnSave: View
    private lateinit var btnSaveAndAdd: View
    private lateinit var rowDate: View
    private lateinit var rowAsset: View
    private lateinit var rowDestinationAsset: View
    private var dividerBeforeDate: View? = null
    private var dividerAfterCategory: View? = null
    private lateinit var textAssetLabel: TextView
    private var rowCategory: View? = null
    private var rowAssetSingle: View? = null
    private var textAssetSingleValue: TextView? = null
    private var textSourceAssetLabel: TextView? = null
    private var textDestinationAssetLabel: TextView? = null
    private var textSourceAssetBalance: TextView? = null
    private var textDestinationAssetBalance: TextView? = null
    private var rowFee: View? = null
    private var editFee: EditText? = null
    private var transferAccountsBlock: View? = null
    private lateinit var textDestinationAssetValue: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var amountKeypadController: AmountKeypadController
    private var entryAmountLayoutController: EntryAmountLayoutController? = null
    private var rootView: View? = null
    private var isSystemImeVisible = false
    private var imeGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private var categoryAdapter: RecordCategoryTreeAdapter? = null
    private var quickCategoryAdapter: CategorySelectorAdapter? = null
    private var standardCategoryAdapter: RecordCategoryGroupAdapter? = null
    private var isQuickMode = false
    private var isQuickCategoryMode = false
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
    private var isInitialEntryLoad = true

    /** Current record sheet, exposed internally for device interaction tests. */
    internal var activeSheetDialogForTest: BottomSheetDialog? = null
        private set

    internal fun onAssetPickerDialogShown(dialog: BottomSheetDialog) {
        activeSheetDialogForTest = dialog
    }

    internal fun onAssetPickerSelected(assetId: Long, selectDestination: Boolean) {
        val asset = currentAssets.firstOrNull { it.id == assetId } ?: return
        if (selectDestination) selectedDestinationAsset = asset else selectedAsset = asset
        updateAssetSummary()
        updateTransferRows()
    }

    internal fun onAssetPickerDismissed() {
        openSheet = RecordSheet.NONE
        restoreAmountKeypadAfterSheet()
        activeSheetDialogForTest = null
    }

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
        isInitialEntryLoad = savedInstanceState == null
        isQuickCategoryMode = RecordEntryModePreferences.getMode(requireContext()) == RecordEntryMode.QUICK
        // Both modes use the compact entry shell; only their category surface differs.
        isQuickMode = true
        val view = inflater.inflate(R.layout.fragment_add_record_quick, container, false)
        rootView = view

        textDate = view.findViewById(R.id.text_date)
        textAssetValue = view.findViewById(R.id.text_asset_value)
        textSourceAssetLabel = view.findViewById(R.id.text_asset_label)
        textDestinationAssetLabel = view.findViewById(R.id.text_destination_asset_label)
        textSourceAssetBalance = view.findViewById(R.id.text_source_asset_balance)
        textDestinationAssetBalance = view.findViewById(R.id.text_destination_asset_balance)
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
        btnSave = view.findViewById(R.id.btn_save)
        btnSaveAndAdd = view.findViewById(R.id.btn_save_and_add)
        val amountKeypad = view.findViewById<View>(R.id.layout_amount_keypad)
        amountKeypadController = AmountKeypadController(
            requireContext(),
            editAmount,
            amountKeypad,
            view.findViewById(R.id.layout_buttons),
            alwaysVisible = isQuickMode,
            onConfirm = {
            editAmount.clearFocus()
            }
        ).also { it.bind() }
        rowDate = view.findViewById(R.id.row_date)
        rowAsset = view.findViewById(R.id.row_asset)
        rowDestinationAsset = view.findViewById(R.id.row_destination_asset)
        dividerBeforeDate = view.findViewById(R.id.divider_before_date)
        dividerAfterCategory = view.findViewById(R.id.divider_after_category)
        textAssetLabel = view.findViewById(R.id.text_asset_label)
        rowCategory = view.findViewById(R.id.row_category)
        textDestinationAssetValue = view.findViewById(R.id.text_destination_asset_value)
        rowAssetSingle = view.findViewById(R.id.row_asset_single)
        textAssetSingleValue = view.findViewById(R.id.text_asset_single_value)
        rowFee = view.findViewById(R.id.row_fee)
        editFee = view.findViewById(R.id.edit_fee)
        transferAccountsBlock = view.findViewById(R.id.transfer_accounts_block)
        editFee?.let { fee ->
            amountKeypadController.bindTarget(fee)
            fee.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) prepareNumericTarget()
                false
            }
            fee.setOnClickListener {
                selectNumericTarget(fee)
                updateActiveNumericField()
            }
        }
        rowFee?.setOnClickListener {
            val fee = editFee ?: return@setOnClickListener
            selectNumericTarget(fee)
            updateActiveNumericField()
        }
        editAmount.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) prepareNumericTarget()
            false
        }
        editAmount.setOnClickListener {
            selectNumericTarget(editAmount)
            updateActiveNumericField()
        }
        (editAmount.parent as? View)?.let { amountRow ->
            entryAmountLayoutController = EntryAmountLayoutController(
                amountRow,
                editAmount,
                view.findViewById(R.id.text_amount_prefix)
            ).also { it.attach() }
        }
        view.findViewById<View>(R.id.btn_swap_transfer_assets)?.setOnClickListener {
            val source = selectedAsset
            selectedAsset = selectedDestinationAsset
            selectedDestinationAsset = source
            updateTransferRows()
        }

        databaseHelper = DatabaseHelper(requireContext())

        view.findViewById<RecyclerView>(R.id.recycler_quick_categories)?.let { quickGrid ->
            val columns = (resources.displayMetrics.widthPixels / resources.displayMetrics.density / 72f)
                .toInt()
                .coerceIn(3, 5)
            quickGrid.layoutManager = GridLayoutManager(requireContext(), columns)
            quickCategoryAdapter = CategorySelectorAdapter(
                emptyList(), null,
                { databaseHelper.buildCategoryPathLabel(it.id) ?: it.name }
            ) { category ->
                selectedCategory = category
                updateCategorySummary()
            }
            quickGrid.adapter = quickCategoryAdapter
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                0
            ) {
                private var moved = false

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.adapterPosition
                    val to = target.adapterPosition
                    if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                    quickCategoryAdapter?.moveCategory(from, to)
                    moved = true
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    if (moved) {
                        RecordCategoryOrderPreferences.saveOrder(
                            requireContext(),
                            currentType,
                            quickMode = true,
                            categoryIds = quickCategoryAdapter?.categoryIdsInOrder().orEmpty()
                        )
                        moved = false
                    }
                }
            }).attachToRecyclerView(quickGrid)
        }
        view.findViewById<RecyclerView>(R.id.recycler_standard_categories)?.let { standardGrid ->
            standardCategoryAdapter = RecordCategoryGroupAdapter(emptyList(), null) { category ->
                selectedCategory = category
                updateCategorySummary()
            }
            standardGrid.layoutManager = LinearLayoutManager(requireContext())
            standardGrid.adapter = standardCategoryAdapter
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                0
            ) {
                private var moved = false

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.adapterPosition
                    val to = target.adapterPosition
                    if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                    standardCategoryAdapter?.moveParent(from, to)
                    moved = true
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    if (moved) {
                        RecordCategoryOrderPreferences.saveOrder(
                            requireContext(),
                            currentType,
                            quickMode = false,
                            categoryIds = standardCategoryAdapter?.parentIdsInOrder().orEmpty()
                        )
                        moved = false
                    }
                }
            }).attachToRecyclerView(standardGrid)
        }
        view.findViewById<RecyclerView>(R.id.recycler_quick_categories)?.visibility =
            if (isQuickCategoryMode) View.VISIBLE else View.GONE
        view.findViewById<RecyclerView>(R.id.recycler_standard_categories)?.visibility =
            if (isQuickCategoryMode) View.GONE else View.VISIBLE

        editDescription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                amountKeypadController.hideForSoftKeyboard()
                updateQuickTransferLayout()
            } else if (openSheet == RecordSheet.NONE) {
                amountKeypadController.restoreAfterSoftKeyboard()
                updateQuickTransferLayout()
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            onSystemImeVisibilityChanged(insets.isVisible(WindowInsetsCompat.Type.ime()))
            insets
        }
        observeSystemImeVisibility(view)
        ViewCompat.requestApplyInsets(view)


        editingRecord = editingRecordId?.let(databaseHelper::getRecordById)
        val defaultState = editingRecord?.let { record ->
            RecordFormState(
                amountBuffer = Money.formatYuan(record.amount),
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
                feeBuffer = if (record.type == 2 && record.fee > 0.0) Money.formatYuan(record.fee) else "",
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
        if (isQuickMode && editAmount.text.toString() == getString(R.string.amount_default)) {
            editAmount.setText("")
        }
        editDescription.setText(restoredState.description)
        editFee?.setText(restoredState.feeBuffer)
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
            view.findViewById<TextView>(R.id.text_save_label)?.text = getString(R.string.record_update)
            (btnSave as? TextView)?.text = getString(R.string.record_update)
        }
        updateTypeStyle()
        updateActiveNumericField()
        setupBackNavigation()

        rowDate.setOnClickListener { showDateSheet() }
        rowAsset.setOnClickListener { showAssetSheet() }
        rowDestinationAsset.setOnClickListener { showAssetSheet(selectDestination = true) }
        rowAssetSingle?.setOnClickListener { showAssetSheet() }
        rowCategory?.setOnClickListener { showCategorySheet() }
        btnTakePhoto.setOnClickListener { takePhoto() }

        btnExpense.setOnClickListener {
            selectRecordType(0)
        }

        btnIncome.setOnClickListener {
            selectRecordType(1)
        }

        btnClose.setOnClickListener { navigateBack() }
        btnSave.setOnClickListener { saveRecord(true) }
        btnSaveAndAdd.setOnClickListener { saveRecord(false, true) }

        view.post {
            if (openSheet == RecordSheet.NONE) {
                amountKeypadController.selectTarget(editAmount)
            } else {
                view.requestFocus()
                hideSystemIme()
            }
            restoreOpenSheet()
        }
        return view
    }

    private fun selectNumericTarget(target: EditText) {
        prepareNumericTarget()
        amountKeypadController.selectTarget(target)
    }

    private fun selectRecordType(type: Int) {
        if (currentType == type) return

        // Type tabs must leave note-editing mode before changing the quick layout.
        selectNumericTarget(editAmount)
        hideSystemIme()
        currentType = type
        if (type == 2) selectedCategory = null
        updateTypeStyle()
        if (type != 2) loadCategories(type, null)
    }

    private fun prepareNumericTarget() {
        editDescription.clearFocus()
        hideSystemIme()
    }

    private fun hideSystemIme() {
        val root = rootView ?: view ?: return
        val inputMethod = requireContext().getSystemService(InputMethodManager::class.java)
        fun hide() {
            val hostActivity = activity ?: return
            inputMethod?.hideSoftInputFromWindow(root.windowToken, 0)
            WindowInsetsControllerCompat(hostActivity.window, root).hide(WindowInsetsCompat.Type.ime())
        }
        hide()
        root.post { hide() }
        root.postDelayed({ hide() }, 250)
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
            feeBuffer = editFee?.text?.toString().orEmpty(),
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
            button.setTypeface(null, Typeface.NORMAL)
            button.isSelected = false
        }

        btnTransfer.setOnClickListener {
            selectRecordType(2)
        }
        val selectedButton = when (currentType) {
            1 -> btnIncome
            2 -> btnTransfer
            else -> btnExpense
        }
        selectedButton.setBackgroundResource(R.drawable.bg_record_type_tab_selected)
        selectedButton.setTextColor(ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnSurface))
        selectedButton.setTypeface(null, Typeface.BOLD)
        selectedButton.isSelected = true
        updateTransferRows()
    }

    private fun updateTransferRows() {
        val visible = currentType == 2
        transferAccountsBlock?.visibility = if (visible) View.VISIBLE else View.GONE
        rowAssetSingle?.visibility = if (visible) View.GONE else View.VISIBLE
        rowFee?.visibility = if (visible) View.VISIBLE else View.GONE
        rowCategory?.visibility = if (visible) View.GONE else View.VISIBLE
        rootView?.findViewById<View>(R.id.card_quick_categories)?.visibility =
            if (visible) View.GONE else View.VISIBLE
        dividerAfterCategory?.visibility = if (visible) View.GONE else View.VISIBLE
        textAssetLabel.setText(R.string.record_transfer_from_asset)
        textSourceAssetLabel?.isSelected = true
        textDestinationAssetLabel?.isSelected = true
        updateAssetSummary()
        textDestinationAssetValue.text =
            selectedDestinationAsset?.name ?: getString(R.string.record_transfer_to_asset_hint)
        textDestinationAssetValue.isSelected = true
        if (isQuickMode) {
            val destinationSelected = selectedDestinationAsset != null
            textDestinationAssetValue.setTypeface(
                null,
                if (destinationSelected) Typeface.BOLD else Typeface.NORMAL
            )
            textDestinationAssetValue.setTextColor(
                ThemeColorHelper.resolveColor(
                    requireContext(),
                    if (destinationSelected) {
                        com.google.android.material.R.attr.colorOnSurface
                    } else {
                        com.google.android.material.R.attr.colorOnSurfaceVariant
                    }
                )
            )
        }
        updateQuickTransferLayout()
        if (!visible) {
            editFee?.setText("")
            if (::amountKeypadController.isInitialized) {
                amountKeypadController.resetTarget(editAmount)
                updateActiveNumericField()
            }
        }
    }

    private fun updateActiveNumericField() {
        if (!isQuickMode || !::amountKeypadController.isInitialized) return
        val feeActive = amountKeypadController.currentTarget() === editFee
        val activeColor = ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorPrimary)
        val normalColor = ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnSurface)
        editAmount.setTextColor(if (feeActive) normalColor else activeColor)
        editFee?.setTextColor(if (feeActive) activeColor else normalColor)
    }

    private fun loadCategories(type: Int, selectedCategoryId: Long? = null) {
        if (type == 2) {
            currentCategories = mutableListOf()
            selectedCategory = null
            quickCategoryAdapter?.updateCategories(emptyList())
            quickCategoryAdapter?.setSelectedCategoryId(null)
            standardCategoryAdapter?.updateCategories(emptyList())
            standardCategoryAdapter?.setSelectedCategoryId(null)
            updateQuickCategoryEmpty()
            updateCategorySummary()
            return
        }
        currentCategories = databaseHelper.getCategoryTreeByType(type).toMutableList()
        selectedCategory = selectedCategoryId?.let { id ->
            currentCategories.firstOrNull { it.id == id && isLeafCategory(it) }
        } ?: currentCategories.firstOrNull { isLeafCategory(it) }
        quickCategoryAdapter?.updateCategories(
            RecordCategoryOrderPreferences.orderForMode(requireContext(), currentCategories, quickMode = true)
        )
        quickCategoryAdapter?.setSelectedCategoryId(selectedCategory?.id)
        standardCategoryAdapter?.updateCategories(standardModeCategoriesInOrder())
        standardCategoryAdapter?.setSelectedCategoryId(
            selectedCategory?.id,
            expandParent = !isQuickCategoryMode && isEditing()
        )
        updateQuickCategoryEmpty()
        updateCategorySummary()
    }

    private fun updateQuickCategoryEmpty() {
        val grid = rootView?.findViewById<RecyclerView>(
            if (isQuickCategoryMode) R.id.recycler_quick_categories
            else R.id.recycler_standard_categories
        ) ?: return
        val empty = rootView?.findViewById<TextView>(R.id.text_quick_category_empty)
        val count = if (isQuickCategoryMode) {
            quickCategoryAdapter?.itemCount ?: 0
        } else {
            standardCategoryAdapter?.itemCount ?: 0
        }
        grid.visibility = if (count == 0) View.GONE else View.VISIBLE
        empty?.visibility = if (count == 0) View.VISIBLE else View.GONE
    }


    private fun loadAssets(selectedAssetId: Long? = null, selectedDestinationAssetId: Long? = null) {
        currentAssets = databaseHelper.getAllAssets().toMutableList()
        val defaultAssetId = if (!isEditing() && isInitialEntryLoad && currentType != 2) {
            if (currentType == 1) {
                DefaultRecordAssetPreferences.getIncomeAssetId(requireContext())
            } else {
                DefaultRecordAssetPreferences.getExpenseAssetId(requireContext())
            }
        } else {
            null
        }
        val requestedAssetId = selectedAssetId ?: defaultAssetId
        selectedAsset = requestedAssetId?.let { id -> currentAssets.firstOrNull { it.id == id } }
        if (selectedAsset == null && defaultAssetId != null) {
            if (currentType == 1) {
                DefaultRecordAssetPreferences.saveIncomeAssetId(requireContext(), null)
            } else {
                DefaultRecordAssetPreferences.saveExpenseAssetId(requireContext(), null)
            }
        }
        selectedDestinationAsset = selectedDestinationAssetId?.let { id -> currentAssets.firstOrNull { it.id == id } }
        updateAssetSummary()
        updateTransferRows()
    }

    private fun standardModeCategoriesInOrder(): List<Category> {
        val orderedParents = RecordCategoryOrderPreferences.orderForMode(
            requireContext(),
            currentCategories,
            quickMode = false
        )
        val childrenByParent = currentCategories.filter { it.parentId != null }.groupBy { it.parentId }
        return orderedParents.flatMap { parent ->
            listOf(parent) + childrenByParent[parent.id].orEmpty()
        }
    }

    private fun hideAmountKeypad() {
        if (::amountKeypadController.isInitialized) {
            amountKeypadController.hideForModal()
        }
    }

    private fun restoreAmountKeypadAfterSheet() {
        if (::amountKeypadController.isInitialized) {
            amountKeypadController.restoreAfterModal()
        }
        updateQuickTransferLayout()
    }

    private fun updateQuickTransferLayout() {
        if (!isQuickMode) return
        val root = rootView ?: return
        val transferBlock = root.findViewById<View>(R.id.transfer_accounts_block) ?: return
        val categoryCard = root.findViewById<View>(R.id.card_quick_categories) ?: return
        val panel = root.findViewById<View>(R.id.quick_record_panel) ?: return
        val keypad = root.findViewById<View>(R.id.layout_amount_keypad) ?: return
        val transferParams = transferBlock.layoutParams as? ConstraintLayout.LayoutParams ?: return
        val categoryParams = categoryCard.layoutParams as? ConstraintLayout.LayoutParams ?: return
        val panelParams = panel.layoutParams as? ConstraintLayout.LayoutParams ?: return

        if (currentType == 2) {
            // Transfer uses the same content-card chain as income and expense.
            transferParams.height = 0
            transferParams.bottomToTop = panel.id
            transferParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            panelParams.topToBottom = ConstraintLayout.LayoutParams.UNSET
        } else {
            // A gone transfer card must not participate in the form's vertical chain.
            panelParams.topToBottom = ConstraintLayout.LayoutParams.UNSET
            categoryParams.bottomToTop = panel.id
        }
        panelParams.bottomToTop = keypad.id
        panelParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET

        transferBlock.layoutParams = transferParams
        categoryCard.layoutParams = categoryParams
        panel.layoutParams = panelParams
    }

    private fun restoreQuickAmountKeypadAfterImeDismissal() {
        if (!isQuickMode || openSheet != RecordSheet.NONE || !editDescription.hasFocus()) return

        editDescription.clearFocus()
        updateQuickTransferLayout()
        amountKeypadController.selectTarget(amountKeypadController.currentTarget())
        updateActiveNumericField()
    }

    private fun observeSystemImeVisibility(root: View) {
        imeGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (!isAdded || view !== root) return@OnGlobalLayoutListener
            val visibleFrame = Rect()
            root.getWindowVisibleDisplayFrame(visibleFrame)
            onSystemImeVisibilityChanged(root.rootView.height - visibleFrame.bottom > 120.dp())
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(imeGlobalLayoutListener)
    }

    private fun onSystemImeVisibilityChanged(imeVisible: Boolean) {
        val imeWasVisible = isSystemImeVisible
        isSystemImeVisible = imeVisible
        if (imeWasVisible && !imeVisible) {
            restoreQuickAmountKeypadAfterImeDismissal()
        }
    }

    private fun dismissKeyboardAndClearFocus() {
        view?.clearFocus()
        activity?.currentFocus?.clearFocus()
        hideSystemIme()
    }

    private fun showDateSheet() {
        openSheet = RecordSheet.DATE
        dismissKeyboardAndClearFocus()
        hideAmountKeypad()
        val dialog = BottomSheetDialog(requireContext())
        activeSheetDialogForTest = dialog
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

        adapter = LedgerCalendarAdapter(singleSelection = true, compact = true, showAmounts = false) { day ->
            val picked = day.isoDate ?: return@LedgerCalendarAdapter
            localDate = picked
            displayYear = picked.substring(0, 4).toInt()
            displayMonth = picked.substring(5, 7).toInt() - 1
            renderCalendar()
        }
        recyclerCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        recyclerCalendar.adapter = adapter
        renderCalendar()

        dialog.setOnDismissListener {
            openSheet = RecordSheet.NONE
            restoreAmountKeypadAfterSheet()
            if (activeSheetDialogForTest === dialog) activeSheetDialogForTest = null
        }
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
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        dialog.show()
    }

    private fun showAssetSheet(selectDestination: Boolean = false) {
        openSheet = RecordSheet.ASSET
        dismissKeyboardAndClearFocus()
        hideAmountKeypad()
        val selectedId = if (selectDestination) selectedDestinationAsset?.id else selectedAsset?.id
        RecordAssetPickerBottomSheetFragment.newInstance(
            selectDestination = selectDestination,
            excludedAssetId = if (selectDestination) selectedAsset?.id else selectedDestinationAsset?.id,
            selectedAssetId = selectedId
        ).show(parentFragmentManager, RecordAssetPickerBottomSheetFragment.TAG)
    }

    private fun showCategorySheet() {
        openSheet = RecordSheet.CATEGORY
        dismissKeyboardAndClearFocus()
        hideAmountKeypad()
        val dialog = BottomSheetDialog(requireContext())
        activeSheetDialogForTest = dialog
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_record_category, null)
        dialog.setContentView(sheetView)

        val btnCloseSheet = sheetView.findViewById<ImageButton>(R.id.btn_close_sheet)
        val recyclerCategories = sheetView.findViewById<RecyclerView>(R.id.recycler_categories)

        var pendingCategory = pendingCategoryId?.let { id -> currentCategories.firstOrNull { it.id == id } }
            ?: selectedCategory
        pendingCategoryId = pendingCategory?.id
        categoryAdapter = RecordCategoryTreeAdapter(
            currentCategories,
            pendingCategoryId,
            onCategorySelected = { category ->
                pendingCategory = category
                pendingCategoryId = category.id
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

        dialog.setOnDismissListener {
            openSheet = RecordSheet.NONE
            pendingCategoryId = null
            restoreAmountKeypadAfterSheet()
            if (activeSheetDialogForTest === dialog) activeSheetDialogForTest = null
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
            TablerIconCatalog.resourceId(name).takeIf { it != 0 }
                ?: requireContext().resources.getIdentifier(name, "drawable", requireContext().packageName)
        }?.takeIf { it != 0 } ?: R.drawable.ic_category_other
        imageView.setImageResource(resourceId)
    }

    private fun showIconPickerDialog(selectedIcon: String?, onIconSelected: (String?) -> Unit) {
        dismissKeyboardAndClearFocus()
        IconPickerDialog.show(requireContext(), initialSelectedIcon = selectedIcon, onIconSelected = onIconSelected)
    }


    private fun updateDisplayedDate() {
        textDate.text = if (isQuickMode && selectedDate == databaseHelper.getCurrentDate()) {
            getString(R.string.record_date_today)
        } else {
            formatDisplayDate(selectedDate)
        }
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
        val noneLabel = if (isQuickMode) R.string.record_asset_none_short else R.string.record_asset_none
        textAssetSingleValue?.text = selectedAsset?.name ?: getString(noneLabel)
        val sourceSelected = selectedAsset != null
        textAssetValue.text = selectedAsset?.name
            ?: if (currentType == 2) {
                getString(R.string.record_transfer_from_asset_hint)
            } else {
                getString(noneLabel)
            }
        textAssetValue.isSelected = true
        textSourceAssetBalance?.apply {
            text = "¥${Money.formatYuan(selectedAsset?.amount ?: 0.0)}"
            isSelected = true
        }
        textDestinationAssetBalance?.apply {
            text = "¥${Money.formatYuan(selectedDestinationAsset?.amount ?: 0.0)}"
            isSelected = true
        }
        if (isQuickMode && currentType == 2) {
            textAssetValue.setTypeface(null, if (sourceSelected) Typeface.BOLD else Typeface.NORMAL)
            textAssetValue.setTextColor(
                ThemeColorHelper.resolveColor(
                    requireContext(),
                    if (sourceSelected) {
                        com.google.android.material.R.attr.colorOnSurface
                    } else {
                        com.google.android.material.R.attr.colorOnSurfaceVariant
                    }
                )
            )
        }
    }

    private fun updateCategorySummary() {
        textCategoryValue?.text = selectedCategory?.id?.let(databaseHelper::buildCategoryPathLabel)
            ?: getString(R.string.record_category_unselected)
        imageCategoryIcon?.let { bindCategoryIcon(it, selectedCategory?.icon) }
        quickCategoryAdapter?.setSelectedCategoryId(selectedCategory?.id)
        standardCategoryAdapter?.setSelectedCategoryId(selectedCategory?.id)
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

        val fee = if (currentType == 2) {
            val feeStr = editFee?.text?.toString()?.trim().orEmpty()
            if (feeStr.isEmpty()) {
                0.0
            } else {
                val parsed = evaluateAmountExpression(feeStr)
                if (parsed == null || parsed < 0.0) {
                    Toast.makeText(requireContext(), getString(R.string.record_fee_invalid), Toast.LENGTH_SHORT).show()
                    return
                }
                parsed
            }
        } else {
            0.0
        }

        if (currentType == 2) {
            if (selectedAsset == null || selectedDestinationAsset == null) {
                Toast.makeText(requireContext(), getString(R.string.record_transfer_select_assets), Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedAsset?.id == selectedDestinationAsset?.id) {
                Toast.makeText(requireContext(), getString(R.string.record_transfer_same_asset), Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (category == null) {
            Toast.makeText(requireContext(), getString(R.string.validation_select_category), Toast.LENGTH_SHORT).show()
            return
        }

        if (currentType != 2 && (selectedCategory?.let { isLeafCategory(it) } != true)) {
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
            fee = fee,
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
    private fun evaluateAmountExpression(expression: String): Double? =
        Money.evaluateYuanExpression(expression)?.let(Money::toMajorDouble)

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
            Toast.makeText(requireContext(), getString(R.string.record_photo_limit_reached, maxPhotos), Toast.LENGTH_SHORT).show()
            return
        }

        if (currentType == 2) {
            if (selectedAsset == null || selectedDestinationAsset == null) {
                Toast.makeText(requireContext(), getString(R.string.record_transfer_select_assets), Toast.LENGTH_SHORT).show()
                return
            }
            if (selectedAsset?.id == selectedDestinationAsset?.id) {
                Toast.makeText(requireContext(), getString(R.string.record_transfer_same_asset), Toast.LENGTH_SHORT).show()
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
        val maxPhotos = RecordPhotoSettingsHelper.getMaxPhotos(requireContext())
        rootView?.findViewById<TextView>(R.id.text_photo_count)?.text =
            getString(R.string.record_attachment_count, photoUris.size, maxPhotos)
        layoutPhotoThumbnails.removeAllViews()
        if (photoUris.isEmpty()) {
            cardPhotoPreview.visibility = View.GONE
            return
        }
        val thumbSize = if (isQuickMode) 64.dp() else 88.dp()
        photoUris.forEachIndexed { index, uriString ->
            val frame = FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(thumbSize, thumbSize).apply {
                    if (index > 0) marginStart = 8.dp()
                }
            }
            val image = ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(-1, -1)
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = getString(R.string.record_photo_preview_description, index + 1)
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
                contentDescription = getString(R.string.record_photo_delete_description, index + 1)
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
            contentDescription = getString(R.string.record_photo_fullscreen_description)
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
        rootView?.viewTreeObserver?.let { observer ->
            imeGlobalLayoutListener?.let(observer::removeOnGlobalLayoutListener)
        }
        imeGlobalLayoutListener = null
        entryAmountLayoutController?.detach()
        entryAmountLayoutController = null
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
