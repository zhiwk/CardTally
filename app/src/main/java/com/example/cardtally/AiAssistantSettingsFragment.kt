package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cardtally.util.AiAssistantSettingsHelper

class AiAssistantSettingsFragment : Fragment() {
    private lateinit var editAiApiKey: EditText
    private lateinit var editAiModel: EditText
    private lateinit var editAiRequestUrl: EditText
    private lateinit var buttonSaveAiApiKey: View
    private lateinit var textAiSettingsStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_assistant_settings, container, false)
        view.findViewById<TextView>(R.id.text_title).text = getString(R.string.ai_settings_title)

        editAiApiKey = view.findViewById(R.id.edit_ai_api_key)
        editAiModel = view.findViewById(R.id.edit_ai_model)
        editAiRequestUrl = view.findViewById(R.id.edit_ai_base_url)
        buttonSaveAiApiKey = view.findViewById(R.id.button_save_ai_api_key)
        textAiSettingsStatus = view.findViewById(R.id.text_ai_settings_status)

        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        bindSavedValues()

        buttonSaveAiApiKey.setOnClickListener {
            AiAssistantSettingsHelper.saveApiKey(requireContext(), editAiApiKey.text.toString())
            AiAssistantSettingsHelper.saveModel(requireContext(), editAiModel.text.toString())
            AiAssistantSettingsHelper.saveRequestUrl(requireContext(), editAiRequestUrl.text.toString())
            bindSavedValues()
            Toast.makeText(requireContext(), getString(R.string.toast_ai_api_key_saved), Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun bindSavedValues() {
        editAiApiKey.setText(AiAssistantSettingsHelper.getApiKey(requireContext()))
        editAiModel.setText(AiAssistantSettingsHelper.getModel(requireContext()))
        editAiRequestUrl.setText(AiAssistantSettingsHelper.getRequestUrl(requireContext()))

        editAiApiKey.setSelection(editAiApiKey.text.length)
        editAiModel.setSelection(editAiModel.text.length)
        editAiRequestUrl.setSelection(editAiRequestUrl.text.length)

            textAiSettingsStatus.text = getString(
            R.string.ai_settings_defaults_status,
            AiAssistantSettingsHelper.getModel(requireContext()),
            AiAssistantSettingsHelper.getRequestUrl(requireContext())
        )
    }
}
