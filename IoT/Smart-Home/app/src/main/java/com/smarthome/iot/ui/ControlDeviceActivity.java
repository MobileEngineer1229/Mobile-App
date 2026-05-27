package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.models.Room;
import com.smarthome.iot.models.SceneTask;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.DeviceAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.DeviceHelper;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ControlDeviceActivity extends AppCompatActivity {
    private RecyclerView recyclerViewDevices;
    private DeviceAdapter deviceAdapter;
    private List<Device> devices;
    private List<Room> rooms = new ArrayList<>();
    private String selectedCategory = "all";
    private Integer selectedRoomId = null;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_device);

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        setStatusBarColor();

        initializeViews();
        setupRecyclerView();
        setupCategoryClickListeners();
        setupRoomFilters();
        loadRooms();
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
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);

        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            // TODO: Show more options
        });
    }

    private void setupRecyclerView() {
        devices = new ArrayList<>();
        deviceAdapter = new DeviceAdapter();
        deviceAdapter.setDevices(devices);
        
        // Set click listener for navigating to scene device function selection when clicking card
        deviceAdapter.setOnDeviceClickListener(device -> {
            Intent intent = new Intent(this, SceneDeviceFunctionActivity.class);
            intent.putExtra("device_id", device.getId());
            intent.putExtra("device_name", device.getName());
            intent.putExtra("device_type", device.getType());
            // Get room name from room ID
            String roomName = getRoomNameById(device.getRoomId());
            intent.putExtra("room_name", roomName);
            startActivityForResult(intent, 100);
        });
        
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDevices.setAdapter(deviceAdapter);
    }

    private void setupCategoryClickListeners() {
        LinearLayout cardLightning = findViewById(R.id.cardLightning);
        LinearLayout cardCameras = findViewById(R.id.cardCameras);
        LinearLayout cardElectrical = findViewById(R.id.cardElectrical);

        cardLightning.setOnClickListener(v -> {
            // Navigate to Lightning category list
            Intent intent = new Intent(this, DeviceCategoryListActivity.class);
            intent.putExtra("category", "lightning");
            startActivity(intent);
        });
        cardCameras.setOnClickListener(v -> {
            // Navigate to Cameras category list
            Intent intent = new Intent(this, DeviceCategoryListActivity.class);
            intent.putExtra("category", "cameras");
            startActivity(intent);
        });
        cardElectrical.setOnClickListener(v -> {
            // Navigate to Electrical category list
            Intent intent = new Intent(this, DeviceCategoryListActivity.class);
            intent.putExtra("category", "electrical");
            startActivity(intent);
        });
    }

    private void setupRoomFilters() {
        LinearLayout roomFiltersContainer = findViewById(R.id.linearLayoutRoomFilters);
        roomFiltersContainer.removeAllViews();

        // Add "All Rooms" filter only - rooms will be added after API call
        MaterialButton allRoomsButton = createRoomFilter("All Rooms", null);
        allRoomsButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
        allRoomsButton.setTextColor(ContextCompat.getColor(this, R.color.white));
        roomFiltersContainer.addView(allRoomsButton);
    }
    
    /**
     * Add rooms dynamically after API call without repainting the entire UI
     * @param rooms List of rooms to add
     * @param deviceCounts Map of room ID to device count
     */
    public void addRoomFilters(List<com.smarthome.iot.models.Room> rooms, java.util.Map<Integer, Integer> deviceCounts) {
        if (rooms == null || rooms.isEmpty()) {
            return;
        }
        
        LinearLayout roomFiltersContainer = findViewById(R.id.linearLayoutRoomFilters);
        
        // Only add rooms if they don't already exist (avoid duplicates)
        for (com.smarthome.iot.models.Room room : rooms) {
            // Check if room filter already exists
            boolean exists = false;
            for (int i = 0; i < roomFiltersContainer.getChildCount(); i++) {
                View child = roomFiltersContainer.getChildAt(i);
                if (child instanceof MaterialButton) {
                    MaterialButton button = (MaterialButton) child;
                    Object tag = button.getTag();
                    if (tag != null && tag.equals(room.getId())) {
                        exists = true;
                        break;
                    }
                }
            }
            
            if (!exists) {
                int deviceCount = deviceCounts != null ? deviceCounts.getOrDefault(room.getId(), 0) : 0;
                String roomText = room.getName() + (deviceCount > 0 ? " (" + deviceCount + ")" : "");
                MaterialButton roomButton = createRoomFilter(roomText, room.getId());
                roomButton.setTag(room.getId()); // Tag to identify the button
                roomFiltersContainer.addView(roomButton);
            }
        }
    }

    private MaterialButton createRoomFilter(String text, Integer roomId) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setTextSize(12);
        button.setTypeface(ResourcesCompat.getFont(this, R.font.urbanist));
        button.setTextColor(ContextCompat.getColor(this, R.color.white_alpha_70));
        button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.dark_4));
        button.setCornerRadius(20);
        button.setPadding(24, 12, 24, 12);
        button.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.setMarginEnd(8);
        button.setLayoutParams(params);
        
        // Store room ID as tag for identification
        if (roomId != null) {
            button.setTag(roomId);
        }

        button.setOnClickListener(v -> {
            selectedRoomId = roomId;
            // Update button states
            updateRoomFilterButtons();
            loadDevices();
        });

        return button;
    }

    private void updateRoomFilterButtons() {
        LinearLayout roomFiltersContainer = findViewById(R.id.linearLayoutRoomFilters);
        for (int i = 0; i < roomFiltersContainer.getChildCount(); i++) {
            View child = roomFiltersContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                Object tag = button.getTag();
                boolean isSelected = (selectedRoomId == null && tag == null) || 
                                    (selectedRoomId != null && tag != null && tag.equals(selectedRoomId));
                
                if (isSelected) {
                    button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
                    button.setTextColor(ContextCompat.getColor(this, R.color.white));
                } else {
                    button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.dark_4));
                    button.setTextColor(ContextCompat.getColor(this, R.color.white_alpha_70));
                }
            }
        }
    }

    private void loadRooms() {
        if (!authManager.isLoggedIn() || apiService == null) {
            return;
        }

        // Get primary home
        Home primaryHome = Globals.getPrimaryHome();
        if (primaryHome == null) {
            return;
        }

        // Load rooms for the primary home
        Call<ApiResponse<List<Room>>> call = apiService.getRooms(primaryHome.getId());
        call.enqueue(new Callback<ApiResponse<List<Room>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Room>>> call, Response<ApiResponse<List<Room>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    rooms = response.body().getData();
                    if (rooms != null && !rooms.isEmpty()) {
                        // Count devices per room
                        Map<Integer, Integer> deviceCounts = new HashMap<>();
                        for (Room room : rooms) {
                            deviceCounts.put(room.getId(), 0); // Will be updated when devices are loaded
                        }
                        // Add room filters
                        addRoomFilters(rooms, deviceCounts);
                        // Update device counts after loading devices
                        updateDeviceCounts(rooms, deviceCounts);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Room>>> call, Throwable t) {
                android.util.Log.e("ControlDevice", "Error loading rooms", t);
            }
        });
    }

    private void updateDeviceCounts(List<Room> rooms, Map<Integer, Integer> deviceCounts) {
        if (devices == null || devices.isEmpty()) {
            return;
        }

        for (Device device : devices) {
            if (device.getRoomId() != null) {
                Integer count = deviceCounts.get(device.getRoomId());
                if (count != null) {
                    deviceCounts.put(device.getRoomId(), count + 1);
                }
            }
        }

        // Update room filter button texts
        LinearLayout roomFiltersContainer = findViewById(R.id.linearLayoutRoomFilters);
        for (int i = 0; i < roomFiltersContainer.getChildCount(); i++) {
            View child = roomFiltersContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                Object tag = button.getTag();
                if (tag != null && tag instanceof Integer) {
                    Integer roomId = (Integer) tag;
                    Room room = null;
                    for (Room r : rooms) {
                        if (r.getId() == roomId) {
                            room = r;
                            break;
                        }
                    }
                    if (room != null) {
                        int count = deviceCounts.getOrDefault(roomId, 0);
                        String roomText = room.getName() + (count > 0 ? " (" + count + ")" : "");
                        button.setText(roomText);
                    }
                }
            }
        }
    }

    private void loadDevices() {
        if (!authManager.isLoggedIn() || apiService == null) {
            devices = new ArrayList<>();
            deviceAdapter.setDevices(devices);
            return;
        }

        // Get primary home
        Home primaryHome = Globals.getPrimaryHome();
        if (primaryHome == null) {
            devices = new ArrayList<>();
            deviceAdapter.setDevices(devices);
            return;
        }

        // Prepare query parameters
        Integer homeIdParam = primaryHome.getId();
        Integer roomIdParam = selectedRoomId;
        String typeParam = null;

        // Convert category to device type if needed
        if (selectedCategory != null && !selectedCategory.equals("all")) {
            // Map category to device type
            if (selectedCategory.equals("lightning")) {
                typeParam = "lamp";
            } else if (selectedCategory.equals("cameras")) {
                typeParam = "camera";
            } else if (selectedCategory.equals("electrical")) {
                // For electrical, we'll filter by category client-side
                typeParam = null;
            }
        }

        // Call API to get devices
        Call<ApiResponse<List<Device>>> call = apiService.getDevices(
            roomIdParam,
            homeIdParam,
            null, // status
            typeParam,
            null, // page
            null  // limit
        );

        call.enqueue(new Callback<ApiResponse<List<Device>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Device>>> call, Response<ApiResponse<List<Device>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Device> apiDevices = response.body().getData();
                    if (apiDevices != null) {
                        // Filter by category if needed (client-side)
                        devices = new ArrayList<>();
                        for (Device device : apiDevices) {
                            boolean matchesCategory = true;
                            if (selectedCategory != null && !selectedCategory.equals("all")) {
                                if (selectedCategory.equals("lightning")) {
                                    matchesCategory = DeviceHelper.isDeviceInCategory(device, "lighting");
                                } else if (selectedCategory.equals("cameras")) {
                                    matchesCategory = DeviceHelper.isDeviceInCategory(device, "camera");
                                } else if (selectedCategory.equals("electrical")) {
                                    matchesCategory = DeviceHelper.isDeviceInCategory(device, "electronics");
                                }
                            }

                            if (matchesCategory) {
                                devices.add(device);
                            }
                        }
                    } else {
                        devices = new ArrayList<>();
                    }
                } else {
                    android.util.Log.w("ControlDevice", "Failed to load devices from API");
                    devices = new ArrayList<>();
                }
                deviceAdapter.setDevices(devices);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Device>>> call, Throwable t) {
                android.util.Log.e("ControlDevice", "Error loading devices", t);
                devices = new ArrayList<>();
                deviceAdapter.setDevices(devices);
            }
        });
    }

    /**
     * Helper method to get room name by room ID
     */
    private String getRoomNameById(Integer roomId) {
        if (roomId == null || rooms == null) {
            return "Unknown Room";
        }

        // Look up room name from loaded rooms list
        for (Room room : rooms) {
            if (room.getId() == roomId) {
                return room.getName();
            }
        }

        return "Unknown Room";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Function selected, create task and return
            SceneTask task = new SceneTask();
            task.setType("control_device");
            task.setDeviceId(data.getIntExtra("device_id", 0));
            task.setDeviceName(data.getStringExtra("device_name"));
            task.setRoomName(data.getStringExtra("room_name"));
            task.setFunction(data.getStringExtra("function"));

            Intent resultIntent = new Intent();
            resultIntent.putExtra("task_data", task);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }
}
