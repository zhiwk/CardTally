package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.example.cardtally.util.AssetDisplayHelper
import com.example.cardtally.util.CategoryHierarchySettingsHelper
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
    private lateinit var cardCategoryMaxDepth: View
    private lateinit var cardTheme: View
    private lateinit var cardLanguage: View
    private lateinit var textAiApiKeyStatus: TextView
    private lateinit var textCategoryMaxDepth: TextView
    private lateinit var textCurrentTheme: TextView
    private lateinit var textCurrentLanguage: TextView
    private lateinit var databaseHelper: DatabaseHelper

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
        cardCategoryMaxDepth = view.findViewById(R.id.card_category_max_depth)
        cardTheme = view.findViewById(R.id.card_theme)
        cardLanguage = view.findViewById(R.id.card_language)
        textAiApiKeyStatus = view.findViewById(R.id.text_ai_api_key_status)
        textCategoryMaxDepth = view.findViewById(R.id.text_category_max_depth)
        textCurrentTheme = view.findViewById(R.id.text_current_theme)
        textCurrentLanguage = view.findViewById(R.id.text_current_language)
        databaseHelper = DatabaseHelper(requireContext())

        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchAiAssistant.isChecked = AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())

        updateCategoryMaxDepthText()
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

        cardCategoryMaxDepth.setOnClickListener {
            showCategoryMaxDepthDialog()
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
        updateCategoryMaxDepthText()
        updateCurrentThemeText()
        updateCurrentLanguageText()
        updateAiApiKeyStatus()
        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchAiAssistant.isChecked = AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())
    }

    override fun onDestroyView() {
        if (::databaseHelper.isInitialized) {
            databaseHelper.close()
        }
        super.onDestroyView()
    }

    private fun updateCategoryMaxDepthText() {
        textCategoryMaxDepth.text = getString(
            R.string.settings_category_max_depth_value,
            CategoryHierarchySettingsHelper.getCategoryMaxDepth(requireContext())
        )
    }

    private fun updateCurrentThemeText() {
        val currentTheme = ThemeHelper.getTheme(requireContext())
        textCurrentTheme.text = ThemeHelper.getThemeName(requireContext(), currentTheme)
    }

    private fun updateCurrentLanguageText() {
        textCurrentLanguage.text = LanguageHelper.getCurrentLanguageDisplayName(requireContext())
    }

    private fun updateAiApiKeyStatus() {
        textAiApiKeyStatus.text = if (AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())) {
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
                dialog.dismiss()
                if (selectedLanguage != currentLanguageTag) {
                    view?.post {
                        if (isAdded) {
                            LanguageHelper.updateLanguage(requireContext(), selectedLanguage)
                        }
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showCategoryMaxDepthDialog() {
        val context = requireContext()
        val currentValue = CategoryHierarchySettingsHelper.getCategoryMaxDepth(context)
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentValue.toString())
            setSelection(text.length)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.settings_category_max_depth_dialog_title)
            .setMessage(R.string.settings_category_max_depth_dialog_message)
            .setView(input)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val rawValue = input.text.toString().trim().toIntOrNull()
                if (rawValue == null) {
                    Toast.makeText(
                        context,
                        R.string.settings_category_max_depth_invalid,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val sanitizedDepth = CategoryHierarchySettingsHelper.sanitizeCategoryMaxDepth(rawValue)
                val currentMaxDepth = getCurrentCategoryDepthFromDatabase()
                if (sanitizedDepth < currentMaxDepth) {
                    Toast.makeText(
                        context,
                        getString(R.string.settings_category_max_depth_too_small, currentMaxDepth),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                CategoryHierarchySettingsHelper.saveCategoryMaxDepth(context, sanitizedDepth)
                updateCategoryMaxDepthText()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun getCurrentCategoryDepthFromDatabase(): Int {
        val categories = databaseHelper.getAllCategories()
        if (categories.isEmpty()) {
            return 1
        }

        val categoriesById = categories.associateBy { it.id }
        val depthCache = mutableMapOf<Long, Int>()

        fun resolveDepth(category: Category, visiting: MutableSet<Long> = mutableSetOf()): Int {
            depthCache[category.id]?.let { return it }
            if (!visiting.add(category.id)) {
                return 1
            }

            val depth = category.parentId
                ?.let { parentId -> categoriesById[parentId] }
                ?.let { parent -> resolveDepth(parent, visiting) + 1 }
                ?: 1

            visiting.remove(category.id)
            depthCache[category.id] = depth
            return depth
        }

        return categories.maxOf { resolveDepth(it) }
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
