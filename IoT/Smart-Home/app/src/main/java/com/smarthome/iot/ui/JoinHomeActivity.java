package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.dialogs.SuccessDialog;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JoinHomeActivity extends AppCompatActivity {
    private FrameLayout frameLayoutQRScanner;
    private FrameLayout frameLayoutLoading;
    private LinearLayout layoutManualEntry;
    private MaterialButton buttonEnterCode;
    private MaterialButton buttonJoin;
    private TextInputEditText editTextInvitationCode;
    
    private boolean isQRMode = true;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_home);

        setStatusBarColor();

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
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
        frameLayoutQRScanner = findViewById(R.id.frameLayoutQRScanner);
        frameLayoutLoading = findViewById(R.id.frameLayoutLoading);
        layoutManualEntry = findViewById(R.id.layoutManualEntry);
        buttonEnterCode = findViewById(R.id.buttonEnterCode);
        buttonJoin = findViewById(R.id.buttonJoin);
        editTextInvitationCode = findViewById(R.id.editTextInvitationCode);

        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            // TODO: Show more options
        });
    }

    private void setupClickListeners() {
        buttonEnterCode.setOnClickListener(v -> {
            // Switch to manual entry mode
            frameLayoutQRScanner.setVisibility(View.GONE);
            layoutManualEntry.setVisibility(View.VISIBLE);
            isQRMode = false;
        });
        
        buttonJoin.setOnClickListener(v -> {
            String code = editTextInvitationCode.getText().toString().trim();
            if (code.isEmpty()) {
                editTextInvitationCode.setError("Invitation code is required");
                return;
            }
            
            // Show loading
            layoutManualEntry.setVisibility(View.GONE);
            frameLayoutLoading.setVisibility(View.VISIBLE);
            
            joinHome(code);
        });
    }

    private void joinHome(String invitationCode) {
        if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            // Demo user - simulate success
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                frameLayoutLoading.setVisibility(View.GONE);
                SuccessDialog successDialog = new SuccessDialog(this, 
                    getString(R.string.home_joined_success));
                successDialog.show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("home_joined", true);
                setResult(RESULT_OK, resultIntent);
                new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 2000);
            }, 2000);
            return;
        }

        // Prepare join data
        Map<String, Object> joinData = new HashMap<>();
        joinData.put("invitationCode", invitationCode);

        // Call API to join home
        Call<ApiResponse<com.smarthome.iot.models.Home>> call = apiService.joinHome(joinData);
        call.enqueue(new Callback<ApiResponse<com.smarthome.iot.models.Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.smarthome.iot.models.Home>> call, Response<ApiResponse<com.smarthome.iot.models.Home>> response) {
                frameLayoutLoading.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.smarthome.iot.models.Home joinedHome = response.body().getData();
                    android.util.Log.d("JoinHomeActivity", "Home joined successfully: " + (joinedHome != null ? joinedHome.getName() : "unknown"));
                    
                    // Show success and navigate back
                    SuccessDialog successDialog = new SuccessDialog(JoinHomeActivity.this, 
                        getString(R.string.home_joined_success));
                    successDialog.show();
                    
                    // Return result
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("home_joined", true);
                    if (joinedHome != null) {
                        resultIntent.putExtra("home_id", joinedHome.getId());
                        resultIntent.putExtra("home_name", joinedHome.getName());
                    }
                    setResult(RESULT_OK, resultIntent);
                    
                    // Finish after delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 2000);
                } else {
                    android.util.Log.w("JoinHomeActivity", "Failed to join home via API");
                    layoutManualEntry.setVisibility(View.VISIBLE);
                    String errorMessage = "Failed to join home";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMessage = response.body().getError().getMessage();
                    }
                    Toast.makeText(JoinHomeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.smarthome.iot.models.Home>> call, Throwable t) {
                frameLayoutLoading.setVisibility(View.GONE);
                layoutManualEntry.setVisibility(View.VISIBLE);
                android.util.Log.e("JoinHomeActivity", "Error joining home", t);
                Toast.makeText(JoinHomeActivity.this, 
                    "Error joining home: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
