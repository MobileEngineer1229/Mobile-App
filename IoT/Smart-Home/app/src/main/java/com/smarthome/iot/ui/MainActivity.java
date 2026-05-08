package com.smarthome.iot.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Typeface;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.models.Room;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.network.WebSocketManager;
import com.smarthome.iot.ui.adapters.DeviceAdapter;
import com.smarthome.iot.ui.adapters.LocationSelectorAdapter;
import com.smarthome.iot.ui.dialogs.AssignRoomDialog;
import com.smarthome.iot.ui.HomeManagementActivity;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.BottomNavSpacingHelper;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;
import com.smarthome.iot.utils.VersionCheckService;
import com.smarthome.iot.ui.dialogs.VersionUpdateDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerViewDevices;
    private LinearLayout emptyStateContainer;
    private LinearLayout roomFiltersContainer;
    private ProgressBar progressBar;
    private FrameLayout loadingOverlay;
    private androidx.core.widget.NestedScrollView nestedScrollViewMain;
    private TextView textViewLoadingMessage;
    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList;
    private List<Device> allDevicesList; // Store all devices for filtering
    private List<Room> roomsList = new ArrayList<>();
    
    private View notificationBadge;
    private Integer selectedRoomId = null; // null means "All Rooms"
    private String selectedRoomName = "All Rooms"; // Default to "All Rooms"
    private String selectedCategory = null; // null means "All Categories"
    
    private com.smarthome.iot.models.Home currentHome; // Currently selected home
    private int selectedHomeId = -1; // Track selected home ID
    
    private ApiService apiService;
    private AuthManager authManager;
    private android.widget.PopupWindow addDevicePopupWindow;
    private android.widget.PopupWindow locationSelectorPopupWindow;
    private boolean isLoadingDevices = false; // Flag to prevent multiple simultaneous device loads
    private boolean isFirstOnResume = true; // Skip duplicate loads on first onResume after onCreate

    // WebSocket for real-time device updates
    private WebSocketManager webSocketManager;
    private WebSocketManager.DeviceUpdateListener deviceUpdateListener;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.d("MainActivity", "onCreate started");
        
        // Apply saved theme before setting content view
        ThemeHelper.applySavedTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        android.util.Log.d("MainActivity", "Content view set");

        // Set status bar color to dark
        setStatusBarColor();

        // Initialize API
        try {
            apiService = ApiClient.getClient().create(ApiService.class);
            android.util.Log.d("MainActivity", "API service created");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error creating API service", e);
        }
        
        authManager = new AuthManager(this);
        android.util.Log.d("MainActivity", "AuthManager initialized, isLoggedIn: " + authManager.isLoggedIn());

        // WebSocket connection handled in onResume() via initializeWebSocket()

        initializeViews();
        setupRecyclerView();
        setupBottomNavigation();
        setupFloatingActionButtons();
        checkUnreadNotifications();
        
        // Load cached data FIRST before setting up UI to prevent double rendering
        boolean hasCachedData = loadCachedData();
        
        // Setup UI components after data is loaded (or confirmed empty)
        setupCategoryCards();
        
        // If we have cached data, UI is already set up. Otherwise, load from API
        if (!hasCachedData) {
            // Show loading overlay and hide main content while loading from API
            showLoadingState();
            
            // Initialize current home and load data from API
            if (MockDataProvider.isDemoUser(authManager)) {
                android.util.Log.d("MainActivity", "Demo user detected, using mock data");
                List<com.smarthome.iot.models.Home> homes = MockDataProvider.getMockHomes();
                if (homes != null && !homes.isEmpty()) {
                    for (com.smarthome.iot.models.Home home : homes) {
                        if (home.isPrimary()) {
                            currentHome = home;
                            selectedHomeId = home.getId();
                            updateLocationText(home.getName());
                            break;
                        }
                    }
                    if (currentHome == null) {
                        currentHome = homes.get(0);
                        selectedHomeId = homes.get(0).getId();
                        updateLocationText(homes.get(0).getName());
                    }
                    // Demo user data is loaded immediately, hide loading
                    hideLoadingState();
                } else {
                    hideLoadingState();
                }
            } else if (authManager.isLoggedIn() && apiService != null) {
                // Load primary home from API for logged-in users
                // loadPrimaryHome() will call loadRooms() which will call loadDevices()
                // Loading overlay will be hidden when data is loaded
                loadPrimaryHome();
            } else {
                // If not logged in, still try to load rooms (will use empty list)
                loadRooms();
                // Hide loading overlay after rooms are loaded (even if empty)
                hideLoadingState();
            }
        } else {
            // Cached data loaded, ensure main content is visible
            hideLoadingState();
        }
        
        // Check app version on startup (only if logged in)
        if (authManager.isLoggedIn() && !MockDataProvider.isDemoUser(authManager)) {
            checkAppVersion();
        }
        
        android.util.Log.d("MainActivity", "onCreate completed");
    }
    
    /**
     * Load cached data synchronously before setting up UI to prevent double rendering
     * @return true if cached data was loaded successfully, false otherwise
     */
    private boolean loadCachedData() {
        android.util.Log.d("MainActivity", "loadCachedData() called");
        
        if (MockDataProvider.isDemoUser(authManager)) {
            // Demo user - use mock data
            List<com.smarthome.iot.models.Home> homes = MockDataProvider.getMockHomes();
            if (homes != null && !homes.isEmpty()) {
                for (com.smarthome.iot.models.Home home : homes) {
                    if (home.isPrimary()) {
                        currentHome = home;
                        selectedHomeId = home.getId();
                        updateLocationText(home.getName());
                        break;
                    }
                }
                if (currentHome == null) {
                    currentHome = homes.get(0);
                    selectedHomeId = homes.get(0).getId();
                    updateLocationText(homes.get(0).getName());
                }
                // Demo user always has data, return true
                return true;
            }
            return false;
        }
        
        if (!authManager.isLoggedIn() || apiService == null) {
            // Not logged in - no cached data
            android.util.Log.d("MainActivity", "User not logged in, no cached data");
            return false;
        }
        
        // Check if we have cached primary home
        Home cachedPrimaryHome = Globals.getPrimaryHome();
        if (cachedPrimaryHome == null || !Globals.isHomesCacheValid()) {
            android.util.Log.d("MainActivity", "No valid cached primary home");
            return false;
        }
        
        android.util.Log.d("MainActivity", "Found cached primary home: " + cachedPrimaryHome.getName());
        currentHome = cachedPrimaryHome;
        selectedHomeId = cachedPrimaryHome.getId();
        updateLocationText(cachedPrimaryHome.getName());
        
        // Check if we have cached rooms
        List<Room> cachedRooms = Globals.getUserRooms(cachedPrimaryHome.getId());
        if (cachedRooms == null) {
            android.util.Log.d("MainActivity", "No cached rooms found");
            return false;
        }
        
        android.util.Log.d("MainActivity", "Found cached rooms: " + cachedRooms.size());
        roomsList = cachedRooms;
        // Update adapter with cached rooms
        if (deviceAdapter != null) {
            deviceAdapter.setRooms(roomsList);
        }

        // Check if we have cached devices
        List<Device> cachedDevices = Globals.getCachedDevices(cachedPrimaryHome.getId());
        if (cachedDevices == null) {
            android.util.Log.d("MainActivity", "No cached devices found");
            return false;
        }
        
        android.util.Log.d("MainActivity", "Found cached devices: " + cachedDevices.size());
        allDevicesList = cachedDevices;
        
        // Setup room filters and device list with cached data
        // Category cards will be set up by setupCategoryCards() which is called after this
        setupRoomFilters();
        filterDevicesByCategoryAndRoom();
        
        android.util.Log.d("MainActivity", "Cached data loaded successfully - UI will be set up once by setupCategoryCards()");
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.menu_theme_light) {
            ThemeHelper.applyTheme(ThemeHelper.THEME_LIGHT);
            ThemeHelper.saveTheme(this, ThemeHelper.THEME_LIGHT);
            recreate();
            Toast.makeText(this, R.string.theme_changed, Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_theme_dark) {
            ThemeHelper.applyTheme(ThemeHelper.THEME_DARK);
            ThemeHelper.saveTheme(this, ThemeHelper.THEME_DARK);
            recreate();
            Toast.makeText(this, R.string.theme_changed, Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_theme_system) {
            ThemeHelper.applyTheme(ThemeHelper.THEME_SYSTEM);
            ThemeHelper.saveTheme(this, ThemeHelper.THEME_SYSTEM);
            recreate();
            Toast.makeText(this, R.string.theme_changed, Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handle orientation changes
    }

    private void initializeViews() {
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        roomFiltersContainer = findViewById(R.id.roomFiltersContainer);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        nestedScrollViewMain = findViewById(R.id.nestedScrollViewMain);
        // Get loading message TextView from the included component
        if (loadingOverlay != null) {
            textViewLoadingMessage = loadingOverlay.findViewById(R.id.textViewLoadingMessage);
            // Set loading message for MainActivity
            if (textViewLoadingMessage != null) {
                textViewLoadingMessage.setText(R.string.loading_devices);
            }
        }
        // Progress bar is optional - check if it exists in layout
        try {
            progressBar = findViewById(R.id.progressBar);
        } catch (Exception e) {
            progressBar = null;
        }
        
        notificationBadge = findViewById(R.id.notificationBadge);
        
        TextView textViewMyHome = findViewById(R.id.textViewMyHome);
        ImageButton buttonDropdown = findViewById(R.id.buttonDropdown);
        ImageButton buttonRobot = findViewById(R.id.buttonRobot);
        ImageButton buttonNotifications = findViewById(R.id.buttonNotifications);
        ImageButton buttonMoreOptions = findViewById(R.id.buttonMoreOptions);
        
        // Make "My Home" text clickable
        if (textViewMyHome != null) {
            textViewMyHome.setOnClickListener(v -> {
                showLocationSelector(v);
            });
        }
        
        if (buttonDropdown != null) {
            buttonDropdown.setOnClickListener(v -> {
                showLocationSelector(v);
            });
        }
        
        if (buttonRobot != null) {
            buttonRobot.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ChatbotActivity.class);
                startActivity(intent);
            });
        }
        
        if (buttonNotifications != null) {
            buttonNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
                startActivity(intent);
            });
        }
        
        if (buttonMoreOptions != null) {
            buttonMoreOptions.setOnClickListener(v -> {
                Toast.makeText(this, "More options coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupRecyclerView() {
        deviceList = new ArrayList<>();
        allDevicesList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter();
        deviceAdapter.setDevices(deviceList);
        deviceAdapter.setRooms(roomsList);

        // Set click listener for navigating to device detail when clicking card
        deviceAdapter.setOnDeviceClickListener(device -> {
            Intent intent = new Intent(MainActivity.this, DeviceControlDetailActivity.class);
            intent.putExtra("device_id", device.getId());
            intent.putExtra("device_name", device.getName());
            intent.putExtra("device_type", device.getType());
            intent.putExtra("room_id", device.getRoomId() != null ? device.getRoomId() : 0);
            startActivity(intent);
        });
        
        // Set toggle listener for switch button (toggle device on/off)
        deviceAdapter.setOnDeviceToggleListener((device, isOn) -> {
            // Update UI immediately for better UX
            device.setOn(isOn);
            Toast.makeText(MainActivity.this, 
                device.getName() + " turned " + (isOn ? "ON" : "OFF"), 
                Toast.LENGTH_SHORT).show();
            // Call API to persist the change
            toggleDevicePower(device, isOn);
        });
        
        // Add long click listener to assign device to room
        deviceAdapter.setOnDeviceLongClickListener(device -> {
            showAssignRoomDialog(device);
            return true;
        });

        // Use GridLayoutManager for 2 devices per row
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerViewDevices.setLayoutManager(gridLayoutManager);
        recyclerViewDevices.setAdapter(deviceAdapter);
    }

    private void setupRoomFilters() {
        android.util.Log.d("MainActivity", "setupRoomFilters() called, roomsList size: " + (roomsList != null ? roomsList.size() : "null"));
        
        // Calculate device counts per room
        Map<Integer, Integer> deviceCountsByRoom = calculateDeviceCountsByRoom();
        
        // Check if "All Rooms" already exists
        boolean allRoomsExists = false;
        for (int i = 0; i < roomFiltersContainer.getChildCount(); i++) {
            View child = roomFiltersContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView filterView = (TextView) child;
                String filterText = filterView.getText().toString();
                if (filterText.startsWith("All Rooms")) {
                    allRoomsExists = true;
                    // Update device count if needed
                    int totalDevices = allDevicesList != null ? allDevicesList.size() : 0;
                    filterView.setText("All Rooms" + (totalDevices > 0 ? " (" + totalDevices + ")" : ""));
                    break;
                }
            }
        }
        
        // Only add "All Rooms" if it doesn't exist (initial setup)
        if (!allRoomsExists) {
            int totalDevices = allDevicesList != null ? allDevicesList.size() : 0;
            TextView allRoomsFilter = createRoomFilter("All Rooms", null, totalDevices);
            roomFiltersContainer.addView(allRoomsFilter);
            android.util.Log.d("MainActivity", "Added 'All Rooms' filter, total devices: " + totalDevices);
        }
        
        // Add room filters dynamically - only add new rooms that don't already exist
        if (roomsList != null && !roomsList.isEmpty()) {
            android.util.Log.d("MainActivity", "Adding " + roomsList.size() + " room filters");
            for (Room room : roomsList) {
                // Check if room filter already exists
                boolean roomExists = false;
                for (int i = 0; i < roomFiltersContainer.getChildCount(); i++) {
                    View child = roomFiltersContainer.getChildAt(i);
                    if (child instanceof TextView) {
                        TextView filterView = (TextView) child;
                        Object tag = filterView.getTag();
                        if (tag != null && tag.equals(room.getId())) {
                            roomExists = true;
                            // Update device count if needed
                            int deviceCount = deviceCountsByRoom.getOrDefault(room.getId(), 0);
                            filterView.setText(room.getName() + (deviceCount > 0 ? " (" + deviceCount + ")" : ""));
                            break;
                        }
                    }
                }
                
                if (!roomExists) {
                    int deviceCount = deviceCountsByRoom.getOrDefault(room.getId(), 0);
                    android.util.Log.d("MainActivity", "Adding room filter: " + room.getName() + " (id=" + room.getId() + ", devices=" + deviceCount + ")");
                    TextView roomFilter = createRoomFilter(room.getName(), room.getId(), deviceCount);
                    roomFiltersContainer.addView(roomFilter);
                }
            }
        } else {
            android.util.Log.d("MainActivity", "No rooms to display (roomsList is null or empty)");
        }
        
        // Set initial selection - default to "All Rooms" only if no room is currently selected
        // Don't reset selection if user has already selected a room
        // Only set default if selectedRoomName is null or empty
        if (selectedRoomName == null || selectedRoomName.isEmpty()) {
            selectedRoomName = "All Rooms";
            selectedRoomId = null;
            android.util.Log.d("MainActivity", "Setting default selection to 'All Rooms'");
        } else {
            android.util.Log.d("MainActivity", "Preserving existing selection: " + selectedRoomName + " (roomId=" + selectedRoomId + ")");
        }
        
        // Update filter button backgrounds to show selection
        for (int i = 0; i < roomFiltersContainer.getChildCount(); i++) {
            View child = roomFiltersContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView filterView = (TextView) child;
                String filterText = filterView.getText().toString();
                // Select based on current selection state - handle text with or without device counts
                boolean isSelected = false;
                if (selectedRoomId == null && (filterText.equals("All Rooms") || filterText.startsWith("All Rooms"))) {
                    // "All Rooms" is selected
                    isSelected = true;
                } else if (selectedRoomId != null && (filterText.equals(selectedRoomName) || filterText.startsWith(selectedRoomName + " ("))) {
                    // Specific room is selected
                    isSelected = true;
                }
                
                if (isSelected) {
                    filterView.setBackgroundResource(R.drawable.room_filter_selected);
                    android.util.Log.d("MainActivity", "Selected room filter: " + filterText);
                } else {
                    filterView.setBackgroundResource(R.drawable.room_filter_unselected);
                }
            }
        }
    }
    
    private Map<Integer, Integer> calculateDeviceCountsByRoom() {
        Map<Integer, Integer> counts = new HashMap<>();
        if (allDevicesList != null) {
            for (Device device : allDevicesList) {
                Integer roomId = device.getRoomId();
                if (roomId != null) {
                    counts.put(roomId, counts.getOrDefault(roomId, 0) + 1);
                }
            }
        }
        return counts;
    }

    private TextView createRoomFilter(String roomName, Integer roomId, int deviceCount) {
        TextView filterView = new TextView(this);
        
        // Set text with device count if provided
        String displayText = roomName;
        if (deviceCount > 0 && roomId != null) {
            displayText = roomName + " (" + deviceCount + ")";
        }
        filterView.setText(displayText);
        
        filterView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16); // Set to 16sp to match XML style
        Typeface typeface = ResourcesCompat.getFont(this, R.font.urbanist);
        if (typeface != null) {
            filterView.setTypeface(typeface, Typeface.BOLD);
        } else {
            filterView.setTypeface(null, Typeface.BOLD);
        }
        filterView.setTextColor(getResources().getColor(R.color.white, getTheme()));
        filterView.setBackgroundResource(R.drawable.room_filter_unselected);
        filterView.setPadding(
            (int) (20 * getResources().getDisplayMetrics().density), // Set to 20dp
            (int) (8 * getResources().getDisplayMetrics().density), // Set to 8dp
            (int) (20 * getResources().getDisplayMetrics().density), // Set to 20dp
            (int) (8 * getResources().getDisplayMetrics().density)  // Set to 8dp
        );
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            (int) (42 * getResources().getDisplayMetrics().density) // Reduced height to 42dp
        );
        params.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density)); // Keep 12dp margin
        filterView.setLayoutParams(params);
        
        filterView.setGravity(android.view.Gravity.CENTER_VERTICAL);
        filterView.setClickable(true);
        filterView.setFocusable(true);
        
        // Set room ID as tag for identification
        if (roomId != null) {
            filterView.setTag(roomId);
        }
        
        filterView.setOnClickListener(v -> selectRoom(roomId, roomName));
        
        return filterView;
    }

    private void selectRoom(Integer roomId, String roomName) {
        android.util.Log.d("MainActivity", "selectRoom called: roomId=" + roomId + ", roomName=" + roomName);
        selectedRoomId = roomId;
        selectedRoomName = roomName;
        com.smarthome.iot.utils.Globals.setSelectedRoomId(roomId);
        
        // Update filter button backgrounds
        for (int i = 0; i < roomFiltersContainer.getChildCount(); i++) {
            View child = roomFiltersContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView filterView = (TextView) child;
                String filterText = filterView.getText().toString();
                // Check if this is the selected room - handle text with or without device counts
                boolean isSelected = false;
                if (roomId == null && (filterText.equals("All Rooms") || filterText.startsWith("All Rooms"))) {
                    isSelected = true;
                } else if (roomId != null && (filterText.equals(roomName) || filterText.startsWith(roomName + " ("))) {
                    isSelected = true;
                }
                
                if (isSelected) {
                    filterView.setBackgroundResource(R.drawable.room_filter_selected);
                    android.util.Log.d("MainActivity", "Selected room filter: " + filterText);
                } else {
                    filterView.setBackgroundResource(R.drawable.room_filter_unselected);
                }
            }
        }
        
        // IMPORTANT: Use cached devices from allDevicesList and filter client-side
        // Make sure allDevicesList has data before filtering
        if (allDevicesList == null || allDevicesList.isEmpty()) {
            android.util.Log.w("MainActivity", "No devices in allDevicesList, loading devices first");
            loadDevices();
            return;
        }
        
        android.util.Log.d("MainActivity", "Room filter changed: roomId=" + roomId + ", filtering " + allDevicesList.size() + " cached devices");
        filterDevicesByCategoryAndRoom();
    }

    private void filterDevicesByCategoryAndRoom() {
        deviceList.clear();
        
        if (allDevicesList == null || allDevicesList.isEmpty()) {
            android.util.Log.d("MainActivity", "No devices to filter (allDevicesList is empty)");
            deviceAdapter.setDevices(deviceList);
            deviceAdapter.notifyDataSetChanged();
            updateEmptyState();
            return;
        }
        
        android.util.Log.d("MainActivity", "Filtering " + allDevicesList.size() + " devices - selectedRoomId: " + selectedRoomId + ", selectedCategory: " + selectedCategory);
        
        // Count devices with null room_id for debugging
        int nullRoomDevices = 0;
        for (Device d : allDevicesList) {
            if (d.getRoomId() == null) nullRoomDevices++;
        }
        android.util.Log.d("MainActivity", "Devices with room_id=null in allDevicesList: " + nullRoomDevices);
        
        // Filter devices by room and category
        for (Device device : allDevicesList) {
            // Filter by room - SIMPLE LOGIC
            boolean matchesRoom = false;
            
            if (selectedRoomId == null) {
                // "All Rooms" selected - show ALL devices (including null room_id)
                matchesRoom = true;
            } else {
                // Specific room selected - match by roomId
                Integer deviceRoomId = device.getRoomId();
                if (deviceRoomId != null && deviceRoomId.equals(selectedRoomId)) {
                    matchesRoom = true;
                } else {
                    matchesRoom = false;
                }
            }
            
            // Filter by category
            boolean matchesCategory = true;
            if (selectedCategory != null) {
                matchesCategory = isDeviceInCategory(device, selectedCategory);
            }
            
            // Add device if it matches both filters
            if (matchesRoom && matchesCategory) {
                deviceList.add(device);
                android.util.Log.d("MainActivity", "Added device: " + device.getName() + " (roomId=" + device.getRoomId() + ", category=" + getDeviceCategory(device) + ")");
            }
        }
        
        android.util.Log.d("MainActivity", "Filtered to " + deviceList.size() + " devices (from " + allDevicesList.size() + " total) - Room: " + (selectedRoomId == null ? "All Rooms" : selectedRoomId) + ", Category: " + (selectedCategory == null ? "All" : selectedCategory));
        
        // Update adapter and UI on main thread
        runOnUiThread(() -> {
            deviceAdapter.setDevices(deviceList);
            deviceAdapter.notifyDataSetChanged();
            updateEmptyState();
            
            // Update category counts based on ALL devices (not filtered)
            // Category counts should show total counts, not filtered counts
            if (allDevicesList != null && !allDevicesList.isEmpty()) {
                calculateAndDisplayCategoryCounts();
            }
        });
    }

    /**
     * Assign devices to rooms programmatically when all devices have null room_id
     * Distributes devices evenly across available rooms using round-robin
     */
    private void assignDevicesToRooms(List<Device> devices, List<Room> rooms) {
        if (devices == null || devices.isEmpty() || rooms == null || rooms.isEmpty()) {
            return;
        }
        
        android.util.Log.d("MainActivity", "Assigning " + devices.size() + " devices to " + rooms.size() + " rooms");
        
        // Distribute devices evenly across rooms using round-robin
        int roomIndex = 0;
        for (Device device : devices) {
            if (device.getRoomId() == null) {
                Room room = rooms.get(roomIndex % rooms.size());
                device.setRoomId(room.getId());
                android.util.Log.d("MainActivity", "Assigned device '" + device.getName() + "' (id=" + device.getId() + ") to room '" + room.getName() + "' (id=" + room.getId() + ")");
                roomIndex++;
            }
        }
        
        android.util.Log.d("MainActivity", "Finished assigning devices to rooms");
    }

    /**
     * Check if a device belongs to a specific category based on device type from database
     * Database device types: "camera", "lamp", "electronics"
     * UI categories: "cameras", "lightning", "electrical"
     */
    private boolean isDeviceInCategory(Device device, String category) {
        if (device == null || category == null) {
            return false;
        }
        
        String deviceType = device.getType() != null ? device.getType().toLowerCase().trim() : "";
        String deviceName = device.getName() != null ? device.getName().toLowerCase() : "";
        String categoryLower = category.toLowerCase();
        
        if ("lightning".equals(categoryLower)) {
            // Match devices with type="lamp" or name containing lamp/light/bulb
            return "lamp".equals(deviceType) ||
                   deviceName.contains("lamp") || 
                   deviceName.contains("light") || 
                   deviceName.contains("bulb") || 
                   deviceName.contains("lighting") ||
                   deviceType.contains("lamp") || 
                   deviceType.contains("light");
        } else if ("cameras".equals(categoryLower)) {
            // Match devices with type="camera" or name containing camera/cctv/webcam/security
            return "camera".equals(deviceType) ||
                   deviceName.contains("camera") || 
                   deviceName.contains("cctv") || 
                   deviceName.contains("webcam") || 
                   deviceName.contains("security") ||
                   deviceType.contains("camera");
        } else if ("electrical".equals(categoryLower)) {
            // Match electrical devices (type="electronics" or not lamp/camera)
            // First check if type is directly "electronics"
            if ("electronics".equals(deviceType)) {
                return true;
            }
            
            // Check if device is lightning (type="lamp" or name contains lamp/light/bulb)
            boolean isLightning = "lamp".equals(deviceType) ||
                                 deviceName.contains("lamp") || 
                                 deviceName.contains("light") || 
                                 deviceName.contains("bulb") ||
                                 deviceName.contains("lighting") ||
                                 deviceType.contains("lamp") || 
                                 deviceType.contains("light");
            
            // Check if device is camera (type="camera" or name contains camera/cctv/webcam/security)
            boolean isCamera = "camera".equals(deviceType) ||
                             deviceName.contains("camera") || 
                             deviceName.contains("cctv") || 
                             deviceName.contains("webcam") ||
                             deviceName.contains("security") ||
                             deviceType.contains("camera");
            
            // Everything that's not lightning or camera is electrical
            return !isLightning && !isCamera;
        }
        
        return false;
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation == null) {
            android.util.Log.e("MainActivity", "BottomNavigation is null!");
            return;
        }
        
        // Ensure no icon tint is applied — use drawable's own colors
        bottomNavigation.setItemIconTintList(null);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            android.util.Log.d("MainActivity", "Bottom nav item clicked: " + itemId + " (Home=" + R.id.nav_home + ", Smart=" + R.id.nav_smart + ")");
            
            // Remove active indicator background when selection changes
            BottomNavSpacingHelper.onSelectionChanged(bottomNavigation);
            
            if (itemId == R.id.nav_home) {
                // Already on home
                return true;
            } else if (itemId == R.id.nav_smart) {
                android.util.Log.d("MainActivity", "Smart clicked - navigating to SmartSceneActivity");
                Intent intent = new Intent(MainActivity.this, SmartSceneActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_reports) {
                android.util.Log.d("MainActivity", "Reports clicked - navigating to ReportsActivity");
                Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_account) {
                android.util.Log.d("MainActivity", "Account clicked - navigating to AccountActivity");
                Intent intent = new Intent(MainActivity.this, AccountActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            android.util.Log.w("MainActivity", "Unknown bottom nav item: " + itemId);
            return false;
        });
        
        // Set home as selected
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        
        // Remove spacing between icon and text
        BottomNavSpacingHelper.removeSpacing(bottomNavigation);
        
        // Disable active indicator (blue background)
        BottomNavSpacingHelper.disableActiveIndicator(bottomNavigation);
    }

    private void setupFloatingActionButtons() {
        MaterialButton fabAddDevice = findViewById(R.id.fabAddDevice);
        MaterialButton buttonMic = findViewById(R.id.buttonMic);
        MaterialButton fabClose = findViewById(R.id.fabClose);
        
        fabAddDevice.setOnClickListener(view -> {
            showAddDeviceOptions(view);
        });
        
        buttonMic.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, VoiceAssistantActivity.class);
            startActivity(intent);
        });
        
        // Initialize close button (hidden by default)
        if (fabClose != null) {
            fabClose.setVisibility(View.GONE);
        }
        
        MaterialButton buttonAddDevice = findViewById(R.id.buttonAddDevice);
        buttonAddDevice.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddDeviceActivity.class);
            startActivity(intent);
        });
    }

    private void showAddDeviceOptions(View anchorView) {
        // Dismiss location selector popup if showing
        if (locationSelectorPopupWindow != null && locationSelectorPopupWindow.isShowing()) {
            locationSelectorPopupWindow.dismiss();
            hideLocationSelectorMask();
        }
        
        // Dismiss existing popup if any
        if (addDevicePopupWindow != null && addDevicePopupWindow.isShowing()) {
            addDevicePopupWindow.dismiss();
            hideMaskAndButtons();
            return;
        }

        // Inflate the custom popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_add_device_menu, null);

        // Create PopupWindow with fixed size (185dp × 142dp)
        int popupWidth = (int)(185 * getResources().getDisplayMetrics().density);
        int popupHeight = (int)(142 * getResources().getDisplayMetrics().density);
        
        addDevicePopupWindow = new android.widget.PopupWindow(
            popupView,
            popupWidth,
            popupHeight,
            true
        );

        // Set background and elevation
        addDevicePopupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, android.R.color.transparent));
        addDevicePopupWindow.setElevation(8f);
        addDevicePopupWindow.setOutsideTouchable(true);

        // Set click listeners for menu options
        LinearLayout optionAddDevice = popupView.findViewById(R.id.optionAddDevice);
        LinearLayout optionScan = popupView.findViewById(R.id.optionScan);

        optionAddDevice.setOnClickListener(v -> {
            addDevicePopupWindow.dismiss();
            hideMaskAndButtons();
            Intent intent = new Intent(this, AddDeviceActivity.class);
            startActivity(intent);
        });

        optionScan.setOnClickListener(v -> {
            addDevicePopupWindow.dismiss();
            hideMaskAndButtons();
            Intent intent = new Intent(this, ScanDeviceActivity.class);
            startActivity(intent);
        });

        // Dismiss listener
        addDevicePopupWindow.setOnDismissListener(() -> {
            hideMaskAndButtons();
        });

        // Show popup above the anchor view, aligned to the right
        // Get anchor view location relative to the window
        int[] anchorLocation = new int[2];
        anchorView.getLocationInWindow(anchorLocation);
        
        // Use fixed dimensions (185dp × 142dp)
        int popupWidthDp = (int)(185 * getResources().getDisplayMetrics().density);
        int popupHeightDp = (int)(142 * getResources().getDisplayMetrics().density);
        int anchorWidth = anchorView.getWidth();
        int margin = (int)(8 * getResources().getDisplayMetrics().density); // 8dp margin
        
        // Calculate position: align right edge of popup with right edge of button
        // Position it above the button so it doesn't hide the FAB
        int x = anchorLocation[0] + anchorWidth - popupWidthDp; // Align right edges
        int y = anchorLocation[1] - popupHeightDp - margin; // Show above with margin
        
        // Show mask overlay and update buttons before showing popup
        showMaskAndButtons();
        
        // Show popup at calculated position
        addDevicePopupWindow.showAtLocation(
            anchorView,
            android.view.Gravity.NO_GRAVITY,
            x,
            y
        );
    }

    private void showMaskAndButtons() {
        // Show mask overlay
        View maskOverlay = findViewById(R.id.maskOverlay);
        if (maskOverlay != null) {
            maskOverlay.setVisibility(View.VISIBLE);
            // Dismiss popup when mask is clicked (but allow touches to pass through to bottom navigation)
            maskOverlay.setOnTouchListener((v, event) -> {
                // Get bottom navigation view
                BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
                if (bottomNav != null) {
                    // Get bottom navigation location
                    int[] bottomNavLocation = new int[2];
                    bottomNav.getLocationOnScreen(bottomNavLocation);
                    int bottomNavTop = bottomNavLocation[1];
                    int bottomNavLeft = bottomNavLocation[0];
                    int bottomNavRight = bottomNavLeft + bottomNav.getWidth();
                    
                    // Get touch location
                    int touchY = (int) event.getRawY();
                    int touchX = (int) event.getRawX();
                    
                    // If touch is in bottom navigation area, let it pass through
                    if (touchY >= bottomNavTop && touchX >= bottomNavLeft && touchX <= bottomNavRight) {
                        return false; // Let the event pass through to bottom navigation
                    }
                }
                
                // For other areas, only handle ACTION_UP (click) to dismiss popup
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    if (addDevicePopupWindow != null && addDevicePopupWindow.isShowing()) {
                        addDevicePopupWindow.dismiss();
                    }
                    hideMaskAndButtons();
                    return true; // Consume the click
                }
                
                // For other touch events, don't consume them (let them pass through)
                return false;
            });
        }
        
        // Hide plus button (fabAddDevice)
        MaterialButton fabAddDevice = findViewById(R.id.fabAddDevice);
        if (fabAddDevice != null) {
            fabAddDevice.setVisibility(View.GONE);
        }
        
        // Show close button (fabClose) in the same position
        MaterialButton fabClose = findViewById(R.id.fabClose);
        if (fabClose != null) {
            fabClose.setVisibility(View.VISIBLE);
            fabClose.setOnClickListener(v -> {
                if (addDevicePopupWindow != null && addDevicePopupWindow.isShowing()) {
                    addDevicePopupWindow.dismiss();
                }
                hideMaskAndButtons();
            });
        }
    }

    private void hideMaskAndButtons() {
        // Hide mask overlay
        View maskOverlay = findViewById(R.id.maskOverlay);
        if (maskOverlay != null) {
            maskOverlay.setVisibility(View.GONE);
        }
        
        // Show plus button (fabAddDevice) again
        MaterialButton fabAddDevice = findViewById(R.id.fabAddDevice);
        if (fabAddDevice != null) {
            fabAddDevice.setVisibility(View.VISIBLE);
        }
        
        // Hide close button (fabClose)
        MaterialButton fabClose = findViewById(R.id.fabClose);
        if (fabClose != null) {
            fabClose.setVisibility(View.GONE);
        }
    }

    private void showLocationSelectorMask() {
        // Show mask overlay
        View maskOverlay = findViewById(R.id.maskOverlay);
        if (maskOverlay != null) {
            maskOverlay.setVisibility(View.VISIBLE);
            // Dismiss popup when mask is clicked (but allow touches to pass through to bottom navigation)
            maskOverlay.setOnTouchListener((v, event) -> {
                // Get bottom navigation view
                BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
                if (bottomNav != null) {
                    // Get bottom navigation location
                    int[] bottomNavLocation = new int[2];
                    bottomNav.getLocationOnScreen(bottomNavLocation);
                    int bottomNavTop = bottomNavLocation[1];
                    int bottomNavLeft = bottomNavLocation[0];
                    int bottomNavRight = bottomNavLeft + bottomNav.getWidth();
                    
                    // Get touch location
                    int touchY = (int) event.getRawY();
                    int touchX = (int) event.getRawX();
                    
                    // If touch is in bottom navigation area, let it pass through
                    if (touchY >= bottomNavTop && touchX >= bottomNavLeft && touchX <= bottomNavRight) {
                        return false; // Let the event pass through to bottom navigation
                    }
                }
                
                // For other areas, only handle ACTION_UP (click) to dismiss popup
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    if (locationSelectorPopupWindow != null && locationSelectorPopupWindow.isShowing()) {
                        locationSelectorPopupWindow.dismiss();
                    }
                    hideLocationSelectorMask();
                    return true; // Consume the click
                }
                
                // For other touch events, don't consume them (let them pass through)
                return false;
            });
        }
    }

    private void hideLocationSelectorMask() {
        // Hide mask overlay
        View maskOverlay = findViewById(R.id.maskOverlay);
        if (maskOverlay != null) {
            maskOverlay.setVisibility(View.GONE);
        }
    }

    private void setupCategoryCards() {
        LinearLayout cardLightning = findViewById(R.id.cardLightning);
        LinearLayout cardCameras = findViewById(R.id.cardCameras);
        LinearLayout cardElectrical = findViewById(R.id.cardElectrical);

        // Hide all category cards initially
        if (cardLightning != null) {
            cardLightning.setVisibility(View.GONE);
            cardLightning.setOnClickListener(v -> {
                Intent intent = new Intent(this, DeviceCategoryListActivity.class);
                intent.putExtra("category", "lightning");
                startActivity(intent);
            });
        }

        if (cardCameras != null) {
            cardCameras.setVisibility(View.GONE);
            cardCameras.setOnClickListener(v -> {
                Intent intent = new Intent(this, DeviceCategoryListActivity.class);
                intent.putExtra("category", "cameras");
                startActivity(intent);
            });
        }

        if (cardElectrical != null) {
            cardElectrical.setVisibility(View.GONE);
            cardElectrical.setOnClickListener(v -> {
                Intent intent = new Intent(this, DeviceCategoryListActivity.class);
                intent.putExtra("category", "electrical");
                startActivity(intent);
            });
        }

        // Update category cards container visibility based on current data
        updateCategoryCardsContainerVisibility();

        // Load category counts only if we have devices (will show container if devices exist)
        // If data was already loaded from cache, this will use the cached data
        // If data is being loaded from API, this will wait until devices are loaded
        if (allDevicesList != null && !allDevicesList.isEmpty()) {
            // Data already loaded (from cache), calculate counts now
            calculateAndDisplayCategoryCounts();
        } else {
            // Data not loaded yet, loadCategoryCounts will be called after devices are loaded
            loadCategoryCounts();
        }
    }

    /**
     * Hide a category card
     */
    private void hideCategoryCard(String category) {
        runOnUiThread(() -> {
            LinearLayout card = null;
            if (category.equals("lightning")) {
                card = findViewById(R.id.cardLightning);
            } else if (category.equals("cameras")) {
                card = findViewById(R.id.cardCameras);
            } else if (category.equals("electrical")) {
                card = findViewById(R.id.cardElectrical);
            }
            
            if (card != null) {
                card.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Hide or show the entire category cards container based on device list
     */
    private void updateCategoryCardsContainerVisibility() {
        runOnUiThread(() -> {
            LinearLayout container = findViewById(R.id.categoryCardsContainer);
            if (container != null) {
                if (allDevicesList == null || allDevicesList.isEmpty()) {
                    container.setVisibility(View.GONE);
                } else {
                    container.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadCategoryCounts() {
        if (!authManager.isLoggedIn() || apiService == null) {
            // Hide entire category cards container when not logged in
            updateCategoryCardsContainerVisibility();
            return;
        }

        // Only load counts if devices exist
        if (allDevicesList == null || allDevicesList.isEmpty()) {
            updateCategoryCardsContainerVisibility();
            return;
        }

        // Calculate category counts from the actual filtered device list (allDevicesList)
        // This ensures counts match what's actually displayed
        calculateAndDisplayCategoryCounts();
    }

    /**
     * Calculate category counts from allDevicesList and update UI
     */
    private void calculateAndDisplayCategoryCounts() {
        if (allDevicesList == null || allDevicesList.isEmpty()) {
            updateCategoryCardsContainerVisibility();
            return;
        }

        int lightningCount = 0;
        int camerasCount = 0;
        int electricalCount = 0;

        for (Device device : allDevicesList) {
            String category = getDeviceCategory(device);
            String deviceType = device.getType() != null ? device.getType() : "null";
            android.util.Log.d("MainActivity", "Device: id=" + device.getId() + ", name=" + device.getName() + ", type=" + deviceType + " → category=" + category);
            
            // Every device should have a category (lightning, cameras, or electrical)
            if (category != null) {
                if (category.equals("lightning")) {
                    lightningCount++;
                } else if (category.equals("cameras")) {
                    camerasCount++;
                } else {
                    // Everything else is electrical
                    electricalCount++;
                }
            } else {
                // If category is null (shouldn't happen with updated logic), count as electrical
                electricalCount++;
            }
        }
        
        android.util.Log.d("MainActivity", "Category counts - Lightning: " + lightningCount + ", Cameras: " + camerasCount + ", Electrical: " + electricalCount + " (Total: " + allDevicesList.size() + " devices)");

        // Create final copies for use in lambda expression
        final int finalLightningCount = lightningCount;
        final int finalCamerasCount = camerasCount;
        final int finalElectricalCount = electricalCount;

        // Update UI on main thread
        runOnUiThread(() -> {
            updateCategoryCard("lightning", finalLightningCount, R.id.textViewLightningCount, R.id.cardLightning);
            updateCategoryCard("cameras", finalCamerasCount, R.id.textViewCamerasCount, R.id.cardCameras);
            updateCategoryCard("electrical", finalElectricalCount, R.id.textViewElectricalCount, R.id.cardElectrical);
        });
    }

    /**
     * Get device category based on device type from database
     * Database device types: "camera", "lamp", "electronics"
     * Maps to UI categories: "cameras", "lightning", "electrical"
     */
    private String getDeviceCategory(Device device) {
        if (device == null) {
            return "electrical"; // Default to electrical for null devices
        }
        
        String deviceType = device.getType() != null ? device.getType().toLowerCase().trim() : "";
        String deviceName = device.getName() != null ? device.getName().toLowerCase() : "";
        
        // First, check device type directly from database
        if ("lamp".equals(deviceType)) {
            return "lightning";
        } else if ("camera".equals(deviceType)) {
            return "cameras";
        } else if ("electronics".equals(deviceType)) {
            return "electrical";
        }
        
        // Fallback: Check device name for common patterns
        // Check for lightning (lamps, lights, bulbs)
        if (deviceName.contains("lamp") || deviceName.contains("light") || 
            deviceName.contains("bulb") || deviceName.contains("lighting") ||
            deviceType.contains("lamp") || deviceType.contains("light")) {
            return "lightning";
        }
        
        // Check for cameras
        if (deviceName.contains("camera") || deviceName.contains("cctv") || 
            deviceName.contains("webcam") || deviceName.contains("security") ||
            deviceType.contains("camera")) {
            return "cameras";
        }
        
        // Everything else is electrical/electronics
        return "electrical";
    }

    /**
     * Update a single category card with count
     */
    private void updateCategoryCard(String category, int count, int textViewId, int cardId) {
        LinearLayout card = findViewById(cardId);
        if (card != null) {
            if (count > 0) {
                card.setVisibility(View.VISIBLE);
                TextView textView = findViewById(textViewId);
                if (textView != null) {
                    String text = "";
                    if (category.equals("lightning")) {
                        text = count + " lights";
                    } else if (category.equals("cameras")) {
                        text = count + " cameras";
                    } else if (category.equals("electrical")) {
                        text = count + " devices";
                    }
                    textView.setText(text);
                }
            } else {
                card.setVisibility(View.GONE);
            }
        }
    }

    private void loadCategoryCount(String category, int textViewId, int cardId) {
        if (!authManager.isLoggedIn() || apiService == null) {
            hideCategoryCard(category);
            return;
        }

        // If device list is empty, hide the card immediately
        if (allDevicesList == null || allDevicesList.isEmpty()) {
            hideCategoryCard(category);
            return;
        }

        // Filter by current home when loading category counts
        Integer homeIdParam = (currentHome != null) ? currentHome.getId() : null;
        Call<ApiResponse<List<Device>>> call = apiService.getDevicesByCategory(category, null);
        call.enqueue(new Callback<ApiResponse<List<Device>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Device>>> call, Response<ApiResponse<List<Device>>> response) {
                // Double-check if device list is still empty (might have changed)
                if (allDevicesList == null || allDevicesList.isEmpty()) {
                    hideCategoryCard(category);
                    return;
                }
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Device> devices = response.body().getData();
                    // Filter by home if needed (since getDevicesByCategory doesn't support homeId yet)
                    if (homeIdParam != null && devices != null) {
                        // Filter devices to only include those in rooms belonging to the current home
                        List<Device> filteredDevices = new ArrayList<>();
                        for (Device device : devices) {
                            if (device.getRoomId() != null) {
                                // Check if device's room belongs to current home
                                for (Room room : roomsList) {
                                    if (room.getId() == device.getRoomId() && 
                                        room.getHomeId() != null && 
                                        room.getHomeId().equals(homeIdParam)) {
                                        filteredDevices.add(device);
                                        break;
                                    }
                                }
                            } else {
                                // Include devices without rooms (they belong to the user, not specific to a home)
                                filteredDevices.add(device);
                            }
                        }
                        devices = filteredDevices;
                    }
                    int count = devices != null ? devices.size() : 0;
                    
                    // Show or hide card based on count (on UI thread)
                    runOnUiThread(() -> {
                        // Final check: if device list is empty, hide card regardless of API response
                        if (allDevicesList == null || allDevicesList.isEmpty()) {
                            hideCategoryCard(category);
                            return;
                        }
                        
                        LinearLayout card = findViewById(cardId);
                        if (card != null) {
                            if (count > 0) {
                                card.setVisibility(View.VISIBLE);
                                TextView textView = findViewById(textViewId);
                                if (textView != null) {
                                    String text = "";
                                    if (category.equals("lightning")) {
                                        text = count + " lights";
                                    } else if (category.equals("cameras")) {
                                        text = count + " cameras";
                                    } else if (category.equals("electrical")) {
                                        text = count + " devices";
                                    }
                                    textView.setText(text);
                                }
                            } else {
                                card.setVisibility(View.GONE);
                            }
                        }
                    });
                } else {
                    android.util.Log.w("MainActivity", "Failed to load category count for " + category);
                    // Hide card when API fails
                    hideCategoryCard(category);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Device>>> call, Throwable t) {
                android.util.Log.e("MainActivity", "Error loading category count for " + category, t);
                // Hide card when API fails
                hideCategoryCard(category);
            }
        });
    }

    private void loadRooms() {
        android.util.Log.d("MainActivity", "loadRooms() called, isLoggedIn: " + authManager.isLoggedIn());
        
        if (!authManager.isLoggedIn() || apiService == null) {
            android.util.Log.d("MainActivity", "User not logged in or API service unavailable");
            roomsList = new ArrayList<>();
            setupRoomFilters();
            loadDevices();
            return;
        }

        // Check if we have cached rooms in Globals
        if (currentHome != null) {
            List<Room> cachedRooms = Globals.getUserRooms(currentHome.getId());
            if (cachedRooms != null) {
                android.util.Log.d("MainActivity", "Using cached rooms from Globals: " + cachedRooms.size() + " rooms");
                roomsList = cachedRooms;
                // Update adapter with cached rooms
                if (deviceAdapter != null) {
                    deviceAdapter.setRooms(roomsList);
                }
                setupRoomFilters();
                loadDevices();
                return; // Use cached data, no API call needed
            }
        }

        android.util.Log.d("MainActivity", "Making API call to getRooms, homeId: " + (currentHome != null ? currentHome.getId() : "null"));
        showProgressBar();
        // Filter rooms by home_id if available
        Integer homeIdParam = (currentHome != null) ? currentHome.getId() : null;
        Call<ApiResponse<List<Room>>> call = apiService.getRooms(homeIdParam);
        call.enqueue(new Callback<ApiResponse<List<Room>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Room>>> call, Response<ApiResponse<List<Room>>> response) {
                hideProgressBar();
                android.util.Log.d("MainActivity", "getRooms API response: isSuccessful=" + response.isSuccessful() + ", code=" + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        List<Room> newRoomsList = response.body().getData();
                        if (newRoomsList == null) {
                            newRoomsList = new ArrayList<>();
                        }
                        android.util.Log.d("MainActivity", "Loaded " + newRoomsList.size() + " rooms from API (homeId=" + homeIdParam + ")");
                        
                        // Save rooms to Globals
                        if (homeIdParam != null) {
                            Globals.setUserRooms(newRoomsList, homeIdParam);
                        }
                        
                        // Only update roomsList if:
                        // 1. We got rooms back, OR
                        // 2. We're loading without homeId filter (initial load), OR
                        // 3. Current roomsList is empty
                        if (!newRoomsList.isEmpty() || homeIdParam == null || (roomsList == null || roomsList.isEmpty())) {
                            roomsList = newRoomsList;
                            // Update adapter with new rooms list so device cards show correct room names
                            if (deviceAdapter != null) {
                                deviceAdapter.setRooms(roomsList);
                            }
                            android.util.Log.d("MainActivity", "Updated roomsList to " + roomsList.size() + " rooms");
                            // Log room details for debugging
                            for (Room room : roomsList) {
                                android.util.Log.d("MainActivity", "Room: id=" + room.getId() + ", name=" + room.getName() + ", homeId=" + room.getHomeId());
                            }
                        } else {
                            android.util.Log.d("MainActivity", "Filtered rooms list is empty, preserving existing " + (roomsList != null ? roomsList.size() : 0) + " rooms");
                        }
                    } else {
                        android.util.Log.w("MainActivity", "API returned success=false: " + (response.body().getError() != null ? response.body().getError().getMessage() : "Unknown error"));
                        // Don't clear existing rooms on error - preserve them
                        if (roomsList == null) {
                            roomsList = new ArrayList<>();
                        }
                    }
                } else {
                    android.util.Log.w("MainActivity", "API call unsuccessful: code=" + response.code() + ", message=" + (response.message() != null ? response.message() : "No message"));
                    if (response.body() != null && response.body().getError() != null) {
                        android.util.Log.w("MainActivity", "Error: " + response.body().getError().getMessage());
                    }
                    // Don't clear existing rooms on error - preserve them
                    if (roomsList == null) {
                        roomsList = new ArrayList<>();
                    }
                }
                
                // Always setup room filters after loading rooms
                setupRoomFilters();
                // Load devices after rooms are loaded
                loadDevices();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Room>>> call, Throwable t) {
                hideProgressBar();
                android.util.Log.e("MainActivity", "getRooms API call failed", t);
                android.util.Log.e("MainActivity", "Error message: " + t.getMessage());
                if (t.getCause() != null) {
                    android.util.Log.e("MainActivity", "Cause: " + t.getCause().getMessage());
                }
                // Don't clear existing rooms on failure - preserve them
                if (roomsList == null) {
                    roomsList = new ArrayList<>();
                }
                // Only setup room filters if we have rooms, otherwise preserve existing UI
                if (roomsList != null && !roomsList.isEmpty()) {
                    setupRoomFilters();
                }
                loadDevices();
            }
        });
    }

    private void loadDevices() {
        // Prevent multiple simultaneous API calls
        if (isLoadingDevices) {
            android.util.Log.d("MainActivity", "loadDevices() already in progress, skipping duplicate call");
            return;
        }
        
        if (!authManager.isLoggedIn() || apiService == null) {
            // Show empty state when not logged in
            allDevicesList = new ArrayList<>();
            // Hide entire category cards container when not logged in
            updateCategoryCardsContainerVisibility();
            filterDevicesByCategoryAndRoom();
            setupRoomFilters();
            return;
        }

        // Check if we have valid cached devices in Globals for the current home
        if (currentHome != null) {
            List<Device> cachedDevices = Globals.getCachedDevices(currentHome.getId());
            if (cachedDevices != null) {
                android.util.Log.d("MainActivity", "Using cached devices from Globals: " + cachedDevices.size() + " devices for homeId=" + currentHome.getId());
                allDevicesList = cachedDevices;

                // Sync power state from metadata for all cached devices
                for (Device device : allDevicesList) {
                    device.syncPowerFromMetadata();
                }

                // Fix: If cached devices have null room_id, assign them to rooms programmatically
                int devicesWithNullRoom = 0;
                for (Device device : allDevicesList) {
                    if (device.getRoomId() == null) {
                        devicesWithNullRoom++;
                    }
                }
                if (devicesWithNullRoom > 0 && roomsList != null && !roomsList.isEmpty()) {
                    android.util.Log.d("MainActivity", devicesWithNullRoom + " cached devices have null room_id - assigning to rooms programmatically");
                    assignDevicesToRooms(allDevicesList, roomsList);
                    // Update cache with assigned room_ids
                    Globals.setCachedDevices(allDevicesList, currentHome.getId());
                }
                
                updateCategoryCardsContainerVisibility();
                calculateAndDisplayCategoryCounts();
                filterDevicesByCategoryAndRoom();
                setupRoomFilters();
                
                // Hide loading overlay if it was shown (e.g., during initial API load chain)
                hideLoadingState();
                return; // Use cached data, no API call needed
            }
        }

        // No valid cache, load from API
        isLoadingDevices = true;
        showProgressBar();
        
        // Always load ALL devices for the home (no room filter)
        // Room filtering will be done client-side from cached data
        Integer homeIdParam = null;
        Integer roomIdParam = null; // Always null - we want all devices for the home
        
        if (currentHome != null) {
            homeIdParam = currentHome.getId();
        }
        
        // Create final copies for use in inner class (callback)
        final Integer finalHomeIdParam = homeIdParam;
        
        android.util.Log.d("MainActivity", "Loading ALL devices from API for homeId: " + homeIdParam + " (will cache and filter client-side)");
        
        // Always load all devices for the home (no room filter) - room filtering done client-side
        Call<ApiResponse<List<Device>>> call = apiService.getDevices(null, homeIdParam, null, null, 1, 100);
        call.enqueue(new Callback<ApiResponse<List<Device>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Device>>> call, Response<ApiResponse<List<Device>>> response) {
                isLoadingDevices = false;
                hideProgressBar();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    allDevicesList = response.body().getData();
                    if (allDevicesList == null) {
                        allDevicesList = new ArrayList<>();
                    }
                    
                    android.util.Log.d("MainActivity", "Loaded " + allDevicesList.size() + " devices from API for homeId=" + finalHomeIdParam);
                    
                    // Cache devices in Globals for future use
                    if (finalHomeIdParam != null) {
                        Globals.setCachedDevices(allDevicesList, finalHomeIdParam);
                    }
                    
                    // Log device details for debugging
                    int devicesWithNullRoom = 0;
                    for (Device device : allDevicesList) {
                        String category = getDeviceCategory(device);
                        if (device.getRoomId() == null) {
                            devicesWithNullRoom++;
                            android.util.Log.d("MainActivity", "Device with NULL room_id: id=" + device.getId() + ", name=" + device.getName() + ", category=" + category);
                        }
                        android.util.Log.d("MainActivity", "Device: id=" + device.getId() + ", name=" + device.getName() + ", roomId=" + device.getRoomId() + ", category=" + category);
                        device.syncStatus();
                        device.syncPowerFromMetadata(); // Sync power state from metadata
                    }
                    android.util.Log.d("MainActivity", "Total devices loaded: " + allDevicesList.size() + ", Devices with room_id=null: " + devicesWithNullRoom);
                    
                    // Fix: If devices have null room_id, assign them to rooms programmatically
                    // This ensures room filtering works even when backend doesn't assign room_id
                    if (devicesWithNullRoom > 0 && roomsList != null && !roomsList.isEmpty()) {
                        android.util.Log.d("MainActivity", devicesWithNullRoom + " devices have null room_id - assigning to rooms programmatically");
                        assignDevicesToRooms(allDevicesList, roomsList);
                        // Update cache with assigned room_ids
                        if (finalHomeIdParam != null) {
                            Globals.setCachedDevices(allDevicesList, finalHomeIdParam);
                        }
                    }
                    
                    // Hide entire category cards container if no devices exist
                    if (allDevicesList.isEmpty()) {
                        updateCategoryCardsContainerVisibility();
                    } else {
                        // Show container and calculate category counts from actual device list
                        updateCategoryCardsContainerVisibility();
                        calculateAndDisplayCategoryCounts();
                    }
                    
                    // Apply current room and category filters (client-side filtering)
                    filterDevicesByCategoryAndRoom();
                    // Refresh room filters to update device counts (preserve user's room selection)
                    setupRoomFilters();
                    
                    // Hide loading overlay after devices are loaded (final step in data loading chain)
                    hideLoadingState();
                } else {
                    android.util.Log.w("MainActivity", "API call failed - showing empty state");
                    // Show empty state when API fails
                    allDevicesList = new ArrayList<>();
                    // Hide entire category cards container when API fails
                    updateCategoryCardsContainerVisibility();
                    filterDevicesByCategoryAndRoom();
                    // Only update room filters if rooms are already loaded
                    if (roomsList != null && !roomsList.isEmpty()) {
                        setupRoomFilters();
                    }
                    
                    // Hide loading overlay even on failure
                    hideLoadingState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Device>>> call, Throwable t) {
                isLoadingDevices = false;
                hideProgressBar();
                android.util.Log.e("MainActivity", "Error loading devices from API", t);
                
                // Hide loading overlay on failure
                hideLoadingState();
                // Show empty state when API fails
                allDevicesList = new ArrayList<>();
                // Hide entire category cards container when API fails
                updateCategoryCardsContainerVisibility();
                filterDevicesByCategoryAndRoom();
                // Only update room filters if rooms are already loaded
                if (roomsList != null && !roomsList.isEmpty()) {
                    setupRoomFilters();
                }
            }
        });
    }

    private void showAssignRoomDialog(Device device) {
        if (roomsList == null || roomsList.isEmpty()) {
            Toast.makeText(this, "No rooms available. Please create a room first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find current room for this device
        Room currentRoom = null;
        if (device.getRoomId() != null) {
            for (Room room : roomsList) {
                if (room.getId() == device.getRoomId()) {
                    currentRoom = room;
                    break;
                }
            }
        }

        AssignRoomDialog dialog = AssignRoomDialog.newInstance(roomsList, currentRoom);
        dialog.setOnRoomSelectedListener(new AssignRoomDialog.OnRoomSelectedListener() {
            @Override
            public void onRoomSelected(Room room) {
                assignDeviceToRoom(device, room);
            }

            @Override
            public void onNoRoomSelected() {
                assignDeviceToRoom(device, null);
            }
        });
        dialog.show(getSupportFragmentManager(), "AssignRoomDialog");
    }

    private void assignDeviceToRoom(Device device, Room room) {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressBar();
        
        Map<String, Object> updateData = new HashMap<>();
        if (room != null) {
            updateData.put("roomId", room.getId());
        } else {
            updateData.put("roomId", null);
        }

        Call<ApiResponse<Device>> call = apiService.updateDevice(device.getId(), updateData);
        call.enqueue(new Callback<ApiResponse<Device>>() {
            @Override
            public void onResponse(Call<ApiResponse<Device>> call, Response<ApiResponse<Device>> response) {
                hideProgressBar();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Device updatedDevice = response.body().getData();
                    if (updatedDevice != null) {
                        // Update device in local list
                        for (int i = 0; i < allDevicesList.size(); i++) {
                            if (allDevicesList.get(i).getId().equals(device.getId())) {
                                allDevicesList.set(i, updatedDevice);
                                break;
                            }
                        }
                        
                        // Update Globals cache
                        if (currentHome != null) {
                            Globals.setCachedDevices(allDevicesList, currentHome.getId());
                            // Also update the device in Globals
                            Globals.addDeviceToCache(updatedDevice);
                        }
                        
                        // Refresh filtered list
                        filterDevicesByCategoryAndRoom();
                        setupRoomFilters(); // Update room filter counts
                        
                        String message = room != null 
                            ? "Device assigned to " + room.getName()
                            : "Device removed from room";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "Failed to assign device to room";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMessage = response.body().getError().getMessage();
                    }
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Device>> call, Throwable t) {
                hideProgressBar();
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        if (deviceList.isEmpty()) {
            recyclerViewDevices.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            recyclerViewDevices.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
    
    /**
     * Show loading overlay and hide main content during API loading
     * Uses same pattern as SignInActivity and SignUpActivity
     */
    private void showLoadingState() {
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);
            }
            if (nestedScrollViewMain != null) {
                nestedScrollViewMain.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * Hide loading overlay and show main content after data is loaded
     * Uses same pattern as SignInActivity and SignUpActivity
     */
    private void hideLoadingState() {
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }
            if (nestedScrollViewMain != null) {
                nestedScrollViewMain.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showLocationSelector(View anchorView) {
        // Dismiss add device popup if showing
        if (addDevicePopupWindow != null && addDevicePopupWindow.isShowing()) {
            addDevicePopupWindow.dismiss();
            hideMaskAndButtons();
        }
        
        if (!authManager.isLoggedIn()) {
            // Show demo locations if not logged in
            showDemoLocationSelector();
            return;
        }
        
        // Use mock data for demo user
        if (MockDataProvider.isDemoUser(authManager)) {
            List<com.smarthome.iot.models.Home> homes = MockDataProvider.getMockHomes();
            // Use the anchorView parameter passed to this method
            showHomeSelector(homes, anchorView);
            return;
        }
        
        // Check Globals cache first
        if (Globals.isHomesCacheValid()) {
            List<com.smarthome.iot.models.Home> cachedHomes = Globals.getUserHomes();
            if (cachedHomes != null && !cachedHomes.isEmpty()) {
                android.util.Log.d("MainActivity", "Using cached homes from Globals: " + cachedHomes.size() + " homes");
                // Use the parameter anchorView instead of creating a new one
                showHomeSelector(cachedHomes, anchorView);
                return;
            }
        }
        
        // Load homes from API for non-demo users
        Call<ApiResponse<List<com.smarthome.iot.models.Home>>> call = apiService.getHomes();
        call.enqueue(new Callback<ApiResponse<List<com.smarthome.iot.models.Home>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<com.smarthome.iot.models.Home>>> call, 
                                 Response<ApiResponse<List<com.smarthome.iot.models.Home>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.smarthome.iot.models.Home> homes = response.body().getData();
                    if (homes != null && !homes.isEmpty()) {
                        // Save to Globals
                        Globals.setUserHomes(homes);
                        
                        // Use the parameter anchorView instead of creating a new one
                        showHomeSelector(homes, anchorView);
                    } else {
                        showDemoLocationSelector();
                    }
                } else {
                    showDemoLocationSelector();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<com.smarthome.iot.models.Home>>> call, Throwable t) {
                showDemoLocationSelector();
            }
        });
    }
    
    private void showHomeSelector(List<com.smarthome.iot.models.Home> homes, View anchorView) {
        // Dismiss existing popup if any
        if (locationSelectorPopupWindow != null && locationSelectorPopupWindow.isShowing()) {
            locationSelectorPopupWindow.dismiss();
            hideLocationSelectorMask();
            return;
        }
        
        // Find primary home or first home as default selection
        if (selectedHomeId == -1 && homes != null && !homes.isEmpty()) {
            for (com.smarthome.iot.models.Home home : homes) {
                if (home.isPrimary()) {
                    selectedHomeId = home.getId();
                    currentHome = home;
                    break;
                }
            }
            if (selectedHomeId == -1) {
                selectedHomeId = homes.get(0).getId();
                currentHome = homes.get(0);
            }
        }
        
        // Inflate the custom popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.dialog_location_selector, null);
        RecyclerView recyclerViewHomes = popupView.findViewById(R.id.recyclerViewHomes);
        LinearLayout layoutHomeManagement = popupView.findViewById(R.id.layoutHomeManagement);
        
        // Measure the popup view to get its height
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec((int)(248 * getResources().getDisplayMetrics().density), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int popupHeight = popupView.getMeasuredHeight();
        int popupWidth = (int)(248 * getResources().getDisplayMetrics().density);
        
        // Create PopupWindow with fixed width (248dp)
        locationSelectorPopupWindow = new android.widget.PopupWindow(
            popupView,
            popupWidth,
            popupHeight,
            true
        );
        
        // Set background and elevation
        locationSelectorPopupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, android.R.color.transparent));
        locationSelectorPopupWindow.setElevation(8f);
        locationSelectorPopupWindow.setOutsideTouchable(true);
        
        // Ensure homes list is not null
        if (homes == null) {
            homes = new ArrayList<>();
        }
        
        // Setup RecyclerView with popup reference
        LocationSelectorAdapter adapter = new LocationSelectorAdapter(  
            homes, 
            selectedHomeId, 
            home -> {
                selectedHomeId = home.getId();
                currentHome = home;
                updateLocationText(home.getName());
                
                // Update Globals
                Globals.addOrUpdateHome(home);
                
                // Reload rooms and devices for selected home
                loadRooms();
                locationSelectorPopupWindow.dismiss();
                hideLocationSelectorMask();
            }
        );
        
        recyclerViewHomes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHomes.setAdapter(adapter);
        
        // Ensure Home Management is always visible
        layoutHomeManagement.setVisibility(View.VISIBLE);
        
        // Home Management click - navigate to HomeManagementActivity
        layoutHomeManagement.setOnClickListener(v -> {
            locationSelectorPopupWindow.dismiss();
            hideLocationSelectorMask();
            Intent intent = new Intent(this, HomeManagementActivity.class);
            startActivity(intent);
        });
        
        // Dismiss listener
        locationSelectorPopupWindow.setOnDismissListener(() -> {
            hideLocationSelectorMask();
        });
        
        // Position popup below the "My Home" text view, aligned from the left edge
        int[] location = new int[2];
        if (anchorView != null) {
            anchorView.getLocationInWindow(location);
        } else {
            // Fallback: use default position
            location[0] = 24;
            location[1] = 100;
        }
        
        // Convert dp to pixels for spacing and margin
        float density = getResources().getDisplayMetrics().density;
        int spacingPx = (int) (8 * density); // 8dp spacing
        int leftMarginPx = (int) (24 * density); // 24dp left margin from screen edge
        
        // Align popup from the left edge of the screen with 24dp margin
        int x = leftMarginPx;
        int y;
        if (anchorView != null) {
            y = location[1] + anchorView.getHeight() + spacingPx; // Just below the text view
        } else {
            y = location[1] + spacingPx; // Fallback position
        }
        
        // Show mask overlay before showing popup
        showLocationSelectorMask();
        
        // Show popup at calculated position
        locationSelectorPopupWindow.showAtLocation(
            anchorView != null ? anchorView : findViewById(android.R.id.content),
            android.view.Gravity.NO_GRAVITY,
            x,
            y
        );
    }
    
    private void updateLocationText(String homeName) {
        TextView textViewMyHome = findViewById(R.id.textViewMyHome);
        if (textViewMyHome != null) {
            textViewMyHome.setText(homeName);
        }
    }
    
    /**
     * Load primary home from API and update the "My Home" text view
     */
    private void loadPrimaryHome() {
        if (apiService == null || !authManager.isLoggedIn()) {
            android.util.Log.w("MainActivity", "Cannot load primary home: API service not initialized or user not logged in");
            hideLoadingState();
            return;
        }
        
        android.util.Log.d("MainActivity", "Loading primary home from API");
        Call<ApiResponse<com.smarthome.iot.models.Home>> call = apiService.getPrimaryHome();
        call.enqueue(new Callback<ApiResponse<com.smarthome.iot.models.Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.smarthome.iot.models.Home>> call, 
                                 Response<ApiResponse<com.smarthome.iot.models.Home>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.smarthome.iot.models.Home primaryHome = response.body().getData();
                    if (primaryHome != null) {
                        // Clear cache if home changed
                        if (currentHome != null && currentHome.getId() != primaryHome.getId()) {
                            android.util.Log.d("MainActivity", "Home changed, clearing device cache");
                            Globals.clearDeviceCache();
                        }
                        
                        currentHome = primaryHome;
                        selectedHomeId = primaryHome.getId();
                        updateLocationText(primaryHome.getName());
                        android.util.Log.d("MainActivity", "Primary home loaded: " + primaryHome.getName());
                        
                        // Save to Globals
                        Globals.addOrUpdateHome(primaryHome);
                        
                        // Load all homes and save to Globals
                        loadAllHomesForGlobals();
                        
                        // Load rooms after home is loaded (rooms will then load devices)
                        // This ensures proper sequencing: Home → Rooms → Devices
                        loadRooms();
                    } else {
                        android.util.Log.w("MainActivity", "Primary home response has no data");
                        // Fallback: try to get all homes and find primary
                        loadAllHomesAndSetPrimary();
                    }
                } else {
                    android.util.Log.w("MainActivity", "Failed to load primary home, trying to get all homes");
                    // Fallback: try to get all homes and find primary
                    loadAllHomesAndSetPrimary();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.smarthome.iot.models.Home>> call, Throwable t) {
                android.util.Log.e("MainActivity", "Error loading primary home", t);
                // Hide loading overlay on failure
                hideLoadingState();
                // Fallback: try to get all homes and find primary
                loadAllHomesAndSetPrimary();
            }
        });
    }
    
    /**
     * Fallback: Load all homes and set the primary one
     */
    /**
     * Load all homes and save to Globals (called after login or when needed)
     */
    private void loadAllHomesForGlobals() {
        if (apiService == null || !authManager.isLoggedIn()) {
            return;
        }
        
        android.util.Log.d("MainActivity", "Loading all homes for Globals");
        Call<ApiResponse<List<com.smarthome.iot.models.Home>>> call = apiService.getHomes();
        call.enqueue(new Callback<ApiResponse<List<com.smarthome.iot.models.Home>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<com.smarthome.iot.models.Home>>> call, 
                                 Response<ApiResponse<List<com.smarthome.iot.models.Home>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.smarthome.iot.models.Home> homes = response.body().getData();
                    if (homes != null && !homes.isEmpty()) {
                        // Save all homes to Globals
                        Globals.setUserHomes(homes);
                        android.util.Log.d("MainActivity", "Saved " + homes.size() + " homes to Globals");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<List<com.smarthome.iot.models.Home>>> call, Throwable t) {
                android.util.Log.e("MainActivity", "Error loading all homes for Globals", t);
            }
        });
    }
    
    private void loadAllHomesAndSetPrimary() {
        if (apiService == null || !authManager.isLoggedIn()) {
            return;
        }
        
        android.util.Log.d("MainActivity", "Loading all homes to find primary");
        Call<ApiResponse<List<com.smarthome.iot.models.Home>>> call = apiService.getHomes();
        call.enqueue(new Callback<ApiResponse<List<com.smarthome.iot.models.Home>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<com.smarthome.iot.models.Home>>> call, 
                                 Response<ApiResponse<List<com.smarthome.iot.models.Home>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.smarthome.iot.models.Home> homes = response.body().getData();
                    if (homes != null && !homes.isEmpty()) {
                        // Save all homes to Globals
                        Globals.setUserHomes(homes);
                        // Find primary home
                        for (com.smarthome.iot.models.Home home : homes) {
                            if (home.isPrimary()) {
                                // Clear cache if home changed
                                if (currentHome != null && currentHome.getId() != home.getId()) {
                                    android.util.Log.d("MainActivity", "Home changed, clearing device cache");
                                    Globals.clearDeviceCache();
                                }
                                
                                currentHome = home;
                                selectedHomeId = home.getId();
                                updateLocationText(home.getName());
                                android.util.Log.d("MainActivity", "Primary home found from all homes: " + home.getName());
                                
                                // Save to Globals (already saved in setUserHomes, but ensure it's set)
                                Globals.addOrUpdateHome(home);
                                
                                // Load rooms after home is loaded (rooms will then load devices)
                                loadRooms();
                                return;
                            }
                        }
                        // If no primary found, use first home
                        com.smarthome.iot.models.Home firstHome = homes.get(0);
                        // Clear cache if home changed
                        if (currentHome != null && currentHome.getId() != firstHome.getId()) {
                            android.util.Log.d("MainActivity", "Home changed, clearing device cache");
                            Globals.clearDeviceCache();
                        }
                        
                        currentHome = firstHome;
                        selectedHomeId = firstHome.getId();
                        updateLocationText(firstHome.getName());
                        android.util.Log.d("MainActivity", "No primary home found, using first home: " + firstHome.getName());
                        
                        // Save to Globals (already saved in setUserHomes, but ensure it's set)
                        Globals.addOrUpdateHome(firstHome);
                        
                        // Load rooms after home is loaded (rooms will then load devices)
                        loadRooms();
                    } else {
                        android.util.Log.w("MainActivity", "No homes found for user");
                        // New user with no homes - show empty state
                        allDevicesList = new ArrayList<>();
                        roomsList = new ArrayList<>();
                        updateCategoryCardsContainerVisibility();
                        filterDevicesByCategoryAndRoom();
                        setupRoomFilters();
                        hideLoadingState();
                    }
                } else {
                    android.util.Log.w("MainActivity", "Failed to load homes for user");
                    allDevicesList = new ArrayList<>();
                    roomsList = new ArrayList<>();
                    updateCategoryCardsContainerVisibility();
                    filterDevicesByCategoryAndRoom();
                    setupRoomFilters();
                    hideLoadingState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<com.smarthome.iot.models.Home>>> call, Throwable t) {
                android.util.Log.e("MainActivity", "Error loading all homes", t);
                // Hide loading overlay on failure
                hideLoadingState();
                // Keep default "My Home" text
            }
        });
    }
    
    private void showDemoLocationSelector() {
        // Use mock homes for demo
        List<com.smarthome.iot.models.Home> demoHomes = MockDataProvider.getMockHomes();
        View anchorView = findViewById(R.id.textViewMyHome);
        showHomeSelector(demoHomes, anchorView);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Handle when MainActivity is brought to front via FLAG_ACTIVITY_CLEAR_TOP
        android.util.Log.d("MainActivity", "onNewIntent called - checking cache first");
        // Check cache first, only call API if cache is invalid
        if (authManager != null && authManager.isLoggedIn()) {
            if (currentHome != null && Globals.isDeviceCacheValid(currentHome.getId())) {
                List<Device> cachedDevices = Globals.getCachedDevices(currentHome.getId());
                if (cachedDevices != null) {
                    allDevicesList = cachedDevices;
                    filterDevicesByCategoryAndRoom();
                    setupRoomFilters();
                } else {
                    loadRooms();
                }
            } else {
                loadRooms();
            }
        }
    }
    
    /**
     * Force refresh devices from API (clears cache and reloads)
     * Call this when devices are added/updated/deleted from other activities
     */
    public void refreshDevicesFromApi() {
        android.util.Log.d("MainActivity", "Force refreshing devices from API");
        if (currentHome != null) {
            Globals.clearDeviceCache();
        }
        loadDevices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Skip the first onResume since onCreate already loaded everything
        if (isFirstOnResume) {
            isFirstOnResume = false;
            return;
        }
        checkUnreadNotifications();

        // Initialize WebSocket for real-time device updates
        initializeWebSocket();

        // Reload data when returning to activity
        if (authManager != null && authManager.isLoggedIn()) {
            // Check if we have cached devices - if cache is valid, use it instead of API call
            if (currentHome != null && Globals.isDeviceCacheValid(currentHome.getId())) {
                android.util.Log.d("MainActivity", "onResume: Using cached devices, no API call needed");
                List<Device> cachedDevices = Globals.getCachedDevices(currentHome.getId());
                if (cachedDevices != null) {
                    allDevicesList = cachedDevices;
                    // Sync power state from metadata for all cached devices
                    for (Device device : allDevicesList) {
                        device.syncPowerFromMetadata();
                    }
                    filterDevicesByCategoryAndRoom();
                    setupRoomFilters();
                }
            } else {
                // Refresh primary home name in case it was changed in another activity
                if (!MockDataProvider.isDemoUser(authManager) && apiService != null) {
                    // Only refresh if we don't have a current home, or refresh to show latest primary
                    // This ensures the "My Home" text shows the current primary home name
                    if (currentHome == null || (currentHome != null && currentHome.isPrimary())) {
                        loadPrimaryHome();
                    } else {
                        // If user selected a non-primary home, check cache first
                        if (currentHome != null && Globals.isDeviceCacheValid(currentHome.getId())) {
                            List<Device> cachedDevices = Globals.getCachedDevices(currentHome.getId());
                            if (cachedDevices != null) {
                                allDevicesList = cachedDevices;
                                filterDevicesByCategoryAndRoom();
                                setupRoomFilters();
                            } else {
                                loadRooms();
                            }
                        } else {
                            loadRooms();
                        }
                    }
                } else {
                    loadRooms();
                }
            }
        }
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void checkAppVersion() {
        VersionCheckService versionCheckService = new VersionCheckService(this);
        versionCheckService.checkVersion(new VersionCheckService.VersionCheckCallback() {
            @Override
            public void onVersionCheckComplete(com.smarthome.iot.models.VersionCheckResponse response) {
                // Only show dialog if update is available or required
                if (response.isUpdateAvailable() || response.isUpdateRequired()) {
                    runOnUiThread(() -> {
                        VersionUpdateDialog dialog = new VersionUpdateDialog(MainActivity.this);
                        dialog.show(response);
                    });
                }
            }

            @Override
            public void onVersionCheckError(String error) {
                android.util.Log.e("MainActivity", "Version check error: " + error);
                // Silently fail - don't block user if version check fails
            }
        });
    }

    private void checkUnreadNotifications() {
        if (notificationBadge == null || apiService == null) {
            return;
        }

        if (!authManager.isLoggedIn()) {
            // Not logged in - hide badge
            notificationBadge.setVisibility(View.GONE);
            return;
        }

        // Always use API to check notification stats for unread count
        Call<com.smarthome.iot.models.ApiResponse<com.smarthome.iot.models.NotificationStats>> call = 
            apiService.getNotificationStats();
        call.enqueue(new Callback<com.smarthome.iot.models.ApiResponse<com.smarthome.iot.models.NotificationStats>>() {
            @Override
            public void onResponse(Call<com.smarthome.iot.models.ApiResponse<com.smarthome.iot.models.NotificationStats>> call, 
                                 Response<com.smarthome.iot.models.ApiResponse<com.smarthome.iot.models.NotificationStats>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.smarthome.iot.models.NotificationStats stats = response.body().getData();
                    if (stats != null && stats.getUnread() > 0) {
                        notificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                    }
                } else {
                    // Fallback: check notifications directly
                    checkUnreadNotificationsFallback();
                }
            }

            @Override
            public void onFailure(Call<com.smarthome.iot.models.ApiResponse<com.smarthome.iot.models.NotificationStats>> call, Throwable t) {
                // Fallback: check notifications directly
                checkUnreadNotificationsFallback();
            }
        });
    }

    private void checkUnreadNotificationsFallback() {
        if (apiService == null || notificationBadge == null) {
            return;
        }

        // Fallback: Get notifications and check for unread
        Call<com.smarthome.iot.models.ApiResponse<List<com.smarthome.iot.models.Notification>>> call = 
            apiService.getNotifications(null, 100, null, null, false); // Get unread notifications
        call.enqueue(new Callback<com.smarthome.iot.models.ApiResponse<List<com.smarthome.iot.models.Notification>>>() {
            @Override
            public void onResponse(Call<com.smarthome.iot.models.ApiResponse<List<com.smarthome.iot.models.Notification>>> call, 
                                 Response<com.smarthome.iot.models.ApiResponse<List<com.smarthome.iot.models.Notification>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.smarthome.iot.models.Notification> notifications = response.body().getData();
                    boolean hasUnread = notifications != null && !notifications.isEmpty();
                    if (notificationBadge != null) {
                        notificationBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                    }
                } else {
                    if (notificationBadge != null) {
                        notificationBadge.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<com.smarthome.iot.models.ApiResponse<List<com.smarthome.iot.models.Notification>>> call, Throwable t) {
                if (notificationBadge != null) {
                    notificationBadge.setVisibility(View.GONE);
                }
            }
        });
    }

    private void toggleDevicePower(Device device, boolean isOn) {
        if (device == null || device.getId() == null) {
            android.util.Log.w("MainActivity", "Invalid device, cannot toggle power");
            return;
        }

        if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            // Demo user - just log
            android.util.Log.d("MainActivity", "Demo user - Device power toggled: " + isOn);
            return;
        }

        // Prepare power data
        Map<String, Boolean> powerData = new HashMap<>();
        powerData.put("power", isOn);

        Call<ApiResponse<Map<String, Object>>> call = apiService.controlDevicePower(device.getId(), powerData);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> data = response.body().getData();
                    Object commandId = data != null ? data.get("commandId") : null;
                    android.util.Log.d("MainActivity", "Device power toggled successfully: " + isOn +
                        " (commandId: " + commandId + ", deviceId: " + device.getId() + ")");
                    // Device state update will come via WebSocket from backend after MQTT/EMQX confirmation
                } else {
                    // Revert device state on failure
                    device.setOn(!isOn);
                    if (deviceAdapter != null) {
                        deviceAdapter.notifyDataSetChanged();
                    }
                    android.util.Log.w("MainActivity", "Failed to toggle device power via API for deviceId: " + device.getId());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                // Revert device state on failure
                device.setOn(!isOn);
                if (deviceAdapter != null) {
                    deviceAdapter.notifyDataSetChanged();
                }
                android.util.Log.e("MainActivity", "Error toggling device power for deviceId: " + device.getId(), t);
            }
        });
    }

    // ========== WebSocket Methods ==========

    /**
     * Initialize WebSocket connection for real-time device updates
     * Called in onResume when user is logged in
     */
    private void initializeWebSocket() {
        android.util.Log.d("MainActivity", "initializeWebSocket() called");

        if (authManager == null || !authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            android.util.Log.d("MainActivity", "Not logged in or demo user, skipping WebSocket initialization");
            return;
        }

        String token = authManager.getToken();
        if (token == null || token.isEmpty()) {
            android.util.Log.w("MainActivity", "Cannot initialize WebSocket: token is null or empty");
            return;
        }

        // Get WebSocketManager instance
        webSocketManager = WebSocketManager.getInstance();

        // Setup device update listener
        deviceUpdateListener = new WebSocketManager.DeviceUpdateListener() {
            @Override
            public void onDeviceStatusUpdate(JsonObject deviceData) {
                android.util.Log.d("MainActivity", "WebSocket device status update: " + deviceData.toString());
                handleDeviceStatusUpdate(deviceData);
            }

            @Override
            public void onDevicePropertyUpdate(JsonObject propertyData) {
                android.util.Log.d("MainActivity", "WebSocket device property update: " + propertyData.toString());
                handleDevicePropertyUpdate(propertyData);
            }
        };

        // Add listener and connect
        webSocketManager.addDeviceUpdateListener(deviceUpdateListener);
        webSocketManager.addConnectionListener(new WebSocketManager.ConnectionListener() {
            @Override
            public void onConnected() {
                android.util.Log.d("MainActivity", "WebSocket connected successfully");
                runOnUiThread(() -> {
                    // Optionally show a toast or indicator
                    // Toast.makeText(MainActivity.this, "Real-time updates enabled", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDisconnected(String reason) {
                android.util.Log.d("MainActivity", "WebSocket disconnected: " + reason);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("MainActivity", "WebSocket error: " + error);
            }
        });

        webSocketManager.connect(token);
        android.util.Log.d("MainActivity", "WebSocket connection initiated");
    }

    /**
     * Handle device status update from WebSocket
     * Updates device in list and refreshes UI
     */
    private void handleDeviceStatusUpdate(JsonObject deviceData) {
        try {
            // Extract device information from WebSocket message
            // Expected format: { "productId": "...", "deviceNum": "...", "status": "online/offline", ... }
            if (!deviceData.has("productId") || !deviceData.has("deviceNum")) {
                android.util.Log.w("MainActivity", "Device status update missing productId or deviceNum");
                return;
            }

            String productId = deviceData.get("productId").getAsString();
            String deviceNum = deviceData.get("deviceNum").getAsString();

            // Find device in list by productId and deviceNum (or other identifier)
            Device targetDevice = null;
            for (Device device : allDevicesList) {
                // Match by productId/deviceNum if available, otherwise by ID
                if (device.getMetadata() != null) {
                    // Convert Map to JsonObject
                    Map<String, Object> metadataMap = device.getMetadata();
                    JsonObject metadata = gson.toJsonTree(metadataMap).getAsJsonObject();
                    if (metadata.has("productId") && metadata.has("deviceNum")) {
                        String devProductId = metadata.get("productId").getAsString();
                        String devDeviceNum = metadata.get("deviceNum").getAsString();
                        if (devProductId.equals(productId) && devDeviceNum.equals(deviceNum)) {
                            targetDevice = device;
                            break;
                        }
                    }
                }
            }

            if (targetDevice == null) {
                android.util.Log.w("MainActivity", "Device not found in list: " + productId + "/" + deviceNum);
                return;
            }

            // Update device status
            if (deviceData.has("status")) {
                String status = deviceData.get("status").getAsString();
                targetDevice.setStatus(status);
                android.util.Log.d("MainActivity", "Device status updated: " + targetDevice.getName() + " -> " + status);
            }

            // Update power state if present
            if (deviceData.has("power")) {
                boolean power = deviceData.get("power").getAsBoolean();
                targetDevice.setOn(power);
                android.util.Log.d("MainActivity", "Device power updated: " + targetDevice.getName() + " -> " + power);
            }

            // Refresh UI on main thread
            runOnUiThread(() -> {
                if (deviceAdapter != null) {
                    deviceAdapter.notifyDataSetChanged();
                    android.util.Log.d("MainActivity", "Device adapter notified of changes");
                }
            });

        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error handling device status update", e);
        }
    }

    /**
     * Handle device property update from WebSocket
     * Updates device properties and refreshes UI
     */
    private void handleDevicePropertyUpdate(JsonObject propertyData) {
        try {
            // Extract device information
            if (!propertyData.has("productId") || !propertyData.has("deviceNum")) {
                android.util.Log.w("MainActivity", "Device property update missing productId or deviceNum");
                return;
            }

            String productId = propertyData.get("productId").getAsString();
            String deviceNum = propertyData.get("deviceNum").getAsString();

            // Find device in list
            Device targetDevice = null;
            for (Device device : allDevicesList) {
                if (device.getMetadata() != null) {
                    // Convert Map to JsonObject
                    Map<String, Object> metadataMap = device.getMetadata();
                    JsonObject metadata = gson.toJsonTree(metadataMap).getAsJsonObject();
                    if (metadata.has("productId") && metadata.has("deviceNum")) {
                        String devProductId = metadata.get("productId").getAsString();
                        String devDeviceNum = metadata.get("deviceNum").getAsString();
                        if (devProductId.equals(productId) && devDeviceNum.equals(deviceNum)) {
                            targetDevice = device;
                            break;
                        }
                    }
                }
            }

            if (targetDevice == null) {
                android.util.Log.w("MainActivity", "Device not found for property update: " + productId + "/" + deviceNum);
                return;
            }

            // Update device properties (brightness, temperature, etc.)
            if (propertyData.has("properties")) {
                JsonObject properties = propertyData.getAsJsonObject("properties");
                // Merge properties into device metadata
                Map<String, Object> metadataMap = targetDevice.getMetadata();
                JsonObject metadata = metadataMap != null ? gson.toJsonTree(metadataMap).getAsJsonObject() : new JsonObject();

                // Copy all properties to metadata
                for (String key : properties.keySet()) {
                    metadata.add(key, properties.get(key));
                }

                // Convert JsonObject back to Map and set it
                Map<String, Object> updatedMetadataMap = gson.fromJson(metadata, Map.class);
                targetDevice.setMetadata(updatedMetadataMap);
                android.util.Log.d("MainActivity", "Device properties updated: " + targetDevice.getName());
            }

            // Refresh UI on main thread
            runOnUiThread(() -> {
                if (deviceAdapter != null) {
                    deviceAdapter.notifyDataSetChanged();
                }
            });

        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error handling device property update", e);
        }
    }

    /**
     * Disconnect WebSocket when activity is paused or destroyed
     */
    private void disconnectWebSocket() {
        android.util.Log.d("MainActivity", "disconnectWebSocket() called");

        if (webSocketManager != null) {
            if (deviceUpdateListener != null) {
                webSocketManager.removeDeviceUpdateListener(deviceUpdateListener);
                deviceUpdateListener = null;
            }
            webSocketManager.disconnect();
            android.util.Log.d("MainActivity", "WebSocket disconnected");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.d("MainActivity", "onPause called");
        // Disconnect WebSocket when activity is not visible
        disconnectWebSocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.d("MainActivity", "onDestroy called");
        // Ensure WebSocket is disconnected
        disconnectWebSocket();
    }
}
