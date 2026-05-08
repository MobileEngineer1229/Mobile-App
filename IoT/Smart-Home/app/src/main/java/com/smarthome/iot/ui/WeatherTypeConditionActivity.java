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

public class WeatherTypeConditionActivity extends AppCompatActivity {
    private LinearLayout layoutLocation;
    private TextView textViewLocation;
    private RadioGroup radioGroupWeather;
    private RadioButton radioSunny;
    private RadioButton radioCloudy;
    private RadioButton radioRainy;
    private RadioButton radioSnowy;
    private RadioButton radioHazy;
    private MaterialButton buttonContinue;
    
    private String selectedWeather = "rainy";
    private String selectedLocation = "New York City";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_type_condition);

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
        radioGroupWeather = findViewById(R.id.radioGroupWeather);
        radioSunny = findViewById(R.id.radioSunny);
        radioCloudy = findViewById(R.id.radioCloudy);
        radioRainy = findViewById(R.id.radioRainy);
        radioSnowy = findViewById(R.id.radioSnowy);
        radioHazy = findViewById(R.id.radioHazy);
        buttonContinue = findViewById(R.id.buttonContinue);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        layoutLocation.setOnClickListener(v -> {
            // TODO: Show location selector
            Toast.makeText(this, "Location selector coming soon", Toast.LENGTH_SHORT).show();
        });
        
        radioGroupWeather.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioSunny) {
                selectedWeather = "sunny";
            } else if (checkedId == R.id.radioCloudy) {
                selectedWeather = "cloudy";
            } else if (checkedId == R.id.radioRainy) {
                selectedWeather = "rainy";
            } else if (checkedId == R.id.radioSnowy) {
                selectedWeather = "snowy";
            } else if (checkedId == R.id.radioHazy) {
                selectedWeather = "hazy";
            }
        });
        
        buttonContinue.setOnClickListener(v -> {
            // Create condition and return
            SceneCondition condition = new SceneCondition();
            condition.setType("weather");
            condition.setOperator(selectedWeather);
            condition.setLocation(selectedLocation);
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("condition_type", "weather");
            resultIntent.putExtra("condition_data", condition);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
