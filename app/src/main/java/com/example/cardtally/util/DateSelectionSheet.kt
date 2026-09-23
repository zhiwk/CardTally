package com.example.cardtally.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.adapter.LedgerCalendarAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Shared calendar sheet used by ordinary and recurring record forms. */
object DateSelectionSheet {
    fun create(
        context: Context,
        initialDate: String,
        commitOnSelection: Boolean = false,
        onConfirmed: (String) -> Unit
    ): BottomSheetDialog {
        val dialog = BottomSheetDialog(context)
        val sheet = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_record_date, null)
        dialog.setContentView(sheet)

        val monthTitle = sheet.findViewById<TextView>(R.id.text_date_month_title)
        val selectionValue = sheet.findViewById<TextView>(R.id.text_date_selection_value)
        val recyclerCalendar = sheet.findViewById<RecyclerView>(R.id.recycler_date_calendar)
        val btnPrevMonth = sheet.findViewById<ImageButton>(R.id.btn_date_prev_month)
        val btnNextMonth = sheet.findViewById<ImageButton>(R.id.btn_date_next_month)
        val textSelectToday = sheet.findViewById<TextView>(R.id.text_select_today)

        var localDate = initialDate
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
            if (commitOnSelection) {
                onConfirmed(localDate)
                dialog.dismiss()
            }
        }
        recyclerCalendar.layoutManager = GridLayoutManager(context, 7)
        recyclerCalendar.adapter = adapter
        renderCalendar()

        sheet.findViewById<View>(R.id.btn_close_sheet).setOnClickListener { dialog.dismiss() }
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
        textSelectToday.setOnClickListener {
            val calendar = Calendar.getInstance()
            localDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
            displayYear = calendar.get(Calendar.YEAR)
            displayMonth = calendar.get(Calendar.MONTH)
            renderCalendar()
        }
        sheet.findViewById<View>(R.id.btn_confirm_date).setOnClickListener {
            onConfirmed(localDate)
            dialog.dismiss()
        }
        dialog.setOnShowListener {
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

    private fun formatDisplayDate(rawDate: String): String = runCatching {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(rawDate)!!
        SimpleDateFormat("yyyy年M月d日EEEE", Locale.CHINA).format(date)
    }.getOrDefault(rawDate)
}
