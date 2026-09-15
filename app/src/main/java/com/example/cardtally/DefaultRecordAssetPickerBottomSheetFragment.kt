package com.example.cardtally

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.cardtally.model.Asset
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DefaultRecordAssetPickerBottomSheetFragment :
    BottomSheetDialogFragment(), AssetFragment.AssetSelectionHost {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_record_asset_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.record_asset_picker_container,
                    AssetFragment.newPickerInstance(null, selectedAssetId)
                )
                .commitNow()
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.let {
            it.behavior.skipCollapsed = true
            it.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onAssetSelected(asset: Asset) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putLong(ARG_SELECTED_ID, asset.id)
                putBoolean(ARG_FOR_INCOME, forIncome)
            }
        )
        dismiss()
    }

    override fun onAssetSelectionClosed() = dismiss()

    private val forIncome: Boolean get() = requireArguments().getBoolean(ARG_FOR_INCOME)
    private val selectedAssetId: Long?
        get() = requireArguments().getLong(ARG_SELECTED_ID).takeIf { it > 0L }

    companion object {
        const val TAG = "default_record_asset_picker"
        const val RESULT_KEY = "default_record_asset_selected"
        private const val ARG_FOR_INCOME = "for_income"
        private const val ARG_SELECTED_ID = "selected_id"

        fun newInstance(forIncome: Boolean, selectedAssetId: Long?) =
            DefaultRecordAssetPickerBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_FOR_INCOME, forIncome)
                    putLong(ARG_SELECTED_ID, selectedAssetId ?: 0L)
                }
            }
    }
}
