package com.example.cardtally;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardtally.adapter.CategoryAdapter;
import com.example.cardtally.database.DatabaseHelper;
import com.example.cardtally.model.Category;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class CategoryManageFragment extends Fragment {
    private TabLayout tabLayout;
    private RecyclerView recyclerCategories;
    private TextView textEmpty;
    private Button btnAdd;
    private DatabaseHelper databaseHelper;
    private CategoryAdapter adapter;
    private int currentType = 0; // 0: 支出, 1: 收入

    public CategoryManageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_manage, container, false);

        tabLayout = view.findViewById(R.id.tab_layout);
        recyclerCategories = view.findViewById(R.id.recycler_categories);
        textEmpty = view.findViewById(R.id.text_empty);
        btnAdd = view.findViewById(R.id.btn_add);

        databaseHelper = new DatabaseHelper(getContext());

        recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        loadCategories();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = tab.getPosition();
                loadCategories();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });

        return view;
    }

    private void loadCategories() {
        List<Category> categories = databaseHelper.getCategoriesByType(currentType);

        if (categories.isEmpty()) {
            textEmpty.setVisibility(View.VISIBLE);
            recyclerCategories.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.GONE);
            recyclerCategories.setVisibility(View.VISIBLE);

            if (adapter == null) {
                adapter = new CategoryAdapter(categories, new CategoryAdapter.OnCategoryActionListener() {
                    @Override
                    public void onEdit(Category category) {
                        showEditDialog(category);
                    }

                    @Override
                    public void onDelete(Category category) {
                        showDeleteDialog(category);
                    }
                });
                recyclerCategories.setAdapter(adapter);
            } else {
                adapter.updateCategories(categories);
            }
        }
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("添加分类");

        final EditText input = new EditText(getContext());
        input.setHint("请输入分类名称");
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            Category category = new Category(name, currentType);
            long id = databaseHelper.addCategory(category);
            if (id != -1) {
                Toast.makeText(getContext(), "添加成功", Toast.LENGTH_SHORT).show();
                loadCategories();
            } else {
                Toast.makeText(getContext(), "添加失败", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showEditDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("编辑分类");

        final EditText input = new EditText(getContext());
        input.setText(category.getName());
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            category.setName(name);
            int rowsAffected = databaseHelper.updateCategory(category);
            if (rowsAffected > 0) {
                Toast.makeText(getContext(), "更新成功", Toast.LENGTH_SHORT).show();
                loadCategories();
            } else {
                Toast.makeText(getContext(), "更新失败", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("删除分类");
        builder.setMessage("确定要删除\"" + category.getName() + "\"吗？");

        builder.setPositiveButton("确定", (dialog, which) -> {
            databaseHelper.deleteCategory(category.getId());
            Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
            loadCategories();
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
