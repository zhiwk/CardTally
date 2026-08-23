package com.example.cardtally

import android.os.Bundle

/**
 * Compatibility entry point for existing edit-record navigation.
 *
 * The form and persistence logic live in [AddRecordFragment]. Supplying a
 * record id makes that shared screen load and update the existing record;
 * without one it behaves as the new-record screen.
 */
class EditRecordFragment : AddRecordFragment() {
    companion object {
        private const val KEY_RECORD_ID = "record_id"

        fun newInstance(recordId: Long): EditRecordFragment {
            return EditRecordFragment().apply {
                arguments = Bundle().apply {
                    putLong(KEY_RECORD_ID, recordId)
                }
            }
        }
    }
}
