package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.BottomNavigationOrderAdapter
import com.example.cardtally.util.AiEntryStyle
import com.example.cardtally.util.AiEntryPosition
import com.example.cardtally.util.BottomNavigationPreferences

class BottomNavigationSettingsFragment : Fragment() {
    private var orderAdapter: BottomNavigationOrderAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.fragment_bottom_navigation_settings, container, false)
        com.example.cardtally.util.SettingsRowLayout.bindChoices(root,
            R.id.row_navigation_floating, R.id.row_navigation_docked,
            R.id.row_ai_entry_menu, R.id.row_ai_entry_circle,
            R.id.row_ai_entry_left, R.id.row_ai_entry_right)
        root.findViewById<TextView>(R.id.text_title).setText(R.string.settings_bottom_navigation)
        orderAdapter = BottomNavigationOrderAdapter { order ->
            BottomNavigationPreferences.saveVisibleMenuOrder(requireContext(), order)
            (activity as? MainActivity)?.refreshBottomNavigationStyle()
        }.also { it.attach(root.findViewById(R.id.recycler_navigation_order)) }
        root.findViewById<View>(R.id.button_reset_navigation_order).setOnClickListener {
            orderAdapter?.finishMove()
            BottomNavigationPreferences.resetMenuOrder(requireContext())
            bindSelection(root)
            (activity as? MainActivity)?.refreshBottomNavigationStyle()
        }
        fun select(docked: Boolean) {
            BottomNavigationPreferences.saveDocked(requireContext(), docked)
            bindSelection(root)
            (activity as? MainActivity)?.refreshBottomNavigationStyle()
        }
        fun selectAi(style: AiEntryStyle) {
            BottomNavigationPreferences.saveAiEntryStyle(requireContext(), style)
            bindSelection(root)
            (activity as? MainActivity)?.refreshBottomNavigationStyle()
        }
        fun selectPosition(position: AiEntryPosition) {
            BottomNavigationPreferences.saveAiEntryPosition(requireContext(), position)
            bindSelection(root)
            (activity as? MainActivity)?.refreshBottomNavigationStyle()
        }
        root.findViewById<View>(R.id.row_navigation_floating).setOnClickListener { select(false) }
        root.findViewById<View>(R.id.row_navigation_docked).setOnClickListener { select(true) }
        root.findViewById<View>(R.id.row_ai_entry_menu).setOnClickListener { selectAi(AiEntryStyle.IN_MENU) }
        root.findViewById<View>(R.id.row_ai_entry_circle).setOnClickListener { selectAi(AiEntryStyle.SEPARATE_CIRCLE) }
        root.findViewById<View>(R.id.row_ai_entry_left).setOnClickListener { selectPosition(AiEntryPosition.LEFT) }
        root.findViewById<View>(R.id.row_ai_entry_right).setOnClickListener { selectPosition(AiEntryPosition.RIGHT) }
        root.findViewById<View>(R.id.btn_back).setOnClickListener { parentFragmentManager.popBackStack() }
        bindSelection(root)
        return root
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        view?.let(::bindSelection)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        orderAdapter?.finishMove()
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        orderAdapter?.detach()
        view?.findViewById<RecyclerView>(R.id.recycler_navigation_order)?.adapter = null
        orderAdapter = null
        super.onDestroyView()
    }

    private fun bindSelection(root: View) {
        val docked = BottomNavigationPreferences.isDocked(requireContext())
        val separateAi = BottomNavigationPreferences.getAiEntryStyle(requireContext()) == AiEntryStyle.SEPARATE_CIRCLE
        val aiOnLeft = BottomNavigationPreferences.getAiEntryPosition(requireContext()) == AiEntryPosition.LEFT
        fun bind(row: Int, radio: Int, selected: Boolean) {
            root.findViewById<RadioButton>(radio).isChecked = selected
            root.findViewById<View>(row).isSelected = selected
        }
        bind(R.id.row_navigation_floating, R.id.radio_navigation_floating, !docked)
        bind(R.id.row_navigation_docked, R.id.radio_navigation_docked, docked)
        bind(R.id.row_ai_entry_menu, R.id.radio_ai_entry_menu, !separateAi)
        bind(R.id.row_ai_entry_circle, R.id.radio_ai_entry_circle, separateAi)
        bind(R.id.row_ai_entry_left, R.id.radio_ai_entry_left, aiOnLeft)
        bind(R.id.row_ai_entry_right, R.id.radio_ai_entry_right, !aiOnLeft)
        root.findViewById<View>(R.id.section_ai_entry_position).visibility = if (separateAi) View.VISIBLE else View.GONE
        orderAdapter?.finishMove()
        orderAdapter?.submitOrder(BottomNavigationPreferences.getVisibleMenuOrder(requireContext()))
    }
}
