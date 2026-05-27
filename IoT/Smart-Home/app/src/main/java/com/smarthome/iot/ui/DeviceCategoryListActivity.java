package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.ui.decorations.GridSpacingItemDecoration;

import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.DeviceAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity for displaying devices by category (Lightning, Cameras, Electrical)
 */
public class DeviceCategoryListActivity extends AppCompatActivity {
    private RecyclerView recyclerViewDevices;
    private DeviceAdapter deviceAdapter;
    private List<Device> devices;
    private String category;
    private TextView textViewTitle;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_category_list);

        setStatusBarColor();

        // Initialize API
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        // Get category from intent
        category = getIntent().getStringExtra("category");
        if (category == null) {
            category = "lightning";
        }

        initializeViews();
        setupRecyclerView();
        loadDevices();
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
        ImageButton buttonMore = findViewById(R.id.buttonMore);
        textViewTitle = findViewById(R.id.textViewTitle);
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);

        // Set title based on category (count will be updated after loading)
        String title = getCategoryTitle(category);
        if (textViewTitle != null) {
            textViewTitle.setText(title + " (0)");
        }

        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            // TODO: Show more options
        });
    }

    private String getCategoryTitle(String category) {
        switch (category) {
            case "lightning":
                return "Lightning";
            case "cameras":
                return "Cameras";
            case "electrical":
                return "Electrical";
            default:
                return "Devices";
        }
    }


    private void setupRecyclerView() {
        devices = new ArrayList<>();
        deviceAdapter = new DeviceAdapter();
        deviceAdapter.setDevices(devices);
        
        // Set click listener for navigating to device detail when clicking card
        deviceAdapter.setOnDeviceClickListener(device -> {
            Intent intent = new Intent(this, DeviceControlDetailActivity.class);
            intent.putExtra("device_id", device.getId());
            intent.putExtra("device_name", device.getName());
            intent.putExtra("device_type", device.getType());
            intent.putExtra("room_id", device.getRoomId() != null ? device.getRoomId() : 0);
            startActivity(intent);
        });
        
        // Set toggle listener for switch button (toggle device on/off via API)
        deviceAdapter.setOnDeviceToggleListener((device, isOn) -> {
            toggleDevicePower(device, isOn);
        });
        
        // Use GridLayoutManager for 2 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerViewDevices.setLayoutManager(gridLayoutManager);
        
        // Add spacing between grid items (4dp converted to pixels)
        int spacingInPixels = (int) (4 * getResources().getDisplayMetrics().density);
        recyclerViewDevices.addItemDecoration(new GridSpacingItemDecoration(2, spacingInPixels, false));
        
        recyclerViewDevices.setAdapter(deviceAdapter);
    }

    private void toggleDevicePower(Device device, boolean isOn) {
        // Update UI optimistically
        device.setOn(isOn);
        deviceAdapter.notifyDataSetChanged();
        
        // If not logged in or demo user, just update locally
        if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            Toast.makeText(this, 
                device.getName() + " turned " + (isOn ? "ON" : "OFF"), 
                Toast.LENGTH_SHORT).show();
            return;
        }

        String token = authManager.getToken();
        if (token == null) {
            // Revert UI change if no token
            device.setOn(!isOn);
            deviceAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call backend API
        Map<String, Boolean> powerData = new HashMap<>();
        powerData.put("power", isOn);

        Call<ApiResponse<Map<String, Object>>> call = apiService.controlDevicePower(device.getId(), powerData);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Update device status based on response
                    device.setOn(isOn);
                    device.setStatus(isOn ? "online" : "offline");
                    deviceAdapter.notifyDataSetChanged();
                    
                    String message = response.body().getData() != null && response.body().getData().containsKey("message")
                        ? (String) response.body().getData().get("message")
                        : device.getName() + " turned " + (isOn ? "ON" : "OFF");
                    Toast.makeText(DeviceCategoryListActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    // Revert UI change on error
                    device.setOn(!isOn);
                    deviceAdapter.notifyDataSetChanged();
                    
                    String errorMessage = "Failed to control device";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMessage = response.body().getError().getMessage();
                    }
                    Toast.makeText(DeviceCategoryListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                // Revert UI change on failure
                device.setOn(!isOn);
                deviceAdapter.notifyDataSetChanged();
                Toast.makeText(DeviceCategoryListActivity.this, 
                    "Error: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDevices() {
        Call<ApiResponse<List<Device>>> call = apiService.getDevicesByCategory(category, null);
        call.enqueue(new Callback<ApiResponse<List<Device>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Device>>> call, Response<ApiResponse<List<Device>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    devices = response.body().getData();
                    if (devices == null) {
                        devices = new ArrayList<>();
                    }
                    for (Device device : devices) {
                        device.syncStatus();
                    }
                    deviceAdapter.setDevices(devices);
                    deviceAdapter.notifyDataSetChanged();
                    updateTitle(devices.size());
                } else {
                    android.util.Log.w("DeviceCategoryList", "Failed to load devices from API");
                    devices = new ArrayList<>();
                    deviceAdapter.setDevices(devices);
                    deviceAdapter.notifyDataSetChanged();
                    updateTitle(0);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Device>>> call, Throwable t) {
                android.util.Log.e("DeviceCategoryList", "Network error loading devices", t);
                devices = new ArrayList<>();
                deviceAdapter.setDevices(devices);
                deviceAdapter.notifyDataSetChanged();
                updateTitle(devices.size());
            }
        });
    }

    private void updateTitle(int count) {
        if (textViewTitle != null) {
            String title = getCategoryTitle(category);
            textViewTitle.setText(title + " (" + count + ")");
        }
    }
}
