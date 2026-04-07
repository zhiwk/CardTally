package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.cardtally.util.AssetDisplayHelper
import com.example.cardtally.util.LanguageHelper
import com.example.cardtally.util.QuickAddHelper
import com.example.cardtally.util.ThemeHelper

class SettingsFragment : Fragment() {
    private lateinit var cardQuickAdd: View
    private lateinit var switchQuickAdd: Switch
    private lateinit var cardShowAsset: View
    private lateinit var switchShowAsset: Switch
    private lateinit var cardCategory: View
    private lateinit var cardTheme: View
    private lateinit var cardLanguage: View
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
        cardShowAsset = view.findViewById(R.id.card_show_asset)
        switchShowAsset = view.findViewById(R.id.switch_show_asset)
        cardCategory = view.findViewById(R.id.card_category)
        cardTheme = view.findViewById(R.id.card_theme)
        cardLanguage = view.findViewById(R.id.card_language)
        textCurrentTheme = view.findViewById(R.id.text_current_theme)
        textCurrentLanguage = view.findViewById(R.id.text_current_language)

        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())

        updateCurrentThemeText()
        updateCurrentLanguageText()

        switchQuickAdd.setOnClickListener {
            QuickAddHelper.saveQuickAdd(requireContext(), switchQuickAdd.isChecked)
        }

        switchShowAsset.setOnClickListener {
            AssetDisplayHelper.saveShowAsset(requireContext(), switchShowAsset.isChecked)
            updateBottomNavigation()
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
        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())
    }

    private fun updateCurrentThemeText() {
        val currentTheme = ThemeHelper.getTheme(requireContext())
        textCurrentTheme.text = ThemeHelper.getThemeName(requireContext(), currentTheme)
    }

    private fun updateCurrentLanguageText() {
        textCurrentLanguage.text = LanguageHelper.getCurrentLanguageDisplayName(requireContext())
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
        val showAsset = AssetDisplayHelper.getShowAsset(requireContext())
        val bottomNavigationView = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        val menu = bottomNavigationView.menu
        val assetItem = menu.findItem(R.id.nav_asset)
        assetItem.isVisible = showAsset
    }
}
