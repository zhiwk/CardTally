package com.example.cardtally

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.CategorySelectorAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class AddRecordFragment : Fragment() {
    private lateinit var textDate: TextView
    private lateinit var editAmount: EditText
    private lateinit var editDescription: EditText
    private lateinit var spinnerAssetSource: Spinner
    private lateinit var cardExpense: CardView
    private lateinit var cardIncome: CardView
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var btnSaveAndContinue: Button
    private lateinit var btnSave: Button
    private lateinit var databaseHelper: DatabaseHelper

    private var categoryAdapter: CategorySelectorAdapter? = null
    private var currentCategories = mutableListOf<Category>()
    private var currentAssets = mutableListOf<Asset>()
    private var currentType = 0
    private var selectedDate: String = ""
    private var selectedCategory: Category? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_record, container, false)

        textDate = view.findViewById(R.id.text_date)
        editAmount = view.findViewById(R.id.edit_amount)
        editDescription = view.findViewById(R.id.edit_description)
        spinnerAssetSource = view.findViewById(R.id.spinner_asset_source)
        cardExpense = view.findViewById(R.id.card_expense)
        cardIncome = view.findViewById(R.id.card_income)
        recyclerCategories = view.findViewById(R.id.recycler_categories)
        btnSaveAndContinue = view.findViewById(R.id.btn_save_and_continue)
        btnSave = view.findViewById(R.id.btn_save)

        databaseHelper = DatabaseHelper(requireContext())

        selectedDate = databaseHelper.getCurrentDate()
        textDate.text = selectedDate

        recyclerCategories.layoutManager = GridLayoutManager(requireContext(), 4)

        loadCategories(0)
        loadAssets()

        textDate.setOnClickListener {
            showDatePicker()
        }

        cardExpense.setOnClickListener {
            if (currentType != 0) {
                currentType = 0
                updateTypeStyle()
                loadCategories(0)
            }
        }

        cardIncome.setOnClickListener {
            if (currentType != 1) {
                currentType = 1
                updateTypeStyle()
                loadCategories(1)
            }
        }

        btnSaveAndContinue.setOnClickListener {
            saveRecord(false)
        }

        btnSave.setOnClickListener {
            saveRecord(true)
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

    private fun updateTypeStyle() {
        if (currentType == 0) {
            cardExpense.setCardBackgroundColor(0xFFF44336.toInt())
            cardIncome.setCardBackgroundColor(0xFFE0E0E0.toInt())
        } else {
            cardExpense.setCardBackgroundColor(0xFFE0E0E0.toInt())
            cardIncome.setCardBackgroundColor(0xFF4CAF50.toInt())
        }
    }

    private fun loadCategories(type: Int) {
        currentCategories = databaseHelper.getCategoriesByType(type).toMutableList()

        if (categoryAdapter == null) {
            categoryAdapter = CategorySelectorAdapter(
                currentCategories,
                if (currentCategories.isNotEmpty()) currentCategories[0] else null
            ) { category ->
                selectedCategory = category
            }
            recyclerCategories.adapter = categoryAdapter
        } else {
            categoryAdapter?.updateCategories(currentCategories)
            categoryAdapter?.setSelectedCategory(if (currentCategories.isNotEmpty()) currentCategories[0] else null)
        }

        selectedCategory = if (currentCategories.isNotEmpty()) currentCategories[0] else null
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

        val parts = selectedDate.split("-")
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = String.format(
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                textDate.text = selectedDate
            },
            parts[0].toInt(),
            parts[1].toInt() - 1,
            parts[2].toInt()
        )
        datePickerDialog.show()
    }

    private fun saveRecord(shouldReturn: Boolean) {
        val date = selectedDate
        val amountStr = editAmount.text.toString().trim()
        val category = selectedCategory?.name
        val assetSourceName = spinnerAssetSource.selectedItem.toString()
        val description = editDescription.text.toString().trim()

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

        if (category == null) {
            Toast.makeText(requireContext(), "请选择分类", Toast.LENGTH_SHORT).show()
            return
        }

        val assetSource = if (assetSourceName == "无") null else assetSourceName

        val record = Record(
            date = date,
            amount = amount,
            category = category,
            type = currentType,
            description = description,
            assetSource = assetSource
        )
        val id = databaseHelper.addRecord(record)

        if (id != -1L) {
            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
            
            if (shouldReturn) {
                parentFragmentManager.popBackStack()
            } else {
                clearAmountAndDescription()
            }
        } else {
            Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAmountAndDescription() {
        editAmount.setText("")
        editDescription.setText("")
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
