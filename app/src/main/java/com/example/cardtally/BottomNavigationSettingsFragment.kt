package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.cardtally.util.BottomNavigationPreferences

class BottomNavigationSettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_bottom_navigation_settings, container, false)
        com.example.cardtally.util.SettingsRowLayout.bindChoices(root,
            R.id.row_navigation_floating, R.id.row_navigation_docked)
        root.findViewById<TextView>(R.id.text_title).setText(R.string.settings_bottom_navigation)
        fun bind() {
            val docked = BottomNavigationPreferences.isDocked(requireContext())
            root.findViewById<RadioButton>(R.id.radio_navigation_floating).isChecked = !docked
            root.findViewById<RadioButton>(R.id.radio_navigation_docked).isChecked = docked
            root.findViewById<View>(R.id.row_navigation_floating).isSelected = !docked
            root.findViewById<View>(R.id.row_navigation_docked).isSelected = docked
        }
        fun select(docked: Boolean) {
            BottomNavigationPreferences.saveDocked(requireContext(), docked)
            bind()
            (activity as? MainActivity)?.refreshBottomNavigationStyle()
        }
        root.findViewById<View>(R.id.row_navigation_floating).setOnClickListener { select(false) }
        root.findViewById<View>(R.id.row_navigation_docked).setOnClickListener { select(true) }
        root.findViewById<View>(R.id.btn_back).setOnClickListener { parentFragmentManager.popBackStack() }
        bind()
        return root
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val root = view ?: return
        val docked = BottomNavigationPreferences.isDocked(requireContext())
        root.findViewById<RadioButton>(R.id.radio_navigation_floating).isChecked = !docked
        root.findViewById<RadioButton>(R.id.radio_navigation_docked).isChecked = docked
        root.findViewById<View>(R.id.row_navigation_floating).isSelected = !docked
        root.findViewById<View>(R.id.row_navigation_docked).isSelected = docked
    }
}
