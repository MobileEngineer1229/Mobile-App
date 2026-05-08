package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.util.List;

public class WeatherConditionAdapter extends RecyclerView.Adapter<WeatherConditionAdapter.ViewHolder> {
    private List<ConditionItem> conditions;
    private OnConditionClickListener listener;

    public static class ConditionItem {
        private String type;
        private String title;
        private int iconResId;
        private int iconColor;

        public ConditionItem(String type, String title, int iconResId, int iconColor) {
            this.type = type;
            this.title = title;
            this.iconResId = iconResId;
            this.iconColor = iconColor;
        }

        public String getType() { return type; }
        public String getTitle() { return title; }
        public int getIconResId() { return iconResId; }
        public int getIconColor() { return iconColor; }
    }

    public interface OnConditionClickListener {
        void onConditionClick(ConditionItem condition);
    }

    public WeatherConditionAdapter(List<ConditionItem> conditions, OnConditionClickListener listener) {
        this.conditions = conditions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_condition, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConditionItem condition = conditions.get(position);
        holder.textViewTitle.setText(condition.getTitle());
        holder.imageViewIcon.setImageResource(condition.getIconResId());
        holder.imageViewIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), condition.getIconColor()));

        // Hide divider for last item
        View divider = holder.itemView.findViewById(R.id.divider);
        if (divider != null) {
            divider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConditionClick(condition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conditions != null ? conditions.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
        }
    }
}
