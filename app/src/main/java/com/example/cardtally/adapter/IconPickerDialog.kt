package com.example.cardtally.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.example.cardtally.R
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Shared category icon picker. Instead of a flat 5000-entry alphabetical list it
 * shows the same curated, group-organised, icon-only browser as the inline
 * add-category screen, so every entry point offers the same choices.
 *
 * Choosing an icon dispatches exactly once and dismisses the sheet; closing
 * without choosing never calls back.
 */
object IconPickerDialog {

    fun show(
        context: Context,
        initialSelectedIcon: String?,
        onIconSelected: (String?) -> Unit
    ) {
        val dialog = BottomSheetDialog(context)
        val sheetView = LayoutInflater.from(context).inflate(R.layout.dialog_icon_picker, null)
        val labels = FeaturedIconLabels.load(context)

        var selectionDispatched = false
        val browser = IconBrowserBinder(
            context = context,
            root = sheetView,
            labels = labels
        ) { icon ->
            if (!selectionDispatched) {
                selectionDispatched = true
                onIconSelected(icon)
                dialog.dismiss()
            }
        }
        browser.bind(initialSelectedIcon)

        sheetView.findViewById<View>(R.id.btn_close_icon_picker).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(sheetView)
        dialog.setOnShowListener {
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        dialog.show()
    }
}
