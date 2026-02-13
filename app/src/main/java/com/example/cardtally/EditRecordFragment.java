package com.example.cardtally;

import android.app.AlertDialog;
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
import com.example.cardtally.model.Category;
import com.example.cardtally.model.Record;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EditRecordFragment extends Fragment {
    private EditText editDate;
    private EditText editAmount;
    private EditText editDescription;
    private Spinner spinnerCategory;
    private RadioGroup radioGroupType;
    private RadioButton radioExpense;
    private RadioButton radioIncome;
    private Button btnUpdate;
    private DatabaseHelper databaseHelper;
    private Record record;
    private long recordId;

    private List<Category> currentCategories = new ArrayList<>();

    public EditRecordFragment() {
    }

    public static EditRecordFragment newInstance(long recordId) {
        EditRecordFragment fragment = new EditRecordFragment();
        Bundle args = new Bundle();
        args.putLong("record_id", recordId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            recordId = getArguments().getLong("record_id");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_record, container, false);

        editDate = view.findViewById(R.id.edit_date);
        editAmount = view.findViewById(R.id.edit_amount);
        editDescription = view.findViewById(R.id.edit_description);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        radioGroupType = view.findViewById(R.id.radio_group_type);
        radioExpense = view.findViewById(R.id.radio_expense);
        radioIncome = view.findViewById(R.id.radio_income);
        btnUpdate = view.findViewById(R.id.btn_update);

        databaseHelper = new DatabaseHelper(getContext());

        loadRecord();

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
                loadCategories(isExpense);
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateRecord();
            }
        });

        return view;
    }

    private void loadRecord() {
        List<Record> records = databaseHelper.getAllRecords();
        for (Record r : records) {
            if (r.getId() == recordId) {
                record = r;
                break;
            }
        }

        if (record != null) {
            editDate.setText(record.getDate());
            editAmount.setText(String.valueOf(record.getAmount()));
            editDescription.setText(record.getDescription());

            if (record.getType() == 0) {
                radioExpense.setChecked(true);
            } else {
                radioIncome.setChecked(true);
            }

            loadCategories(record.getType() == 0);

            int categoryIndex = 0;
            for (int i = 0; i < currentCategories.size(); i++) {
                if (currentCategories.get(i).getName().equals(record.getCategory())) {
                    categoryIndex = i;
                    break;
                }
            }
            spinnerCategory.setSelection(categoryIndex);
        }
    }

    private void loadCategories(boolean isExpense) {
        int type = isExpense ? 0 : 1;
        currentCategories = databaseHelper.getCategoriesByType(type);

        List<String> categoryNames = new ArrayList<>();
        for (Category category : currentCategories) {
            categoryNames.add(category.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, categoryNames);
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

    private void updateRecord() {
        String date = editDate.getText().toString().trim();
        String amountStr = editAmount.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String description = editDescription.getText().toString().trim();
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

        record.setDate(date);
        record.setAmount(amount);
        record.setCategory(category);
        record.setType(type);
        record.setDescription(description);

        int rowsAffected = databaseHelper.updateRecord(record);
        if (rowsAffected > 0) {
            Toast.makeText(getContext(), "更新成功", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        } else {
            Toast.makeText(getContext(), "更新失败", Toast.LENGTH_SHORT).show();
        }
    }
}
