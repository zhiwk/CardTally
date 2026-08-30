package com.example.cardtally

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.adapter.IconPickerAdapter
import com.example.cardtally.util.LedgerSession
import com.example.cardtally.util.TablerIconCatalog

class LedgerSetupFragment : Fragment() {
    companion object {
        private const val ARG_LEDGER_ID = "ledger_id"
        private const val STATE_NAME = "state_ledger_name"
        private const val STATE_SHARE = "state_ledger_share_assets"
        private const val STATE_SOURCE = "state_ledger_asset_source"
        private const val STATE_ICON = "state_ledger_icon"
        fun newEditInstance(ledgerId: Long) = LedgerSetupFragment().apply {
            arguments = Bundle().apply { putLong(ARG_LEDGER_ID, ledgerId) }
        }
    }

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var nameInput: EditText
    private lateinit var shareSwitch: Switch
    private lateinit var assetList: LinearLayout
    private var selectedSourceLedgerId: Long? = null
    private var selectedIconName = "tabler_book"
    private val editingLedgerId: Long? by lazy {
        arguments?.getLong(ARG_LEDGER_ID, -1L)?.takeIf { it > 0L }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_ledger_setup, container, false)
        databaseHelper = DatabaseHelper(requireContext())
        nameInput = view.findViewById(R.id.ledger_setup_name)
        shareSwitch = view.findViewById(R.id.ledger_setup_share_assets)
        assetList = view.findViewById(R.id.ledger_setup_asset_list)
        val assetModeGroup = view.findViewById<RadioGroup>(R.id.ledger_setup_asset_mode_group)
        val independentAssets = view.findViewById<RadioButton>(R.id.ledger_setup_independent_assets)
        val shareOtherAssets = view.findViewById<RadioButton>(R.id.ledger_setup_share_other_assets)
        val assetSourceTitle = view.findViewById<TextView>(R.id.ledger_setup_choose_ledger)
        val iconView = view.findViewById<ImageView>(R.id.ledger_setup_icon)
        val iconLabel = view.findViewById<TextView>(R.id.ledger_setup_icon_label)
        view.findViewById<View>(R.id.ledger_setup_asset_relation_section).visibility =
            if (editingLedgerId == null) View.VISIBLE else View.GONE
        fun refreshIcon() {
            iconView.setImageResource(TablerIconCatalog.resourceId(selectedIconName).takeIf { it != 0 } ?: R.drawable.ic_book)
            iconLabel.text = getString(R.string.ledger_setup_icon_selected)
        }
        view.findViewById<View>(R.id.ledger_setup_icon_picker).setOnClickListener {
            showIconPickerDialog(selectedIconName) { icon ->
                selectedIconName = icon ?: "tabler_book"
                refreshIcon()
            }
        }
        refreshIcon()
        view.findViewById<TextView>(R.id.ledger_setup_title).setText(
            if (editingLedgerId != null) R.string.ledger_setup_edit_title else R.string.ledger_setup_title
        )
        view.findViewById<android.widget.Button>(R.id.button_ledger_setup_save).setText(
            if (editingLedgerId != null) R.string.ledger_setup_update else R.string.ledger_setup_save
        )

