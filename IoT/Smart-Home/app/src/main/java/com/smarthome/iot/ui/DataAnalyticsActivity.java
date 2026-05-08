package com.smarthome.iot.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.DataAnalyticsOption;
import com.smarthome.iot.ui.adapters.DataAnalyticsAdapter;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class DataAnalyticsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewDataAnalytics;
    private DataAnalyticsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_analytics);

        initializeViews();
        setupRecyclerView();
        loadDataAnalyticsOptions();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerViewDataAnalytics = findViewById(R.id.recyclerViewDataAnalytics);
    }

    private void setupRecyclerView() {
        recyclerViewDataAnalytics.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DataAnalyticsAdapter(new ArrayList<>(), this::onOptionClick);
        recyclerViewDataAnalytics.setAdapter(adapter);
    }

    private void onOptionClick(DataAnalyticsOption option) {
        String action = option.getAction();
        switch (action) {
            case "data_usage":
                Toast.makeText(this, "Data usage settings coming soon", Toast.LENGTH_SHORT).show();
                break;
            case "ad_preferences":
                Toast.makeText(this, "Ad preferences coming soon", Toast.LENGTH_SHORT).show();
                break;
            case "download_my_data":
                Toast.makeText(this, "Downloading your data...", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, option.getTitle() + " - Coming soon", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void loadDataAnalyticsOptions() {
        List<DataAnalyticsOption> options = new ArrayList<>();
        options.add(new DataAnalyticsOption(
            getString(R.string.data_usage),
            getString(R.string.data_usage_subtitle),
            "data_usage"
        ));
        options.add(new DataAnalyticsOption(
            getString(R.string.ad_preferences),
            getString(R.string.ad_preferences_subtitle),
            "ad_preferences"
        ));
        options.add(new DataAnalyticsOption(
            getString(R.string.download_my_data),
            getString(R.string.download_my_data_subtitle),
            "download_my_data"
        ));

        adapter = new DataAnalyticsAdapter(options, this::onOptionClick);
        recyclerViewDataAnalytics.setAdapter(adapter);
    }
}

