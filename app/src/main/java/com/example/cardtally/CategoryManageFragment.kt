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
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.CategoryAdapter
import com.example.cardtally.adapter.IconPickerAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Category
import com.google.android.material.tabs.TabLayout

class CategoryManageFragment : Fragment() {
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var btnAdd: Button
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: CategoryAdapter? = null
    private var currentType = 0

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

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        btnAdd.setOnClickListener {
            showAddDialog()
        }

        return view
    }

    private fun loadCategories() {
        val categories = databaseHelper.getCategoriesByType(currentType)

        if (categories.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerCategories.visibility = View.GONE
        } else {
            textEmpty.visibility = View.GONE
            recyclerCategories.visibility = View.VISIBLE

            if (adapter == null) {
                adapter = CategoryAdapter(categories, object : CategoryAdapter.OnCategoryActionListener {
                    override fun onEdit(category: Category) {
                        showEditDialog(category)
                    }

                    override fun onDelete(category: Category) {
                        showDeleteDialog(category)
                    }
                })
                recyclerCategories.adapter = adapter
            } else {
                adapter?.updateCategories(categories)
            }
        }
    }

    private fun showAddDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("添加分类")

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        builder.setView(view)

        val editName = view.findViewById<EditText>(R.id.edit_category_name)
        val imageIcon = view.findViewById<ImageView>(R.id.image_category_icon)
        
        var selectedIcon: String? = null
        
        imageIcon.setOnClickListener {
            showIconPickerDialog { icon ->
                selectedIcon = icon
                if (icon != null) {
                    val resourceId = requireContext().resources.getIdentifier(
                        icon,
                        "drawable",
                        requireContext().packageName
                    )
                    if (resourceId != 0) {
                        imageIcon.setImageResource(resourceId)
                    }
                }
            }
        }

        builder.setPositiveButton("确定") { _, _ ->
            val name = editName.text.toString().trim()
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val category = Category(name = name, type = currentType, icon = selectedIcon)
            val id = databaseHelper.addCategory(category)
            if (id != -1L) {
                Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show()
                loadCategories()
            } else {
                Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("取消", null)

        builder.show()
    }

    private fun showEditDialog(category: Category) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("编辑分类")

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
        builder.setView(view)

        val editName = view.findViewById<EditText>(R.id.edit_category_name)
        val imageIcon = view.findViewById<ImageView>(R.id.image_category_icon)
        
        editName.setText(category.name)
        
        var selectedIcon = category.icon
        
        if (category.icon != null) {
            val resourceId = requireContext().resources.getIdentifier(
                category.icon,
                "drawable",
                requireContext().packageName
            )
            if (resourceId != 0) {
                imageIcon.setImageResource(resourceId)
            }
        }
        
        imageIcon.setOnClickListener {
            showIconPickerDialog { icon ->
                selectedIcon = icon
                if (icon != null) {
                    val resourceId = requireContext().resources.getIdentifier(
                        icon,
                        "drawable",
                        requireContext().packageName
                    )
                    if (resourceId != 0) {
                        imageIcon.setImageResource(resourceId)
                    }
                }
            }
        }

        builder.setPositiveButton("确定") { _, _ ->
            val name = editName.text.toString().trim()
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            category.name = name
            category.icon = selectedIcon
            val rowsAffected = databaseHelper.updateCategory(category)
            if (rowsAffected > 0) {
                Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                loadCategories()
            } else {
                Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("取消", null)

        builder.show()
    }

    private fun showIconPickerDialog(onIconSelected: (String?) -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("选择图标")

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
            .setTitle("删除分类")
            .setMessage("确定要删除\"${category.name}\"吗？")
            .setPositiveButton("确定") { _, _ ->
                databaseHelper.deleteCategory(category.id)
                Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                loadCategories()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
