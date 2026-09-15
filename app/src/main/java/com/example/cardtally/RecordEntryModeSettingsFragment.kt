package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cardtally.util.RecordEntryMode
import com.example.cardtally.util.RecordEntryModePreferences

class RecordEntryModeSettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_record_entry_mode_settings, container, false)
        view.findViewById<TextView>(R.id.text_title).text = getString(R.string.settings_record_entry_mode)
        val standard = view.findViewById<RadioButton>(R.id.radio_record_entry_mode_standard)
        val quick = view.findViewById<RadioButton>(R.id.radio_record_entry_mode_quick)

        fun bindCurrentMode() {
            when (RecordEntryModePreferences.getMode(requireContext())) {
                RecordEntryMode.QUICK -> {
                    standard.isChecked = false
                    quick.isChecked = true
                }
                else -> {
                    standard.isChecked = true
                    quick.isChecked = false
                }
            }
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<View>(R.id.row_record_entry_mode_standard).setOnClickListener {
            selectMode(RecordEntryMode.STANDARD, ::bindCurrentMode)
        }
        view.findViewById<View>(R.id.row_record_entry_mode_quick).setOnClickListener {
            selectMode(RecordEntryMode.QUICK, ::bindCurrentMode)
        }
        bindCurrentMode()
        return view
    }

    private fun selectMode(mode: RecordEntryMode, refresh: () -> Unit) {
        if (mode == RecordEntryModePreferences.getMode(requireContext())) return
        RecordEntryModePreferences.saveMode(requireContext(), mode)
        refresh()
    }
}
