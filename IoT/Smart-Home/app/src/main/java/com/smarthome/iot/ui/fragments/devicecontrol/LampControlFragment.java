package com.smarthome.iot.ui.fragments.devicecontrol;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;

import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.MockDataProvider;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment for controlling Smart Lamp
 * Supports White, Color, and Scene modes
 */
public class LampControlFragment extends Fragment {
    private static final String ARG_DEVICE_ID = "device_id";
    private static final String ARG_DEVICE_NAME = "device_name";
    private static final String ARG_ROOM_ID = "room_id";

    private Integer deviceId;
    private String deviceName;
    private Integer roomId;

    private MaterialButton buttonWhite;
    private MaterialButton buttonColor;
    private MaterialButton buttonScene;
    private String currentMode = "white";

    private SeekBar seekBarBrightness;
    private TextView textViewBrightness;
    private SeekBar seekBarSaturation;
    private TextView textViewSaturation;
    private com.smarthome.iot.ui.views.TemperatureArcView temperatureArcView;
    private com.smarthome.iot.ui.views.ColorWheelView colorWheelView;
    private ApiService apiService;
    private AuthManager authManager;
    private boolean isUpdatingBrightness = false;
    private boolean isUpdatingSaturation = false;

    public static LampControlFragment newInstance(Integer deviceId, String deviceName, Integer roomId) {
        LampControlFragment fragment = new LampControlFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DEVICE_ID, deviceId);
        args.putString(ARG_DEVICE_NAME, deviceName);
        args.putInt(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deviceId = getArguments().getInt(ARG_DEVICE_ID);
            deviceName = getArguments().getString(ARG_DEVICE_NAME);
            roomId = getArguments().getInt(ARG_ROOM_ID);
        }
        
