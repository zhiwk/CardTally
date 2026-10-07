package com.example.cardtally.cloud

import android.os.Bundle
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.cardtally.R
import com.google.android.material.button.MaterialButton
import java.text.DateFormat
import java.util.Date

class CloudBackupFragment : Fragment(R.layout.fragment_cloud_backup) {
    private lateinit var model: CloudBackupViewModel
    private var binding: View? = null
    private var dialog: AlertDialog? = null
    private var rendering = false
    private val fields = listOf(R.id.cloud_endpoint, R.id.cloud_user, R.id.cloud_secret, R.id.cloud_bucket,
        R.id.cloud_region, R.id.cloud_prefix, R.id.cloud_token, R.id.cloud_password, R.id.cloud_keep)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = view
        model = ViewModelProvider(this)[CloudBackupViewModel::class.java]
        view.findViewById<TextView>(R.id.text_title).setText(R.string.cloud_title)
        view.findViewById<View>(R.id.btn_back).setOnClickListener { parentFragmentManager.popBackStack() }
        val types = view.findViewById<RadioGroup>(R.id.cloud_type)
        types.setOnCheckedChangeListener { _, _ -> if (!rendering) {
            updateType(); loadConfig()
        } }
        if (savedInstanceState == null) {
            try { rendering = true
                types.check(if (CloudSettings.read(requireContext()).optString("active") == "s3") R.id.cloud_s3 else R.id.cloud_webdav)
            } catch (_: Exception) { toast(R.string.cloud_config_failed) } finally { rendering = false }
            loadConfig()
        }
        updateType()
        view.findViewById<CheckBox>(R.id.cloud_auto).setOnCheckedChangeListener { _, checked ->
            if (checked) view.findViewById<CheckBox>(R.id.cloud_remember).isChecked = true
        }
        view.findViewById<CheckBox>(R.id.cloud_remember).setOnCheckedChangeListener { _, checked ->
            if (!checked) view.findViewById<CheckBox>(R.id.cloud_auto).isChecked = false
        }
        view.findViewById<View>(R.id.cloud_save).setOnClickListener {
            val config = config() ?: return@setOnClickListener
            dialog = AlertDialog.Builder(requireContext()).setMessage(R.string.cloud_save_consent)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.cloud_save) { _, _ ->
                    try {
                        CloudSettings.save(requireContext(), config.copy(password = if (view.findViewById<CheckBox>(R.id.cloud_remember).isChecked) config.password else ""))
                        toast(R.string.cloud_saved)
                    } catch (_: Exception) { toast(R.string.cloud_config_failed) }
                }.show()
        }
        view.findViewById<View>(R.id.cloud_test).setOnClickListener { run("test") }
        view.findViewById<View>(R.id.cloud_list).setOnClickListener { run("list") }
        view.findViewById<View>(R.id.cloud_backup).setOnClickListener {
            consentNetwork { run("backup") }
        }
        view.findViewById<View>(R.id.cloud_cancel).setOnClickListener { model.cancel() }
        view.findViewById<View>(R.id.cloud_disconnect).setOnClickListener {
            dialog = AlertDialog.Builder(requireContext()).setMessage(R.string.cloud_disconnect_hint)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.cloud_disconnect) { _, _ ->
                    try { CloudSettings.disconnect(requireContext()); loadConfig(); toast(R.string.cloud_disconnected) }
                    catch (_: Exception) { toast(R.string.cloud_config_failed) }
                }.show()
        }
        model.state.observe(viewLifecycleOwner) { ui -> render(ui) }
        val prefs = requireContext().getSharedPreferences("cloud_internal", android.content.Context.MODE_PRIVATE)
        val stamp = prefs.getLong("status_time", 0)
        if (stamp > 0) view.findViewById<TextView>(R.id.cloud_last).text = getString(R.string.cloud_last_value,
            DateFormat.getDateTimeInstance().format(Date(stamp)), message(prefs.getString("status", "idle").orEmpty()))
    }
    private fun type() = if (binding!!.findViewById<RadioGroup>(R.id.cloud_type).checkedRadioButtonId == R.id.cloud_s3) "s3" else "webdav"
    private fun updateType() {
        val view = binding ?: return
        val s3 = type() == "s3"
        view.findViewById<View>(R.id.cloud_s3_fields).visibility = if (s3) View.VISIBLE else View.GONE
        view.findViewById<TextView>(R.id.cloud_user_label).setText(if (s3) R.string.cloud_access_key else R.string.cloud_user)
        view.findViewById<TextView>(R.id.cloud_secret_label).setText(if (s3) R.string.cloud_secret_key else R.string.cloud_secret)
    }
    private fun loadConfig() {
        val view = binding ?: return
        try {
            val all = CloudSettings.read(requireContext())
            val config = all.optJSONObject(type())?.let(CloudConfig::from)
            rendering = true
            fun value(id: Int, text: String) { view.findViewById<EditText>(id).setText(text) }
            value(R.id.cloud_endpoint, config?.endpoint.orEmpty()); value(R.id.cloud_user, config?.user.orEmpty())
            value(R.id.cloud_secret, config?.secret.orEmpty()); value(R.id.cloud_bucket, config?.bucket.orEmpty())
            value(R.id.cloud_region, config?.region ?: "us-east-1"); value(R.id.cloud_prefix, config?.prefix ?: "cardtally")
            value(R.id.cloud_token, config?.sessionToken.orEmpty()); value(R.id.cloud_password, config?.password.orEmpty())
            value(R.id.cloud_keep, (config?.keep ?: 15).toString())
            view.findViewById<CheckBox>(R.id.cloud_path_style).isChecked = config?.pathStyle ?: true
            view.findViewById<CheckBox>(R.id.cloud_remember).isChecked = config == null || config.password.isNotEmpty()
            view.findViewById<CheckBox>(R.id.cloud_auto).isChecked = config == null || (config.automatic && all.optString("active") == type())
        } catch (_: Exception) { toast(R.string.cloud_config_failed) }
        finally { rendering = false }
    }
    private fun config(): CloudConfig? {
        val view = binding ?: return null
        fun field(id: Int) = view.findViewById<EditText>(id)
        fun text(id: Int) = field(id).text.toString()
        fields.forEach { field(it).error = null }
        fun reject(id: Int, message: Int): CloudConfig? {
            field(id).error = getString(message)
            field(id).requestFocus()
            return null
        }
        val endpoint = text(R.id.cloud_endpoint).trim()
        val url = endpoint.toHttpUrlOrNull()
        if (url == null || url.scheme != "https" || url.username.isNotEmpty() || url.password.isNotEmpty() || url.query != null || url.fragment != null)
            return reject(R.id.cloud_endpoint, R.string.cloud_endpoint_error)
        if (text(R.id.cloud_user).isBlank()) return reject(R.id.cloud_user, R.string.cloud_required_error)
        if (text(R.id.cloud_secret).isBlank()) return reject(R.id.cloud_secret, R.string.cloud_required_error)
        if (type() == "s3") {
            if (!Regex("[a-zA-Z0-9.-]{3,63}").matches(text(R.id.cloud_bucket).trim()))
                return reject(R.id.cloud_bucket, R.string.cloud_bucket_error)
            if (!Regex("[a-z0-9-]+").matches(text(R.id.cloud_region).trim()))
                return reject(R.id.cloud_region, R.string.cloud_region_error)
        }
        val prefix = text(R.id.cloud_prefix).trim()
        if (prefix.trim('/').split('/').any { it == "." || it == ".." })
            return reject(R.id.cloud_prefix, R.string.cloud_prefix_error)
        if (text(R.id.cloud_password).length < 8) return reject(R.id.cloud_password, R.string.cloud_password_error)
        val keep = text(R.id.cloud_keep).toIntOrNull()
        if (keep == null || keep !in 1..365) return reject(R.id.cloud_keep, R.string.cloud_keep_error)
        val config = CloudConfig(type(), endpoint, text(R.id.cloud_user).trim(), text(R.id.cloud_secret),
            text(R.id.cloud_bucket).trim(), text(R.id.cloud_region).trim(), prefix,
            view.findViewById<CheckBox>(R.id.cloud_path_style).isChecked, text(R.id.cloud_token), text(R.id.cloud_password),
            view.findViewById<CheckBox>(R.id.cloud_auto).isChecked, keep)
        return try {
            CloudStore(config).cancel() // Validate locally; do not send credentials or perform a request.
            config
        } catch (_: IllegalArgumentException) { toast(R.string.cloud_invalid); null }
    }
    private fun run(action: String, snapshot: CloudSnapshot? = null, balances: Boolean = false) {
        val config = config() ?: return
        model.start(requireContext(), config, action, snapshot, balances)
    }
    private fun consentNetwork(action: () -> Unit) {
        if (CloudNetwork.isWifi(requireContext())) action()
        else dialog = AlertDialog.Builder(requireContext()).setMessage(R.string.cloud_mobile_consent)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok) { _, _ -> action() }.show()
    }
    private fun message(code: String): String = getString(when (code) {
        "idle" -> R.string.cloud_idle; "test", "backup", "list", "preview", "restore", "delete", "prepare" -> R.string.cloud_working
        "transfer" -> R.string.cloud_transferring; "estimate" -> R.string.cloud_estimate
        "success_cleanup" -> R.string.cloud_success_cleanup
        "tested" -> R.string.cloud_tested; "success" -> R.string.cloud_success; "unchanged" -> R.string.cloud_unchanged
        "listed" -> R.string.cloud_listed; "restored" -> R.string.cloud_restored; "deleted" -> R.string.cloud_deleted
        "cancelled" -> R.string.cloud_cancelled; "waiting_wifi" -> R.string.cloud_waiting_wifi
        "integrity" -> R.string.cloud_integrity
        "invalid" -> R.string.cloud_invalid
        "vault_password" -> R.string.cloud_vault_password
        "remote_format" -> R.string.cloud_remote_format
        "compatibility" -> R.string.cloud_compatibility
        "create_protection" -> R.string.cloud_create_protection
        "not_found" -> R.string.cloud_not_found
        "path_conflict" -> R.string.cloud_path_conflict
        "rate_limited" -> R.string.cloud_rate_limited
        "redirect" -> R.string.cloud_redirect; "auth" -> R.string.cloud_auth
        "balance_conflict" -> R.string.cloud_balance_conflict; "preview_ready" -> R.string.cloud_preview
        else -> R.string.cloud_failed
    })
    private fun render(ui: CloudBackupViewModel.Ui) {
        val view = binding ?: return
        val httpDetail = ui.httpStatus?.let { "\n" + getString(R.string.cloud_http_detail, ui.httpMethod, it) }.orEmpty()
        val info = message(ui.message) + httpDetail + if (ui.bytes > 0 || ui.expected > 0) "\n" + getString(R.string.cloud_transfer_value, ui.bytes / 1048576.0, ui.expected / 1048576.0) else ""
        view.findViewById<TextView>(R.id.cloud_status).text = info
        view.findViewById<View>(R.id.cloud_progress).visibility = if (ui.busy) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.cloud_cancel).visibility = if (ui.busy && ui.message != "restore") View.VISIBLE else View.GONE
        fields.forEach { view.findViewById<View>(it).isEnabled = !ui.busy }
        listOf(R.id.cloud_type, R.id.cloud_webdav, R.id.cloud_s3, R.id.cloud_remember, R.id.cloud_auto, R.id.cloud_path_style,
            R.id.cloud_save, R.id.cloud_test, R.id.cloud_backup, R.id.cloud_list, R.id.cloud_disconnect).forEach {
            view.findViewById<View>(it).isEnabled = !ui.busy
        }
        val list = view.findViewById<LinearLayout>(R.id.cloud_snapshots)
        if (ui.busy) { list.visibility = View.GONE; return }
        list.visibility = View.VISIBLE
        list.removeAllViews()
        ui.listing?.let { listing ->
            list.removeAllViews()
            view.findViewById<TextView>(R.id.cloud_usage).text = getString(R.string.cloud_usage_value, listing.bytes / 1048576.0)
            listing.snapshots.forEach { snapshot ->
                val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, (16 * resources.displayMetrics.density).toInt(), 0, (16 * resources.displayMetrics.density).toInt()) }
                row.addView(TextView(requireContext()).apply { text = DateFormat.getDateTimeInstance().format(Date(snapshot.date)) +
                    " · " + getString(if (snapshot.automatic) R.string.cloud_automatic else R.string.cloud_manual) + " · " + snapshot.device.take(8) })
                val buttons = LinearLayout(requireContext())
                buttons.addView(MaterialButton(requireContext()).apply { setText(R.string.cloud_restore); minHeight = (48 * resources.displayMetrics.density).toInt()
                    setOnClickListener { consentNetwork { run("preview", snapshot) } } }, LinearLayout.LayoutParams(0, -2, 1f))
                buttons.addView(MaterialButton(requireContext()).apply { setText(R.string.cloud_delete)
                    setOnClickListener { dialog = AlertDialog.Builder(requireContext()).setMessage(R.string.cloud_delete_hint)
                        .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.cloud_delete) { _, _ -> run("delete", snapshot) }.show() }
                }, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(buttons); list.addView(row)
            }
        }
        // Preview remains an explicit inline action, so recreation never reopens a confirmation or writes data.
        if (ui.message == "preview_ready" && ui.snapshot != null && ui.preview != null) {
            list.removeAllViews()
            val result = ui.preview
            list.addView(TextView(requireContext()).apply { text = getString(R.string.settings_backup_preview,
                result.ledgers, result.categories, result.assets, result.records, result.recurring, result.sessions,
                result.messages, result.skipped, result.different) })
            list.addView(MaterialButton(requireContext()).apply { setText(R.string.cloud_confirm_restore)
                setOnClickListener { dialog = AlertDialog.Builder(requireContext()).setMessage(R.string.cloud_restore_hint)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.cloud_confirm_restore) { _, _ ->
                        run("restore", ui.snapshot, ui.useBalances)
                    }.show() } })
            list.addView(MaterialButton(requireContext()).apply { setText(android.R.string.cancel); setOnClickListener { model.dismissPreview(); list.removeAllViews() } })
        } else if (ui.message == "balance_conflict" && ui.snapshot != null) {
            list.removeAllViews()
            list.addView(MaterialButton(requireContext()).apply { setText(R.string.settings_backup_use_backup_balance)
                setOnClickListener { dialog = AlertDialog.Builder(requireContext()).setMessage(R.string.settings_backup_asset_conflict_choice)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok) { _, _ ->
                        consentNetwork { run("preview", ui.snapshot, true) }
                    }.show() } })
        }
    }
    private fun toast(id: Int) { context?.let { Toast.makeText(it, id, Toast.LENGTH_LONG).show() } }
    override fun onDestroyView() { dialog?.dismiss(); dialog = null; binding = null; super.onDestroyView() }
}
