package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cardtally.util.ThemeHelper

class ThemeSettingsFragment : Fragment() {
    private lateinit var radioGroupTheme: RadioGroup
    private lateinit var radioLight: RadioButton
    private lateinit var radioDark: RadioButton
    private lateinit var radioSystem: RadioButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_theme_settings, container, false)

        radioGroupTheme = view.findViewById(R.id.radio_group_theme)
        radioLight = view.findViewById(R.id.radio_light)
        radioDark = view.findViewById(R.id.radio_dark)
        radioSystem = view.findViewById(R.id.radio_system)

        loadCurrentTheme()

        radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.radio_light -> ThemeHelper.THEME_LIGHT
                R.id.radio_dark -> ThemeHelper.THEME_DARK
                else -> ThemeHelper.THEME_SYSTEM
            }

            ThemeHelper.saveTheme(requireContext(), themeMode)
            Toast.makeText(requireContext(), "主题已更改", Toast.LENGTH_SHORT).show()
            requireActivity().recreate()
        }

        return view
    }

    private fun loadCurrentTheme() {
        val currentTheme = ThemeHelper.getTheme(requireContext())
        when (currentTheme) {
            ThemeHelper.THEME_LIGHT -> radioLight.isChecked = true
            ThemeHelper.THEME_DARK -> radioDark.isChecked = true
            ThemeHelper.THEME_SYSTEM -> radioSystem.isChecked = true
        }
    }
}
