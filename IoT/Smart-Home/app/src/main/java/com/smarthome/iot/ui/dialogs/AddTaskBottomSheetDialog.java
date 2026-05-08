package com.smarthome.iot.ui.dialogs;

import android.app.Activity;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.smarthome.iot.R;
import com.smarthome.iot.ui.adapters.TaskTypeAdapter;

import java.util.ArrayList;
import java.util.List;

public class AddTaskBottomSheetDialog {
    private BottomSheetDialog dialog;
    private OnTaskTypeSelectedListener listener;

    public interface OnTaskTypeSelectedListener {
        void onTaskTypeSelected(String taskType);
    }

    public AddTaskBottomSheetDialog(Activity activity, OnTaskTypeSelectedListener listener) {
        this.listener = listener;
        
        View bottomSheetView = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_add_task, null);
        dialog = new BottomSheetDialog(activity);
        dialog.setContentView(bottomSheetView);
        
        RecyclerView recyclerViewTasks = bottomSheetView.findViewById(R.id.recyclerViewTasks);
        
        List<TaskTypeAdapter.TaskTypeItem> taskTypes = new ArrayList<>();
        taskTypes.add(new TaskTypeAdapter.TaskTypeItem("control_device", activity.getString(R.string.control_single_device), R.drawable.ic_briefcase, R.color.teal_200));
        taskTypes.add(new TaskTypeAdapter.TaskTypeItem("select_scene", activity.getString(R.string.select_smart_scene), R.drawable.ic_check_circle, R.color.green));
        taskTypes.add(new TaskTypeAdapter.TaskTypeItem("change_arm_mode", activity.getString(R.string.change_arm_mode_task), R.drawable.ic_shield, R.color.purple_500));
        taskTypes.add(new TaskTypeAdapter.TaskTypeItem("send_notification", activity.getString(R.string.send_notification), R.drawable.ic_notifications, R.color.error));
        taskTypes.add(new TaskTypeAdapter.TaskTypeItem("delay", activity.getString(R.string.delay_the_action), R.drawable.ic_clock, R.color.teal_200));
        
        TaskTypeAdapter adapter = new TaskTypeAdapter(taskTypes, taskType -> {
            if (listener != null) {
                listener.onTaskTypeSelected(taskType.getType());
            }
            dialog.dismiss();
        });
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(activity));
        recyclerViewTasks.setAdapter(adapter);
    }

    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
