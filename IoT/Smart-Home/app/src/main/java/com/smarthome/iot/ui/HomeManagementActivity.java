package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.HomeAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeManagementActivity extends AppCompatActivity {
    private RecyclerView recyclerViewHomes;
    private MaterialButton buttonCreateHome;
    private MaterialButton buttonJoinHome;
    private ProgressBar progressBar;
    private HomeAdapter adapter;
    private List<Home> homes;
    private AuthManager authManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_management);

        setStatusBarColor();
        
        authManager = new AuthManager(this);
        
        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        
        initializeViews();
        setupRecyclerView();
        loadHomes();
        setupClickListeners();
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
        recyclerViewHomes = findViewById(R.id.recyclerViewHomes);
        progressBar = findViewById(R.id.progressBar);
        buttonCreateHome = findViewById(R.id.buttonCreateHome);
        buttonJoinHome = findViewById(R.id.buttonJoinHome);

        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            Toast.makeText(this, "More options coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        homes = new ArrayList<>();
        adapter = new HomeAdapter(homes, this::onHomeClick);
        recyclerViewHomes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHomes.setAdapter(adapter);
    }

    private void loadHomes() {
        if (!authManager.isLoggedIn()) {
            // If not logged in, use mock data
            homes.clear();
            homes.addAll(MockDataProvider.getMockHomes());
            adapter.notifyDataSetChanged();
            return;
        }

        // Show loading state
        showLoading(true);

        Call<ApiResponse<List<Home>>> call = apiService.getHomes();
        call.enqueue(new Callback<ApiResponse<List<Home>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Home>>> call, Response<ApiResponse<List<Home>>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Home> apiHomes = response.body().getData();
                    if (apiHomes != null && !apiHomes.isEmpty()) {
                        homes.clear();
                        homes.addAll(apiHomes);
                        adapter.notifyDataSetChanged();
                    } else {
                        // No homes found, show empty state
                        homes.clear();
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    android.util.Log.w("HomeManagement", "Failed to load homes from API");
                    homes.clear();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Home>>> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("HomeManagement", "Error loading homes", t);
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewHomes != null) {
            recyclerViewHomes.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        buttonCreateHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateHomeActivity.class);
            startActivityForResult(intent, 100);
        });

        buttonJoinHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, JoinHomeActivity.class);
            startActivityForResult(intent, 101);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 100 || requestCode == 101 || requestCode == 102) && resultCode == RESULT_OK) {
            // Reload homes list after create, join, or detail changes
            loadHomes();
        }
    }

    private void onHomeClick(Home home) {
        Intent intent = new Intent(this, HomeDetailActivity.class);
        intent.putExtra("home_id", home.getId());
        intent.putExtra("home_name", home.getName());
        intent.putExtra("home_address", home.getAddress());
        startActivityForResult(intent, 102);
    }
}
