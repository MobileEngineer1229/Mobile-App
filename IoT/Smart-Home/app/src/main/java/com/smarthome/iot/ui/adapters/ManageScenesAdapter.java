package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.SmartScene;

import java.util.Collections;
import java.util.List;

public class ManageScenesAdapter extends RecyclerView.Adapter<ManageScenesAdapter.ViewHolder> {
    private List<SmartScene> scenes;
    private OnSceneDeleteListener deleteListener;
    private OnDragStartListener dragStartListener;

    public interface OnSceneDeleteListener {
        void onSceneDelete(SmartScene scene);
    }

    public interface OnDragStartListener {
        void onDragStart(RecyclerView.ViewHolder viewHolder);
    }

    public ManageScenesAdapter(List<SmartScene> scenes, OnSceneDeleteListener deleteListener,
                               OnDragStartListener dragStartListener) {
        this.scenes = scenes != null ? scenes : new java.util.ArrayList<>();
        this.deleteListener = deleteListener;
        this.dragStartListener = dragStartListener;
    }

    public void setScenes(List<SmartScene> scenes) {
        this.scenes = scenes != null ? scenes : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    public List<SmartScene> getScenes() {
        return scenes;
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(scenes, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(scenes, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public void removeItem(int position) {
        scenes.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_scene, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmartScene scene = scenes.get(position);
        holder.textViewSceneName.setText(scene.getName() != null ? scene.getName() : "");

        holder.imageViewDragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && dragStartListener != null) {
                dragStartListener.onDragStart(holder);
            }
            return false;
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onSceneDelete(scene);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scenes != null ? scenes.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewDragHandle;
        TextView textViewSceneName;
        ImageButton buttonDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewDragHandle = itemView.findViewById(R.id.imageViewDragHandle);
            textViewSceneName = itemView.findViewById(R.id.textViewSceneName);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
