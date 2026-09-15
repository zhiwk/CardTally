package com.example.cardtally

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.example.cardtally.model.Asset
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Hosts the asset page in selection mode without duplicating its grouped list UI. */
class RecordAssetPickerBottomSheetFragment : BottomSheetDialogFragment(), AssetFragment.AssetSelectionHost {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_record_asset_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.record_asset_picker_container,
                    AssetFragment.newPickerInstance(excludedAssetId, selectedAssetId)
                )
                .commitNow()
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.let { dialog ->
            dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.72f).toInt()
            dialog.behavior.skipCollapsed = false
            dialog.behavior.isDraggable = true
            dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
            recordFragment()?.onAssetPickerDialogShown(dialog)
        }
    }

    override fun onAssetSelected(asset: Asset) {
        recordFragment()?.onAssetPickerSelected(asset.id, selectDestination)
        dismiss()
    }

    override fun onAssetSelectionClosed() {
        dismiss()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        recordFragment()?.onAssetPickerDismissed()
    }

    private fun recordFragment(): AddRecordFragment? =
        parentFragmentManager.findFragmentById(R.id.fragment_container) as? AddRecordFragment

    private val selectDestination: Boolean
        get() = requireArguments().getBoolean(ARG_SELECT_DESTINATION)

    private val excludedAssetId: Long?
        get() = requireArguments().getLong(ARG_EXCLUDED_ASSET_ID).takeIf { it > 0L }

    private val selectedAssetId: Long?
        get() = requireArguments().getLong(ARG_SELECTED_ASSET_ID).takeIf { it > 0L }

    companion object {
        const val TAG = "record_asset_picker"
        private const val ARG_SELECT_DESTINATION = "select_destination"
        private const val ARG_EXCLUDED_ASSET_ID = "excluded_asset_id"
        private const val ARG_SELECTED_ASSET_ID = "selected_asset_id"

        fun newInstance(
            selectDestination: Boolean,
            excludedAssetId: Long?,
            selectedAssetId: Long?
        ) = RecordAssetPickerBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_SELECT_DESTINATION, selectDestination)
                excludedAssetId?.let { putLong(ARG_EXCLUDED_ASSET_ID, it) }
                selectedAssetId?.let { putLong(ARG_SELECTED_ASSET_ID, it) }
            }
        }
    }
}
