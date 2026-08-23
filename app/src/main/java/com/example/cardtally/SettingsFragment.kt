package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.example.cardtally.util.AssetDisplayHelper
import com.example.cardtally.util.LanguageHelper
import com.example.cardtally.util.QuickAddHelper
import com.example.cardtally.util.ScrollTopFabHelper

class SettingsFragment : Fragment() {
    private lateinit var cardQuickAdd: View
    private lateinit var switchQuickAdd: Switch
    private lateinit var cardAiApiKey: View
    private lateinit var switchAiAssistant: Switch
    private lateinit var cardShowAsset: View
    private lateinit var switchShowAsset: Switch
    private lateinit var switchScrollTopFab: Switch
    private lateinit var cardCategory: View
    private lateinit var cardLanguage: View
    private lateinit var textAiApiKeyStatus: TextView
    private lateinit var textCurrentLanguage: TextView
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings_v2, container, false)

        cardQuickAdd = view.findViewById(R.id.card_quick_add)
        switchQuickAdd = view.findViewById(R.id.switch_quick_add)
        cardAiApiKey = view.findViewById(R.id.card_ai_api_key)
        switchAiAssistant = view.findViewById(R.id.switch_ai_assistant)
        cardShowAsset = view.findViewById(R.id.card_show_asset)
        switchShowAsset = view.findViewById(R.id.switch_show_asset)
        switchScrollTopFab = view.findViewById(R.id.switch_scroll_top_fab)
        cardCategory = view.findViewById(R.id.card_category)
        cardLanguage = view.findViewById(R.id.card_language)
        textAiApiKeyStatus = view.findViewById(R.id.text_ai_api_key_status)
        textCurrentLanguage = view.findViewById(R.id.text_current_language)
        databaseHelper = DatabaseHelper(requireContext())

        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchAiAssistant.isChecked = AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())
        switchScrollTopFab.isChecked = ScrollTopFabHelper.isEnabled(requireContext())

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
        switchScrollTopFab.setOnClickListener {
            ScrollTopFabHelper.saveEnabled(requireContext(), switchScrollTopFab.isChecked)
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

        cardLanguage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LanguageSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (view == null || !::switchQuickAdd.isInitialized || !::switchScrollTopFab.isInitialized) {
            return
        }
        updateCurrentLanguageText()
        updateAiApiKeyStatus()
        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchAiAssistant.isChecked = AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())
        switchScrollTopFab.isChecked = ScrollTopFabHelper.isEnabled(requireContext())
    }

    override fun onDestroyView() {
        if (::databaseHelper.isInitialized) {
            databaseHelper.close()
        }
        super.onDestroyView()
    }

    private fun updateCurrentLanguageText() {
        if (!::textCurrentLanguage.isInitialized || !isAdded) return
        textCurrentLanguage.text = LanguageHelper.getCurrentLanguageDisplayName(requireContext())
    }

    private fun updateAiApiKeyStatus() {
        if (!::textAiApiKeyStatus.isInitialized || !isAdded) return
        textAiApiKeyStatus.text = if (AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())) {
            getString(R.string.settings_ai_api_key_status_saved)
        } else {
            getString(R.string.settings_ai_api_key_status_not_set)
        }
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
