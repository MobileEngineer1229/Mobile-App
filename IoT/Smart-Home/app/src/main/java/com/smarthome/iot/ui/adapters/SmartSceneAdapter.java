package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.SceneCondition;
import com.smarthome.iot.models.SceneTask;
import com.smarthome.iot.models.SmartScene;

import java.util.ArrayList;
import java.util.List;

public class SmartSceneAdapter extends RecyclerView.Adapter<SmartSceneAdapter.ViewHolder> {
    private static final int VIEW_TYPE_AUTOMATION = 0;
    private static final int VIEW_TYPE_TAP_TO_RUN = 1;

    private List<SmartScene> scenes;
    private OnSceneClickListener listener;
    private OnSceneToggleListener toggleListener;

    public interface OnSceneClickListener {
        void onSceneClick(SmartScene scene);
    }

    public interface OnSceneToggleListener {
        void onSceneToggle(SmartScene scene, boolean isEnabled);
    }

    public SmartSceneAdapter(OnSceneClickListener listener, OnSceneToggleListener toggleListener) {
        this.scenes = new ArrayList<>();
        this.listener = listener;
        this.toggleListener = toggleListener;
    }

    public void setScenes(List<SmartScene> scenes) {
        try {
            this.scenes = scenes != null ? scenes : new ArrayList<>();
            android.util.Log.d("SmartSceneAdapter", "setScenes called with " + this.scenes.size() + " scenes");
            
            // Force notify on main thread
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                notifyDataSetChanged();
                android.util.Log.d("SmartSceneAdapter", "notifyDataSetChanged called on main thread");
            } else {
                // If not on main thread, post to main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    notifyDataSetChanged();
                    android.util.Log.d("SmartSceneAdapter", "notifyDataSetChanged called on main thread via Handler");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneAdapter", "Error in setScenes: " + e.getMessage(), e);
            this.scenes = new ArrayList<>();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= 0 && position < scenes.size()) {
            SmartScene scene = scenes.get(position);
            if (scene != null && "tap_to_run".equals(scene.getType())) {
                return VIEW_TYPE_TAP_TO_RUN;
            }
        }
        return VIEW_TYPE_AUTOMATION;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            int layoutRes = viewType == VIEW_TYPE_TAP_TO_RUN
                    ? R.layout.item_smart_scene_tap_to_run
                    : R.layout.item_smart_scene;
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(layoutRes, parent, false);
            if (view == null) {
                throw new IllegalStateException("Failed to inflate scene layout");
            }
            return new ViewHolder(view);
        } catch (Exception e) {
            e.printStackTrace();
            // Create a minimal fallback view
            View fallbackView = new android.widget.FrameLayout(parent.getContext());
            fallbackView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(fallbackView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (position < 0 || position >= scenes.size()) {
                android.util.Log.w("SmartSceneAdapter", "Invalid position: " + position + ", scenes size: " + scenes.size());
                return;
            }
            
            SmartScene scene = scenes.get(position);
            if (scene == null) {
                android.util.Log.w("SmartSceneAdapter", "Scene is null at position: " + position);
                return;
            }
            
            android.util.Log.d("SmartSceneAdapter", "Binding scene at position " + position + ": " + scene.getName() + ", Type: " + scene.getType());
            
            // Set card background color based on scene type
            if (holder.cardView != null) {
                if ("tap_to_run".equals(scene.getType()) && scene.getColor() != null && !scene.getColor().isEmpty()) {
                    // Tap-to-Run cards use scene-specific color (matching Figma)
                    try {
                        int color = android.graphics.Color.parseColor(scene.getColor());
                        holder.cardView.setCardBackgroundColor(color);
                    } catch (IllegalArgumentException e) {
                        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                    }
                } else {
                    // Automation cards use dark_4 background
                    holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.dark_4));
                }
            }
            
