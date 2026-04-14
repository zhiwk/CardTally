package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.CategoryAdapter
import com.example.cardtally.adapter.IconPickerAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.util.CategoryHierarchySettingsHelper
import com.google.android.material.tabs.TabLayout

class CategoryManageFragment : Fragment() {
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var btnAdd: Button
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: CategoryAdapter? = null
    private var currentType = 0

    private data class ParentOption(
        val category: Category?,
        val label: String
    ) {
        override fun toString(): String = label
    }

    private val availableIcons = listOf(
        "ic_category_food",
        "ic_category_transport",
        "ic_category_shopping",
        "ic_category_entertainment",
        "ic_category_medical",
        "ic_category_education",
        "ic_category_housing",
        "ic_category_communication",
        "ic_category_salary",
        "ic_category_bonus",
        "ic_category_other",
        "ic_category_clothing",
        "ic_category_beauty",
        "ic_category_sports",
        "ic_category_travel",
        "ic_category_pet",
        "ic_category_gift"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category_manage, container, false)

        tabLayout = view.findViewById(R.id.tab_layout)
        recyclerCategories = view.findViewById(R.id.recycler_categories)
        textEmpty = view.findViewById(R.id.text_empty)
        btnAdd = view.findViewById(R.id.btn_add)

        databaseHelper = DatabaseHelper(requireContext())
        recyclerCategories.layoutManager = LinearLayoutManager(requireContext())

