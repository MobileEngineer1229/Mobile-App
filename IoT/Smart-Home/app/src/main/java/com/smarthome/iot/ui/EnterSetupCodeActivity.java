package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.utils.ThemeHelper;

/**
 * Manual entry screen for FastBee/MQTT device setup codes.
 *
 * This activity provides an alternative to QR code scanning when the user cannot
 * scan the device QR code. The setup code is typically printed on the device body,
 * packaging, or included in the device manual.
 *
 * === Setup Code Format ===
 * {productId}:{deviceNum}
 * Example: 1001:lamp-living-01
 *   - productId: FastBee product identifier (maps to device type)
 *   - deviceNum: Unique device identifier on the MQTT broker
 *
 * === Integration Flow ===
 * 1. User enters setup code in the text field (e.g. "1001:lamp-living-01")
 * 2. Code is validated and parsed into productId + deviceNum
 * 3. Processing overlay is shown with a 3-second animation
 * 4. Navigates to DeviceDetectedActivity with MQTT metadata:
 *    - device_name: Human-readable name derived from productId
 *    - connection_source: "setup_code" (for backend analytics/metadata)
 *    - mqtt_product_id: FastBee productId for MQTT topic subscription
 *    - mqtt_device_num: FastBee deviceNum for MQTT topic subscription
 *    - mqtt_device_type: Device type inferred from productId
 * 5. DeviceDetectedActivity then:
 *    a. Verifies device on MQTT broker: POST /api/v1/devices/verify-mqtt
 *    b. Creates device via backend: POST /api/v1/devices
 *    c. Backend binds device to MQTT topic: /fastbee/{productId}/{deviceNum}/#
 *
 * === FastBee Product ID Mapping ===
 * 1001=lamp, 1002=electronics, 2001/2002=sensor,
 * 3001=camera, 4001=speaker, 5001=thermostat, 6001=lock
 *
 * @see ScanDeviceActivity QR code scanning alternative
 * @see DeviceDetectedActivity Next step: device verification and connection
 * @see DeviceQRCodeActivity Generates QR codes for already-registered MQTT devices
 */
public class EnterSetupCodeActivity extends AppCompatActivity {

    private View processingOverlay;
    private LinearLayout processingCard;
    private MaterialButton buttonContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_setup_code);

        initializeViews();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonContinue = findViewById(R.id.buttonContinue);
        EditText editTextSetupCode = findViewById(R.id.editTextSetupCode);
        processingOverlay = findViewById(R.id.processingOverlay);
        processingCard = findViewById(R.id.processingCard);

        buttonBack.setOnClickListener(v -> finish());

        buttonContinue.setOnClickListener(v -> {
            String setupCode = editTextSetupCode.getText() != null ?
                editTextSetupCode.getText().toString().trim() : "";

            if (setupCode.isEmpty()) {
                Toast.makeText(this, "Please enter a setup code", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse setup code: productId:deviceNum (e.g. 1001:lamp-01)
            String[] parts = setupCode.split(":", 2);
            String productId = parts[0].trim();
            String deviceNum = parts.length > 1 ? parts[1].trim() : "";

            if (deviceNum.isEmpty()) {
                Toast.makeText(this, "Invalid format. Use productId:deviceNum (e.g. 1001:lamp-01)", Toast.LENGTH_LONG).show();
                return;
            }

            String deviceType = inferDeviceType(productId);
            String deviceName = "Smart " + capitalize(deviceType) + " (" + deviceNum + ")";

            // Show processing overlay with spinner
            showProcessing();

            // Navigate to DeviceDetectedActivity after processing delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(this, DeviceDetectedActivity.class);
                intent.putExtra("device_name", deviceName);
                intent.putExtra("connection_source", "setup_code");
                intent.putExtra("mqtt_product_id", productId);
                intent.putExtra("mqtt_device_num", deviceNum);
                intent.putExtra("mqtt_device_type", deviceType);
                startActivity(intent);
                finish();
            }, 3000);
        });
    }

    private void showProcessing() {
        buttonContinue.setEnabled(false);
        buttonContinue.setAlpha(0.5f);
        processingOverlay.setVisibility(View.VISIBLE);
        processingCard.setVisibility(View.VISIBLE);
    }

    private String inferDeviceType(String productId) {
        switch (productId) {
            case "1001": return "lamp";
            case "1002": return "electronics";
            case "2001":
            case "2002": return "sensor";
            case "3001": return "camera";
            case "4001": return "speaker";
            case "5001": return "thermostat";
            case "6001": return "lock";
            default: return "electronics";
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
