package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.SceneCondition;
import com.smarthome.iot.utils.ThemeHelper;

public class SunriseSunsetConditionActivity extends AppCompatActivity {
    private LinearLayout layoutLocation;
    private TextView textViewLocation;
    private RadioGroup radioGroupSunriseSunset;
    private RadioButton radioSunset;
    private RadioButton radioSunrise;
    private MaterialButton buttonContinue;
    
    private String selectedType = "sunset";
    private String selectedLocation = "New York City";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sunrise_sunset_condition);

        setStatusBarColor();

        initializeViews();
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
        layoutLocation = findViewById(R.id.layoutLocation);
        textViewLocation = findViewById(R.id.textViewLocation);
        radioGroupSunriseSunset = findViewById(R.id.radioGroupSunriseSunset);
        radioSunset = findViewById(R.id.radioSunset);
        radioSunrise = findViewById(R.id.radioSunrise);
        buttonContinue = findViewById(R.id.buttonContinue);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        layoutLocation.setOnClickListener(v -> {
            // TODO: Show location selector
            Toast.makeText(this, "Location selector coming soon", Toast.LENGTH_SHORT).show();
        });
        
        radioGroupSunriseSunset.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioSunset) {
                selectedType = "sunset";
            } else if (checkedId == R.id.radioSunrise) {
                selectedType = "sunrise";
            }
        });
        
        buttonContinue.setOnClickListener(v -> {
            // Create condition and return
            SceneCondition condition = new SceneCondition();
            condition.setType("sunrise_sunset");
            condition.setOperator(selectedType);
            condition.setLocation(selectedLocation);
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("condition_type", "sunrise_sunset");
            resultIntent.putExtra("condition_data", condition);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
