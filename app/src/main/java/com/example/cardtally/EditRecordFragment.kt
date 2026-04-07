package com.example.cardtally

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.CategorySelectorAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.google.android.material.bottomnavigation.BottomNavigationView

class EditRecordFragment : Fragment() {
    private lateinit var textTitle: TextView
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
    private lateinit var textSaveLabel: TextView
    private lateinit var imageSaveIcon: ImageView
    private lateinit var databaseHelper: DatabaseHelper
    
    private var record: Record? = null
    private var recordId: Long = 0
    private var categoryAdapter: CategorySelectorAdapter? = null
    private var currentCategories = mutableListOf<Category>()
    private var currentAssets = mutableListOf<Asset>()
    private var currentType = 0
    private var selectedDate: String = ""
    private var selectedCategory: Category? = null

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
        val view = inflater.inflate(R.layout.fragment_add_record, container, false)

        textTitle = view.findViewById(R.id.text_title)
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
        textSaveLabel = view.findViewById(R.id.text_save_label)
        imageSaveIcon = view.findViewById(R.id.image_save_icon)

        databaseHelper = DatabaseHelper(requireContext())

        textTitle.text = getString(R.string.record_title_edit)
        textSaveLabel.text = getString(R.string.record_update)
        imageSaveIcon.setImageResource(R.drawable.ic_check)

        recyclerCategories.layoutManager = GridLayoutManager(requireContext(), 4)

        loadRecord()

        textDate.setOnClickListener { showDatePicker() }

        btnExpense.setOnClickListener {
            if (currentType != 0) {
                currentType = 0
                updateTypeStyle()
                loadCategories(0, null)
            }
        }

        btnIncome.setOnClickListener {
            if (currentType != 1) {
                currentType = 1
                updateTypeStyle()
                loadCategories(1, null)
            }
        }

        btnClose.setOnClickListener { parentFragmentManager.popBackStack() }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
        btnSave.setOnClickListener { updateRecord() }

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
            btnExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary_light))

            btnIncome.setBackgroundResource(android.R.color.transparent)
            btnIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariant_light))
        } else {
            btnExpense.setBackgroundResource(android.R.color.transparent)
            btnExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariant_light))

            btnIncome.setBackgroundResource(R.drawable.shape_button_primary)
            btnIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimary_light))
        }
    }

    private fun loadRecord() {
        record = databaseHelper.getRecordById(recordId)

        record?.let { r ->
            selectedDate = r.date
            textDate.text = r.date
            editAmount.setText(r.amount.toString())
            editDescription.setText(r.description)
            currentType = r.type
            updateTypeStyle()

            loadCategories(r.type, r.category)
            loadAssets(r.assetSource)
        }
    }

    private fun loadCategories(type: Int, selectedCategoryName: String?) {
        currentCategories = databaseHelper.getCategoriesByType(type).toMutableList()

        var selectedCat: Category? = null
        if (selectedCategoryName != null) {
            selectedCat = currentCategories.find { it.name == selectedCategoryName }
        }
        if (selectedCat == null && currentCategories.isNotEmpty()) {
            selectedCat = currentCategories[0]
        }

        if (categoryAdapter == null) {
            categoryAdapter = CategorySelectorAdapter(
                currentCategories,
                selectedCat
            ) { category ->
                selectedCategory = category
            }
            recyclerCategories.adapter = categoryAdapter
        } else {
            categoryAdapter?.updateCategories(currentCategories)
            categoryAdapter?.setSelectedCategory(selectedCat)
        }

        selectedCategory = selectedCat
    }

    private fun loadAssets(selectedAssetSource: String?) {
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

        if (selectedAssetSource != null) {
            val index = assetNames.indexOf(selectedAssetSource)
            if (index >= 0) {
                spinnerAssetSource.setSelection(index)
            }
        }
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

    private fun updateRecord() {
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
            Toast.makeText(requireContext(), getString(R.string.validation_enter_valid_amount_edit), Toast.LENGTH_SHORT).show()
            return
        }

        if (amount == 0.0) {
            Toast.makeText(requireContext(), getString(R.string.validation_zero_amount_edit), Toast.LENGTH_SHORT).show()
            return
        }

        if (category == null) {
            Toast.makeText(requireContext(), getString(R.string.validation_select_category), Toast.LENGTH_SHORT).show()
            return
        }

        val assetSource = if (assetSourceName == getString(R.string.record_asset_none)) null else assetSourceName

        record?.let { r ->
            r.date = date
            r.amount = amount
            r.category = category
            r.type = currentType
            r.description = description
            r.assetSource = assetSource

            val rowsAffected = databaseHelper.updateRecord(r)
            if (rowsAffected > 0) {
                Toast.makeText(requireContext(), getString(R.string.toast_update_success), Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), getString(R.string.toast_update_failed), Toast.LENGTH_SHORT).show()
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
