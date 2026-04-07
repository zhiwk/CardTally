package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.example.cardtally.util.AssetDisplayHelper
import com.example.cardtally.util.LanguageHelper
import com.example.cardtally.util.QuickAddHelper
import com.example.cardtally.util.ThemeHelper

class SettingsFragment : Fragment() {
    private lateinit var cardQuickAdd: View
    private lateinit var switchQuickAdd: Switch
    private lateinit var cardAiApiKey: View
    private lateinit var switchAiAssistant: Switch
    private lateinit var cardShowAsset: View
    private lateinit var switchShowAsset: Switch
    private lateinit var cardCategory: View
    private lateinit var cardTheme: View
    private lateinit var cardLanguage: View
    private lateinit var textAiApiKeyStatus: TextView
    private lateinit var textCurrentTheme: TextView
    private lateinit var textCurrentLanguage: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        cardQuickAdd = view.findViewById(R.id.card_quick_add)
        switchQuickAdd = view.findViewById(R.id.switch_quick_add)
        cardAiApiKey = view.findViewById(R.id.card_ai_api_key)
        switchAiAssistant = view.findViewById(R.id.switch_ai_assistant)
        cardShowAsset = view.findViewById(R.id.card_show_asset)
        switchShowAsset = view.findViewById(R.id.switch_show_asset)
        cardCategory = view.findViewById(R.id.card_category)
        cardTheme = view.findViewById(R.id.card_theme)
        cardLanguage = view.findViewById(R.id.card_language)
        textAiApiKeyStatus = view.findViewById(R.id.text_ai_api_key_status)
        textCurrentTheme = view.findViewById(R.id.text_current_theme)
        textCurrentLanguage = view.findViewById(R.id.text_current_language)

        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchAiAssistant.isChecked = AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())

        updateCurrentThemeText()
        updateCurrentLanguageText()
        updateAiApiKeyStatus()

        switchQuickAdd.setOnClickListener {
            QuickAddHelper.saveQuickAdd(requireContext(), switchQuickAdd.isChecked)
        }

        switchAiAssistant.setOnClickListener {
            AiAssistantSettingsHelper.saveAiAssistantEnabled(requireContext(), switchAiAssistant.isChecked)
            updateBottomNavigation()
        }

        switchShowAsset.setOnClickListener {
            AssetDisplayHelper.saveShowAsset(requireContext(), switchShowAsset.isChecked)
            updateBottomNavigation()
        }

        cardAiApiKey.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AiAssistantSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        cardCategory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CategoryManageFragment())
                .addToBackStack(null)
                .commit()
        }

        cardTheme.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ThemeSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        cardLanguage.setOnClickListener {
            showLanguageDialog()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        updateCurrentThemeText()
        updateCurrentLanguageText()
        updateAiApiKeyStatus()
        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchAiAssistant.isChecked = AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())
    }

    private fun updateCurrentThemeText() {
        val currentTheme = ThemeHelper.getTheme(requireContext())
        textCurrentTheme.text = ThemeHelper.getThemeName(requireContext(), currentTheme)
    }

    private fun updateCurrentLanguageText() {
        textCurrentLanguage.text = LanguageHelper.getCurrentLanguageDisplayName(requireContext())
    }

    private fun updateAiApiKeyStatus() {
        textAiApiKeyStatus.text = if (AiAssistantSettingsHelper.hasApiKey(requireContext())) {
            getString(R.string.settings_ai_api_key_status_saved)
        } else {
            getString(R.string.settings_ai_api_key_status_not_set)
        }
    }

    private fun showLanguageDialog() {
        val languageEntries = resources.getStringArray(R.array.supported_language_entries)
        val languageValues = resources.getStringArray(R.array.supported_language_values)
        val currentLanguageTag = LanguageHelper.getCurrentLanguageTag(requireContext())
        val checkedIndex = languageValues.indexOf(currentLanguageTag).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.language_dialog_title)
            .setSingleChoiceItems(languageEntries, checkedIndex) { dialog, which ->
                val selectedLanguage = languageValues[which]
                if (selectedLanguage != currentLanguageTag) {
                    LanguageHelper.updateLanguage(requireContext(), selectedLanguage)
                    requireActivity().recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun updateBottomNavigation() {
        val showAiAssistant = AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())
        val showAsset = AssetDisplayHelper.getShowAsset(requireContext())
        val bottomNavigationView = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        val menu = bottomNavigationView.menu
        val aiItem = menu.findItem(R.id.nav_agent)
        val assetItem = menu.findItem(R.id.nav_asset)
        aiItem.isVisible = showAiAssistant
        assetItem.isVisible = showAsset
    }
}
