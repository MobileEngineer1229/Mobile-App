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
import com.smarthome.iot.ui.adapters.LocationOptionAdapter;
import com.smarthome.iot.utils.ThemeHelper;

public class LocationSelectionActivity extends AppCompatActivity {
    private RecyclerView recyclerViewOptions;
    private LocationOptionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            ThemeHelper.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_location_selection);

            setStatusBarColor();
            initializeViews();
            setupRecyclerView();
            loadLocationOptions();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("LocationSelectionActivity", "Error in onCreate: " + e.getMessage(), e);
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
            ImageButton buttonMore = findViewById(R.id.buttonMore);
            recyclerViewOptions = findViewById(R.id.recyclerViewOptions);

            if (buttonBack != null) {
                buttonBack.setOnClickListener(v -> finish());
            }
            if (buttonMore != null) {
                buttonMore.setOnClickListener(v -> {
                    // TODO: Show more options
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("LocationSelectionActivity", "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupRecyclerView() {
        if (recyclerViewOptions == null) {
            return;
        }
        
        try {
            adapter = new LocationOptionAdapter(option -> {
                navigateToLocationMap(option);
            });
            recyclerViewOptions.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewOptions.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("LocationSelectionActivity", "Error setting up RecyclerView: " + e.getMessage(), e);
        }
    }

    private void loadLocationOptions() {
        if (adapter == null) {
            return;
        }
        
        try {
            java.util.List<LocationOptionAdapter.LocationOption> options = new java.util.ArrayList<>();
            
            // Arrive at option
            options.add(new LocationOptionAdapter.LocationOption(
                "arrive_at",
                getString(R.string.arrive_at_preset_location),
                getString(R.string.arrive_at_example),
                R.drawable.ic_location,
                R.color.green
            ));
            
            // Leave option
            options.add(new LocationOptionAdapter.LocationOption(
                "leave",
                getString(R.string.leave_preset_location),
                getString(R.string.leave_example),
                R.drawable.ic_location,
                R.color.error
            ));
            
            adapter.setOptions(options);
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("LocationSelectionActivity", "Error loading location options: " + e.getMessage(), e);
        }
    }

    private void navigateToLocationMap(String locationType) {
        try {
            Intent intent = new Intent(this, LocationMapActivity.class);
            intent.putExtra("location_type", locationType);
            startActivityForResult(intent, 100);
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("LocationSelectionActivity", "Error navigating to location map: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Location selected, pass to SceneBuilderActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("condition_type", "location");
            resultIntent.putExtra("condition_data", data.getSerializableExtra("condition_data"));
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }
}
