package com.smarthome.iot.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.AdditionalSetting;
import com.smarthome.iot.ui.adapters.AdditionalSettingAdapter;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class AdditionalSettingsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewSettings;
    private AdditionalSettingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_additional_settings);

        initializeViews();
        setupRecyclerView();
        loadSettings();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerViewSettings = findViewById(R.id.recyclerViewSettings);
    }

    private void setupRecyclerView() {
        recyclerViewSettings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdditionalSettingAdapter(new ArrayList<>(), this::onSettingClick);
        recyclerViewSettings.setAdapter(adapter);
    }

    private void onSettingClick(AdditionalSetting setting) {
        String action = setting.getAction();
        switch (action) {
            case "temperature_units":
                showTemperatureUnitsDialog();
                break;
            case "clear_cache":
                showClearCacheDialog();
                break;
            case "experimental_features":
                Toast.makeText(this, "Experimental features coming soon", Toast.LENGTH_SHORT).show();
                break;
            case "system_permissions":
                Toast.makeText(this, "System permissions coming soon", Toast.LENGTH_SHORT).show();
                break;
            case "legal_information":
                Toast.makeText(this, "Legal information coming soon", Toast.LENGTH_SHORT).show();
                break;
            case "check_for_updates":
                Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, setting.getTitle() + " - Coming soon", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showTemperatureUnitsDialog() {
        String[] units = {getString(R.string.celsius), getString(R.string.fahrenheit)};
        new AlertDialog.Builder(this)
            .setTitle(R.string.temperature_units)
            .setItems(units, (dialog, which) -> {
                String selectedUnit = units[which];
                // Update setting value
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    AdditionalSetting setting = adapter.getItemAt(i);
                    if ("temperature_units".equals(setting.getAction())) {
                        setting.setValue(selectedUnit);
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }
            })
            .show();
    }

    private void showClearCacheDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.clear_cache)
            .setMessage("Are you sure you want to clear the cache?")
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
                // Update cache size
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    AdditionalSetting setting = adapter.getItemAt(i);
                    if ("clear_cache".equals(setting.getAction())) {
                        setting.setValue("0 MB");
                        adapter.notifyItemChanged(i);
                        break;
                    }
                }
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }

    private void loadSettings() {
        List<AdditionalSetting> settings = new ArrayList<>();
        settings.add(new AdditionalSetting(
            getString(R.string.temperature_units),
            getString(R.string.celsius),
            "temperature_units"
        ));
        settings.add(new AdditionalSetting(
            getString(R.string.clear_cache),
            "15.6 MB",
            "clear_cache"
        ));
        settings.add(new AdditionalSetting(
            getString(R.string.experimental_features),
            "",
            "experimental_features"
        ));
        settings.add(new AdditionalSetting(
            getString(R.string.system_permissions),
            "",
            "system_permissions"
        ));
        settings.add(new AdditionalSetting(
            getString(R.string.legal_information),
            "",
            "legal_information"
        ));
        settings.add(new AdditionalSetting(
            getString(R.string.check_for_updates),
            "",
            "check_for_updates"
        ));

        adapter = new AdditionalSettingAdapter(settings, this::onSettingClick);
        recyclerViewSettings.setAdapter(adapter);
    }
}

