package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.SmartScene;

import java.util.ArrayList;
import java.util.List;

public class SelectSceneAdapter extends RecyclerView.Adapter<SelectSceneAdapter.ViewHolder> {
    private List<SmartScene> scenes;
    private OnSceneClickListener listener;
    private String selectedSceneId;

    public interface OnSceneClickListener {
        void onSceneClick(SmartScene scene);
    }

    public SelectSceneAdapter(OnSceneClickListener listener) {
        this.scenes = new ArrayList<>();
        this.listener = listener;
    }

    public void setScenes(List<SmartScene> scenes) {
        this.scenes = scenes != null ? scenes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedSceneId(String sceneId) {
        String previousSelected = selectedSceneId;
        selectedSceneId = sceneId;
        
        // Notify changed for both previous and new selection
        if (previousSelected != null) {
            for (int i = 0; i < scenes.size(); i++) {
                if (scenes.get(i).getId() != null && scenes.get(i).getId().toString().equals(previousSelected)) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
        if (selectedSceneId != null) {
            for (int i = 0; i < scenes.size(); i++) {
                if (scenes.get(i).getId() != null && scenes.get(i).getId().toString().equals(selectedSceneId)) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_scene, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= scenes.size()) {
            return;
        }
        
        SmartScene scene = scenes.get(position);
        if (scene == null) {
            return;
        }
        
        // Set scene name
        if (holder.textViewName != null) {
            holder.textViewName.setText(scene.getName() != null ? scene.getName() : "");
        }
        
        // Set status (Start/Stop)
        if (holder.textViewStatus != null) {
            holder.textViewStatus.setText(scene.isEnabled() ? "Start" : "Stop");
        }
        
        // Set selection state
        if (holder.radioButton != null) {
            boolean isSelected = scene.getId() != null && 
                scene.getId().toString().equals(selectedSceneId);
            holder.radioButton.setChecked(isSelected);
        }
        
        // Set card background color if available
        if (holder.itemView != null && scene.getColor() != null && !scene.getColor().isEmpty()) {
            try {
                int colorInt = android.graphics.Color.parseColor(scene.getColor());
                holder.itemView.setBackgroundColor(colorInt);
            } catch (IllegalArgumentException e) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.dark_4));
            }
        } else if (holder.itemView != null) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.dark_4));
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSceneClick(scene);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scenes != null ? scenes.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewStatus;
        RadioButton radioButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            radioButton = itemView.findViewById(R.id.radioButton);
        }
    }
}
