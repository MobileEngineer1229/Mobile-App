package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.smarthome.iot.R;
import com.smarthome.iot.utils.ThemeHelper;

/**
 * Legacy processing/loading screen activity.
 *
 * NOTE: This activity is now largely superseded by the inline processing overlay
 * built into EnterSetupCodeActivity. The processing animation (spinner + "Processing Device..."
 * text) is shown as a dimmed overlay card directly on the enter setup code screen,
 * matching the Figma design. This activity remains for backward compatibility with
 * any flows that may still reference it directly.
 *
 * The processing overlay simulates a 3-second delay before navigating to
 * DeviceDetectedActivity, where actual MQTT verification and backend API calls occur.
 *
 * @see EnterSetupCodeActivity Preferred: inline processing overlay
 * @see DeviceDetectedActivity Next step: actual MQTT/backend integration
 */
public class ProcessingDeviceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing_device);

        String setupCode = getIntent().getStringExtra("setup_code");

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        // Simulate processing - navigate to device detected after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(this, DeviceDetectedActivity.class);
            intent.putExtra("device_name", "Stereo Speaker");
            startActivity(intent);
            finish();
        }, 3000);
    }
}

