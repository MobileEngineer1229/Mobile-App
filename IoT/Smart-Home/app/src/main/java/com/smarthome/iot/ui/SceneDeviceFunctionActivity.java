package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.utils.DeviceHelper;
import com.smarthome.iot.utils.ThemeHelper;

public class SceneDeviceFunctionActivity extends AppCompatActivity {
    private ImageView imageViewDevice;
    private TextView textViewDeviceName;
    private TextView textViewDeviceLocation;
    private RadioGroup radioGroupFunction;
    private RadioButton radioON;
    private RadioButton radioOFF;
    private MaterialButton buttonOK;

    private int deviceId;
    private String deviceName;
    private String deviceType;
    private String roomName;
    private String selectedFunction = "ON";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_device_function);

        setStatusBarColor();

        // Get device info from intent
        deviceId = getIntent().getIntExtra("device_id", 0);
        deviceName = getIntent().getStringExtra("device_name");
        deviceType = getIntent().getStringExtra("device_type");
        roomName = getIntent().getStringExtra("room_name");

        initializeViews();
        setupDeviceIcon();
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
        imageViewDevice = findViewById(R.id.imageViewDevice);
        textViewDeviceName = findViewById(R.id.textViewDeviceName);
        textViewDeviceLocation = findViewById(R.id.textViewDeviceLocation);
        radioGroupFunction = findViewById(R.id.radioGroupFunction);
        radioON = findViewById(R.id.radioON);
        radioOFF = findViewById(R.id.radioOFF);
        buttonOK = findViewById(R.id.buttonOK);

        buttonBack.setOnClickListener(v -> finish());

        // Set device info
        if (deviceName != null) {
            textViewDeviceName.setText(deviceName);
        }
        if (roomName != null) {
            textViewDeviceLocation.setText(roomName);
        }
    }

    private void setupDeviceIcon() {
        if (imageViewDevice != null) {
            // Get the appropriate icon based on device name and type
            int iconRes = DeviceHelper.getDeviceIcon(this, deviceName, deviceType);
            imageViewDevice.setImageResource(iconRes);
        }
    }

    private void setupClickListeners() {
        radioGroupFunction.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioON) {
                selectedFunction = "ON";
            } else if (checkedId == R.id.radioOFF) {
                selectedFunction = "OFF";
            }
        });
        
        buttonOK.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("device_id", deviceId);
            resultIntent.putExtra("device_name", deviceName);
            resultIntent.putExtra("room_name", roomName);
            resultIntent.putExtra("function", selectedFunction);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
