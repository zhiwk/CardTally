package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.AssetGroupPickerAdapter
import com.example.cardtally.adapter.IconPickerDialog
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.util.LedgerSession
import com.example.cardtally.util.TablerIconCatalog
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Creates a ledger (name, icon and which asset group it uses) or edits an
 * existing ledger's name and icon.
 *
 * New ledgers choose directly between starting an independent asset group and
 * joining one of the existing (de-duplicated) groups; editing never touches the
 * asset relationship.
 */
class LedgerSetupFragment : Fragment() {
    companion object {
        private const val ARG_LEDGER_ID = "ledger_id"
        private const val STATE_NAME = "state_ledger_name"
        private const val STATE_ICON = "state_ledger_icon"
        private const val STATE_MODE = "state_ledger_asset_mode"
        private const val STATE_GROUP_ROOT = "state_ledger_group_root"
        private const val MODE_EXISTING = "existing"
        private const val MODE_INDEPENDENT = "independent"

        fun newEditInstance(ledgerId: Long) = LedgerSetupFragment().apply {
            arguments = Bundle().apply { putLong(ARG_LEDGER_ID, ledgerId) }
        }
    }

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var nameInput: EditText
    private lateinit var iconView: ImageView
    private lateinit var iconLabel: TextView
    private lateinit var groupSummary: TextView
    private lateinit var groupName: TextView
    private lateinit var groupRow: View
    private lateinit var assetList: View
    private lateinit var independentRow: View
    private lateinit var existingRow: View
    private lateinit var independentRadio: android.widget.RadioButton
    private lateinit var existingRadio: android.widget.RadioButton
    private lateinit var saveButton: View

    private var selectedIconName = "tabler_book"
    private var useExistingGroup = true
    private var selectedGroupRootId: Long? = null

    private val editingLedgerId: Long? by lazy {
        arguments?.getLong(ARG_LEDGER_ID, -1L)?.takeIf { it > 0L }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ledger_setup, container, false)
        databaseHelper = DatabaseHelper(requireContext())

        nameInput = view.findViewById(R.id.ledger_setup_name)
        iconView = view.findViewById(R.id.ledger_setup_icon)
        iconLabel = view.findViewById(R.id.ledger_setup_icon_label)
        groupSummary = view.findViewById(R.id.ledger_setup_group_summary)
        groupName = view.findViewById(R.id.ledger_setup_group_name)
        groupRow = view.findViewById(R.id.ledger_setup_group_row)
        assetList = view.findViewById(R.id.ledger_setup_asset_list)
        independentRow = view.findViewById(R.id.ledger_setup_independent_row)
        existingRow = view.findViewById(R.id.ledger_setup_existing_row)
        independentRadio = view.findViewById(R.id.ledger_setup_independent_assets)
        existingRadio = view.findViewById(R.id.ledger_setup_share_other_assets)
        saveButton = view.findViewById(R.id.button_ledger_setup_save)

        val isEditing = editingLedgerId != null
        view.findViewById<View>(R.id.ledger_setup_asset_relation_section).visibility =
            if (isEditing) View.GONE else View.VISIBLE

        view.findViewById<TextView>(R.id.ledger_setup_title).setText(
            if (isEditing) R.string.ledger_setup_edit_title else R.string.ledger_setup_title
        )
        (saveButton as? com.google.android.material.button.MaterialButton)?.setText(
            if (isEditing) R.string.ledger_setup_update else R.string.ledger_setup_save
        )

        view.findViewById<View>(R.id.button_ledger_setup_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<View>(R.id.ledger_setup_icon_picker).setOnClickListener {
            hideSoftKeyboard()
            IconPickerDialog.show(requireContext(), initialSelectedIcon = selectedIconName) { icon ->
                selectedIconName = icon ?: "tabler_book"
                renderIcon()
            }
        }
        independentRow.setOnClickListener { selectIndependentGroup() }
        // The mode rows only change the mode; only the dedicated group row opens the
        // picker, so each tap produces exactly one action. The radios are
        // display-only to avoid a second toggle from the same tap.
        existingRow.setOnClickListener { selectExistingGroup() }
        groupRow.setOnClickListener { openGroupPicker() }
        saveButton.setOnClickListener { saveLedger() }

        independentRadio.isClickable = false
        existingRadio.isClickable = false

        applyInsets()
        restoreState(savedInstanceState, isEditing)
        renderIcon()
        renderGroupState()
        return view
    }

    /**
     * Keeps the save button clear of the real system/IME bottom inset (applied
     * once, so it is never double-counted) instead of a fixed guess.
     */
    private fun applyInsets() {
        val density = resources.displayMetrics.density
        ViewCompat.setOnApplyWindowInsetsListener(saveButton) { target, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val imeInset = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val extra = ((maxOf(bottomInset, imeInset) - (16 * density)).toInt()).coerceAtLeast(0)
            val params = target.layoutParams as? ViewGroup.MarginLayoutParams
            if (params != null && params.bottomMargin != (16 * density).toInt() + extra) {
                params.bottomMargin = (16 * density).toInt() + extra
                target.layoutParams = params
            }
            insets
        }
        ViewCompat.requestApplyInsets(saveButton)
    }

