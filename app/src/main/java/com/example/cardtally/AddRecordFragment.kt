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
import java.util.Calendar

class AddRecordFragment : Fragment() {
    private lateinit var editDate: EditText
    private lateinit var editAmount: EditText
    private lateinit var editDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerAssetSource: Spinner
    private lateinit var radioGroupType: RadioGroup
    private lateinit var radioExpense: RadioButton
    private lateinit var radioIncome: RadioButton
    private lateinit var btnSave: Button
    private lateinit var databaseHelper: DatabaseHelper

    private var currentCategories = mutableListOf<Category>()
    private var currentAssets = mutableListOf<Asset>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_record, container, false)

        editDate = view.findViewById(R.id.edit_date)
        editAmount = view.findViewById(R.id.edit_amount)
        editDescription = view.findViewById(R.id.edit_description)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        spinnerAssetSource = view.findViewById(R.id.spinner_asset_source)
        radioGroupType = view.findViewById(R.id.radio_group_type)
        radioExpense = view.findViewById(R.id.radio_expense)
        radioIncome = view.findViewById(R.id.radio_income)
        btnSave = view.findViewById(R.id.btn_save)

        databaseHelper = DatabaseHelper(requireContext())

        editDate.setText(databaseHelper.getCurrentDate())

        loadCategories(true)
        loadAssets()

        editDate.setOnClickListener {
            showDatePicker()
        }

        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            val isExpense = checkedId == R.id.radio_expense
            loadCategories(isExpense)
        }

        btnSave.setOnClickListener {
            saveRecord()
        }

        return view
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

    private fun loadAssets() {
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

    private fun saveRecord() {
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

        val assetSource = if (assetSourceName == "无") null else assetSourceName

        val record = Record(
            date = date,
            amount = amount,
            category = category,
            type = type,
            description = description,
            assetSource = assetSource
        )
        val id = databaseHelper.addRecord(record)

        if (id != -1L) {
            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
            clearForm()
        } else {
            Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearForm() {
        editDate.setText(databaseHelper.getCurrentDate())
        editAmount.setText("")
        editDescription.setText("")
        radioExpense.isChecked = true
        spinnerCategory.setSelection(0)
        spinnerAssetSource.setSelection(0)
    }
}
