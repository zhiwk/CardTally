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
        com.example.cardtally.util.SettingsRowLayout.bindChoices(view,
            R.id.row_language_chinese, R.id.row_language_english)
        view.findViewById<TextView>(R.id.text_title).text = getString(R.string.settings_language)
        val languageValues = resources.getStringArray(R.array.supported_language_values)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<View>(R.id.row_language_chinese).setOnClickListener {
            selectLanguage(languageValues.getOrNull(0), view)
        }
        view.findViewById<View>(R.id.row_language_english).setOnClickListener {
            selectLanguage(languageValues.getOrNull(1), view)
        }
        bindLanguageSelection(view, LanguageHelper.getCurrentLanguageTag(requireContext()))
        return view
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Framework restoration runs after onCreateView and can restore the old language's check.
        view?.let { bindLanguageSelection(it, LanguageHelper.getCurrentLanguageTag(requireContext())) }
    }

    private fun bindLanguageSelection(root: View, languageTag: String) {
        root.findViewById<RadioButton>(R.id.radio_language_chinese).isChecked =
            languageTag == LanguageHelper.LANGUAGE_CHINESE
        root.findViewById<RadioButton>(R.id.radio_language_english).isChecked =
            languageTag == LanguageHelper.LANGUAGE_ENGLISH
    }

    private fun selectLanguage(languageTag: String?, root: View) {
        if (languageTag.isNullOrBlank()) return
        // Show the user's choice immediately; locale application/recreation is asynchronous.
        bindLanguageSelection(root, LanguageHelper.normalizeLanguageTag(languageTag))
        LanguageHelper.updateLanguage(requireContext(), languageTag)
    }
}
