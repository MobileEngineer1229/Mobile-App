package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.SceneCondition;
import com.smarthome.iot.utils.ThemeHelper;

public class ScheduleTimeActivity extends AppCompatActivity {
    private TimePicker timePicker;
    private TextView textViewRepeat;
    private MaterialButton buttonContinue;
    private String repeatType = "every_day"; // Default
    private int selectedHour = 21;
    private int selectedMinute = 45;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            ThemeHelper.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_schedule_time);

            setStatusBarColor();
            initializeViews();
            setupTimePicker();
            setupClickListeners();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("ScheduleTimeActivity", "Error in onCreate: " + e.getMessage(), e);
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
            textViewRepeat = findViewById(R.id.textViewRepeat);
            buttonContinue = findViewById(R.id.buttonContinue);
            timePicker = findViewById(R.id.timePicker);

            if (buttonBack != null) {
                buttonBack.setOnClickListener(v -> finish());
            }

            if (textViewRepeat != null) {
                textViewRepeat.setText(getString(R.string.every_day));
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("ScheduleTimeActivity", "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupTimePicker() {
        try {
            if (timePicker != null) {
                timePicker.setIs24HourView(false);
                timePicker.setHour(selectedHour);
                timePicker.setMinute(selectedMinute);
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("ScheduleTimeActivity", "Error setting up time picker: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        if (buttonContinue != null) {
            buttonContinue.setOnClickListener(v -> confirmTime());
        }
    }

    private void showRepeatDialog() {
        // TODO: Show dialog for repeat options
    }

    private void confirmTime() {
        try {
            if (timePicker != null) {
                selectedHour = timePicker.getHour();
                selectedMinute = timePicker.getMinute();
            }

            // Create SceneCondition for schedule time
            SceneCondition condition = new SceneCondition();
            condition.setType("schedule_time");
            condition.setOperator(repeatType);
            condition.setValue((double) (selectedHour * 100 + selectedMinute)); // Store as HHMM format

            Intent resultIntent = new Intent();
            resultIntent.putExtra("condition_data", condition);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("ScheduleTimeActivity", "Error confirming time: " + e.getMessage(), e);
        }
    }
}
