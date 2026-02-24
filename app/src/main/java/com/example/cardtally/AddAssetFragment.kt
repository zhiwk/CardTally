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

class AddAssetFragment : Fragment() {
    private lateinit var textTitle: TextView
    private lateinit var editAmount: EditText
    private lateinit var editName: EditText
    private lateinit var radioGroupType: RadioGroup
    private lateinit var cardExpense: CardView
    private lateinit var cardIncome: CardView
    private lateinit var btnSave: Button
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_asset, container, false)

        textTitle = view.findViewById(R.id.text_title)
        editAmount = view.findViewById(R.id.edit_amount)
        editName = view.findViewById(R.id.edit_name)
        radioGroupType = view.findViewById(R.id.radio_group_type)
        btnSave = view.findViewById(R.id.btn_save)

        databaseHelper = DatabaseHelper(requireContext())

        btnSave.setOnClickListener {
            saveAsset()
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

    private fun saveAsset() {
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

        val asset = Asset(name = name, amount = amount, type = type)
        val id = databaseHelper.addAsset(asset)
        if (id != -1L) {
            Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        } else {
            Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show()
        }
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