            // Set scene icon if available
            if (holder.imageViewSceneIcon != null) {
                int iconRes = R.drawable.ic_sun; // Default icon
                if (scene.getIcon() != null && !scene.getIcon().isEmpty()) {
                    iconRes = getIconResourceFromName(scene.getIcon(), scene.getName(), scene.getType());
                } else if (scene.getName() != null) {
                    // Try to get icon from scene name for Tap-to-Run scenes
                    iconRes = getIconResourceFromName(null, scene.getName(), scene.getType());
                }
                holder.imageViewSceneIcon.setImageResource(iconRes);
                
                // For Tap-to-Run scenes, don't apply white tint (use original PNG colors)
                // For Automation scenes, apply white tint for visibility on dark backgrounds
                if ("tap_to_run".equals(scene.getType())) {
                    holder.imageViewSceneIcon.clearColorFilter();
                } else {
                    holder.imageViewSceneIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
                }
            }
            
            if (holder.textViewTitle != null) {
                holder.textViewTitle.setText(scene.getName() != null ? scene.getName() : "");
            }
            
            int taskCount = scene.getTasks() != null ? scene.getTasks().size() : 0;
            String taskText = taskCount == 1 ? 
                taskCount + " " + holder.itemView.getContext().getString(R.string.task) :
                taskCount + " " + holder.itemView.getContext().getString(R.string.tasks);
            if (holder.textViewTaskCount != null) {
                holder.textViewTaskCount.setText(taskText);
            }
            
            // Build condition/task flow visualization (only for automation scenes in list view)
            if (holder.linearLayoutFlow != null) {
                if ("automation".equals(scene.getType())) {
                    holder.linearLayoutFlow.setVisibility(View.VISIBLE);
                    setupFlowIcons(holder, scene);
                } else {
                    holder.linearLayoutFlow.setVisibility(View.GONE);
                }
            }
            
