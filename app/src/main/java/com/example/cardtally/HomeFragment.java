package com.example.cardtally;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardtally.adapter.RecordAdapter;
import com.example.cardtally.database.DatabaseHelper;
import com.example.cardtally.model.Record;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerRecords;
    private TextView textEmpty;
    private FloatingActionButton fabAdd;
    private DatabaseHelper databaseHelper;
    private RecordAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        recyclerRecords = view.findViewById(R.id.recycler_records);
        textEmpty = view.findViewById(R.id.text_empty);
        fabAdd = view.findViewById(R.id.fab_add);
        
        databaseHelper = new DatabaseHelper(getContext());
        
        recyclerRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        
        loadRecords();
        
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AddRecordFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecords();
    }

    private void loadRecords() {
        List<Record> records = databaseHelper.getAllRecords();
        
        if (records.isEmpty()) {
            textEmpty.setVisibility(View.VISIBLE);
            recyclerRecords.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.GONE);
            recyclerRecords.setVisibility(View.VISIBLE);
            
            if (adapter == null) {
                adapter = new RecordAdapter(records, new RecordAdapter.OnRecordActionListener() {
                    @Override
                    public void onEdit(Record record) {
                        EditRecordFragment editFragment = EditRecordFragment.newInstance(record.getId());
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, editFragment)
                                .addToBackStack(null)
                                .commit();
                    }

                    @Override
                    public void onDelete(Record record) {
                        showDeleteDialog(record);
                    }
                });
                recyclerRecords.setAdapter(adapter);
            } else {
                adapter.updateRecords(records);
            }
        }
    }

    private void showDeleteDialog(Record record) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("删除记录");
        builder.setMessage("确定要删除这条记录吗？");

        builder.setPositiveButton("确定", (dialog, which) -> {
            databaseHelper.deleteRecord(record.getId());
            Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
            loadRecords();
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
