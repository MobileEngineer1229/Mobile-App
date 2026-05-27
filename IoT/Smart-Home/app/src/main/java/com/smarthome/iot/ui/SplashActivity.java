package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.smarthome.iot.R;
import com.smarthome.iot.network.HealthCheckService;
import com.smarthome.iot.models.User;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.MockDataProvider;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 2500; // 2.5 seconds

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Don't apply theme for splash - use default blue background
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        authManager = new AuthManager(this);

        // Navigate after 2.5 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (MockDataProvider.isMockupMode()) {
                prepareMockupSession();
                Intent intent = new Intent(SplashActivity.this, SignInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            // Check if user is logged in (auto-login on same device)
            if (authManager.isAutoLoginEnabled()) {
                // Check server health before navigating to MainActivity
                Log.d(TAG, "Auto-login enabled, checking server health...");
                checkServerHealthAndNavigate();
            } else {
                // User not logged in, go to Onboarding
                Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }

    private void prepareMockupSession() {
        authManager.saveToken("mockup-iot-token");
        authManager.saveUserInfo(1, "demo@smartify.com");
        authManager.saveUserName("Demo Owner");
        authManager.saveLoginEmail("demo@smartify.com");

        User user = new User();
        user.setId(1);
        user.setEmail("demo@smartify.com");
        user.setFirstName("Demo");
        user.setLastName("Owner");
        Globals.setCurrentUser(user);
        Globals.setUserHomes(MockDataProvider.getMockHomes());
        Globals.setPrimaryHome(MockDataProvider.getMockHomes().get(0));
        Globals.setUserRooms(MockDataProvider.getMockRooms(1), 1);
        Globals.setCachedDevices(MockDataProvider.getMockDevicesForScene("all", null), 1);
        Globals.setApiHealthStatus(true);
        Globals.setMqttBrokerAvailable(true);
    }

    /**
     * Check server health and navigate to MainActivity if server is available
     */
    private void checkServerHealthAndNavigate() {
        HealthCheckService.getInstance().checkHealth(new HealthCheckService.HealthCheckCallback() {
            @Override
            public void onHealthCheckComplete(boolean isHealthy) {
                runOnUiThread(() -> {
                    if (isHealthy) {
                        // Server is healthy, navigate to MainActivity
                        Log.d(TAG, "Server is healthy, navigating to MainActivity");
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Server is not available, show alert
                        Log.w(TAG, "Server is not available, showing alert dialog");
                        showServerUnavailableDialog();
                    }
                });
            }
        });
    }

    /**
     * Show alert dialog when server is not available
     */
    private void showServerUnavailableDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Server Not Available")
                .setMessage("Unable to connect to the server. Please check your internet connection or try again later.")
                .setCancelable(false)
                .setPositiveButton("Retry", (dialog, which) -> {
                    // Retry server health check
                    Log.d(TAG, "User selected Retry, checking server health again...");
                    checkServerHealthAndNavigate();
                })
                .setNegativeButton("Go to Login", (dialog, which) -> {
                    // Navigate to onboarding/login screen
                    Log.d(TAG, "User selected Go to Login");
                    Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNeutralButton("Exit", (dialog, which) -> {
                    // Exit the app
                    Log.d(TAG, "User selected Exit");
                    finish();
                })
                .show();
    }
}