    private fun hideSoftKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as? InputMethodManager
        imm?.hideSoftInputFromWindow(nameInput.windowToken, 0)
        nameInput.clearFocus()
    }

    private fun restoreState(savedInstanceState: Bundle?, isEditing: Boolean) {
        if (savedInstanceState != null) {
            nameInput.setText(savedInstanceState.getString(STATE_NAME).orEmpty())
            selectedIconName = savedInstanceState.getString(STATE_ICON) ?: "tabler_book"
            useExistingGroup = savedInstanceState.getString(STATE_MODE) == MODE_EXISTING
            selectedGroupRootId = savedInstanceState.getLong(STATE_GROUP_ROOT, -1L).takeIf { it > 0L }
            if (selectedGroupRootId == null && useExistingGroup) {
                selectedGroupRootId = databaseHelper.getMasterLedgerId()
            }
            return
        }
        if (isEditing) {
            // Editing only ever changes the name and icon; the stored values are
            // prefilled and the asset relationship is not touched at all.
            val ledger = databaseHelper.getLedgers().firstOrNull { it.id == editingLedgerId }
            nameInput.setText(ledger?.name.orEmpty())
            selectedIconName = ledger?.iconName ?: "tabler_book"
            return
        }
        // New ledgers default to the clearly visible master asset group.
        useExistingGroup = true
        selectedGroupRootId = databaseHelper.getMasterLedgerId()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_NAME, nameInput.text.toString())
        outState.putString(STATE_ICON, selectedIconName)
        outState.putString(STATE_MODE, if (useExistingGroup) MODE_EXISTING else MODE_INDEPENDENT)
        outState.putLong(STATE_GROUP_ROOT, selectedGroupRootId ?: -1L)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        // The picker is a separate fragment-level surface; re-render on return so a
        // restored draft always matches the stored selection.
        if (::groupSummary.isInitialized) renderGroupState()
    }

    private fun selectIndependentGroup() {
        useExistingGroup = false
        renderGroupState()
    }

    /** Selects the existing-group mode without opening the picker. */
    private fun selectExistingGroup() {
        if (selectedGroupRootId == null) {
            selectedGroupRootId = databaseHelper.getMasterLedgerId()
        }
        useExistingGroup = true
        renderGroupState()
    }

    private fun renderIcon() {
        iconView.setImageResource(
            TablerIconCatalog.resourceId(selectedIconName).takeIf { it != 0 } ?: R.drawable.ic_book
        )
        iconLabel.text = getString(R.string.ledger_setup_icon_selected)
    }

    private fun renderGroupState() {
        if (editingLedgerId != null) return
        independentRadio.isChecked = !useExistingGroup
        existingRadio.isChecked = useExistingGroup
        assetList.visibility = if (useExistingGroup) View.VISIBLE else View.GONE

        val groups = databaseHelper.getAssetGroups()
        val selected = if (useExistingGroup) {
            groups.firstOrNull { it.rootLedgerId == selectedGroupRootId }
                ?: groups.firstOrNull { it.isMaster }
                ?: groups.firstOrNull()
        } else {
            null
        }
        selectedGroupRootId = selected?.rootLedgerId

        // The mode row only states the mode; the group row below it owns the
        // current selection, so the two never repeat the same summary.
        if (selected == null) {
            groupName.text = getString(R.string.ledger_asset_group_empty)
            groupSummary.text = ""
            groupRow.isEnabled = false
        } else {
            groupName.text = displayNameFor(selected)
            groupSummary.text = getString(
                R.string.ledger_asset_group_members_assets,
                selected.memberLedgerNames.joinToString("、"),
                selected.assetCount
            )
            groupRow.isEnabled = true
        }
        groupRow.contentDescription = getString(
            R.string.ledger_asset_group_row_accessibility,
            groupName.text,
            groupSummary.text
        )
    }

    private fun displayNameFor(group: DatabaseHelper.AssetGroupDescriptor): String {
        if (group.isMaster) return getString(R.string.ledger_asset_master_group)
        return getString(R.string.ledger_asset_group_label, group.rootLedgerName)
    }

    private fun openGroupPicker() {
        val groups = databaseHelper.getAssetGroups()
        if (groups.isEmpty()) {
            Toast.makeText(requireContext(), R.string.ledger_asset_group_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.bottom_sheet_asset_group_picker, null)
        val recycler = sheet.findViewById<RecyclerView>(R.id.recycler_asset_groups)
        val empty = sheet.findViewById<TextView>(R.id.text_asset_group_empty)
        empty.visibility = View.GONE
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = AssetGroupPickerAdapter(
            groups = groups,
            selectedRootId = selectedGroupRootId,
            primaryLabel = { group -> displayNameFor(group) },
            secondaryLabel = { group ->
                getString(
                    R.string.ledger_asset_group_members_assets,
                    group.memberLedgerNames.joinToString("、"),
                    group.assetCount
                )
            },
            onSelected = { group ->
                selectedGroupRootId = group.rootLedgerId
                useExistingGroup = true
                renderGroupState()
                dialog.dismiss()
            }
        )
        sheet.findViewById<View>(R.id.btn_close_asset_group_picker).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(sheet)
        dialog.show()
    }

    private fun saveLedger() {
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            nameInput.error = getString(R.string.ledger_setup_name_required)
            nameInput.requestFocus()
            return
        }
        val editingId = editingLedgerId
        if (editingId != null) {
            if (!databaseHelper.updateLedgerDetails(editingId, selectedIconName, name)) return
            Toast.makeText(requireContext(), R.string.ledger_setup_updated, Toast.LENGTH_SHORT).show()
        } else {
            val groupRootId = if (useExistingGroup) selectedGroupRootId else null
            val ledgerId = databaseHelper.createLedgerInAssetGroup(
                name = name,
                sharedSourceLedgerId = groupRootId,
                iconName = selectedIconName
            )
            if (ledgerId == null) {
                Toast.makeText(
                    requireContext(),
                    R.string.ledger_asset_group_created_failed,
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            LedgerSession.setCurrentId(requireContext(), ledgerId)
            Toast.makeText(requireContext(), R.string.ledger_setup_saved, Toast.LENGTH_SHORT).show()
        }
        parentFragmentManager.popBackStack()
    }
}
