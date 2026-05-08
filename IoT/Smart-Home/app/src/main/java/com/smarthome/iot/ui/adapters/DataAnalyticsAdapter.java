package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.DataAnalyticsOption;

import java.util.List;

public class DataAnalyticsAdapter extends RecyclerView.Adapter<DataAnalyticsAdapter.ViewHolder> {
    private List<DataAnalyticsOption> options;
    private OnOptionClickListener listener;

    public interface OnOptionClickListener {
        void onOptionClick(DataAnalyticsOption option);
    }

    public DataAnalyticsAdapter(List<DataAnalyticsOption> options, OnOptionClickListener listener) {
        this.options = options;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_data_analytics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataAnalyticsOption option = options.get(position);
        holder.textViewTitle.setText(option.getTitle());
        holder.textViewSubtitle.setText(option.getSubtitle());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOptionClick(option);
            }
        });
    }

    @Override
    public int getItemCount() {
        return options != null ? options.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textViewTitle;
        TextView textViewSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewSubtitle = itemView.findViewById(R.id.textViewSubtitle);
        }
    }
}

