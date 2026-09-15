package com.example.cardtally.state

import android.os.Bundle

enum class RecordType(val token: String) { EXPENSE("expense"), INCOME("income"), TRANSFER("transfer") }
enum class RecordSheet(val token: String) { NONE("none"), DATE("date"), ASSET("asset"), CATEGORY("category") }

data class RecordFormState(
    val amountBuffer: String,
    val recordType: RecordType,
    val selectedDate: String,
    val selectedAssetId: Long?,
    val selectedDestinationAssetId: Long?,
    val selectedCategoryId: Long?,
    val description: String,
    val feeBuffer: String,
    val photoUri: String?,
    val photoUris: List<String>,
    val openSheet: RecordSheet,
    val pendingCategoryId: Long?
) {
    fun writeTo(bundle: Bundle) {
        bundle.putString(KEY_AMOUNT_BUFFER, amountBuffer)
        bundle.putString(KEY_RECORD_TYPE, recordType.token)
        bundle.putString(KEY_SELECTED_DATE, selectedDate)
        bundle.putNullableLong(KEY_SELECTED_ASSET_ID, selectedAssetId)
        bundle.putNullableLong(KEY_SELECTED_DESTINATION_ASSET_ID, selectedDestinationAssetId)
        bundle.putNullableLong(KEY_SELECTED_CATEGORY_ID, selectedCategoryId)
        bundle.putString(KEY_DESCRIPTION, description)
        bundle.putString(KEY_FEE_BUFFER, feeBuffer)
        bundle.putString(KEY_PHOTO_URI, photoUri)
        bundle.putStringArrayList(KEY_PHOTO_URIS, ArrayList(photoUris))
        bundle.putString(KEY_OPEN_SHEET, openSheet.token)
        bundle.putNullableLong(KEY_PENDING_CATEGORY_ID, pendingCategoryId)
    }

    companion object {
        val DEFAULT = RecordFormState("0.00", RecordType.EXPENSE, "", null, null, null, "", "", null, emptyList(), RecordSheet.NONE, null)
        private const val KEY_AMOUNT_BUFFER = "state_amount_buffer"
        private const val KEY_RECORD_TYPE = "state_record_type"
        private const val KEY_SELECTED_DATE = "state_selected_date"
        private const val KEY_SELECTED_ASSET_ID = "state_selected_asset_id"
        private const val KEY_SELECTED_DESTINATION_ASSET_ID = "state_selected_destination_asset_id"
        private const val KEY_SELECTED_CATEGORY_ID = "state_selected_category_id"
        private const val KEY_DESCRIPTION = "state_description"
        private const val KEY_FEE_BUFFER = "state_fee_buffer"
        private const val KEY_PHOTO_URI = "state_photo_uri"
        private const val KEY_PHOTO_URIS = "state_photo_uris"
        private const val KEY_OPEN_SHEET = "state_open_sheet"
        private const val KEY_PENDING_CATEGORY_ID = "state_pending_category_id"

        fun readFrom(bundle: Bundle?, defaults: RecordFormState): RecordFormState {
            if (bundle == null) return defaults
            return RecordFormState(
                amountBuffer = bundle.getString(KEY_AMOUNT_BUFFER, defaults.amountBuffer),
                recordType = RecordType.values().firstOrNull { it.token == bundle.getString(KEY_RECORD_TYPE) } ?: RecordType.EXPENSE,
                selectedDate = bundle.getString(KEY_SELECTED_DATE, defaults.selectedDate),
                selectedAssetId = bundle.getNullableLong(KEY_SELECTED_ASSET_ID),
                selectedDestinationAssetId = bundle.getNullableLong(KEY_SELECTED_DESTINATION_ASSET_ID),
                selectedCategoryId = bundle.getNullableLong(KEY_SELECTED_CATEGORY_ID),
                description = bundle.getString(KEY_DESCRIPTION, defaults.description),
                feeBuffer = bundle.getString(KEY_FEE_BUFFER, defaults.feeBuffer),
                photoUri = bundle.getString(KEY_PHOTO_URI, defaults.photoUri),
                photoUris = bundle.getStringArrayList(KEY_PHOTO_URIS)?.toList() ?: defaults.photoUris,
                openSheet = RecordSheet.values().firstOrNull { it.token == bundle.getString(KEY_OPEN_SHEET) } ?: RecordSheet.NONE,
                pendingCategoryId = bundle.getNullableLong(KEY_PENDING_CATEGORY_ID)
            )
        }
    }
}

data class EditRecordState(val recordId: Long, val form: RecordFormState) {
    fun writeTo(bundle: Bundle) {
        bundle.putLong(KEY_RECORD_ID, recordId)
        form.writeTo(bundle)
    }

    companion object {
        private const val KEY_RECORD_ID = "state_record_id"

        fun readFrom(bundle: Bundle?, defaults: RecordFormState): EditRecordState? {
            if (bundle == null || !bundle.containsKey(KEY_RECORD_ID)) return null
            return EditRecordState(bundle.getLong(KEY_RECORD_ID), RecordFormState.readFrom(bundle, defaults))
        }
    }
}
