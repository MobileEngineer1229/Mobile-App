package com.smarthome.iot.ui;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.models.Room;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.services.FastBeeBluetoothService;
import com.smarthome.iot.ui.views.CircularProgressView;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Device detection, verification, and connection activity.
 *
 * This is the core screen in the device registration flow where the actual
 * backend API calls and MQTT verification happen. It shows two states:
 *
 * === State 1: Device Detected (Initial) ===
 * Shows the detected device with its icon, name, and a "Connect" button.
 * Displays WiFi/Bluetooth requirements and device selection checkbox.
 *
 * === State 2: Connecting (Progress) ===
 * Shows a circular progress ring (0-100%) around the device image with
 * "Connecting..." text and percentage. Progress animates to 90% while
 * backend API calls are in progress, then completes to 100% on success.
 *
 * === Backend API Integration ===
 * When "Connect" is tapped, the following API calls are made in sequence:
 *
 * 1. MQTT Verification (if device has MQTT metadata from QR/setup code):
 *    POST /api/v1/devices/verify-mqtt
 *    Body: { productId: "1001", deviceNum: "lamp-living-01" }
 *    Response: { reachable: true/false }
 *    - Checks if device is online on FastBee MQTT broker
 *    - Registration proceeds even if device is offline (it may come online later)
 *
 * 2. Get Primary Home:
 *    GET /api/v1/homes/primary
 *    - Required: every device must belong to a home (homeId is mandatory)
 *    - Fallback: GET /api/v1/homes to find any available home
 *
 * 3. Get Rooms:
 *    GET /api/v1/homes/{homeId}/rooms
 *    - Assigns device to the first available room (optional)
 *
 * 4. Create Device:
 *    POST /api/v1/devices
 *    Body: {
 *      name: "Smart Lamp (lamp-living-01)",
 *      type: "lamp",                    // lamp | camera | electronics
 *      macAddress: "AA:BB:CC:DD:EE:FF", // randomly generated
 *      homeId: 1,                       // REQUIRED
 *      roomId: 1,                       // optional
 *      metadata: {
 *        deviceCategory: "lighting",
 *        connectionSource: "qr_scan" | "setup_code" | "nearby" | "manual",
 *        protocol: "mqtt",              // only for QR/setup code devices
 *        productId: "1001",             // FastBee product ID
 *        deviceNum: "lamp-living-01"    // FastBee device number
 *      }
 *    }
 *    - Backend creates device record and binds to MQTT topic if protocol=mqtt
 *    - MQTT topic pattern: /fastbee/{productId}/{deviceNum}/#
 *
 * 5. On success: navigates to DeviceConnectedActivity with device info
 *    On failure: shows error dialog and resets to initial state
 *
 * === Intent Extras (Input) ===
 * - device_name: Human-readable device name
 * - connection_source: "manual" | "nearby" | "qr_scan" | "setup_code"
 * - mqtt_product_id: FastBee productId (optional, from QR/setup code)
 * - mqtt_device_num: FastBee deviceNum (optional, from QR/setup code)
 * - mqtt_device_type: Device type (optional, from QR/setup code)
 *
 * @see ScanDeviceActivity QR code entry point
 * @see EnterSetupCodeActivity Manual code entry point
 * @see DeviceConnectedActivity Success screen after connection
 * @see com.smarthome.iot.services.MqttService Real-time MQTT communication
 */
public class DeviceDetectedActivity extends AppCompatActivity {

    private MaterialButton buttonConnect;
    private ImageView imageViewDevice;
    private LinearLayout progressContainer;
    private CircularProgressView progressBar;
    private ImageView imageViewDeviceProgress;
    private TextView textViewProgress;
    private Integer primaryHomeId = null; // Store primary home ID for device creation (REQUIRED - must be set before creating device)
    private TextView textViewDeviceName;
    private TextView textViewDeviceNameProgress;
    private LinearLayout deviceSelection;
    private LinearLayout deviceSelectionProgress;
    private com.google.android.material.button.MaterialButton buttonNearbyDevices;
    private com.google.android.material.button.MaterialButton buttonAddManual;
    private LinearLayout tabButtonsContainer;
    private TextView textViewCantConnect;
    private TextView textViewCantConnectProgress;
    private TextView textViewConnectingStatus;
    private LinearLayout wifiBluetoothContainer;
    private String deviceName;
    private String connectionSource; // "manual", "nearby", "qr_scan", "setup_code"
    private String mqttProductId;    // FastBee productId from QR/setup code
    private String mqttDeviceNum;    // FastBee deviceNum from QR/setup code
    private String mqttDeviceType;   // Device type inferred from productId
    private int progress = 0;
    private boolean isConnecting = false;
    
