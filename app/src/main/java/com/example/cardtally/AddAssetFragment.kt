package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.appcompat.widget.SwitchCompat
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.state.AssetFormState
import com.example.cardtally.state.AssetType
import com.example.cardtally.util.AmountKeypadController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup

class AddAssetFragment : Fragment() {
    companion object {
        private const val KEY_ASSET_TYPE = "asset_type"
        private const val KEY_ASSET_TYPE_LABEL = "asset_type_label"
        private const val KEY_ASSET_TYPE_ICON_NAME = "asset_type_icon_name"
        private const val KEY_EDIT_ASSET_ID = "edit_asset_id"

        fun newInstance(type: AssetType): AddAssetFragment {
            return newInstance(type, "", "")
        }

        fun newInstance(type: AssetType, label: String, iconName: String): AddAssetFragment {
            return AddAssetFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_ASSET_TYPE, type.token)
                    putString(KEY_ASSET_TYPE_LABEL, label)
                    putString(KEY_ASSET_TYPE_ICON_NAME, iconName)
                }
            }
        }

        fun newEditInstance(assetId: Long): AddAssetFragment {
            return AddAssetFragment().apply {
                arguments = Bundle().apply { putLong(KEY_EDIT_ASSET_ID, assetId) }
            }
        }
    }

    private lateinit var editAmount: EditText
    private lateinit var editName: EditText
    private lateinit var chipGroupType: ChipGroup
    private lateinit var btnSave: Button
    private lateinit var switchIncludeTotal: SwitchCompat
    private lateinit var databaseHelper: DatabaseHelper
    private var editingAsset: Asset? = null
    private val isEditMode: Boolean
        get() = arguments?.getLong(KEY_EDIT_ASSET_ID, 0L) != 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_asset, container, false)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_back)
        editAmount = view.findViewById(R.id.edit_amount)
        editName = view.findViewById(R.id.edit_name)
        chipGroupType = view.findViewById(R.id.chip_group_type)
        btnSave = view.findViewById(R.id.btn_save)
        switchIncludeTotal = view.findViewById(R.id.switch_include_total)
        val textTitle = view.findViewById<TextView>(R.id.text_title)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete)
        AmountKeypadController(
            requireContext(),
            editAmount,
            view.findViewById(R.id.layout_amount_keypad),
            btnSave
        ) {
            // The keypad confirms the amount only. The page CTA owns saving.
            editAmount.clearFocus()
        }.bind()

        databaseHelper = DatabaseHelper(requireContext())
        val loadedAsset = arguments?.getLong(KEY_EDIT_ASSET_ID, 0L)
            ?.takeIf { it != 0L }
            ?.let { id -> databaseHelper.getAllAssets().firstOrNull { it.id == id } }
        editingAsset = loadedAsset
        if (isEditMode && loadedAsset == null) {
            view.post { parentFragmentManager.popBackStack() }
            return view
        }
        if (isEditMode) {
            textTitle.text = getString(R.string.asset_title_edit)
            btnSave.text = getString(R.string.asset_update_cta)
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener { archiveAsset() }
        }

        val initialType = loadedAsset?.type?.let(AssetType::fromDatabaseValue)
            ?: arguments?.getString(KEY_ASSET_TYPE)
            ?.let { token -> AssetType.values().firstOrNull { it.token == token } }
            ?: AssetType.CASH
        val initialLabel = loadedAsset?.categoryLabel
            ?: arguments?.getString(KEY_ASSET_TYPE_LABEL).orEmpty()
        val initialIconName = loadedAsset?.categoryIconName
            ?: arguments?.getString(KEY_ASSET_TYPE_ICON_NAME).orEmpty()
        val defaultState = AssetFormState(
            loadedAsset?.name.orEmpty(),
            loadedAsset?.amount?.toString().orEmpty(),
            initialType,
            initialLabel,
            initialIconName,
            loadedAsset?.includeInTotal ?: true
        )
        val restoredState = AssetFormState.readFrom(
                savedInstanceState,
                defaultState
            )
        applyState(restoredState)
        updateTypePresentation(view, restoredState)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSave.setOnClickListener {
            saveAsset()
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentState().writeTo(outState)
    }

    override fun onResume() {
        super.onResume()
        hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        showBottomNav()
    }

    private fun saveAsset() {
        val name = editName.text.toString().trim()
        val amountStr = editAmount.text.toString().trim()
        val type = when (chipGroupType.checkedChipId) {
            R.id.chip_cash -> 0
            R.id.chip_bank -> 1
            R.id.chip_alipay -> 2
            R.id.chip_wechat -> 3
            else -> 0
        }

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.validation_enter_asset_name), Toast.LENGTH_SHORT).show()
            return
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.validation_enter_amount), Toast.LENGTH_SHORT).show()
            return
        }

        val amount = evaluateAmountExpression(amountStr)
        if (amount == null) {
            Toast.makeText(requireContext(), getString(R.string.validation_enter_valid_amount_edit), Toast.LENGTH_SHORT).show()
            return
        }

        val formState = currentState()
        val asset = editingAsset?.apply {
            this.name = name
            this.amount = amount
            this.type = type
            this.includeInTotal = switchIncludeTotal.isChecked
            this.categoryLabel = formState.assetTypeLabel
            this.categoryIconName = formState.assetTypeIconName
        } ?: Asset(
            name = name,
            amount = amount,
            type = type,
            categoryLabel = formState.assetTypeLabel,
            categoryIconName = formState.assetTypeIconName,
            includeInTotal = switchIncludeTotal.isChecked
        )
        val saved = if (isEditMode) {
            databaseHelper.updateAsset(asset) > 0
        } else {
            databaseHelper.addAsset(asset) != -1L
        }
        if (saved) {
            Toast.makeText(
                requireContext(),
                getString(if (isEditMode) R.string.toast_update_success else R.string.asset_toast_add_success),
                Toast.LENGTH_SHORT
            ).show()
            if (isEditMode) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager.popBackStack("asset_type_select", FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        } else {
            Toast.makeText(requireContext(), getString(if (isEditMode) R.string.toast_update_failed else R.string.asset_toast_add_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun currentState(): AssetFormState {
        val type = when (chipGroupType.checkedChipId) {
            R.id.chip_bank -> AssetType.BANK
            R.id.chip_alipay -> AssetType.ALIPAY
            R.id.chip_wechat -> AssetType.WECHAT
            else -> AssetType.CASH
        }
        return AssetFormState(
            editName.text.toString(),
            editAmount.text.toString(),
            type,
            editingAsset?.categoryLabel ?: arguments?.getString(KEY_ASSET_TYPE_LABEL).orEmpty(),
            editingAsset?.categoryIconName ?: arguments?.getString(KEY_ASSET_TYPE_ICON_NAME).orEmpty(),
            switchIncludeTotal.isChecked
        )
    }

    private fun evaluateAmountExpression(expression: String): Double? {
        val normalized = expression.replace(" ", "")
        if (normalized.isEmpty() || !normalized.matches(Regex("""\d+(\.\d+)?([+-]\d+(\.\d+)?)*"""))) {
            return null
        }
        val tokens = normalized.split(Regex("(?=[+-])|(?<=[+-])"))
        var result = tokens.firstOrNull()?.toDoubleOrNull() ?: return null
        var index = 1
        while (index + 1 < tokens.size) {
            val operand = tokens[index + 1].toDoubleOrNull() ?: return null
            result = when (tokens[index]) {
                "+" -> result + operand
                "-" -> result - operand
                else -> return null
            }
            index += 2
        }
        return result
    }

    private fun applyState(state: AssetFormState) {
        editName.setText(state.assetName)
        editAmount.setText(state.assetAmountBuffer)
        chipGroupType.check(
            when (state.assetType) {
                AssetType.CASH -> R.id.chip_cash
                AssetType.BANK -> R.id.chip_bank
                AssetType.ALIPAY -> R.id.chip_alipay
                AssetType.WECHAT -> R.id.chip_wechat
            }
        )
        switchIncludeTotal.isChecked = state.includeInTotal
    }

    private fun updateTypePresentation(view: View, state: AssetFormState) {
        val (fallbackLabel, fallbackIcon) = when (state.assetType) {
            AssetType.CASH -> "现金" to R.drawable.ms_rounded_attach_money
            AssetType.BANK -> "银行卡" to R.drawable.ms_rounded_account_balance
            AssetType.ALIPAY -> "支付宝" to R.drawable.ms_rounded_payments
            AssetType.WECHAT -> "微信钱包" to R.drawable.ms_rounded_account_balance_wallet
        }
        val label = state.assetTypeLabel.ifBlank { fallbackLabel }
        val icon = state.assetTypeIconName.takeIf { it.isNotBlank() }?.let {
            resources.getIdentifier(it, "drawable", requireContext().packageName)
        }?.takeIf { it != 0 } ?: fallbackIcon
        view.findViewById<TextView>(R.id.text_selected_asset_type).text = label
        view.findViewById<ImageView>(R.id.image_asset_type).setImageResource(icon)
    }

    private fun hideBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.GONE
    }

    private fun showBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.VISIBLE
    }

    private fun archiveAsset() {
        editingAsset?.let {
            databaseHelper.archiveAsset(it.id)
            Toast.makeText(requireContext(), "已归档", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}
