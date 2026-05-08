package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.SceneCondition;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class DeviceStatusConditionActivity extends AppCompatActivity {
    private Spinner spinnerDevice;
    private MaterialButton buttonOnline;
    private MaterialButton buttonOffline;
    private MaterialButton buttonContinue;

    private String selectedStatus = "online";
    private List<Device> devices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_status_condition);

        setStatusBarColor();
        initializeViews();
        loadDevices();
        setupClickListeners();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        spinnerDevice = findViewById(R.id.spinnerDevice);
        buttonOnline = findViewById(R.id.buttonOnline);
        buttonOffline = findViewById(R.id.buttonOffline);
        buttonContinue = findViewById(R.id.buttonContinue);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void loadDevices() {
        devices = Globals.getCachedDevices();
        if (devices == null || devices.isEmpty()) {
            Toast.makeText(this, R.string.no_devices_available, Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> deviceNames = new ArrayList<>();
        for (Device device : devices) {
            deviceNames.add(device.getName() != null ? device.getName() : "Device " + device.getId());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, deviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDevice.setAdapter(adapter);
    }

    private void setupClickListeners() {
        buttonOnline.setOnClickListener(v -> {
            selectedStatus = "online";
            updateStatusButtons();
        });

        buttonOffline.setOnClickListener(v -> {
            selectedStatus = "offline";
            updateStatusButtons();
        });

        buttonContinue.setOnClickListener(v -> {
            if (devices == null || devices.isEmpty()) {
                Toast.makeText(this, R.string.no_devices_available, Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedIndex = spinnerDevice.getSelectedItemPosition();
            if (selectedIndex < 0 || selectedIndex >= devices.size()) {
                Toast.makeText(this, R.string.select_device, Toast.LENGTH_SHORT).show();
                return;
            }

            Device selectedDevice = devices.get(selectedIndex);

            SceneCondition condition = new SceneCondition();
            condition.setType("device_status");
            condition.setDeviceId(String.valueOf(selectedDevice.getId()));
            condition.setDeviceStatus(selectedStatus);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("condition_type", "device_status");
            resultIntent.putExtra("condition_data", condition);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void updateStatusButtons() {
        buttonOnline.setBackgroundTintList(ContextCompat.getColorStateList(this,
                selectedStatus.equals("online") ? R.color.primary : R.color.dark_5));
        buttonOnline.setTextColor(ContextCompat.getColor(this,
                selectedStatus.equals("online") ? R.color.white : R.color.white_alpha_70));

        buttonOffline.setBackgroundTintList(ContextCompat.getColorStateList(this,
                selectedStatus.equals("offline") ? R.color.primary : R.color.dark_5));
        buttonOffline.setTextColor(ContextCompat.getColor(this,
                selectedStatus.equals("offline") ? R.color.white : R.color.white_alpha_70));
    }
}
