package com.example.cardtally;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardtally.adapter.RecordAdapter;
import com.example.cardtally.database.DatabaseHelper;
import com.example.cardtally.model.Record;

import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerRecords;
    private TextView textEmpty;
    private DatabaseHelper databaseHelper;
    private RecordAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        recyclerRecords = view.findViewById(R.id.recycler_records);
        textEmpty = view.findViewById(R.id.text_empty);
        
        databaseHelper = new DatabaseHelper(getContext());
        
        recyclerRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        
        loadRecords();
        
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
                adapter = new RecordAdapter(records);
                recyclerRecords.setAdapter(adapter);
            } else {
                adapter.updateRecords(records);
            }
        }
    }
}
