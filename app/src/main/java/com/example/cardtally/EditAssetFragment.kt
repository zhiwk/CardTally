package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.state.AssetFormState
import com.example.cardtally.state.AssetType
import com.example.cardtally.state.EditAssetState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup

class EditAssetFragment : Fragment() {
    private lateinit var btnBack: ImageButton
    private lateinit var textTitle: TextView
    private lateinit var editAmount: EditText
    private lateinit var editName: EditText
    private lateinit var chipGroupType: ChipGroup
    private lateinit var btnDelete: Button
    private lateinit var btnSave: Button
    private lateinit var databaseHelper: DatabaseHelper

    private var assetId: Long = 0
    private var asset: Asset? = null

    companion object {
        fun newInstance(assetId: Long): EditAssetFragment {
            val fragment = EditAssetFragment()
            val args = Bundle()
            args.putLong("asset_id", assetId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assetId = EditAssetState.readFrom(savedInstanceState, AssetFormState.DEFAULT)?.assetId
            ?: arguments?.getLong("asset_id")
            ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_asset, container, false)

        btnBack = view.findViewById(R.id.btn_back)
        textTitle = view.findViewById(R.id.text_title)
        editAmount = view.findViewById(R.id.edit_amount)
        editName = view.findViewById(R.id.edit_name)
        chipGroupType = view.findViewById(R.id.chip_group_type)
        btnDelete = view.findViewById(R.id.btn_delete)
        btnSave = view.findViewById(R.id.btn_save)

        databaseHelper = DatabaseHelper(requireContext())

        textTitle.text = getString(R.string.asset_title_edit)
        btnSave.text = getString(R.string.asset_update_cta)
        btnDelete.visibility = View.VISIBLE

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (!loadAsset()) {
            view.post { parentFragmentManager.popBackStack() }
            return view
        }
        EditAssetState.readFrom(savedInstanceState, currentState())?.form?.let(::applyState)

        btnSave.setOnClickListener {
            updateAsset()
        }

        btnDelete.setOnClickListener {
            archiveAsset()
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        EditAssetState(assetId, currentState()).writeTo(outState)
    }

    override fun onResume() {
        super.onResume()
        hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        showBottomNav()
    }

    private fun loadAsset(): Boolean {
        asset = databaseHelper.getAllAssets().find { it.id == assetId }

        asset?.let { a ->
            editName.setText(a.name)
            editAmount.setText(a.amount.toString())
            chipGroupType.check(getChipIdForType(a.type))
        }
        return asset != null
    }

    private fun updateAsset() {
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

        asset?.let { a ->
            a.name = name
            a.amount = amount
            a.type = type

            val rowsAffected = databaseHelper.updateAsset(a)
            if (rowsAffected > 0) {
                Toast.makeText(requireContext(), getString(R.string.toast_update_success), Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), getString(R.string.toast_update_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun archiveAsset() {
        databaseHelper.archiveAsset(assetId)
        Toast.makeText(requireContext(), "已归档", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun getChipIdForType(type: Int): Int {
        return when (type) {
            1 -> R.id.chip_bank
            2 -> R.id.chip_alipay
            3 -> R.id.chip_wechat
            else -> R.id.chip_cash
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
        chipGroupType.check(getChipIdForType(state.assetType.databaseValue))
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
