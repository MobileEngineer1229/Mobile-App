package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.utils.ThemeHelper;

/**
 * Success screen shown after a device has been successfully registered via the backend API.
 *
 * This is the final screen in the device registration flow. It confirms that the device
 * was created in the backend database and is now bound to the user's home.
 *
 * === What "Connected" Means ===
 * - Device record created in PostgreSQL via POST /api/v1/devices
 * - Device assigned to user's primary home and first available room
 * - If MQTT device: bound to FastBee MQTT broker topic /fastbee/{productId}/{deviceNum}/#
 * - Device added to Globals in-memory cache for immediate UI availability
 * - MqttService can now publish commands and receive state updates for this device
 *
 * === UI Elements ===
 * - Blue checkmark circle: confirms successful connection
 * - "Connected!" title with device name confirmation message
 * - Device-specific image (speaker, lamp, camera, etc.)
 * - "Show QR Code" button: only visible for MQTT-bound devices, allows sharing
 *   the device's QR code with other users for easy re-registration
 * - "Go to Homepage" button: returns to MainActivity (clears activity stack)
 * - "Control Device" button: navigates to DeviceControlDetailActivity for real-time control
 *
 * === Intent Extras (Input) ===
 * - device_name: Human-readable device name
 * - device_id: Integer ID from backend API response (for DeviceControlDetailActivity)
 * - mqtt_product_id: FastBee productId (enables "Show QR Code" button)
 * - mqtt_device_num: FastBee deviceNum (for QR code generation)
 * - mqtt_device_type: Device type (for QR code metadata)
 *
 * @see DeviceDetectedActivity Previous step: device verification and API registration
 * @see DeviceQRCodeActivity QR code generation for MQTT devices
 * @see DeviceControlDetailActivity Real-time device control via MQTT commands
 */
public class DeviceConnectedActivity extends AppCompatActivity {

    private String deviceName;
    private Integer deviceId;
    private String mqttProductId;
    private String mqttDeviceNum;
    private String mqttDeviceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_connected);

        // Set status bar color to dark
        setStatusBarColor();

        deviceName = getIntent().getStringExtra("device_name");
        if (deviceName == null) {
            deviceName = "Smart VI CCTV";
        }
        
        // Get device ID from intent (if available from API response)
        if (getIntent().hasExtra("device_id")) {
            deviceId = getIntent().getIntExtra("device_id", -1);
            if (deviceId == -1) {
                deviceId = null;
            }
        }

        // Get MQTT metadata (passed through from DeviceDetectedActivity)
        mqttProductId = getIntent().getStringExtra("mqtt_product_id");
        mqttDeviceNum = getIntent().getStringExtra("mqtt_device_num");
        mqttDeviceType = getIntent().getStringExtra("mqtt_device_type");

        initializeViews();
        setupClickListeners();
        
        // Removed automatic navigation - user must click "Control Device" button
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        MaterialButton buttonGoHome = findViewById(R.id.buttonGoHome);
        MaterialButton buttonControlDevice = findViewById(R.id.buttonControlDevice);
        TextView textViewConnectedMessage = findViewById(R.id.textViewConnectedMessage);
        ImageView imageViewDevice = findViewById(R.id.imageViewDevice);

        textViewConnectedMessage.setText(getString(R.string.you_have_connected, deviceName));

        // Set device-specific image
        setupDeviceImage(imageViewDevice);

        buttonGoHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        buttonControlDevice.setOnClickListener(v -> {
            navigateToControlDevice();
        });

        // Show QR Code button — only visible for MQTT-bound devices
        MaterialButton buttonShowQR = findViewById(R.id.buttonShowQR);
        if (mqttProductId != null && mqttDeviceNum != null) {
            buttonShowQR.setVisibility(android.view.View.VISIBLE);
            buttonShowQR.setOnClickListener(v -> {
                Intent qrIntent = new Intent(this, DeviceQRCodeActivity.class);
                qrIntent.putExtra("device_name", deviceName);
                qrIntent.putExtra("mqtt_product_id", mqttProductId);
                qrIntent.putExtra("mqtt_device_num", mqttDeviceNum);
                qrIntent.putExtra("mqtt_device_type", mqttDeviceType);
                startActivity(qrIntent);
            });
        }
    }

    private int getDeviceIdFromName(String deviceName) {
        // Generate a simple ID based on device name hash
        if (deviceName == null) return 1;
        return Math.abs(deviceName.hashCode() % 1000);
    }

    private String getDeviceTypeFromName(String deviceName) {
        if (deviceName == null) return "actuator";
        String nameLower = deviceName.toLowerCase();
        if (nameLower.contains("lamp") || nameLower.contains("light")) {
            return "smart_lamp";
        } else if (nameLower.contains("camera") || nameLower.contains("cctv")) {
            return "camera";
        } else if (nameLower.contains("speaker")) {
            return "speaker";
        } else if (nameLower.contains("air") || nameLower.contains("conditioner") || nameLower.contains("ac")) {
            return "air_conditioner";
        }
        return "actuator";
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
        } else {
            return R.drawable.ic_device;
        }
    }

    private void setupDeviceImage(ImageView imageView) {
        // Set device-specific images using new design images
        int iconResId = getDeviceIconResId(deviceName);
        imageView.setImageResource(iconResId);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    private void setupClickListeners() {
        // Already set up in initializeViews
    }

    private void navigateToControlDevice() {
        // Navigate to device control detail screen
        // Use actual device ID from API response, or fallback to generated ID
        Intent intent = new Intent(this, DeviceControlDetailActivity.class);
        if (deviceId != null) {
            intent.putExtra("device_id", deviceId);
        } else {
            // Fallback: generate ID from name (for backward compatibility)
            intent.putExtra("device_id", getDeviceIdFromName(deviceName));
        }
        intent.putExtra("device_name", deviceName);
        intent.putExtra("device_type", getDeviceTypeFromName(deviceName));
        intent.putExtra("room_id", 1); // Default to room 1
        startActivity(intent);
        finish();
    }
}

