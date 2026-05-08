package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Toast;

import com.smarthome.iot.R;
import com.smarthome.iot.ui.adapters.SceneTriggerAdapter;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class CreateSceneActivity extends AppCompatActivity {
    private RecyclerView recyclerViewTriggers;
    private SceneTriggerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.d("CreateSceneActivity", "onCreate called");
        try {
            ThemeHelper.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            android.util.Log.d("CreateSceneActivity", "Setting content view");
            setContentView(R.layout.activity_create_scene);
            android.util.Log.d("CreateSceneActivity", "Content view set successfully");

            setStatusBarColor();

            initializeViews();
            android.util.Log.d("CreateSceneActivity", "Views initialized");
            setupRecyclerView();
            android.util.Log.d("CreateSceneActivity", "RecyclerView setup complete");
            loadTriggers();
            android.util.Log.d("CreateSceneActivity", "Triggers loaded");
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("CreateSceneActivity", "Error in onCreate: " + e.getMessage(), e);
            android.util.Log.e("CreateSceneActivity", "Stack trace: ", e);
            // Print full stack trace
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            android.util.Log.e("CreateSceneActivity", "Full stack trace: " + sw.toString());
            
            Toast.makeText(this, "Error loading Create Scene: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Don't finish immediately - let user see the error and try to continue
        }
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        try {
            ImageButton buttonBack = findViewById(R.id.buttonBack);
            ImageButton buttonMore = findViewById(R.id.buttonMore);
            recyclerViewTriggers = findViewById(R.id.recyclerViewTriggers);

            if (buttonBack != null) {
                buttonBack.setOnClickListener(v -> finish());
            }
            if (buttonMore != null) {
                buttonMore.setOnClickListener(v -> {
                    // TODO: Show more options
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("CreateSceneActivity", "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupRecyclerView() {
        if (recyclerViewTriggers == null) {
            android.util.Log.e("CreateSceneActivity", "recyclerViewTriggers is null");
            return;
        }
        
        try {
            // Create adapter with empty list first, will be populated in loadTriggers
            adapter = new SceneTriggerAdapter(new ArrayList<>(), trigger -> {
                navigateToTrigger(trigger.getType());
            });
            recyclerViewTriggers.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewTriggers.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("CreateSceneActivity", "Error setting up RecyclerView: " + e.getMessage(), e);
        }
    }

    private void loadTriggers() {
        if (recyclerViewTriggers == null || adapter == null) {
            android.util.Log.e("CreateSceneActivity", "RecyclerView or adapter is null in loadTriggers");
            return;
        }
        
        try {
            List<SceneTriggerAdapter.TriggerItem> triggers = new ArrayList<>();
            triggers.add(new SceneTriggerAdapter.TriggerItem(
                "tap_to_run",
                getString(R.string.tap_to_run_trigger),
                getString(R.string.tap_to_run_example),
                R.drawable.hand_tap
            ));
            triggers.add(new SceneTriggerAdapter.TriggerItem(
                "weather",
                getString(R.string.when_weather_changes),
                getString(R.string.when_weather_changes_example),
                R.drawable.weather
            ));
            triggers.add(new SceneTriggerAdapter.TriggerItem(
                "location",
                getString(R.string.when_location_changes),
                getString(R.string.when_location_changes_example),
                R.drawable.location
            ));
            triggers.add(new SceneTriggerAdapter.TriggerItem(
                "time",
                getString(R.string.schedule_time),
                getString(R.string.schedule_time_example),
                R.drawable.sunrise
            ));
            triggers.add(new SceneTriggerAdapter.TriggerItem(
                "device_status",
                getString(R.string.when_device_status_changes),
                getString(R.string.when_device_status_changes_example),
                R.drawable.category
            ));
            triggers.add(new SceneTriggerAdapter.TriggerItem(
                "arm_mode",
                getString(R.string.change_arm_mode),
                getString(R.string.change_arm_mode_example),
                R.drawable.shield
            ));
            triggers.add(new SceneTriggerAdapter.TriggerItem(
                "alarm",
                getString(R.string.when_alarm_triggered),
                getString(R.string.when_alarm_triggered_example),
                R.drawable.alarm_fill
            ));
            
            // Update adapter with triggers - reuse existing adapter
            if (adapter != null) {
                adapter.setTriggers(triggers);
            } else {
                adapter = new SceneTriggerAdapter(triggers, trigger -> {
                    navigateToTrigger(trigger.getType());
                });
                recyclerViewTriggers.setAdapter(adapter);
            }
            android.util.Log.d("CreateSceneActivity", "Loaded " + triggers.size() + " triggers");
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("CreateSceneActivity", "Error loading triggers: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading triggers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToTrigger(String triggerType) {
        try {
            Intent intent;
            switch (triggerType) {
                case "weather":
                    intent = new Intent(this, WeatherConditionsActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                case "location":
                    // Navigate to location selection (Arrive at/Leave)
                    intent = new Intent(this, LocationSelectionActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                case "time":
                    // Navigate to schedule time picker
                    intent = new Intent(this, ScheduleTimeActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                case "tap_to_run":
                    // Navigate directly to scene builder
                    intent = new Intent(this, SceneBuilderActivity.class);
                    intent.putExtra("trigger_type", "tap_to_run");
                    startActivityForResult(intent, 200);
                    break;
                case "device_status":
                    intent = new Intent(this, DeviceStatusConditionActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                case "arm_mode":
                    intent = new Intent(this, ArmModeConditionActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                default:
                    // TODO: Implement other trigger types
                    Toast.makeText(this, triggerType + " coming soon", Toast.LENGTH_SHORT).show();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("CreateSceneActivity", "Error navigating to trigger: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening " + triggerType + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Weather condition selected, navigate to scene builder
            Intent intent = new Intent(this, SceneBuilderActivity.class);
            if (data != null) {
                intent.putExtra("condition_type", data.getStringExtra("condition_type"));
                intent.putExtra("condition_data", data.getSerializableExtra("condition_data"));
            }
            startActivityForResult(intent, 200);
        } else if (requestCode == 200 && resultCode == RESULT_OK) {
            // Scene created, forward result to SmartSceneActivity
            Intent resultIntent = new Intent();
            if (data != null) {
                // Forward all scene data
                resultIntent.putExtra("scene_name", data.getStringExtra("scene_name"));
                resultIntent.putExtra("scene_color", data.getStringExtra("scene_color"));
                resultIntent.putExtra("scene_icon", data.getStringExtra("scene_icon"));
                resultIntent.putExtra("scene_type", data.getStringExtra("scene_type"));
            }
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }
}
