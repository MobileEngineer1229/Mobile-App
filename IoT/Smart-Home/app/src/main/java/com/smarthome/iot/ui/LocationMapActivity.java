package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.SceneCondition;
import com.smarthome.iot.utils.ThemeHelper;

public class LocationMapActivity extends AppCompatActivity {
    private String locationType; // "arrive_at" or "leave"
    private TextView textViewAddress;
    private MaterialButton buttonConfirm;
    private String selectedAddress = "701 7th Ave, New York, 10036, USA"; // Demo data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            ThemeHelper.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_location_map);

            setStatusBarColor();
            
            locationType = getIntent() != null ? getIntent().getStringExtra("location_type") : "arrive_at";
            
            initializeViews();
            setupClickListeners();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("LocationMapActivity", "Error in onCreate: " + e.getMessage(), e);
        }
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        try {
            ImageButton buttonBack = findViewById(R.id.buttonBack);
            textViewAddress = findViewById(R.id.textViewAddress);
            buttonConfirm = findViewById(R.id.buttonConfirm);

            if (buttonBack != null) {
                buttonBack.setOnClickListener(v -> finish());
            }
            
            // Set title based on location type
            TextView textViewTitle = findViewById(R.id.textViewTitle);
            if (textViewTitle != null) {
                textViewTitle.setText(getString(R.string.arrive_at));
            }
            
            if (textViewAddress != null) {
                textViewAddress.setText(selectedAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("LocationMapActivity", "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        if (buttonConfirm != null) {
            buttonConfirm.setOnClickListener(v -> {
                confirmLocation();
            });
        }
    }

    private void confirmLocation() {
        try {
            // Create SceneCondition for location
            SceneCondition condition = new SceneCondition();
            condition.setType("location_" + locationType); // "location_arrive_at" or "location_leave"
            condition.setLocation(selectedAddress);
            condition.setOperator(locationType); // "arrive_at" or "leave"
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("condition_data", condition);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("LocationMapActivity", "Error confirming location: " + e.getMessage(), e);
        }
    }
}
