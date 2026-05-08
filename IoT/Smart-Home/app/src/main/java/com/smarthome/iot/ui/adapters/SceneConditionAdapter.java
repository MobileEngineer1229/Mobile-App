package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.SceneCondition;

import java.util.ArrayList;
import java.util.List;

public class SceneConditionAdapter extends RecyclerView.Adapter<SceneConditionAdapter.ViewHolder> {
    private List<SceneCondition> conditions;
    private OnConditionRemoveListener listener;

    public interface OnConditionRemoveListener {
        void onConditionRemove(SceneCondition condition);
    }

    public SceneConditionAdapter(List<SceneCondition> conditions, OnConditionRemoveListener listener) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.listener = listener;
    }

    public void setConditions(List<SceneCondition> conditions) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scene_condition, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SceneCondition condition = conditions.get(position);
        
        holder.textViewCondition.setText(condition.getDisplayText());
        String description = condition.getDescriptionText();
        if (description != null && !description.isEmpty()) {
            holder.textViewLocation.setText(description);
            holder.textViewLocation.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.textViewLocation.setVisibility(android.view.View.GONE);
        }
        
        // Set icon based on condition type
        int iconRes = R.drawable.ic_thermometer;
        int tintColor = R.color.error;
        
        switch (condition.getType()) {
            case "tap_to_run":
                iconRes = R.drawable.ic_tap;
                tintColor = R.color.primary;
                break;
            case "location_arrive_at":
                iconRes = R.drawable.ic_location;
                tintColor = R.color.green;
                break;
            case "location_leave":
                iconRes = R.drawable.ic_location;
                tintColor = R.color.error;
                break;
            case "schedule_time":
                iconRes = R.drawable.ic_clock;
                tintColor = R.color.green;
                break;
            case "temperature":
                iconRes = R.drawable.ic_thermometer;
                tintColor = R.color.error;
                break;
            case "humidity":
                iconRes = R.drawable.ic_water_drop;
                tintColor = R.color.primary;
                break;
            case "weather":
                iconRes = R.drawable.ic_sun;
                tintColor = R.color.orange;
                break;
            case "sunrise_sunset":
                iconRes = R.drawable.ic_sun;
                tintColor = R.color.orange;
                break;
            case "wind_speed":
                iconRes = R.drawable.ic_wind;
                tintColor = R.color.cyan;
                break;
        }
        
        holder.imageViewIcon.setImageResource(iconRes);
        // Keep icon white on primary circle background to match Figma design
        holder.imageViewIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        
        holder.buttonRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConditionRemove(condition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conditions != null ? conditions.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewCondition;
        TextView textViewLocation;
        ImageButton buttonRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewCondition = itemView.findViewById(R.id.textViewCondition);
            textViewLocation = itemView.findViewById(R.id.textViewLocation);
            buttonRemove = itemView.findViewById(R.id.buttonRemove);
        }
    }
}
