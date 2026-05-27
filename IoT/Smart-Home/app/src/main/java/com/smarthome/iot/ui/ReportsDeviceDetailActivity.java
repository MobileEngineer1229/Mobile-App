package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.DeviceConsumptionResponse;
import com.smarthome.iot.models.DeviceConsumptionSummary;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.ReportsDeviceDetailAdapter;
import com.smarthome.iot.ui.decorations.GridSpacingItemDecoration;
import com.smarthome.iot.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsDeviceDetailActivity extends AppCompatActivity {
    private static final String TAG = "ReportsDeviceDetailActivity";

    // Views
    private TextView textViewTitle;
    private ImageButton buttonBack;
    private ImageButton buttonMenu;
    private RecyclerView recyclerViewDevices;
    private LinearLayout emptyStateContainer;
    private ProgressBar progressBar;

    // Data
    private String deviceName;
    private String deviceType;
    private Integer deviceId;
    private String dateRange;
    private List<DeviceConsumptionSummary> deviceList;
    private ReportsDeviceDetailAdapter adapter;

    // Services
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports_device_detail);

        // Set status bar color to dark
        setStatusBarColor();

        // Get intent data
        Intent intent = getIntent();
        deviceName = intent.getStringExtra("device_name");
        deviceType = intent.getStringExtra("device_type");
        deviceId = intent.getIntExtra("device_id", -1);
        dateRange = intent.getStringExtra("date_range");
        if (dateRange == null) {
            dateRange = "last_6_months";
        }

        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initViews();
        setupBottomNavigation();
        setupRecyclerView();
        loadData();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_3));
        }
    }

    private void initViews() {
        textViewTitle = findViewById(R.id.textViewTitle);
        buttonBack = findViewById(R.id.buttonBack);
        buttonMenu = findViewById(R.id.buttonMenu);
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        progressBar = findViewById(R.id.progressBar);

        // Set title with device count
        if (deviceName != null) {
            textViewTitle.setText(deviceName);
        }

        buttonBack.setOnClickListener(v -> finish());
        buttonMenu.setOnClickListener(v -> {
            // TODO: Show menu options
            Toast.makeText(this, "Menu options coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_reports);
        
        // Remove spacing between icon and text
        com.smarthome.iot.utils.BottomNavSpacingHelper.removeSpacing(bottomNavigation);
        
        // Disable active indicator (blue background)
        com.smarthome.iot.utils.BottomNavSpacingHelper.disableActiveIndicator(bottomNavigation);
        
        bottomNavigation.setItemIconTintList(null);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            // Remove active indicator background when selection changes
            com.smarthome.iot.utils.BottomNavSpacingHelper.onSelectionChanged(bottomNavigation);
            
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_smart) {
                Intent intent = new Intent(ReportsDeviceDetailActivity.this, SmartSceneActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_account) {
                Intent intent = new Intent(ReportsDeviceDetailActivity.this, AccountActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        if (deviceList == null) {
            deviceList = new ArrayList<>();
        }
        if (adapter == null) {
            adapter = new ReportsDeviceDetailAdapter(deviceList);
        }
        if (recyclerViewDevices != null) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            recyclerViewDevices.setLayoutManager(gridLayoutManager);
            
            int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
            recyclerViewDevices.addItemDecoration(new GridSpacingItemDecoration(2, spacingInPixels, false));
            
            recyclerViewDevices.setAdapter(adapter);
        }
    }

    private void loadData() {
        showProgress(true);
        
        // Load individual devices of this type
        Call<ApiResponse<DeviceConsumptionResponse>> call = apiService.getDeviceConsumption(
            dateRange, null, deviceType, null, null, null, "device"
        );
        
        call.enqueue(new Callback<ApiResponse<DeviceConsumptionResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<DeviceConsumptionResponse>> call, Response<ApiResponse<DeviceConsumptionResponse>> response) {
                showProgress(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    DeviceConsumptionResponse data = response.body().getData();
                    if (data != null && data.getDevices() != null) {
                        deviceList.clear();
                        deviceList.addAll(data.getDevices());
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        showEmptyState(deviceList.isEmpty());
                        
                        // Update title with count
                        if (textViewTitle != null && deviceName != null) {
                            textViewTitle.setText(deviceName + " (" + deviceList.size() + ")");
                        }
                    }
                } else {
                    android.util.Log.w("ReportsDetail", "Failed to load device details from API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<DeviceConsumptionResponse>> call, Throwable t) {
                showProgress(false);
                android.util.Log.e("ReportsDetail", "Network error loading device details", t);
            }
        });
    }

    private void loadDemoData() {
        // Create demo data for individual devices
        deviceList.clear();
        
        String[] rooms = {"Living Room", "Bedroom", "Bathroom", "Kitchen", "Dining Room"};
        double[] consumptions = {16.82, 12.34, 14.67, 18.52, 13.21, 15.89, 10.53, 8.47, 17.13, 10.98, 10.02, 7.43};
        
        for (int i = 0; i < Math.min(consumptions.length, 12); i++) {
            DeviceConsumptionSummary device = new DeviceConsumptionSummary();
            device.setDeviceId(i + 1);
            device.setDeviceName(deviceName != null ? deviceName : "Smart Lamp");
            device.setDeviceType(deviceType != null ? deviceType : "Lighting");
            device.setRoomId(i % rooms.length);
            device.setRoomName(rooms[i % rooms.length]);
            device.setTotalConsumptionKwh(consumptions[i]);
            device.setTotalCostUsd(consumptions[i] * 0.15); // Approximate cost
            deviceList.add(device);
        }
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        showEmptyState(deviceList.isEmpty());
        
        // Update title with count
        if (textViewTitle != null && deviceName != null) {
            textViewTitle.setText(deviceName + " (" + deviceList.size() + ")");
        }
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        if (emptyStateContainer != null && recyclerViewDevices != null) {
            emptyStateContainer.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerViewDevices.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
