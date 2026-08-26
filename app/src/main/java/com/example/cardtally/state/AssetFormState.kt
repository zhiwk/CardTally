package com.example.cardtally.state

import android.os.Bundle

enum class AssetType(val token: String, val databaseValue: Int) {
    CASH("cash", 0), BANK("bank", 1), ALIPAY("alipay", 2), WECHAT("wechat", 3);

    companion object {
        fun fromDatabaseValue(value: Int): AssetType = values().firstOrNull { it.databaseValue == value } ?: CASH
    }
}

data class AssetFormState(
    val assetName: String,
    val assetAmountBuffer: String,
    val assetType: AssetType,
    val assetTypeLabel: String = "",
    val assetTypeIconName: String = "",
    val includeInTotal: Boolean = true
) {
    fun writeTo(bundle: Bundle) {
        bundle.putString(KEY_ASSET_NAME, assetName)
        bundle.putString(KEY_ASSET_AMOUNT_BUFFER, assetAmountBuffer)
        bundle.putString(KEY_ASSET_TYPE, assetType.token)
        bundle.putString(KEY_ASSET_TYPE_LABEL, assetTypeLabel)
        bundle.putString(KEY_ASSET_TYPE_ICON_NAME, assetTypeIconName)
        bundle.putBoolean(KEY_INCLUDE_IN_TOTAL, includeInTotal)
    }

    companion object {
        val DEFAULT = AssetFormState("", "", AssetType.CASH)
        private const val KEY_ASSET_NAME = "state_asset_name"
        private const val KEY_ASSET_AMOUNT_BUFFER = "state_asset_amount_buffer"
        private const val KEY_ASSET_TYPE = "state_asset_type"
        private const val KEY_ASSET_TYPE_LABEL = "state_asset_type_label"
        private const val KEY_ASSET_TYPE_ICON_NAME = "state_asset_type_icon_name"
        private const val KEY_INCLUDE_IN_TOTAL = "state_asset_include_in_total"

        fun readFrom(bundle: Bundle?, defaults: AssetFormState): AssetFormState {
            if (bundle == null) return defaults
            return AssetFormState(
                assetName = bundle.getString(KEY_ASSET_NAME, defaults.assetName),
                assetAmountBuffer = bundle.getString(KEY_ASSET_AMOUNT_BUFFER, defaults.assetAmountBuffer),
                assetType = AssetType.values().firstOrNull { it.token == bundle.getString(KEY_ASSET_TYPE) }
                    ?: defaults.assetType,
                assetTypeLabel = bundle.getString(KEY_ASSET_TYPE_LABEL, defaults.assetTypeLabel),
                assetTypeIconName = bundle.getString(KEY_ASSET_TYPE_ICON_NAME, defaults.assetTypeIconName),
                includeInTotal = bundle.getBoolean(KEY_INCLUDE_IN_TOTAL, defaults.includeInTotal)
            )
        }
    }
}

data class EditAssetState(val assetId: Long, val form: AssetFormState) {
    fun writeTo(bundle: Bundle) {
        bundle.putLong(KEY_ASSET_ID, assetId)
        form.writeTo(bundle)
    }

    companion object {
        private const val KEY_ASSET_ID = "state_asset_id"

        fun readFrom(bundle: Bundle?, defaults: AssetFormState): EditAssetState? {
            if (bundle == null || !bundle.containsKey(KEY_ASSET_ID)) return null
            return EditAssetState(bundle.getLong(KEY_ASSET_ID), AssetFormState.readFrom(bundle, defaults))
        }
    }
}
