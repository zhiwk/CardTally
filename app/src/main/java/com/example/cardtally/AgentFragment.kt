package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cardtally.util.AiAssistantSettingsHelper

class AgentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_agent, container, false)
    }

    override fun onResume() {
        super.onResume()

        if (!AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())) {
            requireActivity()
                .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                .selectedItemId = R.id.nav_home
        }
    }
}
