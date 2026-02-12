package com.example.cardtally;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.cardtally.database.DatabaseHelper;
import com.example.cardtally.model.Record;

import java.util.Calendar;

public class AddRecordFragment extends Fragment {
    private EditText editDate;
    private EditText editAmount;
    private Spinner spinnerCategory;
    private RadioGroup radioGroupType;
    private RadioButton radioExpense;
    private RadioButton radioIncome;
    private Button btnSave;
    private DatabaseHelper databaseHelper;

    private String[] expenseCategories = {"餐饮", "交通", "购物", "娱乐", "医疗", "教育", "住房", "其他"};
    private String[] incomeCategories = {"工资", "奖金", "投资", "兼职", "其他"};

    public AddRecordFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_record, container, false);

        editDate = view.findViewById(R.id.edit_date);
        editAmount = view.findViewById(R.id.edit_amount);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        radioGroupType = view.findViewById(R.id.radio_group_type);
        radioExpense = view.findViewById(R.id.radio_expense);
        radioIncome = view.findViewById(R.id.radio_income);
        btnSave = view.findViewById(R.id.btn_save);

        databaseHelper = new DatabaseHelper(getContext());

        editDate.setText(databaseHelper.getCurrentDate());

        setupCategorySpinner(true);

        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        radioGroupType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean isExpense = checkedId == R.id.radio_expense;
                setupCategorySpinner(isExpense);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRecord();
            }
        });

        return view;
    }

    private void setupCategorySpinner(boolean isExpense) {
        String[] categories = isExpense ? expenseCategories : incomeCategories;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        editDate.setText(date);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void saveRecord() {
        String date = editDate.getText().toString().trim();
        String amountStr = editAmount.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        int type = radioExpense.isChecked() ? 0 : 1;

        if (date.isEmpty()) {
            Toast.makeText(getContext(), "请选择日期", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(getContext(), "请输入金额", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "请输入有效的金额", Toast.LENGTH_SHORT).show();
            return;
        }

        Record record = new Record(date, amount, category, type);
        long id = databaseHelper.addRecord(record);

        if (id != -1) {
            Toast.makeText(getContext(), "保存成功", Toast.LENGTH_SHORT).show();
            clearForm();
        } else {
            Toast.makeText(getContext(), "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        editDate.setText(databaseHelper.getCurrentDate());
        editAmount.setText("");
        radioExpense.setChecked(true);
        spinnerCategory.setSelection(0);
    }
}
