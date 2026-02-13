package com.example.cardtally;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardtally.adapter.StatisticsAdapter;
import com.example.cardtally.database.DatabaseHelper;
import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.Map;

public class StatisticsFragment extends Fragment {
    private TabLayout tabLayout;
    private TextView textExpenseTotal;
    private TextView textIncomeTotal;
    private TextView textEmpty;
    private RecyclerView recyclerStatistics;
    private DatabaseHelper databaseHelper;
    private StatisticsAdapter adapter;

    public StatisticsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        tabLayout = view.findViewById(R.id.tab_layout);
        textExpenseTotal = view.findViewById(R.id.text_expense_total);
        textIncomeTotal = view.findViewById(R.id.text_income_total);
        textEmpty = view.findViewById(R.id.text_empty);
        recyclerStatistics = view.findViewById(R.id.recycler_statistics);

        databaseHelper = new DatabaseHelper(getContext());

        recyclerStatistics.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StatisticsAdapter();
        recyclerStatistics.setAdapter(adapter);

        loadStatistics(0);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadStatistics(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics(tabLayout.getSelectedTabPosition());
    }

    private void loadStatistics(int tabPosition) {
        double expenseTotal = databaseHelper.getTotalByType(0);
        double incomeTotal = databaseHelper.getTotalByType(1);

        textExpenseTotal.setText(String.format("¥%.2f", expenseTotal));
        textIncomeTotal.setText(String.format("¥%.2f", incomeTotal));

        Map<String, Double> statistics;
        if (tabPosition == 0) {
            loadCategoryStatistics();
        } else {
            loadTimeStatistics();
        }
    }

    private void loadCategoryStatistics() {
        Map<String, Double> expenseStats = databaseHelper.getCategoryStatistics(0);
        Map<String, Double> incomeStats = databaseHelper.getCategoryStatistics(1);

        expenseStats.putAll(incomeStats);

        if (expenseStats.isEmpty()) {
            textEmpty.setVisibility(View.VISIBLE);
            recyclerStatistics.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.GONE);
            recyclerStatistics.setVisibility(View.VISIBLE);
            adapter.updateData(expenseStats);
        }
    }

    private void loadTimeStatistics() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        
        Map<String, Double> expenseStats = databaseHelper.getMonthlyStatistics(0, currentYear);
        Map<String, Double> incomeStats = databaseHelper.getMonthlyStatistics(1, currentYear);

        expenseStats.putAll(incomeStats);

        if (expenseStats.isEmpty()) {
            textEmpty.setVisibility(View.VISIBLE);
            recyclerStatistics.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.GONE);
            recyclerStatistics.setVisibility(View.VISIBLE);
            adapter.updateData(expenseStats);
        }
    }
}