        // Initialize API
        if (getContext() != null) {
            ApiClient.initialize(getContext());
            apiService = ApiClient.getApiService();
            authManager = new AuthManager(getContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lamp_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupTabButtons();
        setupControls();
        // Load device state from backend metadata before setting default mode
        loadDeviceState();
    }

    private void initializeViews(View view) {
        buttonWhite = view.findViewById(R.id.buttonWhite);
        buttonColor = view.findViewById(R.id.buttonColor);
        buttonScene = view.findViewById(R.id.buttonScene);
        seekBarBrightness = view.findViewById(R.id.seekBarBrightness);
        textViewBrightness = view.findViewById(R.id.textViewBrightness);
        seekBarSaturation = view.findViewById(R.id.seekBarSaturation);
        textViewSaturation = view.findViewById(R.id.textViewSaturation);
        temperatureArcView = view.findViewById(R.id.temperatureArcView);
        colorWheelView = view.findViewById(R.id.colorWheelView);
    }

    private void setupTabButtons() {
        buttonWhite.setOnClickListener(v -> loadMode("white"));
        buttonColor.setOnClickListener(v -> loadMode("color"));
        buttonScene.setOnClickListener(v -> loadMode("scene"));
    }

    private void loadMode(String mode) {
        currentMode = mode;
        updateTabButtons();
        updateModeViews();
    }

    private void updateTabButtons() {
        // Use new tab button style - works like a switch: one selected, one unselected
        // Selected: Primary color background, Unselected: Dark background
        // Clear background tint to ensure our drawables are used (no stroke)
        
        // Update all buttons
        buttonWhite.setBackgroundTintList(null);
        buttonWhite.setElevation(0f);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            buttonWhite.setStateListAnimator(null);
        }
        buttonWhite.post(() -> {
            if ("white".equals(currentMode)) {
                buttonWhite.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background_rounded));
            } else {
                buttonWhite.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.tab_unselected_background_rounded));
            }
            buttonWhite.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white));
            buttonWhite.invalidate();
        });
        
        buttonColor.setBackgroundTintList(null);
        buttonColor.setElevation(0f);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            buttonColor.setStateListAnimator(null);
        }
        buttonColor.post(() -> {
            if ("color".equals(currentMode)) {
                buttonColor.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background_rounded));
            } else {
                buttonColor.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.tab_unselected_background_rounded));
            }
            buttonColor.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white));
            buttonColor.invalidate();
        });
        
        buttonScene.setBackgroundTintList(null);
        buttonScene.setElevation(0f);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            buttonScene.setStateListAnimator(null);
        }
        buttonScene.post(() -> {
            if ("scene".equals(currentMode)) {
                buttonScene.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background_rounded));
            } else {
                buttonScene.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.tab_unselected_background_rounded));
            }
            buttonScene.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white));
            buttonScene.invalidate();
        });
    }

    private void updateModeViews() {
        // Show/hide views based on mode
        boolean showWhiteControls = "white".equals(currentMode);
        boolean showColorControls = "color".equals(currentMode);
        boolean showSceneControls = "scene".equals(currentMode);

        View whiteModeContainer = getView().findViewById(R.id.whiteModeContainer);
        View colorModeContainer = getView().findViewById(R.id.colorModeContainer);
        View brightnessControlContainer = getView().findViewById(R.id.brightnessControlContainer);
        View saturationControlContainer = getView().findViewById(R.id.saturationControlContainer);

        if (whiteModeContainer != null) {
            whiteModeContainer.setVisibility(showWhiteControls ? View.VISIBLE : View.GONE);
        }
        if (colorModeContainer != null) {
            colorModeContainer.setVisibility(showColorControls ? View.VISIBLE : View.GONE);
        }

        // White mode: Show only brightness slider (no saturation)
        // Color mode: Show both brightness and saturation sliders
        // Scene mode: Hide both sliders
        if (brightnessControlContainer != null) {
            brightnessControlContainer.setVisibility(!showSceneControls ? View.VISIBLE : View.GONE);
        }
        if (saturationControlContainer != null) {
            // Only show saturation slider in Color mode
            saturationControlContainer.setVisibility(showColorControls ? View.VISIBLE : View.GONE);
        }
    }

    private void setupControls() {
        // Brightness control
        if (seekBarBrightness != null) {
            seekBarBrightness.setMax(100);
            seekBarBrightness.setProgress(85);
            if (textViewBrightness != null) {
                textViewBrightness.setText("85%");
            }
            seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (textViewBrightness != null) {
                        textViewBrightness.setText(progress + "%");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isUpdatingBrightness = false;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int brightness = seekBar.getProgress();
                    android.util.Log.d("LampControl", "Brightness changed to: " + brightness);
                    updateLampBrightness(brightness);
                }
            });
        }

        // Temperature control (for white mode) - Three-quarter circle arc control (original design)
        if (temperatureArcView != null) {
            // Keep three-quarter circle mode for lamp white mode (original design)
            temperatureArcView.setFullCircleMode(false);
            // Start at warm position (0% = warm yellow, selector on left side)
            temperatureArcView.setTemperature(0);
            temperatureArcView.setOnTemperatureChangeListener(new com.smarthome.iot.ui.views.TemperatureArcView.OnTemperatureChangeListener() {
                @Override
                public void onTemperatureChanged(float angle, int progress) {
                    // Progress: 0 = warm, 100 = cool
                    android.util.Log.d("LampControl", "Temperature changed to: " + progress + "% (angle: " + angle + ")");
                    updateLampTemperature(progress);
                }
            });
        }

        // Color wheel control (for color mode) - Full circle
        if (colorWheelView != null) {
            // Enable full circle mode for color wheel
            colorWheelView.setFullCircleMode(true);
            // Start at blue position (100% = blue segment, as shown in Figma)
            colorWheelView.setColor(100);
            colorWheelView.setOnColorChangeListener(new com.smarthome.iot.ui.views.ColorWheelView.OnColorChangeListener() {
                @Override
                public void onColorChanged(float angle, int progress) {
                    android.util.Log.d("LampControl", "Color changed to: " + progress + "% (angle: " + angle + ")");
                    updateLampColor(progress);
                }
            });
        }

        // Saturation/Warmth control (second slider)
        if (seekBarSaturation != null) {
            seekBarSaturation.setMax(100);
            seekBarSaturation.setProgress(64);
            if (textViewSaturation != null) {
                textViewSaturation.setText("64%");
            }
            seekBarSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (textViewSaturation != null) {
                        textViewSaturation.setText(progress + "%");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isUpdatingSaturation = false;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int saturation = seekBar.getProgress();
                    android.util.Log.d("LampControl", "Saturation changed to: " + saturation);
                    updateLampSaturation(saturation);
                }
            });
        }

    }

    private void updateLampBrightness(int brightness) {
        if (deviceId == null || deviceId == 0 || isUpdatingBrightness) {
            return;
        }

        if (authManager == null || !authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            return;
        }

        isUpdatingBrightness = true;
        Map<String, Object> lampSettings = new HashMap<>();
        lampSettings.put("brightness", brightness);
        lampSettings.put("mode", currentMode);

        Call<ApiResponse<Map<String, Object>>> call = apiService.controlLamp(deviceId, lampSettings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                isUpdatingBrightness = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("LampControl", "Brightness updated successfully: " + brightness);
                } else {
                    android.util.Log.w("LampControl", "Failed to update brightness via API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                isUpdatingBrightness = false;
                android.util.Log.e("LampControl", "Error updating brightness", t);
            }
        });
    }

    private void updateLampTemperature(int temperature) {
        if (deviceId == null || deviceId == 0) {
            return;
        }

        if (authManager == null || !authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            return;
        }

        Map<String, Object> lampSettings = new HashMap<>();
        lampSettings.put("temperature", temperature);
        lampSettings.put("mode", "white");

        Call<ApiResponse<Map<String, Object>>> call = apiService.controlLamp(deviceId, lampSettings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("LampControl", "Temperature updated successfully: " + temperature);
                } else {
                    android.util.Log.w("LampControl", "Failed to update temperature via API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                android.util.Log.e("LampControl", "Error updating temperature", t);
            }
        });
    }

    private void updateLampColor(int color) {
        if (deviceId == null || deviceId == 0) {
            return;
        }

        if (authManager == null || !authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            return;
        }

        Map<String, Object> lampSettings = new HashMap<>();
        lampSettings.put("color", color);
        lampSettings.put("mode", "color");

        Call<ApiResponse<Map<String, Object>>> call = apiService.controlLamp(deviceId, lampSettings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("LampControl", "Color updated successfully: " + color);
                } else {
                    android.util.Log.w("LampControl", "Failed to update color via API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                android.util.Log.e("LampControl", "Error updating color", t);
            }
        });
    }

    private void updateLampSaturation(int saturation) {
        if (deviceId == null || deviceId == 0 || isUpdatingSaturation) {
            return;
        }

        if (authManager == null || !authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            return;
        }

        isUpdatingSaturation = true;
        Map<String, Object> lampSettings = new HashMap<>();
        lampSettings.put("saturation", saturation);
        lampSettings.put("mode", currentMode);

        Call<ApiResponse<Map<String, Object>>> call = apiService.controlLamp(deviceId, lampSettings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                isUpdatingSaturation = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("LampControl", "Saturation updated successfully: " + saturation);
                } else {
                    android.util.Log.w("LampControl", "Failed to update saturation via API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                isUpdatingSaturation = false;
                android.util.Log.e("LampControl", "Error updating saturation", t);
            }
        });
    }

    /**
     * Load device state from backend and sync metadata to UI controls
     * Reads: brightness, color, temperature, saturation, mode from device metadata
     */
    private void loadDeviceState() {
        if (deviceId == null || deviceId == 0) {
            android.util.Log.w("LampControl", "Invalid device ID, cannot load state");
            loadMode("white"); // Fallback to default white mode
            return;
        }

        if (authManager == null || !authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            android.util.Log.d("LampControl", "Demo user or not logged in, using default values");
            loadMode("white"); // Fallback to default white mode
            return;
        }

        // Fetch device details from API
        Call<ApiResponse<Device>> call = apiService.getDeviceById(deviceId);
        call.enqueue(new Callback<ApiResponse<Device>>() {
            @Override
            public void onResponse(Call<ApiResponse<Device>> call, Response<ApiResponse<Device>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Device device = response.body().getData();
                    if (device != null && device.getMetadata() != null) {
                        Map<String, Object> metadata = device.getMetadata();
                        android.util.Log.d("LampControl", "Loaded device metadata: " + metadata);

                        // Sync mode first (default to "white" if not set)
                        String mode = "white"; // default
                        if (metadata.containsKey("mode")) {
                            Object modeObj = metadata.get("mode");
                            if (modeObj instanceof String) {
                                mode = (String) modeObj;
                            }
                        }
                        currentMode = mode;
                        loadMode(mode);

                        // Sync brightness (0-100)
                        if (metadata.containsKey("brightness") && seekBarBrightness != null) {
                            Object brightnessObj = metadata.get("brightness");
                            if (brightnessObj instanceof Number) {
                                int brightness = ((Number) brightnessObj).intValue();
                                seekBarBrightness.setProgress(brightness);
                                if (textViewBrightness != null) {
                                    textViewBrightness.setText(brightness + "%");
                                }
                                android.util.Log.d("LampControl", "Synced brightness: " + brightness);
                            }
                        }

                        // Sync temperature (0-100, for white mode)
                        if (metadata.containsKey("temperature") && temperatureArcView != null) {
                            Object tempObj = metadata.get("temperature");
                            if (tempObj instanceof Number) {
                                int temperature = ((Number) tempObj).intValue();
                                temperatureArcView.setTemperature(temperature);
                                android.util.Log.d("LampControl", "Synced temperature: " + temperature);
                            }
                        }

                        // Sync color (0-100, for color mode)
                        if (metadata.containsKey("color") && colorWheelView != null) {
                            Object colorObj = metadata.get("color");
                            if (colorObj instanceof Number) {
                                int color = ((Number) colorObj).intValue();
                                colorWheelView.setColor(color);
                                android.util.Log.d("LampControl", "Synced color: " + color);
                            }
                        }

                        // Sync saturation (0-100)
                        if (metadata.containsKey("saturation") && seekBarSaturation != null) {
                            Object saturationObj = metadata.get("saturation");
                            if (saturationObj instanceof Number) {
                                int saturation = ((Number) saturationObj).intValue();
                                seekBarSaturation.setProgress(saturation);
                                if (textViewSaturation != null) {
                                    textViewSaturation.setText(saturation + "%");
                                }
                                android.util.Log.d("LampControl", "Synced saturation: " + saturation);
                            }
                        }
                    } else {
                        android.util.Log.w("LampControl", "Device has no metadata, using defaults");
                        loadMode("white"); // Fallback to default white mode
                    }
                } else {
                    android.util.Log.w("LampControl", "Failed to load device state from API");
                    loadMode("white"); // Fallback to default white mode
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Device>> call, Throwable t) {
                android.util.Log.e("LampControl", "Error loading device state", t);
                loadMode("white"); // Fallback to default white mode
            }
        });
    }

}
