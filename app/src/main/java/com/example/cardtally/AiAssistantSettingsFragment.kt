package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import com.example.cardtally.ai.AiRecordAuditStore
import com.example.cardtally.database.DatabaseHelper
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.cardtally.network.ApiEndpoints
import com.example.cardtally.network.ApiModelClient
import com.example.cardtally.network.ApiModelsResult
import com.example.cardtally.network.MiniMaxConfig
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import okhttp3.Call

class AiAssistantSettingsFragment : Fragment() {
    private lateinit var editAiApiKey: EditText
    private lateinit var editAiModel: MaterialAutoCompleteTextView
    private lateinit var modelInput: TextInputLayout
    private lateinit var editAiRequestUrl: EditText
    private lateinit var buttonSaveAiApiKey: MaterialButton
    private lateinit var buttonFetchModels: MaterialButton
    private lateinit var textAiSettingsStatus: TextView
    private val modelClient = ApiModelClient()
    private var modelCall: Call? = null
    private var requestGeneration = 0L
    private var models = emptyList<String>()
    private var savedRequestUrl = ""
    private var loading = false
    private var auditDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_ai_assistant_settings, container, false)
        root.findViewById<TextView>(R.id.text_title).text = getString(R.string.ai_settings_title)
        editAiApiKey = root.findViewById(R.id.edit_ai_api_key)
        editAiModel = root.findViewById(R.id.edit_ai_model)
        modelInput = root.findViewById(R.id.layout_ai_model)
        editAiRequestUrl = root.findViewById(R.id.edit_ai_base_url)
        buttonSaveAiApiKey = root.findViewById(R.id.button_save_ai_api_key)
        buttonFetchModels = root.findViewById(R.id.button_fetch_ai_models)
        textAiSettingsStatus = root.findViewById(R.id.text_ai_settings_status)
        val toolsSwitch = root.findViewById<MaterialSwitch>(R.id.switch_ai_record_tools)
        toolsSwitch.isChecked = AiAssistantSettingsHelper.getRecordToolsEnabled(requireContext())
        toolsSwitch.setOnCheckedChangeListener { _, checked ->
            AiAssistantSettingsHelper.saveRecordToolsEnabled(requireContext(), checked)
        }
        root.findViewById<View>(R.id.button_ai_record_audit).setOnClickListener { showRecordAudit() }
        editAiModel.keyListener = null
        loading = false
        models = emptyList()

        root.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        val saved = AiAssistantSettingsHelper.getMiniMaxConfig(requireContext())
        savedRequestUrl = saved.requestUrl
        editAiRequestUrl.setText(saved.requestUrl)
        editAiApiKey.setText(saved.apiKey)
        setModels(if (saved.isComplete()) listOf(saved.model) else emptyList(), saved.model)
        textAiSettingsStatus.setText(
            if (saved.isComplete()) R.string.ai_settings_saved_status else R.string.ai_settings_models_prompt
        )
        editAiRequestUrl.doAfterTextChanged { invalidateModels() }
        editAiApiKey.doAfterTextChanged { invalidateModels() }
        editAiModel.setOnItemClickListener { _, _, _, _ -> updateButtons() }
        buttonFetchModels.setOnClickListener { fetchModels(root) }
        buttonSaveAiApiKey.setOnClickListener { saveConfiguration() }
        updateButtons()
        return root
    }

    private fun setModels(values: List<String>, selected: String = "") {
        models = values
        editAiModel.setAdapter(ArrayAdapter(requireContext(), R.layout.item_ai_model, values))
        editAiModel.setText(selected.takeIf { it in values }.orEmpty(), false)
        modelInput.isEnabled = values.isNotEmpty()
    }

    private fun invalidateModels() {
        requestGeneration++
        modelCall?.cancel()
        modelCall = null
        loading = false
        setModels(emptyList())
        editAiRequestUrl.error = null
        editAiApiKey.error = null
        textAiSettingsStatus.setText(R.string.ai_settings_models_prompt)
        updateButtons()
    }

    private fun fetchModels(root: View) {
        val endpoints = ApiEndpoints.from(editAiRequestUrl.text.toString())
        if (endpoints == null) {
            editAiRequestUrl.error = getString(R.string.ai_settings_invalid_url)
            editAiRequestUrl.requestFocus()
            return
        }
        val key = editAiApiKey.text.toString().trim()
        if (!ApiEndpoints.isValidApiKey(key)) {
            editAiApiKey.error = getString(R.string.ai_settings_invalid_key)
            editAiApiKey.requestFocus()
            return
        }
        val generation = ++requestGeneration
        modelCall?.cancel()
        loading = true
        textAiSettingsStatus.setText(R.string.ai_settings_models_loading)
        updateButtons()
        modelCall = modelClient.fetch(endpoints, key) { result ->
            activity?.runOnUiThread {
                // URL/key changes and leaving the screen invalidate every in-flight response.
                if (view !== root || generation != requestGeneration) return@runOnUiThread
                modelCall = null
                loading = false
                when (result) {
                    is ApiModelsResult.Success -> {
                        val selected = editAiModel.text.toString()
                        setModels(result.models, selected)
                        textAiSettingsStatus.text = getString(R.string.ai_settings_models_success, result.models.size)
                    }
                    is ApiModelsResult.Failure -> {
                        textAiSettingsStatus.text = when (result.reason) {
                            ApiModelsResult.Reason.AUTH -> getString(R.string.ai_settings_models_auth_error)
                            ApiModelsResult.Reason.UNSUPPORTED -> getString(R.string.ai_settings_models_unsupported)
                            ApiModelsResult.Reason.HTTP -> getString(R.string.ai_settings_models_http_error, result.statusCode)
                            ApiModelsResult.Reason.NETWORK -> getString(R.string.ai_settings_models_network_error)
                            ApiModelsResult.Reason.INVALID_RESPONSE -> getString(R.string.ai_settings_models_response_error)
                            ApiModelsResult.Reason.EMPTY -> getString(R.string.ai_settings_models_empty)
                        }
                    }
                }
                updateButtons()
            }
        }
    }

    private fun updateButtons() {
        buttonFetchModels.isEnabled = !loading
        buttonFetchModels.alpha = if (loading) 0.5f else 1f
        buttonFetchModels.setText(if (loading) R.string.ai_settings_models_loading else R.string.ai_settings_fetch_models)
        buttonSaveAiApiKey.isEnabled = !loading && editAiModel.text.toString() in models &&
            ApiEndpoints.from(editAiRequestUrl.text.toString()) != null &&
            ApiEndpoints.isValidApiKey(editAiApiKey.text.toString())
        buttonSaveAiApiKey.alpha = if (buttonSaveAiApiKey.isEnabled) 1f else 0.5f
    }

    private fun saveConfiguration() {
        val endpoints = ApiEndpoints.from(editAiRequestUrl.text.toString()) ?: return
        val model = editAiModel.text.toString()
        if (loading || model !in models || !ApiEndpoints.isValidApiKey(editAiApiKey.text.toString())) return
        val requestUrl = if (editAiRequestUrl.text.toString().trim() == savedRequestUrl) {
            savedRequestUrl
        } else {
            endpoints.chat.toString()
        }
        AiAssistantSettingsHelper.saveConfiguration(requireContext(), MiniMaxConfig(
            apiKey = editAiApiKey.text.toString(), model = model, requestUrl = requestUrl
        ))
        Toast.makeText(requireContext(), R.string.toast_ai_api_key_saved, Toast.LENGTH_SHORT).show()
    }

    private fun showRecordAudit() {
        val entries = DatabaseHelper(requireContext()).use { AiRecordAuditStore(it.readableDatabase).recent() }
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val content = entries.joinToString("\n\n") { entry ->
            val action = getString(when (entry.tool) {
                "create_record" -> R.string.ai_record_create
                "update_record" -> R.string.ai_record_update
                "delete_record" -> R.string.ai_record_delete
                "record_options" -> R.string.ai_record_options
                else -> R.string.ai_record_query
            })
            val state = getString(when (entry.state) {
                "proposed" -> R.string.ai_record_audit_proposed
                "confirmed" -> R.string.ai_record_audit_confirmed
                "success" -> R.string.ai_record_audit_success
                "rejected" -> R.string.ai_record_audit_rejected
                "cancelled" -> R.string.ai_record_audit_cancelled
                "expired" -> R.string.ai_record_audit_expired
                else -> R.string.ai_record_audit_failed
            })
            "${format.format(java.util.Date(entry.createdAt))} · $action · $state" +
                entry.target?.let { "\n" + getString(R.string.ai_record_id, it) }.orEmpty()
        }.ifEmpty { getString(R.string.ai_record_audit_empty) }
        auditDialog?.dismiss()
        auditDialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.ai_record_audit)
            .setMessage(content).setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(R.string.ai_record_audit_clear) { _, _ ->
                auditDialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.ai_record_audit_clear)
                    .setMessage(R.string.ai_record_audit_clear_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        DatabaseHelper(requireContext()).use { AiRecordAuditStore(it.writableDatabase).clear() }
                    }.show()
            }.show()
    }

    override fun onDestroyView() {
        auditDialog?.dismiss()
        auditDialog = null
        requestGeneration++
        modelCall?.cancel()
        modelCall = null
        loading = false
        // Password inputs never enter saved view state; release unsaved credentials on exit.
        editAiApiKey.text.clear()
        super.onDestroyView()
    }
}
