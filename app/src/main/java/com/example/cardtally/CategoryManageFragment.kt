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
import com.example.cardtally.adapter.IconPickerDialog
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.example.cardtally.util.CategoryHierarchySettingsHelper
import com.example.cardtally.util.TablerIconCatalog
import com.example.cardtally.util.ThemeColorHelper
import com.google.android.material.tabs.TabLayout

class CategoryManageFragment : Fragment() {
    companion object {
        const val CATEGORY_SAVED_RESULT = "category_saved"
    }

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var btnAdd: Button
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: CategoryAdapter? = null
    private var currentType = 0


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
        parentFragmentManager.setFragmentResultListener(
            CATEGORY_SAVED_RESULT,
            viewLifecycleOwner
        ) { _, _ ->
            loadCategories()
        }
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

        for (position in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(position) ?: continue
            tab.setCustomView(R.layout.item_category_type_tab)
            (tab.customView as? TextView)?.text = tab.text
        }
        tabLayout.getTabAt(currentType)?.select()
        for (position in 0 until tabLayout.tabCount) {
            styleTypeTab(tabLayout.getTabAt(position), position == currentType)
        }
        loadCategories()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentType = tab?.position ?: 0
                styleTypeTab(tab, true)
                loadCategories()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                styleTypeTab(tab, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })

        btnAdd.setOnClickListener {
            openCategoryEditor(parentId = null)
        }

        return view
    }

    private fun styleTypeTab(tab: TabLayout.Tab?, selected: Boolean) {
        val label = tab?.customView as? TextView ?: return
        label.setTextColor(
            if (selected) ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorOnSurface)
            else ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.editorial_text_muted)
        )
        label.paint.isFakeBoldText = selected
        label.invalidate()
    }

    override fun onResume() {
        super.onResume()
        // AddCategoryFragment saves before popping the back stack. Reload here
        // so returning to this page always reflects the persisted categories,
        // including the transition from an empty state.
        if (::databaseHelper.isInitialized && ::recyclerCategories.isInitialized) {
            loadCategories()
        }
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
                        openCategoryEditor(parentId = parent.id)
                    }

                    override fun onChildOrderChanged(children: List<Category>) {
                        databaseHelper.updateCategorySortOrders(children.map { it.id })
                    }
                }
            )
            recyclerCategories.adapter = adapter
        } else {
            adapter?.updateCategories(categories)
            // The Fragment view can be recreated when returning from
            // AddCategoryFragment. The adapter instance survives, but the new
            // RecyclerView does not have an adapter attached yet.
            if (recyclerCategories.adapter !== adapter) {
                recyclerCategories.adapter = adapter
            }
        }
    }

    private fun bindCategoryIcon(imageView: ImageView, icon: String?, categoryName: String? = null) {
        if (icon.isNullOrEmpty()) {
            imageView.setImageResource(
                when (categoryName) {
                    "购物" -> R.drawable.tabler_shopping_cart
                    "餐饮" -> R.drawable.tabler_tools_kitchen
                    "居住", "住房" -> R.drawable.tabler_home
                    "交通" -> R.drawable.tabler_car
                    else -> R.drawable.tabler_category
                }
            )
            return
        }

        val resourceId = TablerIconCatalog.resourceId(requireContext(), icon)
        val hasGenericIcon = icon == "tabler_category" || icon == "ic_category_other"
        if (resourceId != 0 && !hasGenericIcon) {
            imageView.setImageResource(resourceId)
        } else {
            imageView.setImageResource(
                when (categoryName) {
                    "购物" -> R.drawable.tabler_shopping_cart
                    "餐饮" -> R.drawable.tabler_tools_kitchen
                    "居住", "住房" -> R.drawable.tabler_home
                    "交通" -> R.drawable.tabler_car
                    else -> R.drawable.tabler_category
                }
            )
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
        bindCategoryIcon(imageIcon, selectedIcon, category?.name)

        imageIcon.setOnClickListener {
            showIconPickerDialog(selectedIcon) { icon ->
                selectedIcon = icon
                bindCategoryIcon(imageIcon, selectedIcon, category?.name)
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
        IconPickerDialog.show(requireContext(), selectedIcon, onIconSelected)
    }

    private fun openCategoryEditor(parentId: Long?) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AddCategoryFragment.newInstance(currentType, parentId))
            .addToBackStack(null)
            .commit()
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
