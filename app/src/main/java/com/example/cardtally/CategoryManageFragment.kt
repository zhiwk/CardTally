package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.CategoryAdapter
import com.example.cardtally.adapter.IconPickerAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.util.CategoryHierarchySettingsHelper
import com.example.cardtally.util.MaterialSymbolCatalog
import com.google.android.material.tabs.TabLayout

class CategoryManageFragment : Fragment() {
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var btnAdd: Button
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: CategoryAdapter? = null
    private var currentType = 0

    private val availableIcons: List<String> = MaterialSymbolCatalog.icons

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
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        databaseHelper = DatabaseHelper(requireContext())
        recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
        val reorderCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                adapter?.moveParent(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                adapter?.parentIdsInOrder()?.let(databaseHelper::updateCategorySortOrders)
            }
        }
        ItemTouchHelper(reorderCallback).attachToRecyclerView(recyclerCategories)

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
                listener = object : CategoryAdapter.OnCategoryActionListener {
                    override fun onEdit(category: Category) {
                        showCategoryDialog(category)
                    }

                    override fun onDelete(category: Category) {
                        showDeleteDialog(category)
                    }

                    override fun onAddChild(parent: Category) {
                        showCategoryDialog(category = null, initialParentId = parent.id)
                    }

                    override fun onChildOrderChanged(children: List<Category>) {
                        databaseHelper.updateCategorySortOrders(children.map { it.id })
                    }
                }
            )
            recyclerCategories.adapter = adapter
        } else {
            adapter?.updateCategories(categories)
        }
    }

    private fun bindCategoryIcon(imageView: ImageView, icon: String?) {
        if (icon.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_category_other)
            return
        }

        val resourceId = MaterialSymbolCatalog.resourceId(icon)
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

    private fun showCategoryDialog(category: Category?, initialParentId: Long? = null) {
        val isEditing = category != null
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_category, null)
        val editName = dialogView.findViewById<EditText>(R.id.edit_category_name)
        val imageIcon = dialogView.findViewById<ImageView>(R.id.image_category_icon)
        var selectedIcon = category?.icon
        editName.setText(category?.name.orEmpty())
        bindCategoryIcon(imageIcon, selectedIcon)

        imageIcon.setOnClickListener {
            showIconPickerDialog(selectedIcon) { icon ->
                selectedIcon = icon
                bindCategoryIcon(imageIcon, selectedIcon)
            }
        }

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setTitle(if (isEditing) R.string.category_edit_title else R.string.category_add_title)
            .setView(dialogView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm, null)
        // A child category can be removed from the left side of the action row.
        // Parent categories keep the simpler edit flow because deleting them may
        // affect their children and is handled separately by the category list.
        if (isEditing && category?.parentId != null) {
            dialogBuilder.setNeutralButton(R.string.category_remove, null)
        }
        val dialog = dialogBuilder.create()

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
                    this.parentId = category?.parentId ?: initialParentId
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
            if (isEditing && category?.parentId != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                    showDeleteDialog(category)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showIconPickerDialog(
        selectedIcon: String?,
        onIconSelected: (String?) -> Unit
    ) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.category_icon_picker_title)

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_icon_picker, null)
        builder.setView(view)

        val recyclerIcons = view.findViewById<RecyclerView>(R.id.recycler_icons)
        val iconAdapter = IconPickerAdapter(availableIcons, selectedIcon) { icon ->
            onIconSelected(icon)
        }
        view.findViewById<EditText>(R.id.edit_icon_search).addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    iconAdapter.filter(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            }
        )
        recyclerIcons.adapter = iconAdapter

        val dialog = builder.create()
        iconAdapter.setOnIconSelected { icon ->
            onIconSelected(icon)
            dialog.dismiss()
        }
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