        loadCategories()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentType = tab?.position ?: 0
                loadCategories()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })

        btnAdd.setOnClickListener {
            showCategoryDialog(category = null)
        }

        return view
    }

    override fun onDestroyView() {
        if (::databaseHelper.isInitialized) {
            databaseHelper.close()
        }
        super.onDestroyView()
    }

    private fun loadCategories() {
        val categories = databaseHelper.getCategoryTreeByType(currentType)
        val categoryDepths = calculateCategoryDepths(categories)

        if (categories.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerCategories.visibility = View.GONE
            return
        }

        textEmpty.visibility = View.GONE
        recyclerCategories.visibility = View.VISIBLE

        if (adapter == null) {
            adapter = CategoryAdapter(
                categories = categories,
                categoryDepths = categoryDepths,
                listener = object : CategoryAdapter.OnCategoryActionListener {
                    override fun onEdit(category: Category) {
                        showCategoryDialog(category)
                    }

                    override fun onDelete(category: Category) {
                        showDeleteDialog(category)
                    }
                }
            )
            recyclerCategories.adapter = adapter
        } else {
            adapter?.updateCategories(categories, categoryDepths)
        }
    }

    private fun calculateCategoryDepths(categories: List<Category>): Map<Long, Int> {
        val categoriesById = categories.associateBy { it.id }
        val depthCache = mutableMapOf<Long, Int>()

        fun resolveDepth(category: Category, visiting: MutableSet<Long> = mutableSetOf()): Int {
            depthCache[category.id]?.let { return it }
            if (!visiting.add(category.id)) {
                return 0
            }

            val depth = category.parentId
                ?.let { parentId -> categoriesById[parentId] }
                ?.let { parent -> resolveDepth(parent, visiting) + 1 }
                ?: 0

            visiting.remove(category.id)
            depthCache[category.id] = depth
            return depth
        }

        return categories.associate { category -> category.id to resolveDepth(category) }
    }

    private fun buildParentOptions(category: Category?): List<ParentOption> {
        val categories = databaseHelper.getCategoryTreeByType(currentType)
        val categoryDepths = calculateCategoryDepths(categories)
        val excludedIds = category?.let { currentCategory ->
            buildSet {
                add(currentCategory.id)
                collectDescendantIds(currentCategory.id, categories, this)
            }
        } ?: emptySet()

        val options = mutableListOf(ParentOption(null, getString(R.string.category_parent_none)))
        categories.forEach { candidate ->
            if (candidate.id !in excludedIds) {
                val depth = categoryDepths[candidate.id] ?: 0
                options.add(ParentOption(candidate, "    ".repeat(depth) + candidate.name))
            }
        }
        return options
    }

    private fun collectDescendantIds(
        categoryId: Long,
        categories: List<Category>,
        descendants: MutableSet<Long>
    ) {
        categories
            .filter { it.parentId == categoryId }
            .forEach { child ->
                if (descendants.add(child.id)) {
                    collectDescendantIds(child.id, categories, descendants)
                }
            }
    }

    private fun bindCategoryIcon(imageView: ImageView, icon: String?) {
        if (icon.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_category_other)
            return
        }

        val resourceId = requireContext().resources.getIdentifier(
            icon,
            "drawable",
            requireContext().packageName
        )
        if (resourceId != 0) {
            imageView.setImageResource(resourceId)
        } else {
            imageView.setImageResource(R.drawable.ic_category_other)
        }
    }

    private fun categoryOperationMessage(error: DatabaseHelper.CategoryOperationError): String {
        return when (error) {
            DatabaseHelper.CategoryOperationError.PARENT_NOT_FOUND,
            DatabaseHelper.CategoryOperationError.PARENT_TYPE_MISMATCH -> {
                getString(R.string.category_error_invalid_parent)
            }
            DatabaseHelper.CategoryOperationError.SELF_PARENT -> {
                getString(R.string.category_error_self_parent)
            }
            DatabaseHelper.CategoryOperationError.DESCENDANT_CYCLE -> {
                getString(R.string.category_error_cycle)
            }
            DatabaseHelper.CategoryOperationError.MAX_DEPTH_EXCEEDED -> {
                getString(
                    R.string.category_error_depth_exceeded,
                    CategoryHierarchySettingsHelper.getCategoryMaxDepth(requireContext())
                )
            }
            DatabaseHelper.CategoryOperationError.HAS_CHILDREN -> {
                getString(R.string.category_error_has_children)
            }
            DatabaseHelper.CategoryOperationError.IN_USE_BY_RECORDS -> {
                getString(R.string.category_error_in_use)
            }
        }
    }

    private fun showCategoryDialog(category: Category?) {
        val isEditing = category != null
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_category, null)
        val editName = dialogView.findViewById<EditText>(R.id.edit_category_name)
        val imageIcon = dialogView.findViewById<ImageView>(R.id.image_category_icon)
        val spinnerParent = dialogView.findViewById<Spinner>(R.id.spinner_parent_category)
        val parentOptions = buildParentOptions(category)
        val parentAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_small, parentOptions)

        parentAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerParent.adapter = parentAdapter

        var selectedIcon = category?.icon
        editName.setText(category?.name.orEmpty())
        bindCategoryIcon(imageIcon, selectedIcon)

        val selectedParentIndex = category?.parentId?.let { parentId ->
            parentOptions.indexOfFirst { it.category?.id == parentId }
        } ?: 0
        spinnerParent.setSelection(selectedParentIndex.coerceAtLeast(0))

        imageIcon.setOnClickListener {
            showIconPickerDialog { icon ->
                selectedIcon = icon
                bindCategoryIcon(imageIcon, selectedIcon)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (isEditing) R.string.category_edit_title else R.string.category_add_title)
            .setView(dialogView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = editName.text.toString().trim()
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(requireContext(), R.string.category_error_empty_name, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val targetCategory = (category?.copy() ?: Category(type = currentType)).apply {
                    this.name = name
                    this.type = currentType
                    this.icon = selectedIcon
                    this.parentId = parentOptions
                        .getOrNull(spinnerParent.selectedItemPosition)
                        ?.category
                        ?.id
                }

                try {
                    if (isEditing) {
                        val rowsAffected = databaseHelper.updateCategory(targetCategory)
                        if (rowsAffected <= 0) {
                            Toast.makeText(requireContext(), R.string.toast_update_failed, Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        Toast.makeText(requireContext(), R.string.toast_update_success, Toast.LENGTH_SHORT).show()
                    } else {
                        val id = databaseHelper.addCategory(targetCategory)
                        if (id == -1L) {
                            Toast.makeText(requireContext(), R.string.category_add_failed, Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        Toast.makeText(requireContext(), R.string.category_add_success, Toast.LENGTH_SHORT).show()
                    }
                } catch (exception: DatabaseHelper.CategoryOperationException) {
                    Toast.makeText(
                        requireContext(),
                        categoryOperationMessage(exception.error),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                loadCategories()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showIconPickerDialog(onIconSelected: (String?) -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.category_icon_picker_title)

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_icon_picker, null)
        builder.setView(view)

        val recyclerIcons = view.findViewById<RecyclerView>(R.id.recycler_icons)
        val iconAdapter = IconPickerAdapter(availableIcons, null) { icon ->
            onIconSelected(icon)
        }
        recyclerIcons.adapter = iconAdapter

        val dialog = builder.create()
        dialog.show()
    }

    private fun showDeleteDialog(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.category_delete_title)
            .setMessage(getString(R.string.category_delete_message, category.name))
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                try {
                    databaseHelper.deleteCategory(category.id)
                    Toast.makeText(requireContext(), R.string.toast_delete_success, Toast.LENGTH_SHORT).show()
                    loadCategories()
                } catch (exception: DatabaseHelper.CategoryOperationException) {
                    Toast.makeText(
                        requireContext(),
                        categoryOperationMessage(exception.error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
