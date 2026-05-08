package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.fragments.devicecontrol.AirConditionerControlFragment;
import com.smarthome.iot.ui.fragments.devicecontrol.CameraControlFragment;
import com.smarthome.iot.ui.fragments.devicecontrol.LampControlFragment;
import com.smarthome.iot.ui.fragments.devicecontrol.SpeakerControlFragment;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.DeviceHelper;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main activity for device control
 * Routes to device-specific control fragments based on device type
 */
public class DeviceControlDetailActivity extends AppCompatActivity {
    private TextView textViewDeviceName;
    private TextView textViewRoomName;
    private ImageView imageViewDeviceIcon;
    private SwitchMaterial switchPower;
    private ImageButton buttonBack;
    private ImageButton buttonMore;
    private MaterialButton buttonSchedule;
    
    private Integer deviceId;
    private String deviceName;
    private String deviceType;
    private Integer roomId;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control_detail);

        setStatusBarColor();

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        authManager = new AuthManager(this);

        // Get device info from intent
        Intent intent = getIntent();
        deviceId = intent.getIntExtra("device_id", 0);
        deviceName = intent.getStringExtra("device_name");
        deviceType = intent.getStringExtra("device_type");
        roomId = intent.getIntExtra("room_id", 0);

        initializeViews();
        setupDeviceInfo();
        loadDeviceControlFragment();
        loadDevicePowerState(); // Load power state from backend metadata
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        buttonBack = findViewById(R.id.buttonBack);
        buttonMore = findViewById(R.id.buttonMore);
        textViewDeviceName = findViewById(R.id.textViewDeviceName);
        textViewRoomName = findViewById(R.id.textViewRoomName);
        imageViewDeviceIcon = findViewById(R.id.imageViewDeviceIcon);
        switchPower = findViewById(R.id.switchPower);
        
        // Apply consistent switch styling
        if (switchPower != null) {
            com.smarthome.iot.utils.SwitchHelper.applySwitchStyle(this, switchPower);
        }

        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            // TODO: Show more options
        });

        switchPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleDevicePower(isChecked);
        });

        // Setup schedule button
        buttonSchedule = findViewById(R.id.buttonSchedule);
        if (buttonSchedule != null) {
            buttonSchedule.setOnClickListener(v -> {
                // TODO: Navigate to schedule screen
                android.util.Log.d("DeviceControl", "Schedule button clicked");
            });
        }
    }

    private void setupDeviceInfo() {
        if (textViewDeviceName != null && deviceName != null) {
            textViewDeviceName.setText(deviceName);
        }

        if (textViewRoomName != null) {
            // TODO: Get room name from roomId
            textViewRoomName.setText("Living Room");
        }

        // Set device icon based on type using DeviceHelper
        if (imageViewDeviceIcon != null) {
            int iconRes = DeviceHelper.getDeviceIcon(this, deviceName, deviceType);
            imageViewDeviceIcon.setImageResource(iconRes);
        }
    }

    private void loadDeviceControlFragment() {
        Fragment fragment = null;
        
        // Log device type and name for debugging
        android.util.Log.d("DeviceControl", "Loading fragment for device type: " + deviceType + ", device name: " + deviceName);
        
        // Use DeviceHelper to determine which fragment to load
        DeviceHelper.ControlFragmentType fragmentType = DeviceHelper.getControlFragmentType(deviceName, deviceType);
        android.util.Log.d("DeviceControl", "Detected fragment type: " + fragmentType + " for device: " + deviceName);
        
        switch (fragmentType) {
            case LAMP:
                android.util.Log.d("DeviceControl", "Loading LampControlFragment (lightning category) - name: " + deviceName);
                fragment = LampControlFragment.newInstance(deviceId, deviceName, roomId);
                // Show activity's schedule button for lamp
                View layoutScheduleButtonLamp = findViewById(R.id.layoutScheduleButton);
                if (layoutScheduleButtonLamp != null) {
                    layoutScheduleButtonLamp.setVisibility(View.VISIBLE);
                }
                break;
                
            case CAMERA:
                android.util.Log.d("DeviceControl", "Loading CameraControlFragment (cameras category) - name: " + deviceName);
                fragment = CameraControlFragment.newInstance(deviceId, deviceName, roomId);
                // Hide activity's schedule button for camera (not needed)
                View layoutScheduleButton = findViewById(R.id.layoutScheduleButton);
                if (layoutScheduleButton != null) {
                    layoutScheduleButton.setVisibility(View.GONE);
                }
                break;
                
            case SPEAKER:
                android.util.Log.d("DeviceControl", "Loading SpeakerControlFragment (electrical category) - name: " + deviceName);
                fragment = SpeakerControlFragment.newInstance(deviceId, deviceName, roomId);
                // Hide activity's schedule button for speaker (not needed)
                View layoutScheduleButtonSpeaker = findViewById(R.id.layoutScheduleButton);
                if (layoutScheduleButtonSpeaker != null) {
                    layoutScheduleButtonSpeaker.setVisibility(View.GONE);
                }
                break;
                
            case AIR_CONDITIONER:
                android.util.Log.d("DeviceControl", "Loading AirConditionerControlFragment (electrical category) - name: " + deviceName);
                fragment = AirConditionerControlFragment.newInstance(deviceId, deviceName, roomId);
                // Show activity's schedule button with AC-specific text
                View layoutScheduleButtonAC = findViewById(R.id.layoutScheduleButton);
                if (layoutScheduleButtonAC != null) {
                    layoutScheduleButtonAC.setVisibility(View.VISIBLE);
                }
                if (buttonSchedule != null) {
                    buttonSchedule.setText("Schedule Automatic ON/OFF & Smart Scene");
                }
                break;
                
            case BASIC:
            default:
                android.util.Log.d("DeviceControl", "Device: " + deviceName + " (basic control) - using LampControlFragment as template");
                // For devices without specific control fragments, use lamp control as basic template
                // TODO: Create generic control fragment for these device types
                fragment = LampControlFragment.newInstance(deviceId, deviceName, roomId);
                // Show activity's schedule button for basic devices
                View layoutScheduleButtonBasic = findViewById(R.id.layoutScheduleButton);
                if (layoutScheduleButtonBasic != null) {
                    layoutScheduleButtonBasic.setVisibility(View.VISIBLE);
                }
                break;
        }

        // Replace fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private void toggleDevicePower(boolean isOn) {
        if (deviceId == null || deviceId == 0) {
            android.util.Log.w("DeviceControl", "Invalid device ID, cannot toggle power");
            return;
        }

        if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            // Demo user - just log
            android.util.Log.d("DeviceControl", "Demo user - Power toggled: " + isOn);
            return;
        }

        // Prepare power data
        Map<String, Boolean> powerData = new HashMap<>();
        powerData.put("power", isOn);

        Call<ApiResponse<Map<String, Object>>> call = apiService.controlDevicePower(deviceId, powerData);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("DeviceControl", "Device power toggled successfully: " + isOn);
                } else {
                    // Revert switch on failure
                    switchPower.setChecked(!isOn);
                    android.util.Log.w("DeviceControl", "Failed to toggle device power via API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                // Revert switch on failure
                switchPower.setChecked(!isOn);
                android.util.Log.e("DeviceControl", "Error toggling device power", t);
            }
        });
    }

    /**
     * Load device power state from backend metadata and sync to power switch
     */
    private void loadDevicePowerState() {
        if (deviceId == null || deviceId == 0) {
            android.util.Log.w("DeviceControl", "Invalid device ID, cannot load power state");
            return;
        }

        if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            android.util.Log.d("DeviceControl", "Demo user or not logged in, skipping power state sync");
            return;
        }

        // Fetch device details from API to get metadata
        Call<ApiResponse<com.smarthome.iot.models.Device>> call = apiService.getDeviceById(deviceId);
        call.enqueue(new Callback<ApiResponse<com.smarthome.iot.models.Device>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.smarthome.iot.models.Device>> call, Response<ApiResponse<com.smarthome.iot.models.Device>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.smarthome.iot.models.Device device = response.body().getData();
                    if (device != null && device.getMetadata() != null) {
                        Map<String, Object> metadata = device.getMetadata();

                        // Sync power state from metadata
                        if (metadata.containsKey("power") && switchPower != null) {
                            Object powerObj = metadata.get("power");
                            boolean power = false;
                            if (powerObj instanceof Boolean) {
                                power = (Boolean) powerObj;
                            } else if (powerObj instanceof String) {
                                power = "true".equalsIgnoreCase((String) powerObj);
                            }

                            // Update switch without triggering listener (avoid API call)
                            switchPower.setOnCheckedChangeListener(null);
                            switchPower.setChecked(power);
                            switchPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                toggleDevicePower(isChecked);
                            });

                            android.util.Log.d("DeviceControl", "Synced power state from metadata: " + power);
                        }
                    } else {
                        android.util.Log.w("DeviceControl", "Device has no metadata");
                    }
                } else {
                    android.util.Log.w("DeviceControl", "Failed to load device from API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.smarthome.iot.models.Device>> call, Throwable t) {
                android.util.Log.e("DeviceControl", "Error loading device", t);
            }
        });
    }
}
