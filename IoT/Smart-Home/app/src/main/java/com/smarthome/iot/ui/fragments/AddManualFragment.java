package com.smarthome.iot.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.DeviceType;
import com.smarthome.iot.ui.DeviceDetectedActivity;
import com.smarthome.iot.ui.adapters.DeviceTypeAdapter;

import java.util.ArrayList;
import java.util.List;

public class AddManualFragment extends Fragment {

    private static final String TAG = "AddManualFragment";
    
    private RecyclerView recyclerViewDevices;
    private TextView buttonPopular, buttonLightning, buttonCamera, buttonElectronics;
    private DeviceTypeAdapter deviceAdapter;
    private List<DeviceType> deviceList;
    private int selectedCategory = 0; // 0: Popular, 1: Lightning, 2: Camera, 3: Electronics

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_manual, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "onViewCreated called");

        recyclerViewDevices = view.findViewById(R.id.recyclerViewDevices);
        buttonPopular = view.findViewById(R.id.buttonPopular);
        buttonLightning = view.findViewById(R.id.buttonLightning);
        buttonCamera = view.findViewById(R.id.buttonCamera);
        buttonElectronics = view.findViewById(R.id.buttonElectronics);

        if (recyclerViewDevices == null) {
            Log.e(TAG, "RecyclerView is null!");
            return;
        }

        setupRecyclerView();
        setupCategoryButtons();
        loadDevices();
    }

    private void setupCategoryButtons() {
        // Set Popular as selected by default
        updateCategorySelection(0);

        buttonPopular.setOnClickListener(v -> {
            selectedCategory = 0;
            updateCategorySelection(0);
            loadDevices();
        });

        buttonLightning.setOnClickListener(v -> {
            selectedCategory = 1;
            updateCategorySelection(1);
            loadDevices();
        });

        buttonCamera.setOnClickListener(v -> {
            selectedCategory = 2;
            updateCategorySelection(2);
            loadDevices();
        });

        buttonElectronics.setOnClickListener(v -> {
            selectedCategory = 3;
            updateCategorySelection(3);
            loadDevices();
        });
    }

    private void updateCategorySelection(int selected) {
        // Reset all buttons to unselected
        buttonPopular.setBackgroundResource(R.drawable.category_button_unselected);
        buttonLightning.setBackgroundResource(R.drawable.category_button_unselected);
        buttonCamera.setBackgroundResource(R.drawable.category_button_unselected);
        buttonElectronics.setBackgroundResource(R.drawable.category_button_unselected);

        // Set selected button
        switch (selected) {
            case 0:
                buttonPopular.setBackgroundResource(R.drawable.category_button_selected);
                break;
            case 1:
                buttonLightning.setBackgroundResource(R.drawable.category_button_selected);
                break;
            case 2:
                buttonCamera.setBackgroundResource(R.drawable.category_button_selected);
                break;
            case 3:
                buttonElectronics.setBackgroundResource(R.drawable.category_button_selected);
                break;
        }
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView called");
        
        // Initialize device list
        deviceList = new ArrayList<>();
        
        // Create adapter with the list
        deviceAdapter = new DeviceTypeAdapter(deviceList);
        
        // Set click listener
        deviceAdapter.setOnDeviceClickListener(device -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), DeviceDetectedActivity.class);
                intent.putExtra("device_name", device.getName());
                intent.putExtra("connection_source", "manual"); // Flag for manual connection
                startActivity(intent);
            }
        });

        // Setup GridLayoutManager with 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerViewDevices.setLayoutManager(layoutManager);
        recyclerViewDevices.setAdapter(deviceAdapter);
        recyclerViewDevices.setHasFixedSize(false);
        
        Log.d(TAG, "RecyclerView setup complete");
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
        } else if (normalizedName.contains("camera") && !normalizedName.contains("cctv")) {
            return R.drawable.camera;
        } else if (normalizedName.contains("speaker")) {
            return R.drawable.speaker;
        } else if (normalizedName.contains("router") || normalizedName.contains("wifi") || normalizedName.contains("wi-fi")) {
            return R.drawable.router;
        } else if (normalizedName.contains("webcam")) {
            return R.drawable.webcam;
        } else if (normalizedName.contains("air") || normalizedName.contains("conditioner") || normalizedName.contains("ac")) {
            return R.drawable.air_conditioner;
        } else if (normalizedName.contains("compact")) {
            return R.drawable.compact;
        } else {
            return R.drawable.ic_device;
        }
    }

    private void loadDevices() {
        Log.d(TAG, "loadDevices called, category: " + selectedCategory);
        
        if (deviceList == null) {
            deviceList = new ArrayList<>();
        }
        if (deviceAdapter == null) {
            setupRecyclerView();
        }
        
        // Clear existing devices
        deviceList.clear();
        
        // Add devices based on selected category
        switch (selectedCategory) {
            case 0: // Popular - 8 devices matching Figma
                deviceList.add(new DeviceType(getString(R.string.smart_lamp), getDeviceIconResId(getString(R.string.smart_lamp)), "Popular"));
                deviceList.add(new DeviceType(getString(R.string.smart_v2_cctv), getDeviceIconResId(getString(R.string.smart_v2_cctv)), "Popular"));
                deviceList.add(new DeviceType(getString(R.string.stereo_speaker), getDeviceIconResId(getString(R.string.stereo_speaker)), "Popular"));
                deviceList.add(new DeviceType(getString(R.string.wifi_router), getDeviceIconResId(getString(R.string.wifi_router)), "Popular"));
                deviceList.add(new DeviceType(getString(R.string.smart_webcam), getDeviceIconResId(getString(R.string.smart_webcam)), "Popular"));
                deviceList.add(new DeviceType(getString(R.string.smart_plug), getDeviceIconResId(getString(R.string.smart_plug)), "Popular"));
                deviceList.add(new DeviceType(getString(R.string.smart_thermostat), getDeviceIconResId(getString(R.string.smart_thermostat)), "Popular"));
                deviceList.add(new DeviceType(getString(R.string.doorbell_camera), getDeviceIconResId(getString(R.string.doorbell_camera)), "Popular"));
                break;
                
            case 1: // Lightning
                deviceList.add(new DeviceType(getString(R.string.smart_lamp), getDeviceIconResId(getString(R.string.smart_lamp)), "Lightning"));
                deviceList.add(new DeviceType(getString(R.string.smart_bulb), getDeviceIconResId(getString(R.string.smart_bulb)), "Lightning"));
                deviceList.add(new DeviceType(getString(R.string.led_strip), getDeviceIconResId(getString(R.string.led_strip)), "Lightning"));
                deviceList.add(new DeviceType(getString(R.string.smart_switch), getDeviceIconResId(getString(R.string.smart_switch)), "Lightning"));
                deviceList.add(new DeviceType(getString(R.string.smart_dimmer), getDeviceIconResId(getString(R.string.smart_dimmer)), "Lightning"));
                deviceList.add(new DeviceType(getString(R.string.smart_plug), getDeviceIconResId(getString(R.string.smart_plug)), "Lightning"));
                break;
                
            case 2: // Camera
                deviceList.add(new DeviceType(getString(R.string.smart_v1_cctv), getDeviceIconResId(getString(R.string.smart_v1_cctv)), "Camera"));
                deviceList.add(new DeviceType(getString(R.string.smart_webcam), getDeviceIconResId(getString(R.string.smart_webcam)), "Camera"));
                deviceList.add(new DeviceType(getString(R.string.smart_v2_cctv), getDeviceIconResId(getString(R.string.smart_v2_cctv)), "Camera"));
                deviceList.add(new DeviceType(getString(R.string.doorbell_camera), getDeviceIconResId(getString(R.string.doorbell_camera)), "Camera"));
                deviceList.add(new DeviceType(getString(R.string.security_camera), getDeviceIconResId(getString(R.string.security_camera)), "Camera"));
                deviceList.add(new DeviceType(getString(R.string.baby_monitor), getDeviceIconResId(getString(R.string.baby_monitor)), "Camera"));
                break;
                
            case 3: // Electronics
                deviceList.add(new DeviceType(getString(R.string.stereo_speaker), getDeviceIconResId(getString(R.string.stereo_speaker)), "Electronics"));
                deviceList.add(new DeviceType(getString(R.string.wifi_router), getDeviceIconResId(getString(R.string.wifi_router)), "Electronics"));
                deviceList.add(new DeviceType(getString(R.string.smart_tv), getDeviceIconResId(getString(R.string.smart_tv)), "Electronics"));
                deviceList.add(new DeviceType(getString(R.string.smart_refrigerator), getDeviceIconResId(getString(R.string.smart_refrigerator)), "Electronics"));
                deviceList.add(new DeviceType(getString(R.string.smart_ac), getDeviceIconResId(getString(R.string.smart_ac)), "Electronics"));
                deviceList.add(new DeviceType(getString(R.string.smart_fan), getDeviceIconResId(getString(R.string.smart_fan)), "Electronics"));
                break;
        }
        
        Log.d(TAG, "Device list size: " + deviceList.size());
        
        // Update adapter
        if (deviceAdapter != null) {
            deviceAdapter.updateDevices(new ArrayList<>(deviceList));
            Log.d(TAG, "Adapter updated with " + deviceList.size() + " devices");
        } else {
            Log.e(TAG, "Adapter is null!");
        }
    }
}
