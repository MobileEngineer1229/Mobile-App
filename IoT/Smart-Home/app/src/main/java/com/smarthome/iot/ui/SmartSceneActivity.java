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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.SmartScene;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.SmartSceneAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.BottomNavSpacingHelper;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.smarthome.iot.models.Home;
import com.smarthome.iot.ui.adapters.LocationSelectorAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmartSceneActivity extends AppCompatActivity {
    private TextView textViewHomeName;
    private MaterialButton buttonAutomation;
    private MaterialButton buttonTapToRun;
    private RecyclerView recyclerViewScenes;
    private FloatingActionButton fabAddScene;
    private BottomNavigationView bottomNavigation;

    private SmartSceneAdapter adapter;
    private String currentTab = "automation"; // "automation" or "tap_to_run" - Default to automation to show first scene
    private ApiService apiService;
    private AuthManager authManager;
    private int selectedHomeId = -1;
    private PopupWindow homeSelectorPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            ThemeHelper.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_smart_scene);

            setStatusBarColor();

            // Initialize API
            ApiClient.initialize(this);
            apiService = ApiClient.getClient().create(ApiService.class);
            authManager = new AuthManager(this);

            initializeViews();
            setupRecyclerView();
            setupClickListeners();
            updateTabButtons(); // Initialize tab button states
            setupBottomNavigation();
            
            // Force load scenes after a short delay to ensure everything is initialized
            recyclerViewScenes.postDelayed(() -> {
                android.util.Log.d("SmartSceneActivity", "Delayed loadScenes called");
                loadScenes();
            }, 100);
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneActivity", "Error in onCreate: " + e.getMessage(), e);
            // Print full stack trace
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            android.util.Log.e("SmartSceneActivity", "Full stack trace: " + sw.toString());
            
            // Show error toast with truncated message if too long
            try {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.length() > 100) {
                    errorMsg = errorMsg.substring(0, 100) + "...";
                }
                android.widget.Toast.makeText(this, "Error loading Smart Scenes: " + (errorMsg != null ? errorMsg : "Unknown error"), android.widget.Toast.LENGTH_LONG).show();
            } catch (Exception toastError) {
                toastError.printStackTrace();
            }
            // Don't finish - let user navigate away manually
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
            textViewHomeName = findViewById(R.id.textViewHomeName);
            // imageViewChevronDown is an ImageView, not ImageButton
            android.widget.ImageView imageViewChevronDown = findViewById(R.id.imageViewChevronDown);
            ImageButton buttonViewSwitch = findViewById(R.id.buttonViewSwitch);
            ImageButton buttonMore = findViewById(R.id.buttonMore);
            buttonAutomation = findViewById(R.id.buttonAutomation);
            buttonTapToRun = findViewById(R.id.buttonTapToRun);
            recyclerViewScenes = findViewById(R.id.recyclerViewScenes);
            fabAddScene = findViewById(R.id.fabAddScene);
            bottomNavigation = findViewById(R.id.bottomNavigation);
            
            // Debug logging
            if (fabAddScene == null) {
                android.util.Log.e("SmartSceneActivity", "FAB not found in layout!");
            } else {
                android.util.Log.d("SmartSceneActivity", "FAB found successfully");
            }

            // Display primary home name
            updateHomeNameDisplay();

            if (imageViewChevronDown != null) {
                imageViewChevronDown.setOnClickListener(v -> showHomeSelector(v));
            }
            if (textViewHomeName != null) {
                textViewHomeName.setOnClickListener(v -> showHomeSelector(v));
            }
            if (buttonViewSwitch != null) {
                buttonViewSwitch.setOnClickListener(v -> {
                    // Navigate to Logs screen
                    Intent intent = new Intent(this, SceneLogsActivity.class);
                    startActivity(intent);
                });
            }
            if (buttonMore != null) {
                buttonMore.setOnClickListener(v -> {
                    // Navigate to Manage Scenes screen
                    Intent intent = new Intent(this, ManageScenesActivity.class);
                    intent.putExtra("current_tab", currentTab);
                    startActivityForResult(intent, 200);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e; // Re-throw to be caught by onCreate's try-catch
        }
    }

    private void setupRecyclerView() {
        if (recyclerViewScenes == null) {
            android.util.Log.e("SmartSceneActivity", "recyclerViewScenes is null in setupRecyclerView");
            return;
        }
        
        try {
            // Set layout manager first
            updateRecyclerViewLayout();
            
            adapter = new SmartSceneAdapter(scene -> {
                android.util.Log.d("SmartSceneActivity", "Scene clicked: " + (scene != null ? scene.getName() : "null") + ", Type: " + (scene != null ? scene.getType() : "null"));
                if (scene != null && "tap_to_run".equals(scene.getType())) {
                    // Execute Tap-to-Run scene immediately
                    android.util.Log.d("SmartSceneActivity", "Executing tap-to-run scene: " + scene.getName());
                    executeTapToRunScene(scene);
                } else if (scene != null) {
                    // For automation scenes, navigate to edit/detail
                    android.util.Log.d("SmartSceneActivity", "Automation scene clicked: " + scene.getName());
                    // TODO: Navigate to scene detail/edit
                }
            }, (scene, isEnabled) -> {
                // Toggle scene enabled state
                if (scene != null) {
                    scene.setEnabled(isEnabled);
                    android.util.Log.d("SmartSceneActivity", "Scene toggled: " + scene.getName() + " -> " + isEnabled);
                    updateSceneEnabled(scene, isEnabled);
                }
            });
            
            if (recyclerViewScenes != null && adapter != null) {
                recyclerViewScenes.setAdapter(adapter);
                android.util.Log.d("SmartSceneActivity", "Adapter set to RecyclerView");
                // Ensure RecyclerView is visible
                recyclerViewScenes.setVisibility(View.VISIBLE);
                // Load scenes immediately after adapter is set
                // Use post to ensure layout is complete
                recyclerViewScenes.post(() -> {
                    loadScenes();
                });
            } else {
                android.util.Log.e("SmartSceneActivity", "Failed to set adapter - recyclerViewScenes or adapter is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneActivity", "Error in setupRecyclerView: " + e.getMessage(), e);
            // Set empty adapter on error
            if (recyclerViewScenes != null) {
                recyclerViewScenes.setAdapter(null);
            }
        }
    }
    
    private void updateRecyclerViewLayout() {
        if (recyclerViewScenes == null) {
            android.util.Log.e("SmartSceneActivity", "recyclerViewScenes is null in updateRecyclerViewLayout");
            return;
        }
        
        try {
            if ("tap_to_run".equals(currentTab)) {
                GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
                recyclerViewScenes.setLayoutManager(gridLayoutManager);
                
                // Remove existing ItemDecoration if any
                if (recyclerViewScenes.getItemDecorationCount() > 0) {
                    recyclerViewScenes.removeItemDecorationAt(0);
                }
                
                // Add spacing between grid items (16dp)
                // includeEdge=false because RecyclerView already has padding for edges
                int spacing = (int) (16 * getResources().getDisplayMetrics().density);
                recyclerViewScenes.addItemDecoration(new com.smarthome.iot.ui.decorations.GridSpacingItemDecoration(2, spacing, false));
                
                android.util.Log.d("SmartSceneActivity", "Set GridLayoutManager for tap_to_run with 16dp spacing");
            } else {
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
                recyclerViewScenes.setLayoutManager(linearLayoutManager);
                
                // Remove existing ItemDecoration if any
                if (recyclerViewScenes.getItemDecorationCount() > 0) {
                    recyclerViewScenes.removeItemDecorationAt(0);
                }
                
                // Add 24dp spacing between automation items
                int spacing = (int) (24 * getResources().getDisplayMetrics().density);
                recyclerViewScenes.addItemDecoration(new com.smarthome.iot.ui.decorations.VerticalSpacingItemDecoration(spacing));
                
                android.util.Log.d("SmartSceneActivity", "Set LinearLayoutManager for automation with 24dp spacing");
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneActivity", "Error setting layout manager: " + e.getMessage(), e);
            // Fallback to LinearLayoutManager
            try {
                recyclerViewScenes.setLayoutManager(new LinearLayoutManager(this));
            } catch (Exception e2) {
                e2.printStackTrace();
                android.util.Log.e("SmartSceneActivity", "Error setting fallback layout manager: " + e2.getMessage(), e2);
            }
        }
    }
    
    private void executeTapToRunScene(SmartScene scene) {
        if (scene == null || scene.getId() == null) {
            android.util.Log.w("SmartSceneActivity", "executeTapToRunScene called with null scene or scene ID");
            return;
        }
        
        try {
            String sceneName = scene.getName() != null ? scene.getName() : "Scene";
            android.util.Log.d("SmartSceneActivity", "Executing tap-to-run scene: " + sceneName);
            
            if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
                // Demo user or not logged in - show toast only
                android.widget.Toast.makeText(this, "Executing: " + sceneName, android.widget.Toast.LENGTH_SHORT).show();
                com.smarthome.iot.ui.dialogs.SuccessDialog successDialog = 
                    new com.smarthome.iot.ui.dialogs.SuccessDialog(this, getString(R.string.tap_to_run_created));
                successDialog.show();
                return;
            }
            
            // Call API to execute scene
            Call<ApiResponse<Map<String, Object>>> call = apiService.executeScene(scene.getId());
            call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        android.util.Log.d("SmartSceneActivity", "Scene executed successfully: " + sceneName);
                        // Show success dialog
                        com.smarthome.iot.ui.dialogs.SuccessDialog successDialog = 
                            new com.smarthome.iot.ui.dialogs.SuccessDialog(SmartSceneActivity.this, 
                                "Scene executed: " + sceneName);
                        successDialog.show();
                    } else {
                        android.util.Log.w("SmartSceneActivity", "Failed to execute scene via API");
                        android.widget.Toast.makeText(SmartSceneActivity.this, 
                            "Failed to execute scene", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                    android.util.Log.e("SmartSceneActivity", "Error executing scene", t);
                    android.widget.Toast.makeText(SmartSceneActivity.this, 
                        "Error executing scene: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneActivity", "Error executing scene: " + e.getMessage(), e);
            android.widget.Toast.makeText(this, "Error executing scene", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateSceneEnabled(SmartScene scene, boolean isEnabled) {
        if (scene == null || scene.getId() == null) {
            return;
        }
        
        if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            // Demo user - just update locally
            return;
        }
        
        // Prepare update data
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("isEnabled", isEnabled);
        
        Call<ApiResponse<SmartScene>> call = apiService.updateScene(scene.getId(), updateData);
        call.enqueue(new Callback<ApiResponse<SmartScene>>() {
            @Override
            public void onResponse(Call<ApiResponse<SmartScene>> call, Response<ApiResponse<SmartScene>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("SmartSceneActivity", "Scene enabled state updated: " + isEnabled);
                    // Invalidate cache so next load fetches fresh data
                    Globals.clearScenesCache();
                } else {
                    // Revert on failure
                    scene.setEnabled(!isEnabled);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    android.util.Log.w("SmartSceneActivity", "Failed to update scene enabled state");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SmartScene>> call, Throwable t) {
                // Revert on failure
                scene.setEnabled(!isEnabled);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                android.util.Log.e("SmartSceneActivity", "Error updating scene enabled state", t);
            }
        });
    }

    private void setupClickListeners() {
        if (buttonAutomation != null) {
            buttonAutomation.setOnClickListener(v -> {
                if (!currentTab.equals("automation")) {
                    android.util.Log.d("SmartSceneActivity", "Switching to automation tab");
                    currentTab = "automation";
                    updateTabButtons();
                    updateRecyclerViewLayout();
                    loadScenes();
                }
            });
        }

        if (buttonTapToRun != null) {
            buttonTapToRun.setOnClickListener(v -> {
                if (!currentTab.equals("tap_to_run")) {
                    android.util.Log.d("SmartSceneActivity", "Switching to tap_to_run tab");
                    currentTab = "tap_to_run";
                    updateTabButtons();
                    updateRecyclerViewLayout();
                    loadScenes();
                }
            });
        }

        // FAB click is handled via android:onClick="OnCreateScene" in XML
        // But also set programmatically as fallback
        if (fabAddScene != null) {
            android.util.Log.d("SmartSceneActivity", "FAB found, setting click listener");
            fabAddScene.setOnClickListener(v -> OnCreateScene(v));
        } else {
            android.util.Log.e("SmartSceneActivity", "FAB is null! Cannot set click listener");
        }
    }

    /**
     * Called when the FAB (+) button is clicked.
     * This method is referenced in the layout XML via android:onClick="OnCreateScene"
     */
    public void OnCreateScene(View view) {
        android.util.Log.d("SmartSceneActivity", "OnCreateScene called, opening CreateSceneActivity");
        try {
            Intent intent = new Intent(this, CreateSceneActivity.class);
            android.util.Log.d("SmartSceneActivity", "Intent created, starting activity");
            startActivityForResult(intent, 100);
            android.util.Log.d("SmartSceneActivity", "Activity started successfully");
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneActivity", "Error opening CreateSceneActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Error opening Create Scene: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTabButtons() {
        try {
            // Always update both buttons - works like a switch: one selected, one unselected
            // Selected tab: Primary color background
            // Unselected tab: Dark background
            
            boolean isAutomationSelected = currentTab.equals("automation");
            
            android.util.Log.d("SmartSceneActivity", "Updating tab buttons - isAutomationSelected: " + isAutomationSelected);
            
            // Update Automation button
            if (buttonAutomation != null) {
                // Remove all Material Design default styling that causes stroke
                buttonAutomation.setBackgroundTintList(null);
                buttonAutomation.setElevation(0f);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    buttonAutomation.setStateListAnimator(null);
                }
                
                // Set background - use post to ensure it happens after MaterialButton initialization
                buttonAutomation.post(() -> {
                    try {
                        if (isAutomationSelected) {
                            // Selected: Primary color
                            buttonAutomation.setBackground(ContextCompat.getDrawable(SmartSceneActivity.this, R.drawable.tab_selected_background));
                            android.util.Log.d("SmartSceneActivity", "Automation button set to SELECTED (primary)");
                        } else {
                            // Unselected: Dark background
                            buttonAutomation.setBackground(ContextCompat.getDrawable(SmartSceneActivity.this, R.drawable.tab_unselected_background));
                            android.util.Log.d("SmartSceneActivity", "Automation button set to UNSELECTED (dark)");
                        }
                        buttonAutomation.setTextColor(ContextCompat.getColor(SmartSceneActivity.this, R.color.white));
                        buttonAutomation.setBackgroundTintList(null);
                        buttonAutomation.invalidate();
                        buttonAutomation.requestLayout();
                    } catch (Exception e) {
                        android.util.Log.e("SmartSceneActivity", "Error updating Automation button", e);
                    }
                });
            }
            
            // Update Tap-to-Run button
            if (buttonTapToRun != null) {
                // Remove all Material Design default styling that causes stroke
                buttonTapToRun.setBackgroundTintList(null);
                buttonTapToRun.setElevation(0f);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    buttonTapToRun.setStateListAnimator(null);
                }
                
                // Set background - use post to ensure it happens after MaterialButton initialization
                buttonTapToRun.post(() -> {
                    try {
                        if (isAutomationSelected) {
                            // Unselected: Dark background
                            buttonTapToRun.setBackground(ContextCompat.getDrawable(SmartSceneActivity.this, R.drawable.tab_unselected_background));
                            android.util.Log.d("SmartSceneActivity", "Tap-to-Run button set to UNSELECTED (dark)");
                        } else {
                            // Selected: Primary color
                            buttonTapToRun.setBackground(ContextCompat.getDrawable(SmartSceneActivity.this, R.drawable.tab_selected_background));
                            android.util.Log.d("SmartSceneActivity", "Tap-to-Run button set to SELECTED (primary)");
                        }
                        buttonTapToRun.setTextColor(ContextCompat.getColor(SmartSceneActivity.this, R.color.white));
                        buttonTapToRun.setBackgroundTintList(null);
                        buttonTapToRun.invalidate();
                        buttonTapToRun.requestLayout();
                    } catch (Exception e) {
                        android.util.Log.e("SmartSceneActivity", "Error updating Tap-to-Run button", e);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneActivity", "Error updating tab buttons: " + e.getMessage(), e);
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            android.util.Log.e("SmartSceneActivity", "BottomNavigation is null!");
            return;
        }
        
        try {
            // Ensure bottom navigation is clickable and enabled
            bottomNavigation.setClickable(true);
            bottomNavigation.setFocusable(true);
            bottomNavigation.setEnabled(true);
            
            // Set up item selected listener - this fires when an item is selected
            bottomNavigation.setItemIconTintList(null);
            bottomNavigation.setOnItemSelectedListener(item -> {
                try {
                    int itemId = item.getItemId();
                    android.util.Log.d("SmartSceneActivity", "Bottom nav item selected: " + itemId + " (Home=" + R.id.nav_home + ", Smart=" + R.id.nav_smart + ")");
                    
                    // Remove active indicator background when selection changes
                    BottomNavSpacingHelper.onSelectionChanged(bottomNavigation);
                    
                    if (itemId == R.id.nav_home) {
                        android.util.Log.d("SmartSceneActivity", "Home button clicked - Navigating to MainActivity");
                        try {
                            Intent intent = new Intent(SmartSceneActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            android.util.Log.d("SmartSceneActivity", "Starting MainActivity with flags: CLEAR_TOP | SINGLE_TOP");
                            startActivity(intent);
                            android.util.Log.d("SmartSceneActivity", "MainActivity started, finishing SmartSceneActivity");
                            finish();
                        } catch (Exception e) {
                            android.util.Log.e("SmartSceneActivity", "Error navigating to Home: " + e.getMessage(), e);
                            android.widget.Toast.makeText(SmartSceneActivity.this, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    } else if (itemId == R.id.nav_smart) {
                        // Already on smart - do nothing
                        android.util.Log.d("SmartSceneActivity", "Already on Smart tab");
                        return true;
                    } else if (itemId == R.id.nav_reports) {
                        android.util.Log.d("SmartSceneActivity", "Navigating to Reports");
                        Intent intent = new Intent(SmartSceneActivity.this, ReportsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                        return true;
                    } else if (itemId == R.id.nav_account) {
                        android.util.Log.d("SmartSceneActivity", "Navigating to Account");
                        Intent intent = new Intent(SmartSceneActivity.this, AccountActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                        return true;
                    }
                    android.util.Log.w("SmartSceneActivity", "Unknown bottom nav item: " + itemId);
                    return false;
                } catch (Exception e) {
                    android.util.Log.e("SmartSceneActivity", "Error handling bottom nav click: " + e.getMessage(), e);
                    e.printStackTrace();
                    android.widget.Toast.makeText(SmartSceneActivity.this, "Navigation error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            
            // Also set up item reselected listener for better UX
            bottomNavigation.setOnItemReselectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_smart) {
                    // If already on smart and reselected, scroll to top or refresh
                    android.util.Log.d("SmartSceneActivity", "Smart tab reselected");
                }
            });
            
            // Set smart as selected
            bottomNavigation.setSelectedItemId(R.id.nav_smart);
            
            // Remove spacing between icon and text
            BottomNavSpacingHelper.removeSpacing(bottomNavigation);
            
            // Disable active indicator (blue background)
            BottomNavSpacingHelper.disableActiveIndicator(bottomNavigation);
            
            android.util.Log.d("SmartSceneActivity", "Bottom navigation setup completed");
        } catch (Exception e) {
            android.util.Log.e("SmartSceneActivity", "Error setting up bottom navigation: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void loadScenes() {
        try {
            if (adapter == null) {
                android.util.Log.e("SmartSceneActivity", "Adapter is null in loadScenes - creating new adapter");
                setupRecyclerView();
                if (adapter == null) {
                    android.util.Log.e("SmartSceneActivity", "Still null after setupRecyclerView");
                    return;
                }
            }

            if (recyclerViewScenes == null) {
                android.util.Log.e("SmartSceneActivity", "recyclerViewScenes is null in loadScenes");
                return;
            }

            android.util.Log.d("SmartSceneActivity", "Loading scenes for tab: " + currentTab);

            // Check if demo user or not logged in
            if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
                // Use mock data only for demo users
                List<SmartScene> scenes = MockDataProvider.getMockScenes(currentTab);
                if (scenes == null) {
                    scenes = new ArrayList<>();
                }
                displayScenes(scenes);
                return;
            }

            // For real users, load from API
            Integer homeId = null;
            com.smarthome.iot.models.Home primaryHome = Globals.getPrimaryHome();
            if (primaryHome != null) {
                homeId = primaryHome.getId();
            }

            // Check Globals cache first
            List<SmartScene> cachedScenes = Globals.getCachedScenes(homeId, currentTab);
            if (cachedScenes != null) {
                android.util.Log.d("SmartSceneActivity", "Using cached scenes: " + cachedScenes.size() + " for tab: " + currentTab);
                displayScenes(cachedScenes);
                return;
            }

            final Integer finalHomeId = homeId;
            Call<ApiResponse<List<SmartScene>>> call = apiService.getScenes(currentTab, homeId);
            call.enqueue(new Callback<ApiResponse<List<SmartScene>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<SmartScene>>> call, Response<ApiResponse<List<SmartScene>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<SmartScene> scenes = response.body().getData();
                        if (scenes == null) {
                            scenes = new ArrayList<>();
                        }
                        android.util.Log.d("SmartSceneActivity", "Loaded " + scenes.size() + " scenes from API for tab: " + currentTab);
                        // Cache in Globals
                        Globals.setCachedScenes(scenes, finalHomeId, currentTab);
                        displayScenes(scenes);
                    } else {
                        android.util.Log.w("SmartSceneActivity", "Failed to load scenes from API, showing empty");
                        displayScenes(new ArrayList<>());
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<SmartScene>>> call, Throwable t) {
                    android.util.Log.e("SmartSceneActivity", "Error loading scenes from API", t);
                    displayScenes(new ArrayList<>());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneActivity", "Error loading scenes: " + e.getMessage(), e);
            if (adapter != null) {
                adapter.setScenes(new ArrayList<>());
            }
        }
    }
    
    private void displayScenes(List<SmartScene> scenes) {
        try {
            if (adapter == null || recyclerViewScenes == null) {
                return;
            }
            
            if (!scenes.isEmpty()) {
                // Log first scene details for debugging
                SmartScene firstScene = scenes.get(0);
                android.util.Log.d("SmartSceneActivity", "First scene: " + firstScene.getName() + 
                    ", Type: " + firstScene.getType() + 
                    ", Color: " + firstScene.getColor() + 
                    ", Icon: " + firstScene.getIcon() + 
                    ", Tasks: " + (firstScene.getTasks() != null ? firstScene.getTasks().size() : 0));
                
                // Set scenes to adapter
                adapter.setScenes(scenes);
                android.util.Log.d("SmartSceneActivity", "Scenes set to adapter. Adapter item count: " + adapter.getItemCount());
                
                // Ensure RecyclerView is visible and refresh it
                recyclerViewScenes.setVisibility(View.VISIBLE);
                recyclerViewScenes.post(() -> {
                    adapter.notifyDataSetChanged();
                    recyclerViewScenes.invalidate();
                    recyclerViewScenes.requestLayout();
                    android.util.Log.d("SmartSceneActivity", "RecyclerView refreshed. Item count: " + adapter.getItemCount());
                });
            } else {
                android.util.Log.w("SmartSceneActivity", "No scenes available for tab: " + currentTab);
                adapter.setScenes(new ArrayList<>());
                recyclerViewScenes.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SmartSceneActivity", "Error displaying scenes: " + e.getMessage(), e);
            if (adapter != null) {
                adapter.setScenes(new ArrayList<>());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHomeNameDisplay();
    }

    private void updateHomeNameDisplay() {
        Home primaryHome = Globals.getPrimaryHome();
        if (primaryHome != null && textViewHomeName != null) {
            textViewHomeName.setText(primaryHome.getName());
            if (selectedHomeId == -1) {
                selectedHomeId = primaryHome.getId();
            }
        }
    }

    private void showHomeSelector(View anchorView) {
        // Check Globals cache first
        if (Globals.isHomesCacheValid()) {
            List<Home> cachedHomes = Globals.getUserHomes();
            if (cachedHomes != null && !cachedHomes.isEmpty()) {
                showHomeSelectorPopup(cachedHomes, anchorView);
                return;
            }
        }

        // Load homes from API
        Call<ApiResponse<List<Home>>> call = apiService.getHomes();
        call.enqueue(new Callback<ApiResponse<List<Home>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Home>>> call, Response<ApiResponse<List<Home>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Home> homes = response.body().getData();
                    if (homes != null && !homes.isEmpty()) {
                        Globals.setUserHomes(homes);
                        showHomeSelectorPopup(homes, anchorView);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Home>>> call, Throwable t) {
                android.util.Log.e("SmartSceneActivity", "Error loading homes", t);
            }
        });
    }

    private void showHomeSelectorPopup(List<Home> homes, View anchorView) {
        if (homeSelectorPopupWindow != null && homeSelectorPopupWindow.isShowing()) {
            homeSelectorPopupWindow.dismiss();
            hideHomeSelectorMask();
            return;
        }

        // Find selected home
        if (selectedHomeId == -1 && homes != null && !homes.isEmpty()) {
            for (Home home : homes) {
                if (home.isPrimary()) {
                    selectedHomeId = home.getId();
                    break;
                }
            }
            if (selectedHomeId == -1) {
                selectedHomeId = homes.get(0).getId();
            }
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.dialog_location_selector, null);
        RecyclerView recyclerViewHomes = popupView.findViewById(R.id.recyclerViewHomes);
        LinearLayout layoutHomeManagement = popupView.findViewById(R.id.layoutHomeManagement);

        float density = getResources().getDisplayMetrics().density;
        int popupWidth = (int) (248 * density);

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int popupHeight = popupView.getMeasuredHeight();

        homeSelectorPopupWindow = new PopupWindow(popupView, popupWidth, popupHeight, true);
        homeSelectorPopupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, android.R.color.transparent));
        homeSelectorPopupWindow.setElevation(8f);
        homeSelectorPopupWindow.setOutsideTouchable(true);

        LocationSelectorAdapter selectorAdapter = new LocationSelectorAdapter(
            homes,
            selectedHomeId,
            home -> {
                selectedHomeId = home.getId();
                if (textViewHomeName != null) {
                    textViewHomeName.setText(home.getName());
                }
                Globals.setPrimaryHome(home);
                Globals.clearScenesCache();
                loadScenes();
                homeSelectorPopupWindow.dismiss();
                hideHomeSelectorMask();
            }
        );

        recyclerViewHomes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHomes.setAdapter(selectorAdapter);

        if (layoutHomeManagement != null) {
            layoutHomeManagement.setVisibility(View.VISIBLE);
            layoutHomeManagement.setOnClickListener(v -> {
                homeSelectorPopupWindow.dismiss();
                hideHomeSelectorMask();
                Intent intent = new Intent(this, HomeManagementActivity.class);
                startActivity(intent);
            });
        }

        homeSelectorPopupWindow.setOnDismissListener(this::hideHomeSelectorMask);

        int[] location = new int[2];
        if (anchorView != null) {
            anchorView.getLocationInWindow(location);
        }

        int spacingPx = (int) (8 * density);
        int leftMarginPx = (int) (24 * density);
        int x = leftMarginPx;
        int y = anchorView != null ? location[1] + anchorView.getHeight() + spacingPx : 100;

        showHomeSelectorMask();
        homeSelectorPopupWindow.showAtLocation(
            anchorView != null ? anchorView : findViewById(android.R.id.content),
            android.view.Gravity.NO_GRAVITY, x, y
        );
    }

    private void showHomeSelectorMask() {
        View maskOverlay = findViewById(R.id.maskOverlay);
        if (maskOverlay != null) {
            maskOverlay.setVisibility(View.VISIBLE);
            maskOverlay.setOnClickListener(v -> {
                if (homeSelectorPopupWindow != null && homeSelectorPopupWindow.isShowing()) {
                    homeSelectorPopupWindow.dismiss();
                }
                hideHomeSelectorMask();
            });
        }
    }

    private void hideHomeSelectorMask() {
        View maskOverlay = findViewById(R.id.maskOverlay);
        if (maskOverlay != null) {
            maskOverlay.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Scene created, reload list to show new scene
            android.util.Log.d("SmartSceneActivity", "Scene created, reloading scenes");

            // Switch to the appropriate tab if scene type is specified
            if (data != null && data.hasExtra("scene_type")) {
                String sceneType = data.getStringExtra("scene_type");
                if ("tap_to_run".equals(sceneType) && !currentTab.equals("tap_to_run")) {
                    currentTab = "tap_to_run";
                    updateTabButtons();
                    updateRecyclerViewLayout();
                } else if ("automation".equals(sceneType) && !currentTab.equals("automation")) {
                    currentTab = "automation";
                    updateTabButtons();
                    updateRecyclerViewLayout();
                }
            }

            // Clear cache and reload to fetch fresh data from API
            Globals.clearScenesCache();
            loadScenes();

            // Show success dialog on the list screen
            com.smarthome.iot.ui.dialogs.SuccessDialog successDialog =
                new com.smarthome.iot.ui.dialogs.SuccessDialog(this, getString(R.string.tap_to_run_created));
            successDialog.show();
        } else if (requestCode == 200 && resultCode == RESULT_OK) {
            // Scene order changed or scene deleted, reload list
            android.util.Log.d("SmartSceneActivity", "Scenes updated, reloading");
            Globals.clearScenesCache();
            loadScenes();
        }
    }
}
