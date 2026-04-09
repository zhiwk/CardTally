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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.CategorySelectorAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.example.cardtally.util.ThemeColorHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class AddRecordFragment : Fragment() {
    private lateinit var textDate: TextView
    private lateinit var editAmount: EditText
    private lateinit var editDescription: EditText
    private lateinit var spinnerAssetSource: Spinner
    private lateinit var btnExpense: Button
    private lateinit var btnIncome: Button
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var btnClose: View
    private lateinit var btnCancel: View
    private lateinit var btnSave: View
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
        btnExpense = view.findViewById(R.id.btn_expense)
        btnIncome = view.findViewById(R.id.btn_income)
        recyclerCategories = view.findViewById(R.id.recycler_categories)
        btnClose = view.findViewById(R.id.btn_close)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSave = view.findViewById(R.id.btn_save)

        databaseHelper = DatabaseHelper(requireContext())

        selectedDate = databaseHelper.getCurrentDate()
        textDate.text = selectedDate

        recyclerCategories.layoutManager = GridLayoutManager(requireContext(), 4)

        loadCategories(0)
        loadAssets()
        updateTypeStyle()

        textDate.setOnClickListener { showDatePicker() }

        btnExpense.setOnClickListener {
            if (currentType != 0) {
                currentType = 0
                updateTypeStyle()
                loadCategories(0)
            }
        }

        btnIncome.setOnClickListener {
            if (currentType != 1) {
                currentType = 1
                updateTypeStyle()
                loadCategories(1)
            }
        }

        btnClose.setOnClickListener { parentFragmentManager.popBackStack() }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
        btnSave.setOnClickListener { saveRecord(true) }

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
            btnExpense.setBackgroundResource(R.drawable.shape_button_primary)
            btnExpense.setTextColor(ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary))
            
            btnIncome.setBackgroundResource(android.R.color.transparent)
            btnIncome.setTextColor(ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant))
        } else {
            btnExpense.setBackgroundResource(android.R.color.transparent)
            btnExpense.setTextColor(ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant))
            
            btnIncome.setBackgroundResource(R.drawable.shape_button_primary)
            btnIncome.setTextColor(ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary))
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
            categoryAdapter?.setSelectedCategory(
                if (currentCategories.isNotEmpty()) currentCategories[0] else null
            )
        }

        selectedCategory = if (currentCategories.isNotEmpty()) currentCategories[0] else null
    }

    private fun loadAssets() {
        currentAssets = databaseHelper.getAllAssets().toMutableList()

        val assetNames = mutableListOf(getString(R.string.record_asset_none))
        currentAssets.forEach { assetNames.add(it.name) }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_small,
            assetNames
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerAssetSource.adapter = adapter
    }

    private fun showDatePicker() {
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
            Toast.makeText(requireContext(), getString(R.string.validation_select_date), Toast.LENGTH_SHORT).show()
            return
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.validation_enter_amount), Toast.LENGTH_SHORT).show()
            return
        }

        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), getString(R.string.validation_enter_valid_amount), Toast.LENGTH_SHORT).show()
            return
        }

        if (amount == 0.0) {
            Toast.makeText(requireContext(), getString(R.string.validation_zero_amount), Toast.LENGTH_SHORT).show()
            return
        }

        if (category == null) {
            Toast.makeText(requireContext(), getString(R.string.validation_select_category), Toast.LENGTH_SHORT).show()
            return
        }

        val assetSource = if (assetSourceName == getString(R.string.record_asset_none)) null else assetSourceName

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
            Toast.makeText(requireContext(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show()

            if (shouldReturn) {
                parentFragmentManager.popBackStack()
            } else {
                clearAmountAndDescription()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.toast_save_failed), Toast.LENGTH_SHORT).show()
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
