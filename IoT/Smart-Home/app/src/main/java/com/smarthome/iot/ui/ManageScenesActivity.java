package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.SmartScene;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.ManageScenesAdapter;
import com.smarthome.iot.ui.dialogs.DeleteSceneDialog;
import com.smarthome.iot.ui.dialogs.SuccessDialog;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageScenesActivity extends AppCompatActivity {
    private RecyclerView recyclerViewScenes;
    private ManageScenesAdapter adapter;
    private List<SmartScene> scenes;
    private ItemTouchHelper itemTouchHelper;
    private MaterialButton buttonAutomation;
    private MaterialButton buttonTapToRun;
    private String currentTab = "automation";
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_scenes);

        setStatusBarColor();
        
        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        authManager = new AuthManager(this);
        
        initializeViews();
        setupRecyclerView();
        setupTabButtons();
        loadScenes();
        setupItemTouchHelper();
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
        buttonAutomation = findViewById(R.id.buttonAutomation);
        buttonTapToRun = findViewById(R.id.buttonTapToRun);
        recyclerViewScenes = findViewById(R.id.recyclerViewScenes);

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }
        if (buttonMore != null) {
            buttonMore.setOnClickListener(v -> {
                // TODO: Show more options menu
            });
        }
    }

    private void setupTabButtons() {
        if (buttonAutomation != null) {
            buttonAutomation.setOnClickListener(v -> {
                if (!currentTab.equals("automation")) {
                    currentTab = "automation";
                    updateTabButtons();
                    loadScenes();
                }
            });
        }

        if (buttonTapToRun != null) {
            buttonTapToRun.setOnClickListener(v -> {
                if (!currentTab.equals("tap_to_run")) {
                    currentTab = "tap_to_run";
                    updateTabButtons();
                    loadScenes();
                }
            });
        }
        updateTabButtons();
    }

    private void updateTabButtons() {
        try {
            // Always update both buttons - works like a switch: one selected, one unselected
            // Selected tab: Primary color background
            // Unselected tab: Dark background

            boolean isAutomationSelected = currentTab.equals("automation");

            android.util.Log.d("ManageScenesActivity", "Updating tab buttons - isAutomationSelected: " + isAutomationSelected);

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
                            buttonAutomation.setBackground(ContextCompat.getDrawable(ManageScenesActivity.this, R.drawable.tab_selected_background_rounded));
                            buttonAutomation.setTypeface(buttonAutomation.getTypeface(), android.graphics.Typeface.BOLD);
                            android.util.Log.d("ManageScenesActivity", "Automation button set to SELECTED (primary)");
                        } else {
                            // Unselected: Dark background
                            buttonAutomation.setBackground(ContextCompat.getDrawable(ManageScenesActivity.this, R.drawable.tab_unselected_background_rounded));
                            buttonAutomation.setTypeface(buttonAutomation.getTypeface(), android.graphics.Typeface.NORMAL);
                            android.util.Log.d("ManageScenesActivity", "Automation button set to UNSELECTED (dark)");
                        }
                        buttonAutomation.setTextColor(ContextCompat.getColor(ManageScenesActivity.this, R.color.white));
                        buttonAutomation.setBackgroundTintList(null);
                        buttonAutomation.invalidate();
                        buttonAutomation.requestLayout();
                    } catch (Exception e) {
                        android.util.Log.e("ManageScenesActivity", "Error updating Automation button", e);
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
                        if (!isAutomationSelected) {
                            // Selected: Primary color
                            buttonTapToRun.setBackground(ContextCompat.getDrawable(ManageScenesActivity.this, R.drawable.tab_selected_background_rounded));
                            buttonTapToRun.setTypeface(buttonTapToRun.getTypeface(), android.graphics.Typeface.BOLD);
                            android.util.Log.d("ManageScenesActivity", "Tap-to-Run button set to SELECTED (primary)");
                        } else {
                            // Unselected: Dark background
                            buttonTapToRun.setBackground(ContextCompat.getDrawable(ManageScenesActivity.this, R.drawable.tab_unselected_background_rounded));
                            buttonTapToRun.setTypeface(buttonTapToRun.getTypeface(), android.graphics.Typeface.NORMAL);
                            android.util.Log.d("ManageScenesActivity", "Tap-to-Run button set to UNSELECTED (dark)");
                        }
                        buttonTapToRun.setTextColor(ContextCompat.getColor(ManageScenesActivity.this, R.color.white));
                        buttonTapToRun.setBackgroundTintList(null);
                        buttonTapToRun.invalidate();
                        buttonTapToRun.requestLayout();
                    } catch (Exception e) {
                        android.util.Log.e("ManageScenesActivity", "Error updating Tap-to-Run button", e);
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("ManageScenesActivity", "Error in updateTabButtons", e);
            e.printStackTrace();
        }
    }

    private void setupRecyclerView() {
        scenes = new ArrayList<>();
        adapter = new ManageScenesAdapter(
            scenes,
            this::onSceneDelete,
            this::onDragStart
        );
        recyclerViewScenes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewScenes.setAdapter(adapter);
    }

    private void loadScenes() {
        // Load from API
        Integer homeId = null;
        com.smarthome.iot.models.Home primaryHome = Globals.getPrimaryHome();
        if (primaryHome != null) {
            homeId = primaryHome.getId();
        }

        Call<ApiResponse<List<SmartScene>>> call = apiService.getScenes(currentTab, homeId);
        call.enqueue(new Callback<ApiResponse<List<SmartScene>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SmartScene>>> call, Response<ApiResponse<List<SmartScene>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SmartScene> apiScenes = response.body().getData();
                    scenes.clear();
                    if (apiScenes != null) {
                        scenes.addAll(apiScenes);
                    }
                    adapter.setScenes(scenes);
                    android.util.Log.d("ManageScenesActivity", "Loaded " + scenes.size() + " scenes from API");
                } else {
                    android.util.Log.w("ManageScenesActivity", "Failed to load scenes from API");
                    scenes.clear();
                    adapter.setScenes(scenes);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SmartScene>>> call, Throwable t) {
                android.util.Log.e("ManageScenesActivity", "Error loading scenes from API", t);
                scenes.clear();
                adapter.setScenes(scenes);
            }
        });
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN,
            0
        ) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                adapter.moveItem(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used
            }

            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                               int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
                // Save new order to backend
                saveSceneOrder();
            }
        };
        
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewScenes);
    }

    private void saveSceneOrder() {
        if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            // Demo user - just log
            List<SmartScene> orderedScenes = adapter.getScenes();
            List<Integer> sceneIds = new ArrayList<>();
            for (SmartScene scene : orderedScenes) {
                if (scene.getId() != null) {
                    sceneIds.add(scene.getId());
                }
            }
            android.util.Log.d("ManageScenesActivity", "Demo user - scene order: " + sceneIds);
            return;
        }
        
        List<SmartScene> orderedScenes = adapter.getScenes();
        List<Integer> sceneIds = new ArrayList<>();
        for (SmartScene scene : orderedScenes) {
            if (scene.getId() != null) {
                sceneIds.add(scene.getId());
            }
        }
        
        // Prepare order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("sceneIds", sceneIds);
        orderData.put("type", currentTab);
        
        Call<ApiResponse<Void>> call = apiService.updateSceneOrder(orderData);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("ManageScenesActivity", "Scene order saved successfully");
                } else {
                    android.util.Log.w("ManageScenesActivity", "Failed to save scene order");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                android.util.Log.e("ManageScenesActivity", "Error saving scene order", t);
            }
        });
    }

    private void onSceneDelete(SmartScene scene) {
        String sceneName = scene.getName() != null ? scene.getName() : getString(R.string.scene);
        DeleteSceneDialog dialog = new DeleteSceneDialog(this, sceneName, () -> {
            // Delete scene
            int position = -1;
            for (int i = 0; i < scenes.size(); i++) {
                if (scenes.get(i).getId() != null && scenes.get(i).getId().equals(scene.getId())) {
                    position = i;
                    break;
                }
            }
            
            if (position >= 0) {
                if (scene.getId() == null) {
                    // Scene has no ID, just remove locally
                    scenes.remove(position);
                    adapter.removeItem(position);
                    String successMessage = getString(R.string.scene_deleted_success, sceneName);
                    SuccessDialog successDialog = new SuccessDialog(this, successMessage);
                    successDialog.show();
                    setResult(RESULT_OK);
                    return;
                }
                
                // Call API to delete scene
                if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
                    // Demo user - just remove locally
                    scenes.remove(position);
                    adapter.removeItem(position);
                    String successMessage = getString(R.string.scene_deleted_success, sceneName);
                    SuccessDialog successDialog = new SuccessDialog(this, successMessage);
                    successDialog.show();
                    setResult(RESULT_OK);
                    return;
                }
                
                // Create final copy of position for use in inner class
                final int finalPosition = position;
                Call<ApiResponse<Void>> call = apiService.deleteScene(scene.getId());
                call.enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            scenes.remove(finalPosition);
                            adapter.removeItem(finalPosition);
                            String successMessage = getString(R.string.scene_deleted_success, sceneName);
                            SuccessDialog successDialog = new SuccessDialog(ManageScenesActivity.this, successMessage);
                            successDialog.show();
                            setResult(RESULT_OK);
                            android.util.Log.d("ManageScenesActivity", "Scene deleted successfully: " + sceneName);
                        } else {
                            android.util.Log.w("ManageScenesActivity", "Failed to delete scene via API");
                            android.widget.Toast.makeText(ManageScenesActivity.this, 
                                "Failed to delete scene", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        android.util.Log.e("ManageScenesActivity", "Error deleting scene", t);
                        android.widget.Toast.makeText(ManageScenesActivity.this, 
                            "Error deleting scene: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        dialog.show();
    }

    private void onDragStart(RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null) {
            itemTouchHelper.startDrag(viewHolder);
        }
    }
}