            // Hide switch for Tap-to-Run scenes (they don't need enable/disable toggle)
            if (holder.switchEnabled != null) {
                if ("tap_to_run".equals(scene.getType())) {
                    holder.switchEnabled.setVisibility(View.GONE);
                    // Make sure switch doesn't intercept clicks for tap-to-run
                    holder.switchEnabled.setClickable(false);
                    holder.switchEnabled.setFocusable(false);
                } else {
                    // Apply switch styling for automation scenes
                    com.smarthome.iot.utils.SwitchHelper.applySwitchStyle(holder.itemView.getContext(), holder.switchEnabled);
                    holder.switchEnabled.setVisibility(View.VISIBLE);
                    holder.switchEnabled.setClickable(true);
                    holder.switchEnabled.setFocusable(true);
                    holder.switchEnabled.setOnCheckedChangeListener(null); // Clear previous listener
                    holder.switchEnabled.setChecked(scene.isEnabled());
                    holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (toggleListener != null) {
                            toggleListener.onSceneToggle(scene, isChecked);
                        }
                    });
                }
            }
            
            // Make sure all child views don't intercept clicks for tap-to-run
            if ("tap_to_run".equals(scene.getType())) {
                // Disable clicks on all child views for tap-to-run
                if (holder.imageViewSceneIcon != null) {
                    holder.imageViewSceneIcon.setClickable(false);
                    holder.imageViewSceneIcon.setFocusable(false);
                }
                if (holder.textViewTitle != null) {
                    holder.textViewTitle.setClickable(false);
                    holder.textViewTitle.setFocusable(false);
                }
                if (holder.textViewTaskCount != null) {
                    holder.textViewTaskCount.setClickable(false);
                    holder.textViewTaskCount.setFocusable(false);
                }
                if (holder.imageViewArrow != null) {
                    holder.imageViewArrow.setClickable(false);
                    holder.imageViewArrow.setFocusable(false);
                }
                if (holder.linearLayoutFlow != null) {
                    holder.linearLayoutFlow.setClickable(false);
                    holder.linearLayoutFlow.setFocusable(false);
                }
            } else {
                // For automation, only disable arrow
                if (holder.imageViewArrow != null) {
                    holder.imageViewArrow.setClickable(false);
                    holder.imageViewArrow.setFocusable(false);
                }
            }
            
            // Set click listener on the CardView (which is also the itemView/root)
            if (holder.cardView != null) {
                holder.cardView.setClickable(true);
                holder.cardView.setFocusable(true);
                holder.cardView.setOnTouchListener(null);
                holder.cardView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSceneClick(scene);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneAdapter", "Error in onBindViewHolder: " + e.getMessage(), e);
        }
    }

    private void setupFlowIcons(ViewHolder holder, SmartScene scene) {
        try {
            if (holder.linearLayoutFlow == null || scene == null) {
                return;
            }
            
            // Clear existing icons
            holder.linearLayoutFlow.removeAllViews();
            
            // Add condition icons
            if (scene.getConditions() != null && !scene.getConditions().isEmpty()) {
                for (int i = 0; i < scene.getConditions().size(); i++) {
                    SceneCondition condition = scene.getConditions().get(i);
                    if (condition != null && condition.getType() != null) {
                        ImageView icon = createIcon(holder.itemView.getContext(), condition.getType());
                        if (icon != null) {
                            holder.linearLayoutFlow.addView(icon);
                            
                            // Add arrow after condition (if not last or if there are tasks)
                            if (i < scene.getConditions().size() - 1 || 
                                (scene.getTasks() != null && !scene.getTasks().isEmpty())) {
                                ImageView arrow = createArrow(holder.itemView.getContext());
                                if (arrow != null) {
                                    holder.linearLayoutFlow.addView(arrow);
                                }
                            }
                        }
                    }
                }
            }
            
            // Add task icons
            if (scene.getTasks() != null && !scene.getTasks().isEmpty()) {
                for (int i = 0; i < scene.getTasks().size(); i++) {
                    SceneTask task = scene.getTasks().get(i);
                    if (task != null && task.getType() != null) {
                        ImageView icon = createTaskIcon(holder.itemView.getContext(), task.getType());
                        if (icon != null) {
                            holder.linearLayoutFlow.addView(icon);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Log error but don't crash
        }
    }

    private ImageView createIcon(android.content.Context context, String conditionType) {
        if (context == null) {
            return null;
        }
        
        try {
            // Use AppCompatImageView for proper theme support
            AppCompatImageView icon = new AppCompatImageView(context);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                (int) (32 * context.getResources().getDisplayMetrics().density),
                (int) (32 * context.getResources().getDisplayMetrics().density));
            icon.setLayoutParams(params);
        
            int iconRes = R.drawable.ic_clock;
            int tintColor = R.color.primary;
            
            if (conditionType != null) {
                switch (conditionType) {
                    case "tap_to_run":
                        iconRes = R.drawable.ic_tap;
                        tintColor = R.color.primary;
                        break;
                    case "temperature":
                        iconRes = R.drawable.temperature;
                        tintColor = R.color.error;
                        break;
                    case "humidity":
                        iconRes = R.drawable.humidity;
                        tintColor = R.color.primary;
                        break;
                    case "location":
                        iconRes = R.drawable.location;
                        tintColor = R.color.orange;
                        break;
                    case "time":
                        iconRes = R.drawable.sunrise;
                        tintColor = R.color.green;
                        break;
                    case "weather":
                        iconRes = R.drawable.weather;
                        tintColor = R.color.orange;
                        break;
                    case "sunrise_sunset":
                        iconRes = R.drawable.sunrise;
                        tintColor = R.color.orange;
                        break;
                    case "wind_speed":
                        iconRes = R.drawable.ic_wind;
                        tintColor = R.color.primary;
                        break;
                    case "device_status":
                        iconRes = R.drawable.category;
                        tintColor = R.color.primary;
                        break;
                    case "arm_mode":
                        iconRes = R.drawable.ic_shield;
                        tintColor = R.color.purple_500;
                        break;
                    case "alarm":
                        iconRes = R.drawable.ic_alarm;
                        tintColor = R.color.error;
                        break;
                }
            }
            
            icon.setImageResource(iconRes);
            icon.setColorFilter(ContextCompat.getColor(context, tintColor));
            return icon;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ImageView createArrow(android.content.Context context) {
        if (context == null) {
            return null;
        }
        
        try {
            // Use AppCompatImageView for proper theme support
            AppCompatImageView arrow = new AppCompatImageView(context);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                (int) (24 * context.getResources().getDisplayMetrics().density),
                (int) (24 * context.getResources().getDisplayMetrics().density));
            arrow.setLayoutParams(params);
            arrow.setImageResource(R.drawable.ic_arrow_forward);
            arrow.setColorFilter(ContextCompat.getColor(context, R.color.white_alpha_70));
            return arrow;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ImageView createTaskIcon(android.content.Context context, String taskType) {
        if (context == null) {
            return null;
        }
        
        try {
            // Use AppCompatImageView for proper theme support
            AppCompatImageView icon = new AppCompatImageView(context);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                (int) (32 * context.getResources().getDisplayMetrics().density),
                (int) (32 * context.getResources().getDisplayMetrics().density));
            icon.setLayoutParams(params);
            
            int iconRes = R.drawable.ic_sun;
            int tintColor = R.color.orange;
            
            if (taskType != null) {
                switch (taskType) {
                    case "control_device":
                        iconRes = R.drawable.air_conditionor;
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
            }
            
            icon.setImageResource(iconRes);
            icon.setColorFilter(ContextCompat.getColor(context, tintColor));
            return icon;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return scenes != null ? scenes.size() : 0;
    }

    private int getIconResourceFromName(String iconName, String sceneName, String sceneType) {
        // For Tap-to-Run scenes, try to map by scene name first
        if ("tap_to_run".equals(sceneType) && sceneName != null) {
            String nameLower = sceneName.toLowerCase().trim();
            if (nameLower.contains("bedtime")) {
                return R.drawable.smart_bedtime;
            } else if (nameLower.contains("evening")) {
                return R.drawable.smart_evening;
            } else if (nameLower.contains("productivity")) {
                return R.drawable.smart_productivity;
            } else if (nameLower.contains("energized") || nameLower.contains("energize")) {
                return R.drawable.smart_energized;
            } else if (nameLower.contains("home office") || nameLower.contains("office")) {
                return R.drawable.smart_home;
            } else if (nameLower.contains("reading")) {
                return R.drawable.smart_reading;
            } else if (nameLower.contains("outdoor") || nameLower.contains("party")) {
                return R.drawable.smart_outdoor;
            } else if (nameLower.contains("wind")) {
                return R.drawable.smart_wind;
            }
        }
        
        // Map icon names to drawable resources
        if (iconName != null) {
            switch (iconName) {
                case "ic_tap": return R.drawable.ic_tap;
                case "ic_sun": return R.drawable.ic_sun;
                case "ic_moon": return R.drawable.ic_sun; // Use sun as placeholder
                case "ic_clock": return R.drawable.ic_clock;
                case "ic_water_drop": return R.drawable.ic_water_drop;
                case "ic_wind": return R.drawable.ic_wind;
                case "ic_thermometer": return R.drawable.ic_thermometer;
                case "ic_location": return R.drawable.ic_location;
                case "ic_briefcase": return R.drawable.ic_briefcase;
                case "ic_shield": return R.drawable.ic_shield;
                case "ic_notifications": return R.drawable.ic_notifications;
                // New smart icons
                case "smart_bedtime": return R.drawable.smart_bedtime;
                case "smart_evening": return R.drawable.smart_evening;
                case "smart_productivity": return R.drawable.smart_productivity;
                case "smart_energized": return R.drawable.smart_energized;
                case "smart_home": return R.drawable.smart_home;
                case "smart_reading": return R.drawable.smart_reading;
                case "smart_outdoor": return R.drawable.smart_outdoor;
                case "smart_wind": return R.drawable.smart_wind;
                default: return R.drawable.ic_sun;
            }
        }
        
        return R.drawable.ic_sun;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        androidx.cardview.widget.CardView cardView;
        ImageView imageViewSceneIcon;
        TextView textViewTitle;
        TextView textViewTaskCount;
        LinearLayout linearLayoutFlow;
        SwitchMaterial switchEnabled;
        ImageView imageViewArrow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                cardView = itemView.findViewById(R.id.cardViewScene);
                imageViewSceneIcon = itemView.findViewById(R.id.imageViewSceneIcon);
                textViewTitle = itemView.findViewById(R.id.textViewTitle);
                textViewTaskCount = itemView.findViewById(R.id.textViewTaskCount);
                linearLayoutFlow = itemView.findViewById(R.id.linearLayoutFlow);
                switchEnabled = itemView.findViewById(R.id.switchEnabled);
                imageViewArrow = itemView.findViewById(R.id.imageViewArrow);
                // Apply consistent switch styling
                if (switchEnabled != null) {
                    com.smarthome.iot.utils.SwitchHelper.applySwitchStyle(itemView.getContext(), switchEnabled);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Views will be null, but adapter will handle null checks
            }
        }
    }
}
