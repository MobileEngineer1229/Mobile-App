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

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.SmartScene;
import com.smarthome.iot.models.SceneTask;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.SelectSceneAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectSceneActivity extends AppCompatActivity {
    private RecyclerView recyclerViewScenes;
    private SelectSceneAdapter adapter;
    private MaterialButton buttonOk;
    private String currentTab = "automation"; // "automation" or "tap_to_run"
    private String selectedSceneId;
    private ApiService apiService;
    private AuthManager authManager;
    private List<SmartScene> scenes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            ThemeHelper.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_select_scene);

            setStatusBarColor();
            
            // Initialize API
            ApiClient.initialize(this);
            apiService = ApiClient.getClient().create(ApiService.class);
            authManager = new AuthManager(this);
            
            initializeViews();
            setupRecyclerView();
            setupClickListeners();
            updateTabButtons(); // Initialize tab button states
            loadScenes();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SelectSceneActivity", "Error in onCreate: " + e.getMessage(), e);
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
            recyclerViewScenes = findViewById(R.id.recyclerViewScenes);
            buttonOk = findViewById(R.id.buttonOk);
            
            MaterialButton buttonAutomation = findViewById(R.id.buttonAutomation);
            MaterialButton buttonTapToRun = findViewById(R.id.buttonTapToRun);

            if (buttonBack != null) {
                buttonBack.setOnClickListener(v -> finish());
            }
            if (buttonMore != null) {
                buttonMore.setOnClickListener(v -> {
                    // TODO: Show more options
                });
            }
            
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
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SelectSceneActivity", "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void updateTabButtons() {
        try {
            MaterialButton buttonAutomation = findViewById(R.id.buttonAutomation);
            MaterialButton buttonTapToRun = findViewById(R.id.buttonTapToRun);
            
            if (buttonAutomation != null) {
                if (currentTab.equals("automation")) {
                    buttonAutomation.setBackground(ContextCompat.getDrawable(this, R.drawable.tab_selected_background));
                    buttonAutomation.setTextColor(ContextCompat.getColor(this, R.color.white));
                    buttonAutomation.setTypeface(buttonAutomation.getTypeface(), android.graphics.Typeface.BOLD);
                } else {
                    buttonAutomation.setBackground(ContextCompat.getDrawable(this, R.drawable.tab_unselected_background));
                    buttonAutomation.setTextColor(ContextCompat.getColor(this, R.color.white));
                    buttonAutomation.setTypeface(buttonAutomation.getTypeface(), android.graphics.Typeface.NORMAL);
                }
            }
            
            if (buttonTapToRun != null) {
                if (currentTab.equals("tap_to_run")) {
                    buttonTapToRun.setBackground(ContextCompat.getDrawable(this, R.drawable.tab_selected_background));
                    buttonTapToRun.setTextColor(ContextCompat.getColor(this, R.color.white));
                    buttonTapToRun.setTypeface(buttonTapToRun.getTypeface(), android.graphics.Typeface.BOLD);
                } else {
                    buttonTapToRun.setBackground(ContextCompat.getDrawable(this, R.drawable.tab_unselected_background));
                    buttonTapToRun.setTextColor(ContextCompat.getColor(this, R.color.white));
                    buttonTapToRun.setTypeface(buttonTapToRun.getTypeface(), android.graphics.Typeface.NORMAL);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRecyclerView() {
        if (recyclerViewScenes == null) {
            return;
        }
        
        try {
            adapter = new SelectSceneAdapter(scene -> {
                selectedSceneId = scene.getId() != null ? scene.getId().toString() : null;
                adapter.setSelectedSceneId(selectedSceneId);
            });
            recyclerViewScenes.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewScenes.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SelectSceneActivity", "Error setting up RecyclerView: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        if (buttonOk != null) {
            buttonOk.setOnClickListener(v -> {
                confirmSelection();
            });
        }
    }

    private void loadScenes() {
        if (adapter == null) {
            return;
        }
        
        try {
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
                        scenes = response.body().getData();
                        if (scenes == null) {
                            scenes = new ArrayList<>();
                        }
                        adapter.setScenes(scenes);
                        android.util.Log.d("SelectSceneActivity", "Loaded " + scenes.size() + " scenes from API");
                    } else {
                        android.util.Log.w("SelectSceneActivity", "Failed to load scenes from API");
                        scenes = new ArrayList<>();
                        adapter.setScenes(scenes);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<SmartScene>>> call, Throwable t) {
                    android.util.Log.e("SelectSceneActivity", "Error loading scenes from API", t);
                    scenes = new ArrayList<>();
                    adapter.setScenes(scenes);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SelectSceneActivity", "Error loading scenes: " + e.getMessage(), e);
            scenes = new ArrayList<>();
            adapter.setScenes(scenes);
        }
    }

    private void confirmSelection() {
        try {
            if (selectedSceneId == null) {
                android.widget.Toast.makeText(this, "Please select a scene", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Find selected scene from loaded scenes
            SmartScene selectedScene = null;
            for (SmartScene scene : scenes) {
                if (scene.getId() != null && scene.getId().toString().equals(selectedSceneId)) {
                    selectedScene = scene;
                    break;
                }
            }
            
            if (selectedScene == null) {
                android.widget.Toast.makeText(this, "Selected scene not found", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create SceneTask for select_scene
            SceneTask task = new SceneTask("select_scene");
            task.setSceneId(selectedScene.getId());
            task.setSceneName(selectedScene.getName());
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra("task_data", task);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("SelectSceneActivity", "Error confirming selection: " + e.getMessage(), e);
        }
    }
}
