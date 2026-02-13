package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.cardtally.util.QuickAddHelper
import com.example.cardtally.util.ThemeHelper

class SettingsFragment : Fragment() {
    private lateinit var cardQuickAdd: CardView
    private lateinit var switchQuickAdd: Switch
    private lateinit var cardCategory: CardView
    private lateinit var cardTheme: CardView
    private lateinit var textCurrentTheme: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        cardQuickAdd = view.findViewById(R.id.card_quick_add)
        switchQuickAdd = view.findViewById(R.id.switch_quick_add)
        cardCategory = view.findViewById(R.id.card_category)
        cardTheme = view.findViewById(R.id.card_theme)
        textCurrentTheme = view.findViewById(R.id.text_current_theme)

        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())

        updateCurrentThemeText()

        switchQuickAdd.setOnClickListener {
            QuickAddHelper.saveQuickAdd(requireContext(), switchQuickAdd.isChecked)
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

        return view
    }

    override fun onResume() {
        super.onResume()
        updateCurrentThemeText()
        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
    }

    private fun updateCurrentThemeText() {
        val currentTheme = ThemeHelper.getTheme(requireContext())
        val themeName = when (currentTheme) {
            ThemeHelper.THEME_LIGHT -> "浅色主题"
            ThemeHelper.THEME_DARK -> "深色主题"
            ThemeHelper.THEME_SYSTEM -> "跟随系统"
            else -> "浅色主题"
        }
        textCurrentTheme.text = themeName
    }
}
