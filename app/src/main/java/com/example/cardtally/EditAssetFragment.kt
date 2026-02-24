package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.google.android.material.bottomnavigation.BottomNavigationView

class EditAssetFragment : Fragment() {
    private lateinit var textTitle: TextView
    private lateinit var editAmount: EditText
    private lateinit var editName: EditText
    private lateinit var radioGroupType: RadioGroup
    private lateinit var btnDelete: Button
    private lateinit var btnUpdate: Button
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
        arguments?.let {
            assetId = it.getLong("asset_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_asset, container, false)

        textTitle = view.findViewById(R.id.text_title)
        editAmount = view.findViewById(R.id.edit_amount)
        editName = view.findViewById(R.id.edit_name)
        radioGroupType = view.findViewById(R.id.radio_group_type)
        btnDelete = view.findViewById(R.id.btn_delete)
        btnUpdate = view.findViewById(R.id.btn_update)

        databaseHelper = DatabaseHelper(requireContext())

        loadAsset()

        btnUpdate.setOnClickListener {
            updateAsset()
        }

        btnDelete.setOnClickListener {
            deleteAsset()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        showBottomNav()
    }

    private fun loadAsset() {
        val assets = databaseHelper.getAllAssets()
        asset = assets.find { it.id == assetId }

        asset?.let { a ->
            editName.setText(a.name)
            editAmount.setText(a.amount.toString())

            when (a.type) {
                0 -> view?.findViewById<RadioButton>(R.id.radio_cash)?.isChecked = true
                1 -> view?.findViewById<RadioButton>(R.id.radio_bank)?.isChecked = true
                2 -> view?.findViewById<RadioButton>(R.id.radio_alipay)?.isChecked = true
                3 -> view?.findViewById<RadioButton>(R.id.radio_wechat)?.isChecked = true
                else -> view?.findViewById<RadioButton>(R.id.radio_other)?.isChecked = true
            }
        }
    }

    private fun updateAsset() {
        val name = editName.text.toString().trim()
        val amountStr = editAmount.text.toString().trim()
        val type = when (radioGroupType.checkedRadioButtonId) {
            R.id.radio_cash -> 0
            R.id.radio_bank -> 1
            R.id.radio_alipay -> 2
            R.id.radio_wechat -> 3
            else -> 4
        }

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "请输入资产名称", Toast.LENGTH_SHORT).show()
            return
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "请输入有效的金额", Toast.LENGTH_SHORT).show()
            return
        }

        asset?.let { a ->
            a.name = name
            a.amount = amount
            a.type = type

            val rowsAffected = databaseHelper.updateAsset(a)
            if (rowsAffected > 0) {
                Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAsset() {
        databaseHelper.deleteAsset(assetId)
        Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
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
