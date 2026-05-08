package com.smarthome.iot.ui.fragments.devicecontrol;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.views.TemperatureArcView;
import com.smarthome.iot.utils.Globals;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment for controlling Air Conditioner
 * Supports temperature control, mode selection, and various AC features
 */
public class AirConditionerControlFragment extends Fragment {
    private static final String ARG_DEVICE_ID = "device_id";
    private static final String ARG_DEVICE_NAME = "device_name";
    private static final String ARG_ROOM_ID = "room_id";

    private Integer deviceId;
    private String deviceName;
    private Integer roomId;

    private MaterialButton buttonCooling;
    private MaterialButton buttonHeating;
    private MaterialButton buttonPurifying;
    private String currentMode = "cooling";
    private TemperatureArcView temperatureArcView;
    private TextView textViewTemperature;
    private MaterialButton buttonMode;
    private MaterialButton buttonWindSpeed;
    private MaterialButton buttonWindDirection;
    private MaterialButton buttonPrecisionAir;
    private MaterialButton buttonEco;
    private MaterialButton buttonSleep;
    private MaterialButton buttonTimer;
    private MaterialButton buttonMore;

    private ApiService apiService;
    private int currentTemperature = 20; // Default 20°C

    public static AirConditionerControlFragment newInstance(Integer deviceId, String deviceName, Integer roomId) {
        AirConditionerControlFragment fragment = new AirConditionerControlFragment();
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
        
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ac_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupModeButtons();
        setupControls();
        // Initialize mode buttons state first
        updateModeButtons();
        loadDeviceState();
    }

    private void initializeViews(View view) {
        buttonCooling = view.findViewById(R.id.buttonCooling);
        buttonHeating = view.findViewById(R.id.buttonHeating);
        buttonPurifying = view.findViewById(R.id.buttonPurifying);
        temperatureArcView = view.findViewById(R.id.temperatureArcView);
        textViewTemperature = view.findViewById(R.id.textViewTemperature);
        buttonMode = view.findViewById(R.id.buttonMode);
        buttonWindSpeed = view.findViewById(R.id.buttonWindSpeed);
        buttonWindDirection = view.findViewById(R.id.buttonWindDirection);
        buttonPrecisionAir = view.findViewById(R.id.buttonPrecisionAir);
        buttonEco = view.findViewById(R.id.buttonEco);
        buttonSleep = view.findViewById(R.id.buttonSleep);
        buttonTimer = view.findViewById(R.id.buttonTimer);
        buttonMore = view.findViewById(R.id.buttonMore);
    }

    private void setupModeButtons() {
        buttonCooling.setOnClickListener(v -> changeMode("cooling"));
        buttonHeating.setOnClickListener(v -> changeMode("heating"));
        buttonPurifying.setOnClickListener(v -> changeMode("purifying"));
    }

