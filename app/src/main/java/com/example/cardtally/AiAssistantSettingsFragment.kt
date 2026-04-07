package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cardtally.util.AiAssistantSettingsHelper

class AiAssistantSettingsFragment : Fragment() {
    private lateinit var editAiApiKey: EditText
    private lateinit var buttonSaveAiApiKey: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_assistant_settings, container, false)

        editAiApiKey = view.findViewById(R.id.edit_ai_api_key)
        buttonSaveAiApiKey = view.findViewById(R.id.button_save_ai_api_key)

        editAiApiKey.setText(AiAssistantSettingsHelper.getApiKey(requireContext()))
        editAiApiKey.setSelection(editAiApiKey.text.length)

        buttonSaveAiApiKey.setOnClickListener {
            AiAssistantSettingsHelper.saveApiKey(requireContext(), editAiApiKey.text.toString())
            editAiApiKey.setText(AiAssistantSettingsHelper.getApiKey(requireContext()))
            editAiApiKey.setSelection(editAiApiKey.text.length)
            Toast.makeText(requireContext(), getString(R.string.toast_ai_api_key_saved), Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
