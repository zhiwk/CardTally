package com.example.cardtally.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardtally.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatisticsAdapter extends RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder> {
    private List<StatisticsItem> items;

    public StatisticsAdapter() {
        this.items = new ArrayList<>();
    }

    public void updateData(Map<String, Double> data) {
        items.clear();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            items.add(new StatisticsItem(entry.getKey(), entry.getValue()));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatisticsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_statistics, parent, false);
        return new StatisticsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatisticsViewHolder holder, int position) {
        StatisticsItem item = items.get(position);
        holder.textLabel.setText(item.label);
        holder.textAmount.setText(String.format("¥%.2f", item.amount));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class StatisticsViewHolder extends RecyclerView.ViewHolder {
        TextView textLabel;
        TextView textAmount;

        StatisticsViewHolder(View itemView) {
            super(itemView);
            textLabel = itemView.findViewById(R.id.text_label);
            textAmount = itemView.findViewById(R.id.text_amount);
        }
    }

    private static class StatisticsItem {
        String label;
        double amount;

        StatisticsItem(String label, double amount) {
            this.label = label;
            this.amount = amount;
        }
    }
}
