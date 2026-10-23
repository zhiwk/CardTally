package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import androidx.appcompat.widget.ListPopupWindow
import android.graphics.drawable.GradientDrawable
import android.widget.Toast
import android.widget.EditText
import android.widget.LinearLayout
import android.text.InputType
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.example.cardtally.util.AssetDisplayHelper
import com.example.cardtally.util.LanguageHelper
import com.example.cardtally.util.QuickAddHelper
import com.example.cardtally.util.RecordEntryMode
import com.example.cardtally.util.RecordEntryModePreferences
import com.example.cardtally.util.RecordPhotoSettingsHelper
import com.example.cardtally.util.ScrollTopFabHelper
import com.example.cardtally.util.IncomeExpenseColorScheme
import com.example.cardtally.util.BackupArchiveManager
import com.example.cardtally.util.DefaultRecordAssetPreferences
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.ThemeHelper
import java.util.concurrent.Executors

class SettingsFragment : Fragment() {
    private lateinit var cardQuickAdd: View
    private lateinit var switchQuickAdd: Switch
    private lateinit var cardAiApiKey: View
    private lateinit var switchAiAssistant: Switch
    private lateinit var cardShowAsset: View
    private lateinit var switchShowAsset: Switch
    private lateinit var switchScrollTopFab: Switch
    private lateinit var cardCategory: View
    private lateinit var cardLedgerManagement: View
    private lateinit var cardTheme: View
    private lateinit var textCurrentTheme: TextView
    private lateinit var cardLanguage: View
    private lateinit var cardIncomeExpenseColor: View
    private lateinit var textIncomeExpenseColor: TextView
    private lateinit var textAiApiKeyStatus: TextView
    private lateinit var textCurrentLanguage: TextView
    private lateinit var cardRecordPhotoLimit: View
    private lateinit var textRecordPhotoLimit: TextView
    private lateinit var cardRecordEntryMode: View
    private lateinit var textRecordEntryMode: TextView
    private lateinit var cardDefaultExpenseAsset: View
    private lateinit var cardDefaultIncomeAsset: View
    private lateinit var cardRecurringRecords: View
    private lateinit var textDefaultExpenseAsset: TextView
    private lateinit var textDefaultIncomeAsset: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private val transferExecutor = Executors.newSingleThreadExecutor()
    private var pendingExportPassword: CharArray? = null
    private var settingsChoicePopup: ListPopupWindow? = null

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val password = pendingExportPassword ?: return@registerForActivityResult
        pendingExportPassword = null
        if (uri == null) {
            password.fill('\u0000')
            return@registerForActivityResult
        }
        val context = requireContext().applicationContext
        val hostActivity = activity
        transferExecutor.execute {
            val error = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    BackupArchiveManager(context).exportTo(output, password)
                } ?: error("Unable to open export destination")
            }.exceptionOrNull()
            password.fill('\u0000')
            hostActivity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                Toast.makeText(context, if (error == null) R.string.settings_export_success else R.string.settings_transfer_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val encrypted = requireContext().contentResolver.openInputStream(uri)?.use { input ->
            val header = ByteArray(4)
            input.read(header) == 4 && header.contentEquals(byteArrayOf(67, 84, 66, 51))
        } ?: false
        if (encrypted) {
            passwordDialog(confirm = false) { password -> previewImport(uri, password) }
        } else {
            val zip = requireContext().contentResolver.openInputStream(uri)?.use { input ->
                input.read() == 'P'.code && input.read() == 'K'.code
            } ?: false
            if (zip) previewImport(uri, null) else runImport(uri, null)
        }
    }

    private fun previewImport(uri: Uri, password: CharArray?, useBackupBalances: Boolean = false) {
        val context = requireContext().applicationContext
        val hostActivity = activity
        transferExecutor.execute {
            val preview = runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    BackupArchiveManager(context).importFrom(it, password, dryRun = true,
                        useBackupBalances = useBackupBalances)
                } ?: error("Unable to open import file")
            }
            hostActivity?.runOnUiThread {
                if (!isAdded) {
                    password?.fill('\u0000')
                    return@runOnUiThread
                }
                preview.fold(
                    onSuccess = { result ->
                        val details = getString(R.string.settings_backup_preview,
                            result.ledgers, result.categories, result.assets, result.records,
                            result.recurring, result.sessions, result.messages,
                            result.skipped, result.different) +
                            if (result.balanceConflicts > 0) "\n" +
                                getString(R.string.settings_backup_balance_overwrite_count, result.balanceConflicts)
                            else ""
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(R.string.settings_backup_preview_title)
                            .setMessage(details)
                            .setNegativeButton(android.R.string.cancel) { _, _ -> password?.fill('\u0000') }
                            .setPositiveButton(R.string.settings_backup_merge_confirm) { _, _ ->
                                runImport(uri, password, useBackupBalances)
                            }
                            .setOnCancelListener { password?.fill('\u0000') }
                            .show()
                    },
                    onFailure = { error ->
                        if (error is BackupArchiveManager.AssetBalanceConflictException &&
                            !useBackupBalances) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setMessage(R.string.settings_backup_asset_conflict_choice)
                                .setNegativeButton(android.R.string.cancel) { _, _ ->
                                    password?.fill('\u0000')
                                }
                                .setPositiveButton(R.string.settings_backup_use_backup_balance) { _, _ ->
                                    previewImport(uri, password, useBackupBalances = true)
                                }
                                .setOnCancelListener { password?.fill('\u0000') }
                                .show()
                        } else {
                            password?.fill('\u0000')
                            val message = when (error) {
                                is BackupArchiveManager.AssetBalanceConflictException -> R.string.settings_backup_asset_conflict
                                is BackupArchiveManager.BackupIntegrityException -> R.string.settings_backup_integrity_failed
                                else -> R.string.settings_import_failed
                            }
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setMessage(message).setPositiveButton(android.R.string.ok, null).show()
                        }
                    }
                )
            }
        }
    }

    private fun runImport(uri: Uri, password: CharArray?, useBackupBalances: Boolean = false) {
        val context = requireContext().applicationContext
        val hostActivity = activity
        transferExecutor.execute {
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    BackupArchiveManager(context).importFrom(it, password,
                        useBackupBalances = useBackupBalances)
                }
                    ?: error("Unable to open import file")
            }
            password?.fill('\u0000')
            hostActivity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                if (result.isSuccess && view != null) {
                    LanguageHelper.applySavedLanguage(requireContext())
                    refreshSettingsFromStorage()
                }
                val message = result.fold(
                    onSuccess = {
                        if (it.legacy) getString(R.string.settings_legacy_import_success)
                        else getString(R.string.settings_backup_import_success, it.ledgers, it.categories, it.assets, it.records, it.recurring, it.sessions, it.messages)
                    },
                    onFailure = { error ->
                        when (error) {
                            is BackupArchiveManager.AssetBalanceConflictException ->
                                getString(R.string.settings_backup_asset_conflict)
                            is BackupArchiveManager.BackupIntegrityException ->
                                getString(R.string.settings_backup_integrity_failed)
                            else -> getString(R.string.settings_import_failed)
                        }
                    }
                )
                val summary = result.getOrNull()?.takeIf { !it.legacy && it.different > 0 }
                    ?.let { "\n" + getString(R.string.settings_backup_differences, it.different) }.orEmpty()
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setMessage(message + summary)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    private fun passwordDialog(confirm: Boolean, onReady: (CharArray) -> Unit) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 8.dp(), 24.dp(), 0)
        }
        fun field(hint: Int) = EditText(requireContext()).apply {
            setHint(hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            container.addView(this)
        }
        val first = field(R.string.settings_backup_password)
        val second = if (confirm) field(R.string.settings_backup_password_confirm) else null
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(if (confirm) R.string.settings_backup_password_title else R.string.settings_backup_password_import_title)
            .apply { if (confirm) setMessage(R.string.settings_backup_password_warning) }
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = first.text.toString()
                if (value.length < 8 || (second != null && value != second.text.toString())) {
                    Toast.makeText(requireContext(), R.string.settings_backup_password_invalid, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                onReady(value.toCharArray())
            }
        }
        dialog.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        parentFragmentManager.setFragmentResultListener(
            DefaultRecordAssetPickerBottomSheetFragment.RESULT_KEY,
            this
        ) { _, result ->
            onDefaultRecordAssetSelected(
                result.getLong("selected_id"),
                result.getBoolean("for_income")
            )
        }
        val view = inflater.inflate(R.layout.fragment_settings_v2, container, false)
        com.example.cardtally.util.SettingsRowLayout.bind(view)

        cardQuickAdd = view.findViewById(R.id.card_quick_add)
        switchQuickAdd = view.findViewById(R.id.switch_quick_add)
        cardAiApiKey = view.findViewById(R.id.card_ai_api_key)
        switchAiAssistant = view.findViewById(R.id.switch_ai_assistant)
        cardShowAsset = view.findViewById(R.id.card_show_asset)
        switchShowAsset = view.findViewById(R.id.switch_show_asset)
        switchScrollTopFab = view.findViewById(R.id.switch_scroll_top_fab)
        cardCategory = view.findViewById(R.id.card_category)
        cardLedgerManagement = view.findViewById(R.id.card_ledger_management)
        cardTheme = view.findViewById(R.id.card_theme)
        textCurrentTheme = view.findViewById(R.id.text_current_theme)
        cardLanguage = view.findViewById(R.id.card_language)
        cardIncomeExpenseColor = view.findViewById(R.id.card_income_expense_color)
        textIncomeExpenseColor = view.findViewById(R.id.text_income_expense_color)
        textAiApiKeyStatus = view.findViewById(R.id.text_ai_api_key_status)
        textCurrentLanguage = view.findViewById(R.id.text_current_language)
        cardRecordPhotoLimit = view.findViewById(R.id.card_record_photo_limit)
        textRecordPhotoLimit = view.findViewById(R.id.text_record_photo_limit)
        cardRecordEntryMode = view.findViewById(R.id.card_record_entry_mode)
        textRecordEntryMode = view.findViewById(R.id.text_record_entry_mode)
        cardDefaultExpenseAsset = view.findViewById(R.id.card_default_expense_asset)
        cardDefaultIncomeAsset = view.findViewById(R.id.card_default_income_asset)
        cardRecurringRecords = view.findViewById(R.id.card_recurring_records)
        textDefaultExpenseAsset = view.findViewById(R.id.text_default_expense_asset)
        textDefaultIncomeAsset = view.findViewById(R.id.text_default_income_asset)
        databaseHelper = DatabaseHelper(requireContext())

        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchAiAssistant.isChecked = AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())
        switchScrollTopFab.isChecked = ScrollTopFabHelper.isEnabled(requireContext())

        updateThemeText()
        updateCurrentLanguageText()
        updateAiApiKeyStatus()
        updateRecordPhotoLimitText()
        updateIncomeExpenseColorText()
        updateRecordEntryModeText()
        updateDefaultRecordAssetText()

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
        val analyticsSwitch = view.findViewById<Switch>(R.id.switch_launch_analytics)
        analyticsSwitch.isChecked = com.example.cardtally.analytics.LaunchAnalytics.enabled(requireContext())
        analyticsSwitch.isEnabled = com.example.cardtally.analytics.LaunchAnalytics.available()
        view.findViewById<TextView>(R.id.text_launch_analytics_hint).setText(
            if (analyticsSwitch.isEnabled) R.string.analytics_hint else R.string.analytics_unavailable)
        view.findViewById<TextView>(R.id.text_launch_analytics_hint).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.analytics_title)
                .setMessage(R.string.analytics_consent)
                .setPositiveButton(android.R.string.ok, null).show()
        }
        analyticsSwitch.setOnClickListener {
            if (!analyticsSwitch.isChecked) {
                com.example.cardtally.analytics.LaunchAnalytics.setEnabled(requireContext(), false)
            } else {
                analyticsSwitch.isChecked = false
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.analytics_title)
                    .setMessage(R.string.analytics_consent)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.analytics_enable) { _, _ ->
                        if (isAdded && this.view === view) {
                            com.example.cardtally.analytics.LaunchAnalytics.setEnabled(requireContext(), true)
                            analyticsSwitch.isChecked = true
                            com.example.cardtally.analytics.LaunchAnalytics.recordLaunch(requireContext())
                        }
                    }.show()
            }
        }
        cardRecordPhotoLimit.setOnClickListener { showRecordPhotoLimitDialog() }
        cardIncomeExpenseColor.setOnClickListener { showIncomeExpenseColorDialog() }
        cardRecordEntryMode.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RecordEntryModeSettingsFragment())
                .addToBackStack(null)
                .commit()
        }
        cardDefaultExpenseAsset.setOnClickListener { showDefaultRecordAssetPicker(false) }
        cardDefaultIncomeAsset.setOnClickListener { showDefaultRecordAssetPicker(true) }
        cardRecurringRecords.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RecurringRecordsFragment())
                .addToBackStack(null)
                .commit()
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

        cardLedgerManagement.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LedgerManagementFragment.newAssetManagementInstance())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.card_cloud_backup).setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragment_container, com.example.cardtally.cloud.CloudBackupFragment())
                .addToBackStack(null).commit()
        }
        view.findViewById<View>(R.id.card_about).setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragment_container, AboutFragment())
                .addToBackStack(null).commit()
        }

        view.findViewById<View>(R.id.card_bottom_navigation).setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.fragment_container, BottomNavigationSettingsFragment())
                .addToBackStack(null).commit()
        }
        cardTheme.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AppearanceSettingsFragment())
                .addToBackStack(null)
                .commit()
        }
        cardLanguage.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LanguageSettingsFragment())
                .addToBackStack(null)
                .commit()
        }
        view.findViewById<View>(R.id.card_export_data).setOnClickListener {
            passwordDialog(confirm = true) { password ->
                pendingExportPassword?.fill('\u0000')
                pendingExportPassword = password
                exportLauncher.launch("cardtally-${System.currentTimeMillis()}.ctb")
            }
        }
        view.findViewById<View>(R.id.card_import_data).setOnClickListener {
            importLauncher.launch(arrayOf("application/zip", "application/json", "*/*"))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (view == null || !::switchQuickAdd.isInitialized || !::switchScrollTopFab.isInitialized) {
            return
        }
        refreshSettingsFromStorage()
    }

    private fun refreshSettingsFromStorage() {
        view?.findViewById<TextView>(R.id.text_bottom_navigation_style)?.setText(
            if (com.example.cardtally.util.BottomNavigationPreferences.isDocked(requireContext()))
                R.string.navigation_style_docked else R.string.navigation_style_floating)

        updateThemeText()
        updateCurrentLanguageText()
        updateAiApiKeyStatus()
        updateRecordPhotoLimitText()
        updateIncomeExpenseColorText()
        updateRecordEntryModeText()
        updateDefaultRecordAssetText()
        switchQuickAdd.isChecked = QuickAddHelper.getQuickAdd(requireContext())
        switchAiAssistant.isChecked = AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())
        switchShowAsset.isChecked = AssetDisplayHelper.getShowAsset(requireContext())
        switchScrollTopFab.isChecked = ScrollTopFabHelper.isEnabled(requireContext())
    }

    override fun onDestroyView() {
        settingsChoicePopup?.dismiss()
        settingsChoicePopup = null
        if (::databaseHelper.isInitialized) {
            databaseHelper.close()
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = null
        transferExecutor.shutdown()
        super.onDestroy()
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

    private fun updateRecordEntryModeText() {
        if (!::textRecordEntryMode.isInitialized || !isAdded) return
        textRecordEntryMode.text = getString(
            when (RecordEntryModePreferences.getMode(requireContext())) {
                RecordEntryMode.QUICK -> R.string.record_entry_mode_quick_title
                else -> R.string.record_entry_mode_standard_title
            }
        )
    }

    private fun updateRecordPhotoLimitText() {
        if (!::textRecordPhotoLimit.isInitialized || !isAdded) return
        textRecordPhotoLimit.text = getString(
            R.string.settings_record_photo_limit_value,
            RecordPhotoSettingsHelper.getMaxPhotos(requireContext())
        )
    }

    private fun updateDefaultRecordAssetText() {
        if (!isAdded) return
        textDefaultExpenseAsset.text = defaultAssetName(DefaultRecordAssetPreferences.getExpenseAssetId(requireContext()))
        textDefaultIncomeAsset.text = defaultAssetName(DefaultRecordAssetPreferences.getIncomeAssetId(requireContext()))
    }

    private fun defaultAssetName(assetId: Long?): String =
        assetId?.let { id -> databaseHelper.getAllAssets().firstOrNull { it.id == id }?.name }
            ?: getString(R.string.settings_default_asset_not_set)

    private fun showDefaultRecordAssetPicker(forIncome: Boolean) {
        val selectedId = if (forIncome) {
            DefaultRecordAssetPreferences.getIncomeAssetId(requireContext())
        } else {
            DefaultRecordAssetPreferences.getExpenseAssetId(requireContext())
        }
        DefaultRecordAssetPickerBottomSheetFragment.newInstance(forIncome, selectedId)
            .show(parentFragmentManager, DefaultRecordAssetPickerBottomSheetFragment.TAG)
    }

    private fun onDefaultRecordAssetSelected(assetId: Long, forIncome: Boolean) {
        if (forIncome) {
            DefaultRecordAssetPreferences.saveIncomeAssetId(requireContext(), assetId)
        } else {
            DefaultRecordAssetPreferences.saveExpenseAssetId(requireContext(), assetId)
        }
        updateDefaultRecordAssetText()
    }

    private fun showRecordPhotoLimitDialog() {
        val labels = (1..9).map { getString(R.string.settings_record_photo_limit_value, it) }
        showSettingsChoiceMenu(cardRecordPhotoLimit, labels,
            RecordPhotoSettingsHelper.getMaxPhotos(requireContext()) - 1, 200) { position ->
            RecordPhotoSettingsHelper.saveMaxPhotos(requireContext(), position + 1)
            updateRecordPhotoLimitText()
        }
    }

    private fun showSettingsChoiceMenu(anchor: View, labels: List<String>, selected: Int,
        widthDp: Int, onSelected: (Int) -> Unit) {
        settingsChoicePopup?.dismiss()
        val context = requireContext()
        val density = resources.displayMetrics.density
        val adapter = object : ArrayAdapter<String>(context, R.layout.item_record_photo_count, labels) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                (super.getView(position, convertView, parent) as CheckedTextView).apply {
                    isChecked = position == selected
                }
        }
        val popup = ListPopupWindow(context).apply {
            anchorView = anchor
            isModal = true
            setDropDownGravity(android.view.Gravity.END)
            width = minOf((widthDp * density).toInt(), anchor.width)
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            setBackgroundDrawable(GradientDrawable().apply {
                setColor(ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_light))
                cornerRadius = 20 * density
            })
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                onSelected(position)
                dismiss()
            }
            setOnDismissListener { settingsChoicePopup = null }
        }
        settingsChoicePopup = popup
        popup.show()
        popup.listView?.apply {
            divider = null
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
            setSelection(selected)
            clipToOutline = true
        }
    }

    private fun updateIncomeExpenseColorText() {
        if (!::textIncomeExpenseColor.isInitialized || !isAdded) return
        textIncomeExpenseColor.text = when (IncomeExpenseColorScheme.getMode(requireContext())) {
            IncomeExpenseColorScheme.INCOME_GREEN_EXPENSE_RED -> getString(R.string.settings_income_expense_color_income_green_expense_red)
            IncomeExpenseColorScheme.BOTH_BLACK -> getString(R.string.settings_income_expense_color_both_black)
            else -> getString(R.string.settings_income_expense_color_income_red_expense_green)
        }
    }

    private fun updateThemeText() {
        textCurrentTheme.setText(when (ThemeHelper.getTheme(requireContext())) {
            ThemeHelper.THEME_DARK -> R.string.theme_dark
            ThemeHelper.THEME_SYSTEM -> R.string.theme_system
            ThemeHelper.THEME_WALLPAPER -> when (ThemeHelper.getWallpaperPalette(requireContext())) {
                ThemeHelper.THEME_LIGHT -> R.string.theme_wallpaper_light
                ThemeHelper.THEME_SYSTEM -> R.string.theme_wallpaper_system
                else -> R.string.theme_wallpaper_dark
            }
            else -> R.string.theme_light
        })
    }

    private fun showIncomeExpenseColorDialog() {
        val labels = listOf(
            getString(R.string.settings_income_expense_color_income_green_expense_red),
            getString(R.string.settings_income_expense_color_income_red_expense_green),
            getString(R.string.settings_income_expense_color_both_black)
        )
        showSettingsChoiceMenu(cardIncomeExpenseColor, labels,
            IncomeExpenseColorScheme.getMode(requireContext()), 280) { index ->
            IncomeExpenseColorScheme.saveMode(requireContext(), index)
            updateIncomeExpenseColorText()
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

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
