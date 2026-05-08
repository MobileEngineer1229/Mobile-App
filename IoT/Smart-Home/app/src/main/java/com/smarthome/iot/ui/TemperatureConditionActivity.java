package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.SceneCondition;
import com.smarthome.iot.utils.ThemeHelper;

public class TemperatureConditionActivity extends AppCompatActivity {
    private LinearLayout layoutLocation;
    private TextView textViewLocation;
    private MaterialButton buttonLessThan;
    private MaterialButton buttonEquals;
    private MaterialButton buttonGreaterThan;
    private TextView textViewTemperature;
    private SeekBar seekBarTemperature;
    private MaterialButton buttonContinue;
    
    private String selectedOperator = ">";
    private String selectedLocation = "New York City";
    private int temperatureValue = 20; // -50 to 50, displayed as 20°C

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature_condition);

        setStatusBarColor();

        initializeViews();
        setupClickListeners();
        updateTemperatureDisplay();
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
        buttonLessThan = findViewById(R.id.buttonLessThan);
        buttonEquals = findViewById(R.id.buttonEquals);
        buttonGreaterThan = findViewById(R.id.buttonGreaterThan);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        seekBarTemperature = findViewById(R.id.seekBarTemperature);
        buttonContinue = findViewById(R.id.buttonContinue);

        buttonBack.setOnClickListener(v -> finish());
        
        // Set initial progress (20°C = 70 out of 100, where 0 = -50°C, 100 = 50°C)
        seekBarTemperature.setProgress(70);
        
        seekBarTemperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert progress (0-100) to temperature (-50 to 50)
                temperatureValue = progress - 50;
                updateTemperatureDisplay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupClickListeners() {
        layoutLocation.setOnClickListener(v -> {
            // TODO: Show location selector
            Toast.makeText(this, "Location selector coming soon", Toast.LENGTH_SHORT).show();
        });
        
        buttonLessThan.setOnClickListener(v -> {
            selectedOperator = "<";
            updateOperatorButtons();
        });
        
        buttonEquals.setOnClickListener(v -> {
            selectedOperator = "=";
            updateOperatorButtons();
        });
        
        buttonGreaterThan.setOnClickListener(v -> {
            selectedOperator = ">";
            updateOperatorButtons();
        });
        
        buttonContinue.setOnClickListener(v -> {
            // Create condition and return
            SceneCondition condition = new SceneCondition();
            condition.setType("temperature");
            condition.setOperator(selectedOperator);
            condition.setValue((double) temperatureValue);
            condition.setUnit("C");
            condition.setLocation(selectedLocation);
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("condition_type", "temperature");
            resultIntent.putExtra("condition_data", condition);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void updateOperatorButtons() {
        buttonLessThan.setBackgroundTintList(ContextCompat.getColorStateList(this, 
            selectedOperator.equals("<") ? R.color.primary : R.color.dark_5));
        buttonLessThan.setTextColor(ContextCompat.getColor(this, 
            selectedOperator.equals("<") ? R.color.white : R.color.white_alpha_70));
        
        buttonEquals.setBackgroundTintList(ContextCompat.getColorStateList(this, 
            selectedOperator.equals("=") ? R.color.primary : R.color.dark_5));
        buttonEquals.setTextColor(ContextCompat.getColor(this, 
            selectedOperator.equals("=") ? R.color.white : R.color.white_alpha_70));
        
        buttonGreaterThan.setBackgroundTintList(ContextCompat.getColorStateList(this, 
            selectedOperator.equals(">") ? R.color.primary : R.color.dark_5));
        buttonGreaterThan.setTextColor(ContextCompat.getColor(this, 
            selectedOperator.equals(">") ? R.color.white : R.color.white_alpha_70));
    }

    private void updateTemperatureDisplay() {
        textViewTemperature.setText(temperatureValue + "°C");
    }
}
