package com.example.cardtally

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Entry and recurring forms reuse the grouped picker without sharing default-setting writes. */
class RecordAssetPickerBottomSheetFragment : AssetPickerSheetFragment() {
    override val titleRes: Int get() = when {
        selectDestination -> R.string.record_destination_asset_label
        else -> R.string.record_asset_sheet_title
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.let { dialog ->
            (hostFragment() as? AddRecordFragment)?.onAssetPickerDialogShown(dialog)
        }
    }

    override fun returnAssetSelection(assetId: Long?) {
        if (assetId == null && !allowNoAsset) return
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putLong(RESULT_ASSET_ID, assetId ?: NO_ASSET_ID)
                putBoolean(RESULT_SELECT_DESTINATION, selectDestination)
            }
        )
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        (hostFragment() as? AddRecordFragment)?.onAssetPickerDismissed()
    }

    private fun hostFragment() =
        parentFragmentManager.findFragmentById(R.id.fragment_container)

    private val selectDestination: Boolean
        get() = requireArguments().getBoolean(ARG_SELECT_DESTINATION)

    override val allowNoAsset: Boolean
        get() = requireArguments().getBoolean(ARG_ALLOW_NO_ASSET) && !selectDestination

    override val excludedAssetId: Long?
        get() = requireArguments().getLong(ARG_EXCLUDED_ASSET_ID).takeIf { it > 0L }

    override val selectedAssetId: Long?
        get() = requireArguments().getLong(ARG_SELECTED_ASSET_ID).takeIf { it > 0L }

    override val pickerLedgerId: Long?
        get() = requireArguments().getLong(ARG_PICKER_LEDGER_ID).takeIf { it > 0L }

    companion object {
        const val TAG = "record_asset_picker"
        const val RESULT_KEY = "record_asset_selected"
        const val RESULT_ASSET_ID = "selected_asset_id"
        const val RESULT_SELECT_DESTINATION = "select_destination"
        const val NO_ASSET_ID = 0L
        private const val ARG_SELECT_DESTINATION = "select_destination"
        private const val ARG_EXCLUDED_ASSET_ID = "excluded_asset_id"
        private const val ARG_SELECTED_ASSET_ID = "selected_asset_id"
        private const val ARG_PICKER_LEDGER_ID = "picker_ledger_id"
        private const val ARG_ALLOW_NO_ASSET = "allow_no_asset"

        fun newInstance(
            selectDestination: Boolean,
            excludedAssetId: Long?,
            selectedAssetId: Long?,
            ledgerId: Long? = null,
            allowNoAsset: Boolean = false
        ) = RecordAssetPickerBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_SELECT_DESTINATION, selectDestination)
                putBoolean(ARG_ALLOW_NO_ASSET, allowNoAsset)
                excludedAssetId?.let { putLong(ARG_EXCLUDED_ASSET_ID, it) }
                selectedAssetId?.let { putLong(ARG_SELECTED_ASSET_ID, it) }
                ledgerId?.let { putLong(ARG_PICKER_LEDGER_ID, it) }
            }
        }
    }
}
