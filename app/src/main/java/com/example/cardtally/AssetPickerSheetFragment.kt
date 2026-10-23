package com.example.cardtally

import android.app.Dialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.DefaultRecordAssetPickerAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.util.AssetDisplayHelper
import com.example.cardtally.util.BankIconCatalog
import com.example.cardtally.util.DefaultAssetPickerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Shared presentation only; concrete hosts own result contracts and persistence. */
abstract class AssetPickerSheetFragment : BottomSheetDialogFragment() {
    protected abstract val selectedAssetId: Long?
    protected open val pickerLedgerId: Long? = null
    protected open val excludedAssetId: Long? = null
    protected open val allowNoAsset: Boolean = false
    protected abstract val titleRes: Int
    protected open val descriptionRes: Int? = null
    protected abstract fun returnAssetSelection(assetId: Long?)
    private var pickerAdapter: DefaultRecordAssetPickerAdapter? = null
    private var assets = emptyList<Asset>()
    private var query = ""
    private var ledgerId: Long? = null
    private var returningSelection = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_default_record_asset_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = view.findViewById<TextView>(R.id.text_default_asset_title)
        title.setText(titleRes)
        ViewCompat.setAccessibilityHeading(title, true)
        ViewCompat.setAccessibilityPaneTitle(view, title.text)
        view.findViewById<TextView>(R.id.text_default_asset_description).apply {
            descriptionRes?.let(::setText)
            visibility = if (descriptionRes == null) View.GONE else View.VISIBLE
        }
        view.findViewById<View>(R.id.button_close_default_asset).setOnClickListener { dismiss() }
        DatabaseHelper(requireContext(), pickerLedgerId).use { helper ->
            ledgerId = helper.getCurrentLedger()?.id
            assets = helper.getAllAssets(pickerLedgerId).filter { it.id != excludedAssetId }
        }
        val list = view.findViewById<RecyclerView>(R.id.recycler_default_assets)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.itemAnimator = null
        pickerAdapter = DefaultRecordAssetPickerAdapter(selectedAssetId, ::selectAsset)
        list.adapter = pickerAdapter
        val search = view.findViewById<EditText>(R.id.edit_default_asset_search)
        search.isSaveEnabled = false
        query = savedInstanceState?.getString(KEY_QUERY).orEmpty()
        search.setText(query)
        search.visibility = if (assets.size > 5 || query.isNotEmpty()) View.VISIBLE else View.GONE
        search.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(search.windowToken, 0)
                search.clearFocus()
                true
            } else false
        }
        search.doAfterTextChanged {
            query = it?.toString().orEmpty()
            showAssets(view, revealSelected = false)
        }
        showAssets(view, revealSelected = savedInstanceState == null)
        val bottomPadding = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { root, insets ->
            root.setPadding(root.paddingLeft, root.paddingTop, root.paddingRight,
                bottomPadding + insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
    }

    private fun showAssets(root: View, revealSelected: Boolean) {
        val results = DefaultAssetPickerOptions.groupedRows(assets, query, allowNoAsset) { asset ->
            BankIconCatalog.forCard(asset.categoryLabel, asset.categoryIconName, asset.type)?.let {
                BankIconCatalog.displayLabel(requireContext(), asset.categoryLabel, it)
            }.orEmpty()
        }
        root.findViewById<TextView>(R.id.text_default_asset_empty).apply {
            visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            setText(if (assets.isNotEmpty()) R.string.default_asset_no_matches
                else if (AssetDisplayHelper.getShowAsset(requireContext())) R.string.record_asset_sheet_empty
                else R.string.record_asset_sheet_empty_nav)
        }
        root.findViewById<View>(R.id.recycler_default_assets).visibility =
            if (results.isEmpty()) View.GONE else View.VISIBLE
        val submittedQuery = query
        pickerAdapter?.submitList(results) {
            if (view !== root || query != submittedQuery || !revealSelected) return@submitList
            val index = results.indexOfFirst {
                (it is DefaultAssetPickerOptions.Row.Account && it.asset.id == selectedAssetId) ||
                    (it is DefaultAssetPickerOptions.Row.NoAsset && selectedAssetId == null)
            }
            if (index >= 0) root.findViewById<RecyclerView>(R.id.recycler_default_assets).scrollToPosition(index)
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.let { sheetDialog ->
            sheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            val visibleFrame = Rect()
            requireActivity().window.decorView.getWindowVisibleDisplayFrame(visibleFrame)
            val availableHeight = visibleFrame.height().takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
            sheetDialog.behavior.maxHeight = (availableHeight * 0.75f).toInt()
            sheetDialog.behavior.isFitToContents = true
            sheetDialog.behavior.skipCollapsed = true
            sheetDialog.behavior.isDraggable = true
            sheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            sheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
                background = ColorDrawable(Color.TRANSPARENT)
                layoutParams = layoutParams.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
            }
            view?.let(ViewCompat::requestApplyInsets)
        }
    }

    private fun selectAsset(asset: Asset?) {
        if (returningSelection || ledgerId == null) return
        if (asset == null) {
            if (!allowNoAsset) return
            returningSelection = true
            returnAssetSelection(null)
            dismiss()
            return
        }
        // Revalidate against the original ledger's available pool, not asset ownership.
        val available = DatabaseHelper(requireContext(), ledgerId).use { helper ->
            helper.getAllAssets(pickerLedgerId).any { it.id == asset.id && it.id != excludedAssetId }
        }
        if (!available) {
            DatabaseHelper(requireContext(), ledgerId).use {
                assets = it.getAllAssets(pickerLedgerId).filter { candidate -> candidate.id != excludedAssetId }
            }
            view?.let { showAssets(it, revealSelected = false) }
            return
        }
        returningSelection = true
        returnAssetSelection(asset.id)
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_QUERY, query)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.recycler_default_assets)?.adapter = null
        pickerAdapter = null
        assets = emptyList()
        super.onDestroyView()
    }

    companion object {
        private const val KEY_QUERY = "default_asset_query"
    }
}
