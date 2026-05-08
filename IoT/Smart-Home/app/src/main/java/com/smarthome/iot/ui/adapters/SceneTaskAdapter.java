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
import com.smarthome.iot.models.SceneTask;

import java.util.ArrayList;
import java.util.List;

public class SceneTaskAdapter extends RecyclerView.Adapter<SceneTaskAdapter.ViewHolder> {
    private List<SceneTask> tasks;
    private OnTaskRemoveListener listener;

    public interface OnTaskRemoveListener {
        void onTaskRemove(SceneTask task);
    }

    public SceneTaskAdapter(List<SceneTask> tasks, OnTaskRemoveListener listener) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.listener = listener;
    }

    public void setTasks(List<SceneTask> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scene_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SceneTask task = tasks.get(position);
        
        // Set task name based on type
        String taskName = "";
        String taskDetails = "";
        
        switch (task.getType()) {
            case "control_device":
                taskName = task.getDeviceName() != null ? task.getDeviceName() : "Control Device";
                taskDetails = task.getRoomName() != null ? task.getRoomName() + " - " + task.getFunction() : task.getFunction();
                break;
            case "select_scene":
                taskName = task.getSceneName() != null ? task.getSceneName() : "Select Smart Scene";
                taskDetails = "Automation: Enable";
                break;
            case "delay":
                taskName = "Delay the Action";
                if (task.getDelaySeconds() != null) {
                    int hours = task.getDelaySeconds() / 3600;
                    int minutes = (task.getDelaySeconds() % 3600) / 60;
                    int seconds = task.getDelaySeconds() % 60;
                    if (hours > 0) {
                        taskDetails = hours + "h " + minutes + "m " + seconds + "s";
                    } else if (minutes > 0) {
                        taskDetails = minutes + " min " + seconds + " secs";
                    } else {
                        taskDetails = seconds + " secs";
                    }
                }
                break;
            case "change_arm_mode":
                taskName = "Change Arm Mode";
                taskDetails = task.getArmMode() != null ? task.getArmMode() : "";
                break;
            case "send_notification":
                taskName = "Send Notification";
                taskDetails = task.getNotificationMessage() != null ? task.getNotificationMessage() : "";
                break;
            default:
                taskName = task.getType();
                taskDetails = task.getDisplayText();
                break;
        }
        
        holder.textViewTask.setText(taskName);
        holder.textViewDetails.setText(taskDetails);
        
        // Set icon based on task type
        int iconRes = R.drawable.ic_sun;
        int tintColor = R.color.orange;
        
        switch (task.getType()) {
            case "control_device":
                iconRes = R.drawable.ic_air_conditioner;
                tintColor = R.color.teal_200;
                break;
            case "select_scene":
                iconRes = R.drawable.ic_check_circle;
                tintColor = R.color.green;
                break;
            case "change_arm_mode":
                iconRes = R.drawable.ic_shield;
                tintColor = R.color.purple_500;
                break;
            case "send_notification":
                iconRes = R.drawable.ic_notifications;
                tintColor = R.color.error;
                break;
            case "delay":
                iconRes = R.drawable.ic_clock;
                tintColor = R.color.teal_200;
                break;
        }
        
        holder.imageViewIcon.setImageResource(iconRes);
        holder.imageViewIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), tintColor));
        
        holder.buttonRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskRemove(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewTask;
        TextView textViewDetails;
        ImageButton buttonRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewTask = itemView.findViewById(R.id.textViewTask);
            textViewDetails = itemView.findViewById(R.id.textViewDetails);
            buttonRemove = itemView.findViewById(R.id.buttonRemove);
        }
    }
}