    private void changeMode(String mode) {
        currentMode = mode;
        updateModeButtons();
        
        // Update mode via API
        Map<String, Object> settings = new HashMap<>();
        settings.put("mode", mode);
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlAC(deviceId, settings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(getContext(), "Mode changed to " + mode, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to change mode", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateModeButtons() {
        // Use new tab button style - works like a switch: one selected, one unselected
        // Selected: Primary color background, Unselected: Dark background
        // Clear background tint to ensure our drawables are used (no stroke)
        
        // Update all buttons
        buttonCooling.setBackgroundTintList(null);
        buttonCooling.setElevation(0f);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            buttonCooling.setStateListAnimator(null);
        }
        buttonCooling.post(() -> {
            if ("cooling".equals(currentMode.toLowerCase())) {
                buttonCooling.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background));
            } else {
                buttonCooling.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.tab_unselected_background));
            }
            buttonCooling.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            buttonCooling.invalidate();
        });
        
        buttonHeating.setBackgroundTintList(null);
        buttonHeating.setElevation(0f);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            buttonHeating.setStateListAnimator(null);
        }
        buttonHeating.post(() -> {
            if ("heating".equals(currentMode.toLowerCase())) {
                buttonHeating.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background));
            } else {
                buttonHeating.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.tab_unselected_background));
            }
            buttonHeating.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            buttonHeating.invalidate();
        });
        
        buttonPurifying.setBackgroundTintList(null);
        buttonPurifying.setElevation(0f);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            buttonPurifying.setStateListAnimator(null);
        }
        buttonPurifying.post(() -> {
            if ("purifying".equals(currentMode.toLowerCase())) {
                buttonPurifying.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background));
            } else {
                buttonPurifying.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.tab_unselected_background));
            }
            buttonPurifying.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            buttonPurifying.invalidate();
        });
    }

    private void setupControls() {
        // Temperature arc control - set to three-quarter circle mode with AC blue color
        if (temperatureArcView != null) {
            temperatureArcView.setFullCircleMode(false); // Three-quarter circle mode (same as lamp white mode)
            temperatureArcView.setFullCircleType(true); // AC mode: solid blue color (#405FF2)
            // Temperature range: 16°C to 30°C (0-100 progress maps to 16-30)
            // Default 20°C = (20-16)/(30-16) * 100 = 4/14 * 100 ≈ 29%
            int defaultProgress = (int) ((20 - 16) / 14.0 * 100);
            temperatureArcView.setTemperature(defaultProgress);
            temperatureArcView.setOnTemperatureChangeListener((angle, progress) -> {
                // Map progress (0-100) to temperature (16-30)
                int temperature = 16 + (progress * 14 / 100);
                currentTemperature = temperature;
                if (textViewTemperature != null) {
                    textViewTemperature.setText(temperature + "°C");
                }
                // Update temperature via API
                updateTemperature(temperature);
            });
        }

        if (textViewTemperature != null) {
            textViewTemperature.setText(currentTemperature + "°C");
        }

        // Quick control buttons
        if (buttonMode != null) {
            buttonMode.setOnClickListener(v -> {
                // TODO: Show mode selection dialog
                Toast.makeText(getContext(), "Mode selection coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        if (buttonWindSpeed != null) {
            buttonWindSpeed.setOnClickListener(v -> {
                // TODO: Show wind speed selection
                Toast.makeText(getContext(), "Wind speed selection coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        if (buttonWindDirection != null) {
            buttonWindDirection.setOnClickListener(v -> {
                // TODO: Show wind direction selection
                Toast.makeText(getContext(), "Wind direction selection coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        if (buttonPrecisionAir != null) {
            buttonPrecisionAir.setOnClickListener(v -> {
                // TODO: Toggle precision air
                Toast.makeText(getContext(), "Precision air toggle coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        if (buttonEco != null) {
            buttonEco.setOnClickListener(v -> toggleEcoMode());
        }

        if (buttonSleep != null) {
            buttonSleep.setOnClickListener(v -> toggleSleepMode());
        }

        if (buttonTimer != null) {
            buttonTimer.setOnClickListener(v -> {
                // TODO: Show timer dialog
                Toast.makeText(getContext(), "Timer selection coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        if (buttonMore != null) {
            buttonMore.setOnClickListener(v -> {
                // TODO: Show more options
                Toast.makeText(getContext(), "More options coming soon", Toast.LENGTH_SHORT).show();
            });
        }

    }

    private void updateTemperature(int temperature) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("temperature", temperature);
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlAC(deviceId, settings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Temperature updated successfully
                } else {
                    Toast.makeText(getContext(), "Failed to update temperature", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleEcoMode() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("eco", true); // Toggle state would need to be tracked
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlAC(deviceId, settings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(getContext(), "Eco mode toggled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to toggle eco mode", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleSleepMode() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("sleep", true); // Toggle state would need to be tracked
        
        Call<ApiResponse<Map<String, Object>>> call = apiService.controlAC(deviceId, settings);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(getContext(), "Sleep mode toggled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to toggle sleep mode", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Load device state from backend metadata and sync to UI controls
     * Reads: temperature, mode from device metadata
     */
    private void loadDeviceState() {
        if (deviceId == null || deviceId == 0) {
            android.util.Log.w("ACControl", "Invalid device ID, cannot load state");
            return;
        }

        // Fetch device details from API to get metadata
        Call<ApiResponse<Device>> call = apiService.getDeviceById(deviceId);
        call.enqueue(new Callback<ApiResponse<Device>>() {
            @Override
            public void onResponse(Call<ApiResponse<Device>> call, Response<ApiResponse<Device>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Device device = response.body().getData();
                    if (device != null && device.getMetadata() != null) {
                        Map<String, Object> metadata = device.getMetadata();
                        android.util.Log.d("ACControl", "Loaded device metadata: " + metadata);

                        // Sync temperature from metadata (16-30°C range)
                        if (metadata.containsKey("temperature")) {
                            Object tempObj = metadata.get("temperature");
                            if (tempObj instanceof Number) {
                                currentTemperature = ((Number) tempObj).intValue();
                                if (textViewTemperature != null) {
                                    textViewTemperature.setText(currentTemperature + "°C");
                                }
                                // Map temperature (16-30) to progress (0-100)
                                int progress = ((currentTemperature - 16) * 100) / 14;
                                if (temperatureArcView != null) {
                                    temperatureArcView.setTemperature(progress);
                                }
                                android.util.Log.d("ACControl", "Synced temperature: " + currentTemperature + "°C");
                            }
                        } else {
                            // Set default temperature display
                            if (textViewTemperature != null) {
                                textViewTemperature.setText(currentTemperature + "°C");
                            }
                        }

                        // Sync mode from metadata (cooling, heating, purifying)
                        if (metadata.containsKey("mode")) {
                            Object modeObj = metadata.get("mode");
                            if (modeObj instanceof String) {
                                String mode = (String) modeObj;
                                currentMode = mode;
                                updateModeButtons();
                                android.util.Log.d("ACControl", "Synced mode: " + mode);
                            }
                        }
                    } else {
                        android.util.Log.w("ACControl", "Device has no metadata, using defaults");
                        if (textViewTemperature != null) {
                            textViewTemperature.setText(currentTemperature + "°C");
                        }
                    }
                } else {
                    android.util.Log.w("ACControl", "Failed to load device from API");
                    if (textViewTemperature != null) {
                        textViewTemperature.setText(currentTemperature + "°C");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Device>> call, Throwable t) {
                android.util.Log.e("ACControl", "Error loading device", t);
                if (textViewTemperature != null) {
                    textViewTemperature.setText(currentTemperature + "°C");
                }
            }
        });
    }

}
