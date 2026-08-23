package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cardtally.util.LanguageHelper

class LanguageSettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_language_settings, container, false)
        view.findViewById<TextView>(R.id.text_title).text = getString(R.string.settings_language)
        val chinese = view.findViewById<RadioButton>(R.id.radio_language_chinese)
        val english = view.findViewById<RadioButton>(R.id.radio_language_english)
        val languageValues = resources.getStringArray(R.array.supported_language_values)

        fun bindCurrentLanguage() {
            val current = LanguageHelper.getCurrentLanguageTag(requireContext())
            chinese.isChecked = current == languageValues.getOrNull(0)
            english.isChecked = current == languageValues.getOrNull(1)
        }

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<View>(R.id.row_language_chinese).setOnClickListener {
            selectLanguage(languageValues.getOrNull(0)) { bindCurrentLanguage() }
        }
        view.findViewById<View>(R.id.row_language_english).setOnClickListener {
            selectLanguage(languageValues.getOrNull(1)) { bindCurrentLanguage() }
        }
        bindCurrentLanguage()
        return view
    }

    private fun selectLanguage(languageTag: String?, refresh: () -> Unit) {
        if (languageTag.isNullOrBlank()) return
        val current = LanguageHelper.getCurrentLanguageTag(requireContext())
        if (languageTag == current) return
        LanguageHelper.updateLanguage(requireContext(), languageTag)
        refresh()
    }
}