    private ApiService apiService;
    private AuthManager authManager;
    private Handler progressHandler;
    private Runnable progressRunnable;
    private FastBeeBluetoothService fastBeeBluetoothService;
    private ActivityResultLauncher<String[]> bluetoothPermissionLauncher;
    private Runnable pendingBluetoothAction;
    private Integer pendingProvisionDeviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detected);

        // Set status bar color to dark
        setStatusBarColor();

        deviceName = getIntent().getStringExtra("device_name");
        if (deviceName == null) {
            deviceName = "Smart VI CCTV";
        }
        
        // Get connection source (manual, nearby, qr_scan, setup_code)
        connectionSource = getIntent().getStringExtra("connection_source");
        if (connectionSource == null) {
            connectionSource = "nearby"; // Default to nearby
        }

        // Get MQTT metadata from QR scan or setup code entry
        mqttProductId = getIntent().getStringExtra("mqtt_product_id");
        mqttDeviceNum = getIntent().getStringExtra("mqtt_device_num");
        mqttDeviceType = getIntent().getStringExtra("mqtt_device_type");

        // Initialize API service and auth manager
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        // Initialize FastBee Bluetooth service for device provisioning
        fastBeeBluetoothService = new FastBeeBluetoothService(this);

        // Register Bluetooth permission launcher before STARTED (required by Activity Result API)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean scanGranted = result.get(Manifest.permission.BLUETOOTH_SCAN);
                    Boolean connectGranted = result.get(Manifest.permission.BLUETOOTH_CONNECT);
                    if (Boolean.TRUE.equals(scanGranted) && Boolean.TRUE.equals(connectGranted)) {
                        if (pendingBluetoothAction != null) {
                            pendingBluetoothAction.run();
                            pendingBluetoothAction = null;
                        }
                    } else {
                        Toast.makeText(this, "Bluetooth permissions denied. Device registered without provisioning.", Toast.LENGTH_LONG).show();
                        // Still complete the flow - device is already registered in backend
                        completeProgressAnimation(pendingProvisionDeviceId);
                    }
                }
            );
        }

        initializeViews();
        setupTabButtons(); // Setup tab buttons before showing initial state
        setupDeviceSpecificContent();
        setupClickListeners();
        
        // Initialize to first state (initial detection) - after all setup is done
        showInitialState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Bluetooth connections
        if (fastBeeBluetoothService != null) {
            fastBeeBluetoothService.disconnect();
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
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        ImageButton buttonMore = findViewById(R.id.buttonMore);
        buttonConnect = findViewById(R.id.buttonConnect);
        imageViewDevice = findViewById(R.id.imageViewDevice);
        progressContainer = findViewById(R.id.progressContainer);
        progressBar = findViewById(R.id.progressBar);
        imageViewDeviceProgress = findViewById(R.id.imageViewDeviceProgress);
        textViewProgress = findViewById(R.id.textViewProgress);
        textViewDeviceName = findViewById(R.id.textViewDeviceName);
        textViewDeviceNameProgress = findViewById(R.id.textViewDeviceNameProgress);
        deviceSelection = findViewById(R.id.deviceSelection);
        deviceSelectionProgress = findViewById(R.id.deviceSelectionProgress);
        buttonNearbyDevices = findViewById(R.id.buttonNearbyDevices);
        buttonAddManual = findViewById(R.id.buttonAddManual);
        tabButtonsContainer = findViewById(R.id.tabButtonsContainer);
        textViewCantConnect = findViewById(R.id.textViewCantConnect);
        textViewCantConnectProgress = findViewById(R.id.textViewCantConnectProgress);
        textViewConnectingStatus = findViewById(R.id.textViewConnectingStatus);
        wifiBluetoothContainer = findViewById(R.id.wifiBluetoothContainer);

        textViewDeviceName.setText(deviceName);
        if (textViewDeviceNameProgress != null) {
            textViewDeviceNameProgress.setText(deviceName);
        }

        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            showChatbotHelp();
        });
    }
    
    private void showInitialState() {
        // Show initial state: checkbox, device image, connect button
        deviceSelection.setVisibility(View.VISIBLE);
        imageViewDevice.setVisibility(View.VISIBLE);
        buttonConnect.setVisibility(View.VISIBLE);
        progressContainer.setVisibility(View.GONE);
        if (textViewCantConnect != null) {
            textViewCantConnect.setVisibility(View.VISIBLE);
        }
        if (textViewCantConnectProgress != null) {
            textViewCantConnectProgress.setVisibility(View.GONE);
        }
        isConnecting = false;
        
        // Re-enable tab buttons if they exist and are visible
        if (tabButtonsContainer != null && tabButtonsContainer.getVisibility() == View.VISIBLE) {
            buttonNearbyDevices.setClickable(true);
            buttonAddManual.setClickable(true);
            buttonNearbyDevices.setAlpha(1.0f);
            buttonAddManual.setAlpha(1.0f);
        }
    }

    private void setupTabButtons() {
        // Show tab buttons only for "Nearby Device" mode, hide for "Add Manual" mode
        if ("manual".equals(connectionSource)) {
            tabButtonsContainer.setVisibility(View.GONE);
        } else {
            // Show tab buttons for "nearby" mode
            tabButtonsContainer.setVisibility(View.VISIBLE);
            
            // Set initial state based on connection source
            updateTabButtonStates();
            
            // Make tab buttons clickable and functional
            buttonNearbyDevices.setOnClickListener(v -> {
                if (!"nearby".equals(connectionSource) && !isConnecting) {
                    connectionSource = "nearby";
                    updateTabButtonStates();
                    // Keep tab buttons visible for nearby mode
                    tabButtonsContainer.setVisibility(View.VISIBLE);
                    showInitialState();
                }
            });
            
            buttonAddManual.setOnClickListener(v -> {
                if (!"manual".equals(connectionSource) && !isConnecting) {
                    connectionSource = "manual";
                    updateTabButtonStates();
                    // Hide tab buttons for manual mode
                    tabButtonsContainer.setVisibility(View.GONE);
                    showInitialState();
                }
            });
        }
    }
    
    private void updateTabButtonStates() {
        // Use new tab button style - works like a switch: one selected, one unselected
        // Clear background tint to ensure our drawables are used (no stroke)
        buttonNearbyDevices.setBackgroundTintList(null);
        buttonNearbyDevices.setElevation(0f);
        buttonAddManual.setBackgroundTintList(null);
        buttonAddManual.setElevation(0f);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            buttonNearbyDevices.setStateListAnimator(null);
            buttonAddManual.setStateListAnimator(null);
        }
        
        if ("nearby".equals(connectionSource)) {
            // Nearby Devices selected, Add Manual unselected
            buttonNearbyDevices.post(() -> {
                buttonNearbyDevices.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_selected_background));
                buttonNearbyDevices.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                buttonNearbyDevices.invalidate();
            });
            buttonAddManual.post(() -> {
                buttonAddManual.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_unselected_background));
                buttonAddManual.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                buttonAddManual.invalidate();
            });
        } else if ("manual".equals(connectionSource)) {
            // Nearby Devices unselected, Add Manual selected
            buttonNearbyDevices.post(() -> {
                buttonNearbyDevices.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_unselected_background));
                buttonNearbyDevices.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                buttonNearbyDevices.invalidate();
            });
            buttonAddManual.post(() -> {
                buttonAddManual.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_selected_background));
                buttonAddManual.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                buttonAddManual.invalidate();
            });
        }
    }

    /**
     * Get drawable resource ID for device based on device name
     */
    private int getDeviceIconResId(String deviceName) {
        if (deviceName == null) {
            return R.drawable.ic_device;
        }
        
        // Normalize device name for comparison
        String normalizedName = deviceName.toLowerCase();
        
        if (normalizedName.contains("lamp") || normalizedName.contains("light")) {
            return R.drawable.lamp;
        } else if (normalizedName.contains("v1 cctv") || normalizedName.contains("smart v1")) {
            return R.drawable.cctv_v1;
        } else if (normalizedName.contains("v2 cctv") || normalizedName.contains("smart v2")) {
            return R.drawable.cctv_v2;
        } else if (normalizedName.contains("speaker")) {
            return R.drawable.speaker;
        } else if (normalizedName.contains("router") || normalizedName.contains("wifi") || normalizedName.contains("wi-fi")) {
            return R.drawable.router;
        } else if (normalizedName.contains("webcam")) {
            return R.drawable.webcam;
        } else if (normalizedName.contains("air") || normalizedName.contains("conditioner") || normalizedName.contains("ac")) {
            return R.drawable.air_conditioner;
        } else if (normalizedName.contains("compact")) {
            return R.drawable.compact;
        } else {
            return R.drawable.ic_device;
        }
    }

    private void setupDeviceSpecificContent() {
        // Set device-specific images using new design images
        int iconResId = getDeviceIconResId(deviceName);
        imageViewDevice.setImageResource(iconResId);
        imageViewDeviceProgress.setImageResource(iconResId);
    }

    private void setupClickListeners() {
        buttonConnect.setOnClickListener(v -> {
            startConnecting();
        });

        TextView textViewLearnMore = findViewById(R.id.textViewLearnMore);
        textViewLearnMore.setOnClickListener(v -> {
            showChatbotHelp();
        });
    }

    private void startConnecting() {
        if (isConnecting) {
            return; // Already connecting
        }
        
        isConnecting = true;
        
        // Hide initial state elements
        deviceSelection.setVisibility(View.GONE);
        imageViewDevice.setVisibility(View.GONE);
        buttonConnect.setVisibility(View.GONE);
        
        // Show "Can't connect" text at bottom during connecting (above "Learn more")
        if (textViewCantConnect != null) {
            textViewCantConnect.setVisibility(View.VISIBLE);
        }
        
        // Disable tab buttons during connecting (but keep them visible if they were visible)
        if (tabButtonsContainer != null && tabButtonsContainer.getVisibility() == View.VISIBLE) {
            buttonNearbyDevices.setClickable(false);
            buttonAddManual.setClickable(false);
            buttonNearbyDevices.setAlpha(0.5f);
            buttonAddManual.setAlpha(0.5f);
        }
        
        // Show connecting state
        progressContainer.setVisibility(View.VISIBLE);
        // Hide the "Can't connect" text inside progress container (we show the one at bottom instead)
        if (textViewCantConnectProgress != null) {
            textViewCantConnectProgress.setVisibility(View.GONE);
        }

        // Initialize progress
        progress = 0;
        progressBar.setProgress(0);
        textViewProgress.setText("0%");

        // Start progress animation
        startProgressAnimation();
        
        // Start API call to create device
        createDeviceViaAPI();
    }

    /**
     * Start progress animation (visual feedback)
     */
    private void startProgressAnimation() {
        progressHandler = new Handler(Looper.getMainLooper());
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (progress < 90) { // Stop at 90%, wait for API response
                    progress += 2;
                    progressBar.setProgress(progress * 100);
                    textViewProgress.setText(progress + "%");
                    progressHandler.postDelayed(this, 150);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    /**
     * Create device via API.
     * If MQTT metadata is present, use the MQTT registration flow which:
     * 1. Registers device in backend
     * 2. Creates EMQX credentials
     * 3. Sends credentials to IoT device via FastBee MQTT
     */
    private void createDeviceViaAPI() {
        if (!authManager.isLoggedIn()) {
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        if (mqttProductId != null && mqttDeviceNum != null) {
            // MQTT device flow: get home first, then register with MQTT credentials
            getHomeForMqttDevice();
        } else {
            // No MQTT metadata — proceed with regular device creation
            getPrimaryHomeAndCreateDevice();
        }
    }

    /**
     * Get home ID before MQTT device registration
     */
    private void getHomeForMqttDevice() {
        Call<ApiResponse<Home>> call = apiService.getPrimaryHome();
        call.enqueue(new Callback<ApiResponse<Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<Home>> call, Response<ApiResponse<Home>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Home primaryHome = response.body().getData();
                    if (primaryHome != null) {
                        primaryHomeId = primaryHome.getId();
                        android.util.Log.d("DeviceDetected", "Got primary home: " + primaryHomeId);
                    }
                }
                // Proceed with MQTT registration (homeId may be null, backend will find one)
                verifyMqttDeviceThenRegister();
            }

            @Override
            public void onFailure(Call<ApiResponse<Home>> call, Throwable t) {
                android.util.Log.w("DeviceDetected", "Failed to get home, proceeding anyway", t);
                verifyMqttDeviceThenRegister();
            }
        });
    }

    /**
     * Register MQTT device via /mqtt/register-device endpoint.
     * This creates the device in database AND generates EMQX credentials.
     * Then provisions the IoT device with credentials via FastBee MQTT.
     */
    private void verifyMqttDeviceThenRegister() {
        // First verify device is reachable (optional check)
        java.util.Map<String, Object> verifyBody = new java.util.HashMap<>();
        verifyBody.put("productId", mqttProductId);
        verifyBody.put("deviceNum", mqttDeviceNum);

        apiService.verifyMqttDevice(verifyBody).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                boolean reachable = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Object data = response.body().getData();
                    if (data instanceof java.util.Map) {
                        Object r = ((java.util.Map<?, ?>) data).get("reachable");
                        reachable = Boolean.TRUE.equals(r);
                    }
                }
                android.util.Log.d("DeviceDetected", "MQTT device reachable: " + reachable);
                // Now register the device and get MQTT credentials
                registerMqttDeviceWithCredentials();
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                android.util.Log.w("DeviceDetected", "MQTT verify failed, proceeding anyway", t);
                registerMqttDeviceWithCredentials();
            }
        });
    }

    /**
     * Register MQTT device and get credentials from backend.
     * Backend will create device in database and register credentials in EMQX.
     */
    private void registerMqttDeviceWithCredentials() {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("productId", mqttProductId);
        body.put("deviceNum", mqttDeviceNum);
        body.put("name", deviceName);
        body.put("type", mqttDeviceType != null ? mqttDeviceType : getDeviceTypeFromName(deviceName));

        // Add homeId if we have it
        if (primaryHomeId != null) {
            body.put("homeId", primaryHomeId);
        }

        // Add roomId from currently selected room in main UI (null/0 = All Rooms, no assignment)
        Integer selectedRoomId = com.smarthome.iot.utils.Globals.getSelectedRoomId();
        if (selectedRoomId != null && selectedRoomId != 0) {
            body.put("roomId", selectedRoomId);
        }

        android.util.Log.d("DeviceDetected", "Registering MQTT device: " + mqttProductId + "/" + mqttDeviceNum + " (roomId=" + selectedRoomId + ")");

        apiService.registerMqttDevice(body).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> data = response.body().getData();

                    Integer deviceId = null;
                    Map<String, Object> mqttCredentials = null;

                    if (data != null) {
                        // Extract device ID
                        Object idObj = data.get("deviceId");
                        if (idObj instanceof Number) {
                            deviceId = ((Number) idObj).intValue();
                        }

                        // Extract MQTT credentials
                        Object mqttObj = data.get("mqtt");
                        if (mqttObj instanceof Map) {
                            mqttCredentials = (Map<String, Object>) mqttObj;
                        }
                    }

                    android.util.Log.d("DeviceDetected", "MQTT device registered: deviceId=" + deviceId);

                    if (mqttCredentials != null) {
                        android.util.Log.d("DeviceDetected", "Got MQTT credentials - sending to IoT device via FastBee");
                        // Send credentials to IoT device via FastBee MQTT
                        provisionIoTDevice(deviceId, mqttCredentials);
                    } else {
                        android.util.Log.w("DeviceDetected", "No MQTT credentials in response");
                        completeProgressAnimation(deviceId);
                    }
                } else {
                    // Token expired or invalid — redirect to sign in
                    if (response.code() == 401) {
                        Intent intent = new Intent(DeviceDetectedActivity.this, SignInActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    // Check if device already exists (might return existing device info)
                    String errorMessage = "Failed to register device";
                    if (response.body() != null) {
                        if (response.body().getError() != null && response.body().getError().getMessage() != null) {
                            errorMessage = response.body().getError().getMessage();
                        } else if (response.body().getData() != null) {
                            // Device might already be registered - still success
                            Map<String, Object> data = response.body().getData();
                            Object idObj = data.get("deviceId");
                            if (idObj instanceof Number) {
                                Integer deviceId = ((Number) idObj).intValue();
                                android.util.Log.d("DeviceDetected", "Device already registered: " + deviceId);
                                completeProgressAnimation(deviceId);
                                return;
                            }
                        }
                    }
                    showError(errorMessage);
                    resetConnectionState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                android.util.Log.e("DeviceDetected", "Error registering MQTT device", t);
                // Fallback to regular device creation
                android.util.Log.d("DeviceDetected", "Falling back to regular device creation");
                getPrimaryHomeAndCreateDevice();
            }
        });
    }

    /**
     * Send MQTT credentials to IoT device via FastBee Bluetooth.
     * Mobile connects to device via BLE and sends credentials directly.
     * Device receives credentials and connects to EMQX broker via 4G/5G.
     */
    private void provisionIoTDevice(Integer deviceId, Map<String, Object> mqttCredentials) {
        android.util.Log.d("DeviceDetected", "Provisioning IoT device via FastBee Bluetooth: " + mqttProductId + "/" + mqttDeviceNum);

        // Skip Bluetooth provisioning in test mode
        if (com.smarthome.iot.utils.Globals.isTestMode()) {
            android.util.Log.d("DeviceDetected", "Test mode enabled - skipping Bluetooth provisioning");
            completeProgressAnimation(deviceId);
            return;
        }

        // Check Bluetooth permissions
        if (!fastBeeBluetoothService.hasBluetoothPermissions()) {
            android.util.Log.w("DeviceDetected", "Bluetooth permissions not granted, requesting...");
            pendingProvisionDeviceId = deviceId;
            requestBluetoothPermissions(() -> provisionViaBluetoothInternal(deviceId, mqttCredentials));
            return;
        }

        // Check if Bluetooth is enabled
        if (!fastBeeBluetoothService.isBluetoothEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth to provision the device", Toast.LENGTH_LONG).show();
            completeProgressAnimation(deviceId);
            return;
        }

        provisionViaBluetoothInternal(deviceId, mqttCredentials);
    }

    /**
     * Internal method to provision device via Bluetooth after permissions are granted
     */
    private void provisionViaBluetoothInternal(Integer deviceId, Map<String, Object> mqttCredentials) {
        // Extract MQTT credentials from response
        String brokerUrl = (String) mqttCredentials.get("brokerUrl");
        String username = (String) mqttCredentials.get("username");
        String password = (String) mqttCredentials.get("password");
        String clientId = (String) mqttCredentials.get("clientId");

        if (brokerUrl == null || username == null || password == null) {
            android.util.Log.e("DeviceDetected", "Missing MQTT credentials");
            completeProgressAnimation(deviceId);
            return;
        }

        // Construct FastBee device name: "FastBee_{productId}_{deviceNum}"
        String fastBeeDeviceName = "FastBee_" + mqttProductId + "_" + mqttDeviceNum;

        // Set up provisioning callback
        FastBeeBluetoothService.ProvisioningCallback callback = new FastBeeBluetoothService.ProvisioningCallback() {
            @Override
            public void onScanStarted() {
                android.util.Log.d("DeviceDetected", "Scanning for FastBee device: " + fastBeeDeviceName);
                Toast.makeText(DeviceDetectedActivity.this, "Scanning for device...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceFound(BluetoothDevice device, int rssi) {
                android.util.Log.d("DeviceDetected", "Device found: " + device.getAddress() + " (RSSI: " + rssi + ")");
                Toast.makeText(DeviceDetectedActivity.this, "Device found, connecting...", Toast.LENGTH_SHORT).show();

                // Connect and provision
                fastBeeBluetoothService.provisionDevice(device.getAddress(), brokerUrl, username, password, clientId);
            }

            @Override
            public void onConnecting() {
                android.util.Log.d("DeviceDetected", "Connecting to device...");
            }

            @Override
            public void onConnected() {
                android.util.Log.d("DeviceDetected", "Connected to device");
                Toast.makeText(DeviceDetectedActivity.this, "Connected, sending credentials...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCredentialsSent() {
                android.util.Log.d("DeviceDetected", "Credentials sent successfully");
                Toast.makeText(DeviceDetectedActivity.this, "Credentials sent via Bluetooth", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProvisioningComplete() {
                android.util.Log.d("DeviceDetected", "Provisioning complete");
                com.smarthome.iot.utils.Globals.clearDeviceCache();
                completeProgressAnimation(deviceId);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("DeviceDetected", "Provisioning error: " + error);
                Toast.makeText(DeviceDetectedActivity.this, "Provisioning error: " + error, Toast.LENGTH_LONG).show();
                com.smarthome.iot.utils.Globals.clearDeviceCache();
                // Complete anyway - device is registered in backend
                completeProgressAnimation(deviceId);
            }
        };

        // Start scanning for the device
        fastBeeBluetoothService.scanForDevice(fastBeeDeviceName, callback);
    }

    /**
     * Request Bluetooth permissions
     */
    private void requestBluetoothPermissions(Runnable onGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingBluetoothAction = onGranted;
            bluetoothPermissionLauncher.launch(new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            });
        } else {
            // For older Android versions, permissions are granted at install time
            onGranted.run();
        }
    }

    /**
     * Get primary home, then get rooms, then create device
     */
    private void getPrimaryHomeAndCreateDevice() {
        Call<ApiResponse<Home>> call = apiService.getPrimaryHome();
        call.enqueue(new Callback<ApiResponse<Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<Home>> call, Response<ApiResponse<Home>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Home primaryHome = response.body().getData();
                    if (primaryHome != null) {
                        // Store primary home ID for device creation (REQUIRED - never null)
                        primaryHomeId = primaryHome.getId();
                        android.util.Log.d("DeviceDetected", "Primary home found: id=" + primaryHomeId);
                        // Get rooms for the primary home
                        getRoomsAndCreateDevice(primaryHome.getId());
                    } else {
                        // No primary home found - try to get all homes
                        android.util.Log.w("DeviceDetected", "No primary home found, trying to get all homes");
                        getAllHomesAndCreateDevice();
                    }
                } else {
                    // Try to get all homes
                    getAllHomesAndCreateDevice();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Home>> call, Throwable t) {
                android.util.Log.e("DeviceDetected", "Error getting primary home", t);
                // Try to get all homes as fallback
                getAllHomesAndCreateDevice();
            }
        });
    }

    /**
     * Fallback: Get all homes and find primary
     */
    private void getAllHomesAndCreateDevice() {
        Call<ApiResponse<List<Home>>> call = apiService.getHomes();
        call.enqueue(new Callback<ApiResponse<List<Home>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Home>>> call, Response<ApiResponse<List<Home>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Home> homes = response.body().getData();
                    if (homes != null && !homes.isEmpty()) {
                        // Find primary home or use first home
                        Home selectedHome = null;
                        for (Home home : homes) {
                            if (home.isPrimary()) {
                                selectedHome = home;
                                break;
                            }
                        }
                        if (selectedHome == null) {
                            selectedHome = homes.get(0);
                        }
                        // Store home ID for device creation (REQUIRED - never null)
                        primaryHomeId = selectedHome.getId();
                        android.util.Log.d("DeviceDetected", "Selected home: id=" + primaryHomeId);
                        getRoomsAndCreateDevice(selectedHome.getId());
                    } else {
                        // No homes available - cannot create device without a home
                        android.util.Log.e("DeviceDetected", "No homes available for user - cannot create device");
                        showError("No home found. Please create a home first.");
                        resetConnectionState();
                    }
                } else {
                    // No homes available - cannot create device without a home
                    android.util.Log.e("DeviceDetected", "No homes available for user - cannot create device");
                    showError("No home found. Please create a home first.");
                    resetConnectionState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Home>>> call, Throwable t) {
                android.util.Log.e("DeviceDetected", "Error getting homes", t);
                // Cannot create device without a home
                showError("Failed to load homes. Please try again.");
                resetConnectionState();
            }
        });
    }

    /**
     * Get rooms for home, then create device
     */
    private void getRoomsAndCreateDevice(Integer homeId) {
        Call<ApiResponse<List<Room>>> call = apiService.getRooms(homeId);
        call.enqueue(new Callback<ApiResponse<List<Room>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Room>>> call, Response<ApiResponse<List<Room>>> response) {
                Integer roomId = null;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Room> rooms = response.body().getData();
                    if (rooms != null && !rooms.isEmpty()) {
                        // Use first room
                        roomId = rooms.get(0).getId();
                        android.util.Log.d("DeviceDetected", "Using first room: id=" + roomId);
                    }
                }
                // primaryHomeId should already be set at this point
                createDevice(roomId);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Room>>> call, Throwable t) {
                android.util.Log.e("DeviceDetected", "Error getting rooms", t);
                // Create device without roomId (but with homeId which is already set)
                // primaryHomeId should already be set at this point
                createDevice(null);
            }
        });
    }

    /**
     * Create device via API
     */
    private void createDevice(Integer roomId) {
        // Validate that we have a homeId (REQUIRED - never null)
        if (primaryHomeId == null) {
            android.util.Log.e("DeviceDetected", "Cannot create device: primaryHomeId is null");
            showError("No home found. Please create a home first.");
            resetConnectionState();
            return;
        }
        
        // Prepare device data
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("name", deviceName);
        // Use MQTT device type if available, otherwise infer from name
        deviceData.put("type", mqttDeviceType != null ? mqttDeviceType : getDeviceTypeFromName(deviceName));
        deviceData.put("macAddress", generateMacAddress());

        if (roomId != null) {
            deviceData.put("roomId", roomId);
        }

        // homeId is REQUIRED - always send it
        deviceData.put("homeId", primaryHomeId);
        android.util.Log.d("DeviceDetected", "Creating device with homeId=" + primaryHomeId + ", roomId=" + roomId);

        // Add metadata with device-specific info
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("deviceCategory", getDeviceCategoryFromName(deviceName));
        metadata.put("connectionSource", connectionSource);

        // Include MQTT/FastBee binding if device was added via QR scan or setup code
        if (mqttProductId != null && mqttDeviceNum != null) {
            metadata.put("protocol", "mqtt");
            metadata.put("productId", mqttProductId);
            metadata.put("deviceNum", mqttDeviceNum);
            android.util.Log.d("DeviceDetected", "MQTT device: productId=" + mqttProductId + ", deviceNum=" + mqttDeviceNum);
        }

        deviceData.put("metadata", metadata);

        // Make API call
        Call<ApiResponse<Device>> call = apiService.createDevice(deviceData);
        call.enqueue(new Callback<ApiResponse<Device>>() {
            @Override
            public void onResponse(Call<ApiResponse<Device>> call, Response<ApiResponse<Device>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Device createdDevice = response.body().getData();
                    android.util.Log.d("DeviceDetected", "Device created successfully: " + createdDevice.getName() + " (ID: " + createdDevice.getId() + ")");
                    
                    // Add device to Globals cache
                    com.smarthome.iot.utils.Globals.addDeviceToCache(createdDevice);
                    
                    // Store device ID for passing to next activity
                    final Integer deviceId = createdDevice.getId();
                    
                    // Complete progress animation
                    completeProgressAnimation(deviceId);
                } else {
                    if (response.code() == 401) {
                        Intent intent = new Intent(DeviceDetectedActivity.this, SignInActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }
                    String errorMessage = "Failed to connect device";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMessage = response.body().getError().getMessage();
                    }
                    showError(errorMessage);
                    resetConnectionState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Device>> call, Throwable t) {
                android.util.Log.e("DeviceDetected", "Error creating device", t);
                showError("Connection failed: " + t.getMessage());
                resetConnectionState();
            }
        });
    }

    /**
     * Complete progress animation and navigate to success screen
     */
    private void completeProgressAnimation(final Integer deviceId) {
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
        
        // Animate to 100%
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable completeRunnable = new Runnable() {
            @Override
            public void run() {
                if (progress < 100) {
                    progress += 5;
                    if (progress > 100) progress = 100;
                    progressBar.setProgress(progress * 100);
                    textViewProgress.setText(progress + "%");
                    
                    if (progress < 100) {
                        handler.postDelayed(this, 100);
                    } else {
                        // Clear device cache so MainActivity reloads with the new device
                        com.smarthome.iot.utils.Globals.clearDeviceCache();
                        // Navigate to connected screen
                        handler.postDelayed(() -> {
                            Intent intent = new Intent(DeviceDetectedActivity.this, DeviceConnectedActivity.class);
                            intent.putExtra("device_name", deviceName);
                            if (deviceId != null) {
                                intent.putExtra("device_id", deviceId.intValue());
                            }
                            // Pass MQTT metadata so DeviceConnectedActivity can show QR button
                            if (mqttProductId != null) {
                                intent.putExtra("mqtt_product_id", mqttProductId);
                                intent.putExtra("mqtt_device_num", mqttDeviceNum);
                                intent.putExtra("mqtt_device_type", mqttDeviceType);
                            }
                            startActivity(intent);
                            finish();
                        }, 500);
                    }
                }
            }
        };
        handler.post(completeRunnable);
    }

    /**
     * Reset connection state on error
     */
    private void resetConnectionState() {
        isConnecting = false;
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
        showInitialState();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("Connection Failed")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    /**
     * Get device type from device name (maps to backend DeviceType enum)
     */
    /**
     * Get device type from device name
     * Backend expects: "lamp", "camera", or "electronics"
     */
    private String getDeviceTypeFromName(String deviceName) {
        if (deviceName == null) {
            return "electronics"; // Default type
        }
        
        String normalizedName = deviceName.toLowerCase();
        
        // Check for lamp/light devices
        if (normalizedName.contains("lamp") || normalizedName.contains("light") || 
            normalizedName.contains("bulb") || normalizedName.contains("lighting")) {
            return "lamp";
        }
        
        // Check for camera devices
        if (normalizedName.contains("camera") || normalizedName.contains("cctv") || 
            normalizedName.contains("webcam") || normalizedName.contains("security")) {
            return "camera";
        }
        
        // Everything else is electronics
        return "electronics";
    }

    /**
     * Get device category for metadata
     */
    private String getDeviceCategoryFromName(String deviceName) {
        if (deviceName == null) {
            return "other";
        }
        
        String normalizedName = deviceName.toLowerCase();
        
        if (normalizedName.contains("lamp") || normalizedName.contains("light")) {
            return "lighting";
        } else if (normalizedName.contains("camera") || normalizedName.contains("cctv")) {
            return "cameras";
        } else if (normalizedName.contains("speaker")) {
            return "audio";
        } else if (normalizedName.contains("air") || normalizedName.contains("conditioner") || normalizedName.contains("ac")) {
            return "electrical";
        } else {
            return "other";
        }
    }

    /**
     * Generate a random MAC address for the device
     * In production, this should come from Bluetooth discovery
     */
    private String generateMacAddress() {
        Random random = new Random();
        StringBuilder mac = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) mac.append(":");
            int value = random.nextInt(256);
            mac.append(String.format("%02X", value));
        }
        return mac.toString();
    }

    private void showChatbotHelp() {
        Intent intent = new Intent(this, ChatbotActivity.class);
        intent.putExtra("help_topic", "device_setup");
        intent.putExtra("device_name", deviceName);
        startActivity(intent);
    }
}
