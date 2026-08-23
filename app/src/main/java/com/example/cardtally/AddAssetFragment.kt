package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.state.AssetFormState
import com.example.cardtally.state.AssetType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup

class AddAssetFragment : Fragment() {
    companion object {
        private const val KEY_ASSET_TYPE = "asset_type"

        fun newInstance(type: AssetType): AddAssetFragment {
            return AddAssetFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_ASSET_TYPE, type.token)
                }
            }
        }
    }

    private lateinit var editAmount: EditText
    private lateinit var editName: EditText
    private lateinit var chipGroupType: ChipGroup
    private lateinit var btnSave: Button
    private lateinit var databaseHelper: DatabaseHelper

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

        databaseHelper = DatabaseHelper(requireContext())
        val initialType = arguments?.getString(KEY_ASSET_TYPE)
            ?.let { token -> AssetType.values().firstOrNull { it.token == token } }
            ?: AssetType.CASH
        applyState(
            AssetFormState.readFrom(
                savedInstanceState,
                AssetFormState("", "", initialType)
            )
        )

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

        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), getString(R.string.validation_enter_valid_amount_edit), Toast.LENGTH_SHORT).show()
            return
        }

        val asset = Asset(name = name, amount = amount, type = type)
        val id = databaseHelper.addAsset(asset)
        if (id != -1L) {
            Toast.makeText(requireContext(), getString(R.string.asset_toast_add_success), Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        } else {
            Toast.makeText(requireContext(), getString(R.string.asset_toast_add_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun currentState(): AssetFormState {
        val type = when (chipGroupType.checkedChipId) {
            R.id.chip_bank -> AssetType.BANK
            R.id.chip_alipay -> AssetType.ALIPAY
            R.id.chip_wechat -> AssetType.WECHAT
            else -> AssetType.CASH
        }
        return AssetFormState(editName.text.toString(), editAmount.text.toString(), type)
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
    }

    private fun hideBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.GONE
    }

    private fun showBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.VISIBLE
    }
}
