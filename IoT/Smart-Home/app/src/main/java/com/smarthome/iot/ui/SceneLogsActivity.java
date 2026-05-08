package com.smarthome.iot.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.SceneLog;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.SceneLogsAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SceneLogsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewLogs;
    private SceneLogsAdapter adapter;
    private ApiService apiService;
    private AuthManager authManager;
    private Integer sceneId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_logs);

        setStatusBarColor();
        
        // Get scene ID from intent
        sceneId = getIntent().getIntExtra("scene_id", -1);
        
        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        authManager = new AuthManager(this);
        
        initializeViews();
        setupRecyclerView();
        loadLogs();
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
        ImageButton buttonCalendar = findViewById(R.id.buttonCalendar);
        recyclerViewLogs = findViewById(R.id.recyclerViewLogs);

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }
        if (buttonCalendar != null) {
            buttonCalendar.setOnClickListener(v -> {
                // TODO: Show calendar picker for filtering logs by date
                Toast.makeText(this, "Calendar filter coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new SceneLogsAdapter();
        recyclerViewLogs.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLogs.setAdapter(adapter);
    }

    private void loadLogs() {
        if (sceneId == -1) {
            adapter.setLogs(new ArrayList<>());
            return;
        }

        // Load from API
        Call<ApiResponse<List<SceneLog>>> call = apiService.getSceneLogs(sceneId, 1, 100);
        call.enqueue(new Callback<ApiResponse<List<SceneLog>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<SceneLog>>> call, Response<ApiResponse<List<SceneLog>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SceneLog> logs = response.body().getData();
                    if (logs == null) {
                        logs = new ArrayList<>();
                    }
                    adapter.setLogs(logs);
                    android.util.Log.d("SceneLogsActivity", "Loaded " + logs.size() + " logs from API");
                } else {
                    android.util.Log.w("SceneLogsActivity", "Failed to load logs from API");
                    adapter.setLogs(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<SceneLog>>> call, Throwable t) {
                android.util.Log.e("SceneLogsActivity", "Error loading logs from API", t);
                adapter.setLogs(new ArrayList<>());
            }
        });
    }

}
