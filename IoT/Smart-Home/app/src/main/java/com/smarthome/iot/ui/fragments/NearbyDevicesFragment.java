package com.smarthome.iot.ui.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.DiscoveredDevice;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.DeviceDetectedActivity;
import com.smarthome.iot.ui.adapters.DiscoveredDeviceAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.DeviceHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NearbyDevicesFragment extends Fragment {

    private MaterialButton buttonConnectAll;
    private TextView textViewLearnMore;
    private FrameLayout scanningContainer;
    private ImageView imageViewProfile;
    private ImageView deviceIcon1, deviceIcon2, deviceIcon3, deviceIcon4, deviceIcon5;
    private List<DiscoveredDevice> discoveredDevices;
    private Handler discoveryHandler;
    private ApiService apiService;
    private AuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby_devices, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize API
        ApiClient.initialize(requireContext());
        apiService = ApiClient.getClient().create(ApiService.class);
        authManager = new AuthManager(requireContext());

        buttonConnectAll = view.findViewById(R.id.buttonConnectAll);
        textViewLearnMore = view.findViewById(R.id.textViewLearnMore);
        scanningContainer = view.findViewById(R.id.scanningContainer);
        imageViewProfile = view.findViewById(R.id.imageViewProfile);
        deviceIcon1 = view.findViewById(R.id.deviceIcon1);
        deviceIcon2 = view.findViewById(R.id.deviceIcon2);
        deviceIcon3 = view.findViewById(R.id.deviceIcon3);
        deviceIcon4 = view.findViewById(R.id.deviceIcon4);
        deviceIcon5 = view.findViewById(R.id.deviceIcon5);

        discoveredDevices = new ArrayList<>();
        discoveryHandler = new Handler(Looper.getMainLooper());
        
        // Start finding nearby devices
        findNearbyDevices();
        setupClickListeners();
    }

    /**
     * Get drawable resource ID for device based on device name
     */
    private int getDeviceIconResId(String deviceName) {
        if (deviceName == null) {
            return R.drawable.ic_device;
        }
        
        // Normalize device name for comparison
        String normalizedName = deviceName.toLowerCase();
        
        if (normalizedName.contains("lamp") || normalizedName.contains("light")) {
            return R.drawable.lamp;
        } else if (normalizedName.contains("v1 cctv") || normalizedName.contains("smart v1")) {
            return R.drawable.cctv_v1;
        } else if (normalizedName.contains("v2 cctv") || normalizedName.contains("smart v2")) {
            return R.drawable.cctv_v2;
        } else if (normalizedName.contains("speaker")) {
            return R.drawable.speaker;
        } else if (normalizedName.contains("router") || normalizedName.contains("wifi") || normalizedName.contains("wi-fi")) {
            return R.drawable.router;
        } else if (normalizedName.contains("webcam")) {
            return R.drawable.webcam;
        } else if (normalizedName.contains("air") || normalizedName.contains("conditioner") || normalizedName.contains("ac")) {
            return R.drawable.air_conditioner;
        } else {
            return R.drawable.ic_device;
        }
    }

    /**
     * Find nearby devices and display them on the UI
     * Calls API to discover devices, falls back to mock data if API fails
     */
    private void findNearbyDevices() {
        // Clear existing devices
        discoveredDevices.clear();
        hideAllDeviceIcons();

        if (!authManager.isLoggedIn() || apiService == null) {
            // Fallback to mock data
            loadMockDevices();
            return;
        }

        // Call API to discover devices
        Call<ApiResponse<Object>> call = apiService.discoverDevices();
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Object data = response.body().getData();
                    if (data != null) {
                        // Parse the response data
                        parseDiscoveredDevices(data);
                    } else {
                        // No devices found, use mock data
                        loadMockDevices();
                    }
                } else {
                    // API call failed, use mock data
                    loadMockDevices();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                android.util.Log.e("NearbyDevices", "Error discovering devices", t);
                // Fallback to mock data
                loadMockDevices();
            }
        });
    }

    /**
     * Parse discovered devices from API response
     * Handles both List<Device> and custom response structures
     */
    private void parseDiscoveredDevices(Object data) {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(data);
            JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

            List<DiscoveredDevice> parsedDevices = new ArrayList<>();

            if (jsonElement.isJsonArray()) {
                // Response is an array of devices
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                for (JsonElement element : jsonArray) {
                    if (element.isJsonObject()) {
                        JsonObject deviceObj = element.getAsJsonObject();
                        DiscoveredDevice device = parseDeviceFromJson(deviceObj);
                        if (device != null) {
                            parsedDevices.add(device);
                        }
                    }
                }
            } else if (jsonElement.isJsonObject()) {
                // Response might have a "devices" field
                JsonObject jsonObj = jsonElement.getAsJsonObject();
                if (jsonObj.has("devices") && jsonObj.get("devices").isJsonArray()) {
                    JsonArray devicesArray = jsonObj.getAsJsonArray("devices");
                    for (JsonElement element : devicesArray) {
                        if (element.isJsonObject()) {
                            JsonObject deviceObj = element.getAsJsonObject();
                            DiscoveredDevice device = parseDeviceFromJson(deviceObj);
                            if (device != null) {
                                parsedDevices.add(device);
                            }
                        }
                    }
                } else {
                    // Try to parse as a single device object
                    DiscoveredDevice device = parseDeviceFromJson(jsonObj);
                    if (device != null) {
                        parsedDevices.add(device);
                    }
                }
            }

            // Display discovered devices with animation delays
            if (!parsedDevices.isEmpty()) {
                displayDiscoveredDevices(parsedDevices);
            } else {
                // No devices found, use mock data
                loadMockDevices();
            }
        } catch (Exception e) {
            android.util.Log.e("NearbyDevices", "Error parsing discovered devices", e);
            // Fallback to mock data
            loadMockDevices();
        }
    }

    /**
     * Parse a single device from JSON object
     */
    private DiscoveredDevice parseDeviceFromJson(JsonObject deviceObj) {
        try {
            String name = null;
            String type = null;

            if (deviceObj.has("name")) {
                name = deviceObj.get("name").getAsString();
            } else if (deviceObj.has("deviceName")) {
                name = deviceObj.get("deviceName").getAsString();
            }

            if (deviceObj.has("type")) {
                type = deviceObj.get("type").getAsString();
            } else if (deviceObj.has("deviceType")) {
                type = deviceObj.get("deviceType").getAsString();
            }

            if (name != null) {
                int iconResId = getDeviceIconResId(name);
                String category = type != null ? DeviceHelper.getDeviceCategoryFromName(name) : "Unknown";
                return new DiscoveredDevice(name, category, iconResId);
            }
        } catch (Exception e) {
            android.util.Log.e("NearbyDevices", "Error parsing device from JSON", e);
        }
        return null;
    }

    /**
     * Display discovered devices with animation delays
     */
    private void displayDiscoveredDevices(List<DiscoveredDevice> devices) {
        ImageView[] iconViews = {deviceIcon1, deviceIcon2, deviceIcon3, deviceIcon4, deviceIcon5};
        int delay = 2000; // Start delay

        for (int i = 0; i < Math.min(devices.size(), iconViews.length); i++) {
            DiscoveredDevice device = devices.get(i);
            ImageView iconView = iconViews[i];
            int iconResId = device.getIconResId();

            final int index = i;
            discoveryHandler.postDelayed(() -> {
                discoveredDevices.add(device);
                showDeviceIcon(iconView, iconResId, device);
            }, delay);

            delay += 500; // Increment delay for next device
        }
    }

    /**
     * Load mock devices as fallback
     */
    private void loadMockDevices() {
        // Device 1: Top - Smart Light Bulb
        discoveryHandler.postDelayed(() -> {
            String deviceName = "Smart Lamp";
            int iconResId = getDeviceIconResId(deviceName);
            DiscoveredDevice device = new DiscoveredDevice(deviceName, "Lighting", iconResId);
            discoveredDevices.add(device);
            showDeviceIcon(deviceIcon1, iconResId, device);
        }, 2000);

        // Device 2: Top Right - Security Camera
        discoveryHandler.postDelayed(() -> {
            String deviceName = "Smart V1 CCTV";
            int iconResId = getDeviceIconResId(deviceName);
            DiscoveredDevice device = new DiscoveredDevice(deviceName, "Camera", iconResId);
            discoveredDevices.add(device);
            showDeviceIcon(deviceIcon2, iconResId, device);
        }, 2500);

        // Device 3: Bottom Right - Wi-Fi Router
        discoveryHandler.postDelayed(() -> {
            String deviceName = "Wi-Fi Router";
            int iconResId = getDeviceIconResId(deviceName);
            DiscoveredDevice device = new DiscoveredDevice(deviceName, "Network", iconResId);
            discoveredDevices.add(device);
            showDeviceIcon(deviceIcon3, iconResId, device);
        }, 3000);

        // Device 4: Bottom Center - Air Conditioner
        discoveryHandler.postDelayed(() -> {
            String deviceName = "Air Conditioner";
            int iconResId = getDeviceIconResId(deviceName);
            DiscoveredDevice device = new DiscoveredDevice(deviceName, "Climate", iconResId);
            discoveredDevices.add(device);
            showDeviceIcon(deviceIcon4, iconResId, device);
        }, 3500);

        // Device 5: Middle Left - Smart Speaker
        discoveryHandler.postDelayed(() -> {
            String deviceName = "Stereo Speaker";
            int iconResId = getDeviceIconResId(deviceName);
            DiscoveredDevice device = new DiscoveredDevice(deviceName, "Audio", iconResId);
            discoveredDevices.add(device);
            showDeviceIcon(deviceIcon5, iconResId, device);
        }, 4000);
    }

    /**
     * Show device icon with animation
     */
    private void showDeviceIcon(ImageView iconView, int iconResId, DiscoveredDevice device) {
        if (iconView == null) return;
        
        iconView.setImageResource(iconResId);
        iconView.setVisibility(View.VISIBLE);
        iconView.setAlpha(0f);
        iconView.setScaleX(0f);
        iconView.setScaleY(0f);

        // Animate icon appearance
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(iconView, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iconView, "scaleX", 0f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iconView, "scaleY", 0f, 1f);

        animatorSet.playTogether(fadeIn, scaleX, scaleY);
        animatorSet.setDuration(500);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();

        // Set click listener to navigate to device connection screen
        iconView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), DeviceDetectedActivity.class);
            intent.putExtra("device_name", device.getName());
            intent.putExtra("connection_source", "nearby"); // Flag for nearby connection
            startActivity(intent);
        });
    }

    /**
     * Hide all device icons
     */
    private void hideAllDeviceIcons() {
        if (deviceIcon1 != null) deviceIcon1.setVisibility(View.GONE);
        if (deviceIcon2 != null) deviceIcon2.setVisibility(View.GONE);
        if (deviceIcon3 != null) deviceIcon3.setVisibility(View.GONE);
        if (deviceIcon4 != null) deviceIcon4.setVisibility(View.GONE);
        if (deviceIcon5 != null) deviceIcon5.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        buttonConnectAll.setOnClickListener(v -> {
            if (!discoveredDevices.isEmpty()) {
                // Show device list dialog
                showDeviceListDialog();
            } else {
                // No devices found yet
                android.widget.Toast.makeText(getContext(), "No devices found. Please wait...", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        textViewLearnMore.setOnClickListener(v -> {
            // Show chatbot or help dialog
            android.widget.Toast.makeText(getContext(), "Learn more clicked", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeviceListDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_device_list, null);
        dialog.setContentView(dialogView);

        RecyclerView recyclerViewDevices = dialogView.findViewById(R.id.recyclerViewDevices);
        TextView textViewTitle = dialogView.findViewById(R.id.textViewTitle);
        MaterialButton buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        textViewTitle.setText("Select Device to Connect");

        // Setup RecyclerView
        DiscoveredDeviceAdapter adapter = new DiscoveredDeviceAdapter(discoveredDevices);
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewDevices.setAdapter(adapter);

        // Handle device selection
        adapter.setOnDeviceClickListener(device -> {
            dialog.dismiss();
            // Navigate to device connection screen
            Intent intent = new Intent(getContext(), DeviceDetectedActivity.class);
            intent.putExtra("device_name", device.getName());
            intent.putExtra("connection_source", "nearby"); // Flag for nearby connection
            startActivity(intent);
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up handler to prevent memory leaks
        if (discoveryHandler != null) {
            discoveryHandler.removeCallbacksAndMessages(null);
        }
    }
}
