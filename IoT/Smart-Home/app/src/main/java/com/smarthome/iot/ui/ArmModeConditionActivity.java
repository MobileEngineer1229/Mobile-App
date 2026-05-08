package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.SceneCondition;
import com.smarthome.iot.utils.ThemeHelper;

public class ArmModeConditionActivity extends AppCompatActivity {
    private MaterialButton buttonDisarmed;
    private MaterialButton buttonArmStay;
    private MaterialButton buttonArmAway;
    private MaterialButton buttonContinue;

    private String selectedArmMode = "disarmed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arm_mode_condition);

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
        buttonDisarmed = findViewById(R.id.buttonDisarmed);
        buttonArmStay = findViewById(R.id.buttonArmStay);
        buttonArmAway = findViewById(R.id.buttonArmAway);
        buttonContinue = findViewById(R.id.buttonContinue);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        buttonDisarmed.setOnClickListener(v -> {
            selectedArmMode = "disarmed";
            updateModeButtons();
        });

        buttonArmStay.setOnClickListener(v -> {
            selectedArmMode = "arm_stay";
            updateModeButtons();
        });

        buttonArmAway.setOnClickListener(v -> {
            selectedArmMode = "arm_away";
            updateModeButtons();
        });

        buttonContinue.setOnClickListener(v -> {
            SceneCondition condition = new SceneCondition();
            condition.setType("arm_mode");
            condition.setArmMode(selectedArmMode);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("condition_type", "arm_mode");
            resultIntent.putExtra("condition_data", condition);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void updateModeButtons() {
        buttonDisarmed.setBackgroundTintList(ContextCompat.getColorStateList(this,
                selectedArmMode.equals("disarmed") ? R.color.primary : R.color.dark_5));
        buttonDisarmed.setTextColor(ContextCompat.getColor(this,
                selectedArmMode.equals("disarmed") ? R.color.white : R.color.white_alpha_70));

        buttonArmStay.setBackgroundTintList(ContextCompat.getColorStateList(this,
                selectedArmMode.equals("arm_stay") ? R.color.primary : R.color.dark_5));
        buttonArmStay.setTextColor(ContextCompat.getColor(this,
                selectedArmMode.equals("arm_stay") ? R.color.white : R.color.white_alpha_70));

        buttonArmAway.setBackgroundTintList(ContextCompat.getColorStateList(this,
                selectedArmMode.equals("arm_away") ? R.color.primary : R.color.dark_5));
        buttonArmAway.setTextColor(ContextCompat.getColor(this,
                selectedArmMode.equals("arm_away") ? R.color.white : R.color.white_alpha_70));
    }
}
