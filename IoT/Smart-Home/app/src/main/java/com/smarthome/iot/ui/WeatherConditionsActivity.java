package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.ui.adapters.WeatherConditionAdapter;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class WeatherConditionsActivity extends AppCompatActivity {
    // Note: This activity shows the list of weather condition types
    // Individual condition activities (Temperature, Humidity, etc.) are separate
    private RecyclerView recyclerViewConditions;
    private WeatherConditionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_conditions);

        setStatusBarColor();

        initializeViews();
        setupRecyclerView();
        loadConditions();
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
        recyclerViewConditions = findViewById(R.id.recyclerViewConditions);

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }

        if (recyclerViewConditions == null) {
            android.util.Log.e("WeatherConditions", "RecyclerView is null!");
        }
    }

    private void setupRecyclerView() {
        recyclerViewConditions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadConditions() {
        List<WeatherConditionAdapter.ConditionItem> conditions = new ArrayList<>();
        conditions.add(new WeatherConditionAdapter.ConditionItem("temperature", getString(R.string.temperature), R.drawable.temperature, R.color.error));
        conditions.add(new WeatherConditionAdapter.ConditionItem("humidity", getString(R.string.humidity), R.drawable.humidity, R.color.light_blue));
        conditions.add(new WeatherConditionAdapter.ConditionItem("weather", getString(R.string.weather), R.drawable.weather, R.color.orange));
        conditions.add(new WeatherConditionAdapter.ConditionItem("sunrise_sunset", getString(R.string.sunrise_sunset), R.drawable.sunrise, R.color.orange));
        conditions.add(new WeatherConditionAdapter.ConditionItem("wind_speed", getString(R.string.wind_speed), R.drawable.wind, R.color.cyan));

        adapter = new WeatherConditionAdapter(conditions, condition -> {
            android.util.Log.d("WeatherConditions", "Condition clicked: " + condition.getType());
            navigateToCondition(condition.getType());
        });
        recyclerViewConditions.setAdapter(adapter);
        android.util.Log.d("WeatherConditions", "Loaded " + conditions.size() + " weather conditions");
    }

    private void navigateToCondition(String conditionType) {
        android.util.Log.d("WeatherConditions", "Navigating to condition: " + conditionType);
        try {
            Intent intent;
            switch (conditionType) {
                case "temperature":
                    intent = new Intent(this, TemperatureConditionActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                case "humidity":
                    intent = new Intent(this, HumidityConditionActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                case "weather":
                    intent = new Intent(this, WeatherTypeConditionActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                case "sunrise_sunset":
                    intent = new Intent(this, SunriseSunsetConditionActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                case "wind_speed":
                    intent = new Intent(this, WindSpeedConditionActivity.class);
                    startActivityForResult(intent, 100);
                    break;
                default:
                    android.util.Log.w("WeatherConditions", "Unknown condition type: " + conditionType);
                    finish();
                    return;
            }
        } catch (Exception e) {
            android.util.Log.e("WeatherConditions", "Error navigating to condition: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Condition configured, pass to scene builder
            setResult(RESULT_OK, data);
            finish();
        }
    }
}
