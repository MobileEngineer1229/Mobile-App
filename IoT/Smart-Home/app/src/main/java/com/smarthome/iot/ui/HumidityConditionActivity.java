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

public class HumidityConditionActivity extends AppCompatActivity {
    private LinearLayout layoutLocation;
    private TextView textViewLocation;
    private RadioGroup radioGroupHumidity;
    private RadioButton radioDry;
    private RadioButton radioComfortable;
    private RadioButton radioMoist;
    private MaterialButton buttonContinue;
    
    private String selectedHumidity = "dry";
    private String selectedLocation = "New York City";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_humidity_condition);

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
        radioGroupHumidity = findViewById(R.id.radioGroupHumidity);
        radioDry = findViewById(R.id.radioDry);
        radioComfortable = findViewById(R.id.radioComfortable);
        radioMoist = findViewById(R.id.radioMoist);
        buttonContinue = findViewById(R.id.buttonContinue);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        layoutLocation.setOnClickListener(v -> {
            // TODO: Show location selector
            Toast.makeText(this, "Location selector coming soon", Toast.LENGTH_SHORT).show();
        });
        
        radioGroupHumidity.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioDry) {
                selectedHumidity = "dry";
            } else if (checkedId == R.id.radioComfortable) {
                selectedHumidity = "comfortable";
            } else if (checkedId == R.id.radioMoist) {
                selectedHumidity = "moist";
            }
        });
        
        buttonContinue.setOnClickListener(v -> {
            // Create condition and return
            SceneCondition condition = new SceneCondition();
            condition.setType("humidity");
            condition.setOperator(selectedHumidity);
            condition.setLocation(selectedLocation);
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("condition_type", "humidity");
            resultIntent.putExtra("condition_data", condition);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