        view.findViewById<View>(R.id.button_ledger_setup_back).setOnClickListener { parentFragmentManager.popBackStack() }
        independentAssets.isChecked = true
        shareOtherAssets.isChecked = false
        shareOtherAssets.visibility = if (databaseHelper.getLedgers().any { it.id != databaseHelper.getMasterLedgerId() && it.id != editingLedgerId }) View.VISIBLE else View.GONE
        shareSwitch.setOnCheckedChangeListener { _, checked ->
            assetModeGroup.visibility = if (checked) View.GONE else View.VISIBLE
            assetSourceTitle.visibility = if (!checked && shareOtherAssets.isChecked) View.VISIBLE else View.GONE
            assetList.visibility = if (!checked && shareOtherAssets.isChecked) View.VISIBLE else View.GONE
            if (checked) selectedSourceLedgerId = databaseHelper.getMasterLedgerId()
        }
        independentAssets.setOnClickListener {
            selectedSourceLedgerId = null
            assetSourceTitle.visibility = View.GONE
            assetList.visibility = View.GONE
        }
        shareOtherAssets.setOnClickListener {
            if (selectedSourceLedgerId == null) {
                selectedSourceLedgerId = databaseHelper.getLedgers()
                    .firstOrNull { it.id != databaseHelper.getMasterLedgerId() && it.id != editingLedgerId }?.id
            }
            assetSourceTitle.visibility = View.VISIBLE
            assetList.visibility = View.VISIBLE
            renderAssets()
        }
        assetModeGroup.visibility = if (shareSwitch.isChecked) View.GONE else View.VISIBLE
        assetSourceTitle.visibility = View.GONE
        renderAssets()
        if (savedInstanceState == null && editingLedgerId != null) {
            val ledger = databaseHelper.getLedgers().firstOrNull { it.id == editingLedgerId }
            nameInput.setText(ledger?.name.orEmpty())
            selectedIconName = ledger?.iconName ?: "tabler_book"
            refreshIcon()
            selectedSourceLedgerId = databaseHelper.getSharedSourceLedgerId(editingLedgerId!!)
            val masterId = databaseHelper.getMasterLedgerId()
            shareSwitch.isChecked = selectedSourceLedgerId == masterId
            if (!shareSwitch.isChecked) {
                independentAssets.isChecked = selectedSourceLedgerId == null
                shareOtherAssets.isChecked = selectedSourceLedgerId != null
                assetSourceTitle.visibility = if (shareOtherAssets.isChecked) View.VISIBLE else View.GONE
                assetList.visibility = if (shareOtherAssets.isChecked) View.VISIBLE else View.GONE
            }
        }
        savedInstanceState?.let { state ->
            nameInput.setText(state.getString(STATE_NAME).orEmpty())
            selectedSourceLedgerId = state.getLong(STATE_SOURCE, -1L).takeIf { it > 0L }
            selectedIconName = state.getString(STATE_ICON) ?: "tabler_book"
            shareSwitch.isChecked = state.getBoolean(STATE_SHARE, false)
            assetModeGroup.visibility = if (shareSwitch.isChecked) View.GONE else View.VISIBLE
            refreshIcon()
        }
        if (editingLedgerId != null) {
            assetModeGroup.visibility = View.GONE
            assetSourceTitle.visibility = View.GONE
            assetList.visibility = View.GONE
        }
        view.findViewById<View>(R.id.button_ledger_setup_save).setOnClickListener { saveLedger() }
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_NAME, nameInput.text.toString())
        outState.putBoolean(STATE_SHARE, shareSwitch.isChecked)
        outState.putLong(STATE_SOURCE, selectedSourceLedgerId ?: -1L)
        outState.putString(STATE_ICON, selectedIconName)
        super.onSaveInstanceState(outState)
    }

    private fun renderAssets() {
        assetList.removeAllViews()
        val currentId = LedgerSession.getCurrentId(requireContext())
        val masterId = databaseHelper.getMasterLedgerId()
        val ledgers = databaseHelper.getLedgers().filter { it.id != editingLedgerId && it.id != masterId }
        if (ledgers.isEmpty()) {
            assetList.addView(TextView(requireContext()).apply {
                text = getString(R.string.ledger_setup_no_assets)
                textSize = 13f
                setTextColor(Color.GRAY)
                setPadding(0, 8.dp, 0, 8.dp)
            })
            return
        }
        ledgers.forEach { ledger ->
            val option = android.widget.RadioButton(requireContext()).apply {
                text = ledger.name
                textSize = 15f
                setTextColor(Color.rgb(32, 33, 35))
                minHeight = 48.dp
                gravity = Gravity.CENTER_VERTICAL
                isChecked = selectedSourceLedgerId == ledger.id || (selectedSourceLedgerId == null && ledger.id == currentId)
                setOnClickListener {
                    for (index in 0 until assetList.childCount) {
                        (assetList.getChildAt(index) as? RadioButton)?.isChecked = false
                    }
                    isChecked = true
                    selectedSourceLedgerId = ledger.id
                }
            }
            assetList.addView(option, LinearLayout.LayoutParams(-1, 48.dp))
        }
    }

    private fun saveLedger() {
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            nameInput.error = getString(R.string.ledger_setup_name_required)
            nameInput.requestFocus()
            return
        }
        if (editingLedgerId != null) {
            if (!databaseHelper.updateLedgerDetails(editingLedgerId!!, selectedIconName, name)) return
            Toast.makeText(requireContext(), R.string.ledger_setup_updated, Toast.LENGTH_SHORT).show()
        } else {
            val sourceId = when {
                shareSwitch.isChecked -> databaseHelper.getMasterLedgerId()
                else -> selectedSourceLedgerId
            }
            val ledgerId = databaseHelper.addLedgerWithAssetPool(
                name,
                sharedSourceLedgerId = sourceId,
                iconName = selectedIconName
            )
            LedgerSession.setCurrentId(requireContext(), ledgerId)
            Toast.makeText(requireContext(), R.string.ledger_setup_saved, Toast.LENGTH_SHORT).show()
        }
        parentFragmentManager.popBackStack()
    }

    private fun showIconPickerDialog(selectedIcon: String?, onIconSelected: (String?) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_icon_picker, null)
        val recycler = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_icons)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.category_icon_picker_title)
            .setView(dialogView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
        recycler.adapter = IconPickerAdapter(TablerIconCatalog.icons, selectedIcon) { icon ->
            onIconSelected(icon)
            dialog.dismiss()
        }
        dialog.show()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
