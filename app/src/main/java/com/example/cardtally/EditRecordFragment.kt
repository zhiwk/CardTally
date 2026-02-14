package com.example.cardtally

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class EditRecordFragment : Fragment() {
    private lateinit var editDate: EditText
    private lateinit var editAmount: EditText
    private lateinit var editDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerAssetSource: Spinner
    private lateinit var radioGroupType: RadioGroup
    private lateinit var radioExpense: RadioButton
    private lateinit var radioIncome: RadioButton
    private lateinit var btnUpdate: Button
    private lateinit var databaseHelper: DatabaseHelper
    private var record: Record? = null
    private var recordId: Long = 0

    private var currentCategories = mutableListOf<Category>()
    private var currentAssets = mutableListOf<Asset>()

    companion object {
        fun newInstance(recordId: Long): EditRecordFragment {
            val fragment = EditRecordFragment()
            val args = Bundle()
            args.putLong("record_id", recordId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recordId = it.getLong("record_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_record, container, false)

        editDate = view.findViewById(R.id.edit_date)
        editAmount = view.findViewById(R.id.edit_amount)
        editDescription = view.findViewById(R.id.edit_description)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        spinnerAssetSource = view.findViewById(R.id.spinner_asset_source)
        radioGroupType = view.findViewById(R.id.radio_group_type)
        radioExpense = view.findViewById(R.id.radio_expense)
        radioIncome = view.findViewById(R.id.radio_income)
        btnUpdate = view.findViewById(R.id.btn_update)

        databaseHelper = DatabaseHelper(requireContext())

        loadRecord()

        editDate.setOnClickListener {
            showDatePicker()
        }

        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            val isExpense = checkedId == R.id.radio_expense
            loadCategories(isExpense)
        }

        btnUpdate.setOnClickListener {
            updateRecord()
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

    private fun loadRecord() {
        val records = databaseHelper.getAllRecords()
        record = records.find { it.id == recordId }

        record?.let { r ->
            editDate.setText(r.date)
            editAmount.setText(r.amount.toString())
            editDescription.setText(r.description)

            if (r.type == 0) {
                radioExpense.isChecked = true
            } else {
                radioIncome.isChecked = true
            }

            loadCategories(r.type == 0)
            loadAssets(r.assetSource)

            var categoryIndex = 0
            for (i in currentCategories.indices) {
                if (currentCategories[i].name == r.category) {
                    categoryIndex = i
                    break
                }
            }
            spinnerCategory.setSelection(categoryIndex)
        }
    }

    private fun loadCategories(isExpense: Boolean) {
        val type = if (isExpense) 0 else 1
        currentCategories = databaseHelper.getCategoriesByType(type).toMutableList()

        val categoryNames = currentCategories.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun loadAssets(selectedAssetSource: String?) {
        currentAssets = databaseHelper.getAllAssets().toMutableList()

        val assetNames = mutableListOf("无")
        currentAssets.forEach { assetNames.add(it.name) }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            assetNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAssetSource.adapter = adapter

        if (selectedAssetSource != null) {
            val index = assetNames.indexOf(selectedAssetSource)
            if (index >= 0) {
                spinnerAssetSource.setSelection(index)
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format(
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                editDate.setText(date)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun updateRecord() {
        val date = editDate.text.toString().trim()
        val amountStr = editAmount.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val assetSourceName = spinnerAssetSource.selectedItem.toString()
        val description = editDescription.text.toString().trim()
        val type = if (radioExpense.isChecked) 0 else 1

        if (date.isEmpty()) {
            Toast.makeText(requireContext(), "请选择日期", Toast.LENGTH_SHORT).show()
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

        if (amount == 0.0) {
            Toast.makeText(requireContext(), "金额不能为0", Toast.LENGTH_SHORT).show()
            return
        }

        val assetSource = if (assetSourceName == "无") null else assetSourceName

        record?.let { r ->
            r.date = date
            r.amount = amount
            r.category = category
            r.type = type
            r.description = description
            r.assetSource = assetSource

            val rowsAffected = databaseHelper.updateRecord(r)
            if (rowsAffected > 0) {
                Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show()
            }
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
