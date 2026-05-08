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

public class TaskTypeAdapter extends RecyclerView.Adapter<TaskTypeAdapter.ViewHolder> {
    private List<TaskTypeItem> taskTypes;
    private OnTaskTypeClickListener listener;

    public static class TaskTypeItem {
        private String type;
        private String title;
        private int iconResId;
        private int iconColor;

        public TaskTypeItem(String type, String title, int iconResId, int iconColor) {
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

    public interface OnTaskTypeClickListener {
        void onTaskTypeClick(TaskTypeItem taskType);
    }

    public TaskTypeAdapter(List<TaskTypeItem> taskTypes, OnTaskTypeClickListener listener) {
        this.taskTypes = taskTypes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_type, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskTypeItem taskType = taskTypes.get(position);
        holder.textViewTitle.setText(taskType.getTitle());
        holder.imageViewIcon.setImageResource(taskType.getIconResId());
        holder.imageViewIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), taskType.getIconColor()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskTypeClick(taskType);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskTypes != null ? taskTypes.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewTitle;
        ImageView imageViewArrow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            imageViewArrow = itemView.findViewById(R.id.imageViewArrow);
        }
    }
}
