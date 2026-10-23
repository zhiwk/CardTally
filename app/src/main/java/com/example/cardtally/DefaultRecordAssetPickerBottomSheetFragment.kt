package com.example.cardtally

import android.os.Bundle

/** Default-account settings keep their own result contract; no writes belong to the shared UI. */
class DefaultRecordAssetPickerBottomSheetFragment : AssetPickerSheetFragment() {
    private val forIncome: Boolean get() = requireArguments().getBoolean(ARG_FOR_INCOME)
    override val selectedAssetId: Long?
        get() = requireArguments().getLong(ARG_SELECTED_ID).takeIf { it > 0L }
    override val titleRes: Int
        get() = if (forIncome) R.string.settings_default_income_asset else R.string.settings_default_expense_asset
    override val descriptionRes: Int
        get() = if (forIncome) R.string.default_asset_income_description else R.string.default_asset_expense_description

    override fun returnAssetSelection(assetId: Long?) {
        if (assetId == null) return
        parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle().apply {
            putLong(ARG_SELECTED_ID, assetId)
            putBoolean(ARG_FOR_INCOME, forIncome)
        })
    }

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
