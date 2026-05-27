package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.SceneCondition;
import com.smarthome.iot.models.SceneTask;
import com.smarthome.iot.models.SmartScene;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.SceneConditionAdapter;
import com.smarthome.iot.ui.adapters.SceneTaskAdapter;
import android.widget.TextView;

import com.smarthome.iot.ui.dialogs.AddTaskBottomSheetDialog;
import com.smarthome.iot.ui.dialogs.StyleBottomSheetDialog;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SceneBuilderActivity extends AppCompatActivity {
    private TextInputLayout textInputLayoutLogic;
    private TextInputEditText editTextLogic;
    private ImageButton buttonAddCondition;
    private RecyclerView recyclerViewConditions;
    private ImageButton buttonAddTask;
    private RecyclerView recyclerViewTasks;
    private MaterialButton buttonAddTaskLarge;
    private MaterialButton buttonSave;
    
    private SceneConditionAdapter conditionAdapter;
    private SceneTaskAdapter taskAdapter;
    private List<SceneCondition> conditions;
    private List<SceneTask> tasks;
    private String conditionLogic = "any";
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            ThemeHelper.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_scene_builder);

            setStatusBarColor();
            
            // Initialize API
            ApiClient.initialize(this);
            apiService = ApiClient.getApiService();
            authManager = new AuthManager(this);

            conditions = new ArrayList<>();
            tasks = new ArrayList<>();
            
            // Check if condition was passed from previous screen
            if (getIntent() != null) {
                String triggerType = getIntent().getStringExtra("trigger_type");
                if ("tap_to_run".equals(triggerType)) {
                    // Create Tap-to-Run condition
                    SceneCondition tapToRunCondition = new SceneCondition();
                    tapToRunCondition.setType("tap_to_run");
                    tapToRunCondition.setOperator("manual");
                    conditions.add(tapToRunCondition);
                } else {
                    String conditionType = getIntent().getStringExtra("condition_type");
                    if (conditionType != null) {
                        SceneCondition condition = (SceneCondition) getIntent().getSerializableExtra("condition_data");
                        if (condition != null) {
                            conditions.add(condition);
                        }
                    }
                }
            }

            initializeViews();
            setupRecyclerViews();
            setupClickListeners();
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SceneBuilderActivity", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading Scene Builder: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private View layoutStyle;
    private ImageView imageViewStyleIcon;
    private String selectedColor = "#405FF2"; // Default primary blue
    private String selectedIcon = "ic_sun"; // Default icon
    private int selectedIconRes = R.drawable.ic_sun;
    
    private void initializeViews() {
        try {
            ImageButton buttonBack = findViewById(R.id.buttonBack);
            ImageButton buttonMore = findViewById(R.id.buttonMore);
            textInputLayoutLogic = findViewById(R.id.textInputLayoutLogic);
            editTextLogic = findViewById(R.id.editTextLogic);
            buttonAddCondition = findViewById(R.id.buttonAddCondition);
            recyclerViewConditions = findViewById(R.id.recyclerViewConditions);
            buttonAddTask = findViewById(R.id.buttonAddTask);
            recyclerViewTasks = findViewById(R.id.recyclerViewTasks);
            buttonAddTaskLarge = findViewById(R.id.buttonAddTaskLarge);
            buttonSave = findViewById(R.id.buttonSave);
            layoutStyle = findViewById(R.id.layoutStyle);
            imageViewStyleIcon = findViewById(R.id.imageViewStyleIcon);

            if (buttonBack != null) {
                buttonBack.setOnClickListener(v -> finish());
            }
            if (buttonMore != null) {
                buttonMore.setOnClickListener(v -> {
                    // TODO: Show more options
                });
            }
            if (layoutStyle != null) {
                layoutStyle.setOnClickListener(v -> showStyleDialog());
            }
            if (editTextLogic != null) {
                editTextLogic.setOnClickListener(v -> showConditionLogicDialog());
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SceneBuilderActivity", "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupRecyclerViews() {
        try {
            if (recyclerViewConditions != null) {
                conditionAdapter = new SceneConditionAdapter(conditions, condition -> {
                    conditions.remove(condition);
                    if (conditionAdapter != null) {
                        conditionAdapter.setConditions(conditions);
                    }
                    updateUI();
                });
                recyclerViewConditions.setLayoutManager(new LinearLayoutManager(this));
                recyclerViewConditions.setAdapter(conditionAdapter);
            }
            
            if (recyclerViewTasks != null) {
                taskAdapter = new SceneTaskAdapter(tasks, task -> {
                    tasks.remove(task);
                    if (taskAdapter != null) {
                        taskAdapter.setTasks(tasks);
                    }
                    updateUI();
                });
                recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
                recyclerViewTasks.setAdapter(taskAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SceneBuilderActivity", "Error setting up RecyclerViews: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        try {
            if (buttonAddCondition != null) {
                buttonAddCondition.setOnClickListener(v -> {
                    try {
                        // Navigate back to CreateSceneActivity to add another condition
                        Intent intent = new Intent(this, CreateSceneActivity.class);
                        startActivityForResult(intent, 100);
                    } catch (Exception e) {
                        e.printStackTrace();
                        android.util.Log.e("SceneBuilderActivity", "Error opening CreateSceneActivity: " + e.getMessage(), e);
                        Toast.makeText(this, "Error opening Create Scene: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            if (buttonAddTask != null) {
                buttonAddTask.setOnClickListener(v -> showAddTaskDialog());
            }
            if (buttonAddTaskLarge != null) {
                buttonAddTaskLarge.setOnClickListener(v -> showAddTaskDialog());
            }
            
            if (buttonSave != null) {
                buttonSave.setOnClickListener(v -> {
                    try {
                        // For tap_to_run scenes, conditions are optional (they're auto-added)
                        String triggerType = getIntent() != null ? getIntent().getStringExtra("trigger_type") : null;
                        boolean isTapToRun = "tap_to_run".equals(triggerType);
                        
                        if (!isTapToRun && (conditions == null || conditions.isEmpty())) {
                            Toast.makeText(this, "Please add at least one condition", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (tasks == null || tasks.isEmpty()) {
                            Toast.makeText(this, "Please add at least one task", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Show scene name dialog
                        showSceneNameDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                        android.util.Log.e("SceneBuilderActivity", "Error in save button click: " + e.getMessage(), e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SceneBuilderActivity", "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void showConditionLogicDialog() {
        String[] options = {
            getString(R.string.when_any_condition_met),
            getString(R.string.when_all_conditions_met)
        };
        
        new AlertDialog.Builder(this)
            .setItems(options, (dialog, which) -> {
                conditionLogic = which == 0 ? "any" : "all";
                editTextLogic.setText(options[which]);
            })
            .show();
    }

    private void showAddTaskDialog() {
        try {
            AddTaskBottomSheetDialog dialog = new AddTaskBottomSheetDialog(this, taskType -> {
                navigateToTask(taskType);
            });
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SceneBuilderActivity", "Error showing add task dialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening task dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToTask(String taskType) {
        try {
            Intent intent;
            switch (taskType) {
                case "control_device":
                    intent = new Intent(this, ControlDeviceActivity.class);
                    startActivityForResult(intent, 200);
                    break;
                case "select_scene":
                    // Navigate to select scene (automation/tap-to-run)
                    intent = new Intent(this, SelectSceneActivity.class);
                    startActivityForResult(intent, 200);
                    break;
                case "delay":
                    // Navigate to delay action picker
                    intent = new Intent(this, DelayActionActivity.class);
                    startActivityForResult(intent, 200);
                    break;
                case "change_arm_mode":
                case "send_notification":
                    // TODO: Implement these task types
                    Toast.makeText(this, taskType + " coming soon", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this, taskType + " coming soon", Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SceneBuilderActivity", "Error navigating to task: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening " + taskType + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSceneNameDialog() {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_scene_name, null);
            TextInputEditText editTextSceneName = dialogView.findViewById(R.id.editTextSceneName);
            MaterialButton buttonCancel = dialogView.findViewById(R.id.buttonCancel);
            MaterialButton buttonSave = dialogView.findViewById(R.id.buttonSave);
            
            if (editTextSceneName == null || buttonCancel == null || buttonSave == null) {
                android.util.Log.e("SceneBuilderActivity", "Dialog views are null");
                Toast.makeText(this, "Error loading dialog", Toast.LENGTH_SHORT).show();
                return;
            }
            
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
            
            buttonCancel.setOnClickListener(v -> {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            buttonSave.setOnClickListener(v -> {
                try {
                    String sceneName = editTextSceneName.getText().toString().trim();
                    if (sceneName.isEmpty()) {
                        Toast.makeText(this, "Please enter a scene name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                    saveScene(sceneName);
                } catch (Exception e) {
                    e.printStackTrace();
                    android.util.Log.e("SceneBuilderActivity", "Error in save button: " + e.getMessage(), e);
                }
            });
            
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SceneBuilderActivity", "Error showing scene name dialog: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveScene(String sceneName) {
        try {
            String triggerType = getIntent().getStringExtra("trigger_type");
            String sceneType = triggerType != null && "tap_to_run".equals(triggerType) ? "tap_to_run" : "automation";
            
            // Check if demo user or not logged in
            if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
                // Demo user - save locally only
                SmartScene newScene = new SmartScene();
                newScene.setName(sceneName);
                newScene.setColor(selectedColor);
                newScene.setIcon(selectedIcon);
                newScene.setType(sceneType);
                newScene.setEnabled(true);
                newScene.setConditionLogic(conditionLogic);
                newScene.setConditions(conditions != null ? conditions : new ArrayList<>());
                newScene.setTasks(tasks != null ? tasks : new ArrayList<>());
                
                // Add to MockDataProvider so it appears in the list immediately
                MockDataProvider.addDynamicScene(newScene);
                
                // Return result with scene data
                Intent resultIntent = new Intent();
                resultIntent.putExtra("scene_name", sceneName);
                resultIntent.putExtra("scene_color", selectedColor);
                resultIntent.putExtra("scene_icon", selectedIcon);
                resultIntent.putExtra("scene_type", sceneType);
                setResult(RESULT_OK, resultIntent);
                finish();
                return;
            }
            
            // Prepare scene data for API
            Map<String, Object> sceneData = new HashMap<>();
            sceneData.put("name", sceneName);
            sceneData.put("color", selectedColor);
            sceneData.put("icon", selectedIcon);
            sceneData.put("type", sceneType);
            sceneData.put("isEnabled", true);
            sceneData.put("conditionLogic", conditionLogic);
            
            // Add home ID if available
            com.smarthome.iot.models.Home primaryHome = Globals.getPrimaryHome();
            if (primaryHome != null) {
                sceneData.put("homeId", primaryHome.getId());
            }
            
            // Convert conditions to maps
            List<Map<String, Object>> conditionsList = new ArrayList<>();
            if (conditions != null) {
                for (SceneCondition condition : conditions) {
                    Map<String, Object> conditionMap = new HashMap<>();
                    conditionMap.put("type", condition.getType());
                    conditionMap.put("operator", condition.getOperator());
                    conditionMap.put("value", condition.getValue());
                    conditionMap.put("unit", condition.getUnit());
                    conditionMap.put("location", condition.getLocation());
                    conditionMap.put("deviceId", condition.getDeviceId());
                    conditionMap.put("deviceStatus", condition.getDeviceStatus());
                    conditionMap.put("armMode", condition.getArmMode());
                    conditionsList.add(conditionMap);
                }
            }
            sceneData.put("conditions", conditionsList);
            
            // Convert tasks to maps
            List<Map<String, Object>> tasksList = new ArrayList<>();
            if (tasks != null) {
                for (SceneTask task : tasks) {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("type", task.getType());
                    taskMap.put("deviceId", task.getDeviceId());
                    taskMap.put("deviceName", task.getDeviceName());
                    taskMap.put("roomName", task.getRoomName());
                    taskMap.put("function", task.getFunction());
                    taskMap.put("sceneId", task.getSceneId());
                    taskMap.put("sceneName", task.getSceneName());
                    taskMap.put("armMode", task.getArmMode());
                    taskMap.put("notificationMessage", task.getNotificationMessage());
                    taskMap.put("delaySeconds", task.getDelaySeconds());
                    tasksList.add(taskMap);
                }
            }
            sceneData.put("tasks", tasksList);
            
            // Show loading
            Toast.makeText(this, "Saving scene...", Toast.LENGTH_SHORT).show();
            
            // Call API to create scene
            Call<ApiResponse<SmartScene>> call = apiService.createScene(sceneData);
            call.enqueue(new Callback<ApiResponse<SmartScene>>() {
                @Override
                public void onResponse(Call<ApiResponse<SmartScene>> call, Response<ApiResponse<SmartScene>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        SmartScene createdScene = response.body().getData();
                        android.util.Log.d("SceneBuilderActivity", "Scene created successfully: " + createdScene.getName());
                        
                        // Also add to MockDataProvider for immediate display
                        MockDataProvider.addDynamicScene(createdScene);
                        
                        // Return result with scene data
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("scene_name", sceneName);
                        resultIntent.putExtra("scene_color", selectedColor);
                        resultIntent.putExtra("scene_icon", selectedIcon);
                        resultIntent.putExtra("scene_type", sceneType);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        android.util.Log.w("SceneBuilderActivity", "Failed to create scene via API");
                        Toast.makeText(SceneBuilderActivity.this, 
                            "Failed to save scene", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<SmartScene>> call, Throwable t) {
                    android.util.Log.e("SceneBuilderActivity", "Error creating scene", t);
                    Toast.makeText(SceneBuilderActivity.this, 
                        "Error saving scene: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving scene: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showStyleDialog() {
        StyleBottomSheetDialog styleDialog = new StyleBottomSheetDialog(
            this,
            selectedColor,
            selectedIcon,
            selectedIconRes,
            (color, icon, iconRes) -> {
                selectedColor = color;
                selectedIcon = icon;
                selectedIconRes = iconRes;
                updateStyleIcon();
            }
        );
        styleDialog.show();
    }
    
    private void updateStyleIcon() {
        if (imageViewStyleIcon != null) {
            imageViewStyleIcon.setImageResource(selectedIconRes);
            try {
                int colorInt = android.graphics.Color.parseColor(selectedColor);
                imageViewStyleIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorInt));
            } catch (IllegalArgumentException e) {
                imageViewStyleIcon.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            }
        }
    }

    private void updateUI() {
        // Show/hide Add Task Large button based on task count
        buttonAddTaskLarge.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerViewTasks.setVisibility(tasks.isEmpty() ? View.GONE : View.VISIBLE);
        
        // Update style icon
        updateStyleIcon();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Condition added
            SceneCondition condition = (SceneCondition) data.getSerializableExtra("condition_data");
            if (condition != null) {
                conditions.add(condition);
                conditionAdapter.setConditions(conditions);
            }
        } else if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            // Task added
            SceneTask task = (SceneTask) data.getSerializableExtra("task_data");
            if (task != null) {
                tasks.add(task);
                taskAdapter.setTasks(tasks);
                updateUI();
            }
        }
    }
}
