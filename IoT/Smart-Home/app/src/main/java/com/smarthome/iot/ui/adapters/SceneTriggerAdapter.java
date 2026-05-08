package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.util.List;

public class SceneTriggerAdapter extends RecyclerView.Adapter<SceneTriggerAdapter.ViewHolder> {
    private List<TriggerItem> triggers;
    private OnTriggerClickListener listener;

    public static class TriggerItem {
        private String type;
        private String title;
        private String example;
        private int iconResId;

        public TriggerItem(String type, String title, String example, int iconResId) {
            this.type = type;
            this.title = title;
            this.example = example;
            this.iconResId = iconResId;
        }

        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getExample() { return example; }
        public int getIconResId() { return iconResId; }
    }

    public interface OnTriggerClickListener {
        void onTriggerClick(TriggerItem trigger);
    }

    public SceneTriggerAdapter(List<TriggerItem> triggers, OnTriggerClickListener listener) {
        this.triggers = triggers != null ? triggers : new java.util.ArrayList<>();
        this.listener = listener;
    }
    
    public void setTriggers(List<TriggerItem> triggers) {
        this.triggers = triggers != null ? triggers : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scene_trigger, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TriggerItem trigger = triggers.get(position);
        holder.textViewTitle.setText(trigger.getTitle());
        holder.textViewExample.setText(trigger.getExample());
        holder.imageViewIcon.setImageResource(trigger.getIconResId());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTriggerClick(trigger);
            }
        });
    }

    @Override
    public int getItemCount() {
        return triggers != null ? triggers.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewTitle;
        TextView textViewExample;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewExample = itemView.findViewById(R.id.textViewExample);
        }
    }
}
