package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.CategoryAdapter
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
    private var currentType = 0 // 0: 支出, 1: 收入

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

        val input = EditText(requireContext())
        input.hint = "请输入分类名称"
        builder.setView(input)

        builder.setPositiveButton("确定") { _, _ ->
            val name = input.text.toString().trim()
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val category = Category(name = name, type = currentType)
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

        val input = EditText(requireContext())
        input.setText(category.name)
        builder.setView(input)

        builder.setPositiveButton("确定") { _, _ ->
            val name = input.text.toString().trim()
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            category.name = name
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
