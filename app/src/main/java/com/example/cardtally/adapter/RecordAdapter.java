package com.example.cardtally.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardtally.R;
import com.example.cardtally.model.Record;

import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {
    private List<Record> records;
    private OnRecordActionListener listener;

    public interface OnRecordActionListener {
        void onEdit(Record record);
        void onDelete(Record record);
    }

    public RecordAdapter(List<Record> records, OnRecordActionListener listener) {
        this.records = records;
        this.listener = listener;
    }

    public RecordAdapter(List<Record> records) {
        this.records = records;
        this.listener = null;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        Record record = records.get(position);
        holder.textDate.setText(record.getDate());
        holder.textCategory.setText(record.getCategory());
        
        String amountText;
        if (record.getType() == 0) {
            amountText = String.format("-%.2f", record.getAmount());
            holder.textAmount.setTextColor(0xFFF44336);
        } else {
            amountText = String.format("+%.2f", record.getAmount());
            holder.textAmount.setTextColor(0xFF4CAF50);
        }
        holder.textAmount.setText(amountText);
        
        if (record.getDescription() != null && !record.getDescription().isEmpty()) {
            holder.textDescription.setText(record.getDescription());
            holder.textDescription.setVisibility(View.VISIBLE);
        } else {
            holder.textDescription.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onEdit(record);
                }
            }
        });

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onDelete(record);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void updateRecords(List<Record> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView textDate;
        TextView textCategory;
        TextView textDescription;
        TextView textAmount;
        Button btnEdit;
        Button btnDelete;

        RecordViewHolder(View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_date);
            textCategory = itemView.findViewById(R.id.text_category);
            textDescription = itemView.findViewById(R.id.text_description);
            textAmount = itemView.findViewById(R.id.text_amount);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
