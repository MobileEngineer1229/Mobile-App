package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.models.Room;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.utils.AuthManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnboardingCompleteActivity extends AppCompatActivity {

    private ImageButton buttonClose;
    private MaterialButton buttonGetStarted;
    private android.widget.FrameLayout loadingOverlay;
    private TextView textViewLoadingMessage;

    private int userId;
    private String email;
    private String country;
    private String homeName;
    private ArrayList<String> rooms;
    private String address;
    private double latitude;
    private double longitude;

    private ApiService apiService;
    private AuthManager authManager;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_complete);

        // Set status bar color
        setStatusBarColor();

        // Get data from previous activity
        userId = getIntent().getIntExtra("userId", -1);
        email = getIntent().getStringExtra("email");
        country = getIntent().getStringExtra("country");
        homeName = getIntent().getStringExtra("homeName");
        rooms = getIntent().getStringArrayListExtra("rooms");
        address = getIntent().getStringExtra("address");
        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
        setupListeners();
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        buttonClose = findViewById(R.id.buttonClose);
        buttonGetStarted = findViewById(R.id.buttonGetStarted);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        
        // Get loading message text view from overlay
        if (loadingOverlay != null) {
            textViewLoadingMessage = loadingOverlay.findViewById(R.id.textViewLoadingMessage);
        }
        
        // Hide loading initially
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        buttonClose.setOnClickListener(v -> {
            if (!isProcessing) {
                navigateToMain();
            }
        });

        buttonGetStarted.setOnClickListener(v -> {
            if (!isProcessing) {
                completeOnboarding();
            }
        });
    }

    private void completeOnboarding() {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            navigateToMain();
            return;
        }

        // Verify token exists
        String token = authManager.getToken();
        if (token == null || token.isEmpty()) {
            android.util.Log.e("OnboardingComplete", "No authentication token found");
            Toast.makeText(this, "Authentication error. Please sign in again.", Toast.LENGTH_LONG).show();
            navigateToMain();
            return;
        }

        android.util.Log.d("OnboardingComplete", "Starting onboarding completion - User ID: " + userId + ", Token exists: " + (token != null));

        isProcessing = true;
        showLoadingState();
        buttonGetStarted.setEnabled(false);
        buttonClose.setEnabled(false);

        // Default home name if not provided
        String finalHomeName = (homeName != null && !homeName.trim().isEmpty()) 
            ? homeName.trim() 
            : getString(R.string.my_home);

        // Create home
        createHome(finalHomeName);
    }

    private void createHome(String name) {
        android.util.Log.d("OnboardingComplete", "Creating home: " + name);
        
        if (textViewLoadingMessage != null) {
            textViewLoadingMessage.setText("Creating your home...");
        }
        
        Map<String, Object> homeData = new HashMap<>();
        homeData.put("name", name);
        homeData.put("isPrimary", true);
        
        if (address != null && !address.trim().isEmpty()) {
            homeData.put("address", address);
        }
        if (latitude != 0) {
            homeData.put("latitude", latitude);
        }
        if (longitude != 0) {
            homeData.put("longitude", longitude);
        }
        if (country != null && !country.trim().isEmpty()) {
            homeData.put("country", country);
        }

        android.util.Log.d("OnboardingComplete", "Home data: " + homeData.toString());
        android.util.Log.d("OnboardingComplete", "Auth token exists: " + (authManager.getToken() != null));
        android.util.Log.d("OnboardingComplete", "User ID: " + userId);

        Call<ApiResponse<Home>> call = apiService.createHome(homeData);
        call.enqueue(new Callback<ApiResponse<Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<Home>> call, Response<ApiResponse<Home>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Home home = response.body().getData();
                    if (home != null) {
                        android.util.Log.d("OnboardingComplete", "Home created successfully with ID: " + home.getId());
                        // Home created successfully, now create rooms
                        createRooms(home.getId());
                    } else {
                        android.util.Log.e("OnboardingComplete", "Home creation response missing data");
                        showError("Failed to create home: No data received");
                    }
                } else {
                    String errorMsg = "Failed to create home";
                    int statusCode = response.code();
                    android.util.Log.e("OnboardingComplete", "Home creation failed with status code: " + statusCode);
                    
                    if (response.body() != null && response.body().getError() != null) {
                        errorMsg = response.body().getError().getMessage();
                        android.util.Log.e("OnboardingComplete", "Error message: " + errorMsg);
                    } else if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("OnboardingComplete", "Error response body: " + errorBody);
                            // Try to extract error message from JSON
                            if (errorBody.contains("\"message\"")) {
                                int messageIndex = errorBody.indexOf("\"message\"");
                                if (messageIndex != -1) {
                                    int start = errorBody.indexOf("\"", messageIndex + 10) + 1;
                                    int end = errorBody.indexOf("\"", start);
                                    if (start > 0 && end > start) {
                                        errorMsg = errorBody.substring(start, end);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("OnboardingComplete", "Error reading error body", e);
                        }
                    }
                    
                    // Check for authentication errors
                    if (statusCode == 401) {
                        errorMsg = "Authentication failed. Please sign in again.";
                        android.util.Log.e("OnboardingComplete", "401 Unauthorized - Token may be invalid");
                    } else if (statusCode == 400) {
                        errorMsg = "Invalid request: " + errorMsg;
                    }
                    
                    android.util.Log.e("OnboardingComplete", "Home creation failed: " + errorMsg);
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Home>> call, Throwable t) {
                android.util.Log.e("OnboardingComplete", "Network error creating home", t);
                String errorMsg = "Network error";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("Failed to connect") || t.getMessage().contains("failed to connect")) {
                        errorMsg = "Cannot connect to server. Please check your internet connection.";
                    } else {
                        errorMsg = "Network error: " + t.getMessage();
                    }
                }
                showError(errorMsg);
            }
        });
    }

    private void createRooms(int homeId) {
        if (rooms == null || rooms.isEmpty()) {
            // No rooms to create, navigate to main
            android.util.Log.d("OnboardingComplete", "No rooms to create, navigating to main");
            navigateToMain();
            return;
        }

        // Filter out "Add Room" option if present
        ArrayList<String> validRooms = new ArrayList<>();
        for (String room : rooms) {
            if (room != null && !room.equals(getString(R.string.add_room))) {
                validRooms.add(room);
            }
        }

        if (validRooms.isEmpty()) {
            android.util.Log.d("OnboardingComplete", "No valid rooms to create, navigating to main");
            navigateToMain();
            return;
        }

        android.util.Log.d("OnboardingComplete", "Creating " + validRooms.size() + " rooms for home ID: " + homeId);
        
        // Update loading message
        if (textViewLoadingMessage != null) {
            textViewLoadingMessage.setText("Creating rooms...");
        }
        
        // Create rooms one by one
        createRoomRecursive(homeId, validRooms, 0);
    }

    private void createRoomRecursive(int homeId, ArrayList<String> roomNames, int index) {
        if (index >= roomNames.size()) {
            // All rooms created, navigate to main
            android.util.Log.d("OnboardingComplete", "All rooms created successfully");
            navigateToMain();
            return;
        }

        String roomName = roomNames.get(index);
        android.util.Log.d("OnboardingComplete", "Creating room: " + roomName + " (index: " + index + "/" + roomNames.size() + ")");
        
        // Update loading message
        if (textViewLoadingMessage != null) {
            textViewLoadingMessage.setText("Creating room: " + roomName + " (" + (index + 1) + "/" + roomNames.size() + ")");
        }
        
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", roomName);
        roomData.put("homeId", homeId); // homeId should be Integer, not String

        android.util.Log.d("OnboardingComplete", "Room data: " + roomData.toString());

        Call<ApiResponse<Room>> call = apiService.createRoom(roomData);
        call.enqueue(new Callback<ApiResponse<Room>>() {
            @Override
            public void onResponse(Call<ApiResponse<Room>> call, Response<ApiResponse<Room>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Room created successfully, continue with next room
                    android.util.Log.d("OnboardingComplete", "Room created successfully: " + roomName);
                    createRoomRecursive(homeId, roomNames, index + 1);
                } else {
                    // Log error but continue with next room (non-blocking)
                    int statusCode = response.code();
                    String errorMsg = "Failed to create room: " + roomName;
                    android.util.Log.w("OnboardingComplete", "Room creation failed with status code: " + statusCode);
                    
                    if (response.body() != null && response.body().getError() != null) {
                        errorMsg += " - " + response.body().getError().getMessage();
                    } else if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.w("OnboardingComplete", "Error response body: " + errorBody);
                        } catch (Exception e) {
                            android.util.Log.w("OnboardingComplete", "Error reading error body", e);
                        }
                    }
                    
                    // Check for authentication errors
                    if (statusCode == 401) {
                        android.util.Log.e("OnboardingComplete", "401 Unauthorized when creating room - Token may be invalid");
                    }
                    
                    android.util.Log.w("OnboardingComplete", errorMsg);
                    createRoomRecursive(homeId, roomNames, index + 1);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Room>> call, Throwable t) {
                // Log error but continue with next room (non-blocking)
                android.util.Log.w("OnboardingComplete", "Network error creating room: " + roomName, t);
                createRoomRecursive(homeId, roomNames, index + 1);
            }
        });
    }

    private void showLoadingState() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
        if (textViewLoadingMessage != null) {
            textViewLoadingMessage.setText("Setting up your home...");
        }
    }

    private void hideLoadingState() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        isProcessing = false;
        hideLoadingState();
        buttonGetStarted.setEnabled(true);
        buttonClose.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void navigateToMain() {
        isProcessing = false;
        hideLoadingState();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

