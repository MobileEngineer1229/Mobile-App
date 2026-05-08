package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.SceneTask;
import com.smarthome.iot.utils.ThemeHelper;

public class DelayActionActivity extends AppCompatActivity {
    private NumberPicker numberPickerHours;
    private NumberPicker numberPickerMinutes;
    private NumberPicker numberPickerSeconds;
    private MaterialButton buttonContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            ThemeHelper.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_delay_action);

            setStatusBarColor();
            initializeViews();
            setupNumberPickers();
            setupClickListeners();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("DelayActionActivity", "Error in onCreate: " + e.getMessage(), e);
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
            numberPickerHours = findViewById(R.id.numberPickerHours);
            numberPickerMinutes = findViewById(R.id.numberPickerMinutes);
            numberPickerSeconds = findViewById(R.id.numberPickerSeconds);
            buttonContinue = findViewById(R.id.buttonContinue);

            if (buttonBack != null) {
                buttonBack.setOnClickListener(v -> finish());
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("DelayActionActivity", "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupNumberPickers() {
        try {
            if (numberPickerHours != null) {
                numberPickerHours.setMinValue(0);
                numberPickerHours.setMaxValue(23);
                numberPickerHours.setValue(0);
                numberPickerHours.setWrapSelectorWheel(true);
            }
            
            if (numberPickerMinutes != null) {
                numberPickerMinutes.setMinValue(0);
                numberPickerMinutes.setMaxValue(59);
                numberPickerMinutes.setValue(15);
                numberPickerMinutes.setWrapSelectorWheel(true);
            }
            
            if (numberPickerSeconds != null) {
                numberPickerSeconds.setMinValue(0);
                numberPickerSeconds.setMaxValue(59);
                numberPickerSeconds.setValue(30);
                numberPickerSeconds.setWrapSelectorWheel(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("DelayActionActivity", "Error setting up number pickers: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        if (buttonContinue != null) {
            buttonContinue.setOnClickListener(v -> {
                confirmDelay();
            });
        }
    }

    private void confirmDelay() {
        try {
            int hours = numberPickerHours != null ? numberPickerHours.getValue() : 0;
            int minutes = numberPickerMinutes != null ? numberPickerMinutes.getValue() : 0;
            int seconds = numberPickerSeconds != null ? numberPickerSeconds.getValue() : 0;
            
            int totalSeconds = hours * 3600 + minutes * 60 + seconds;
            
            // Create SceneTask for delay
            SceneTask task = new SceneTask("delay");
            task.setDelaySeconds(totalSeconds);
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("task_data", task);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("DelayActionActivity", "Error confirming delay: " + e.getMessage(), e);
        }
    }
}
