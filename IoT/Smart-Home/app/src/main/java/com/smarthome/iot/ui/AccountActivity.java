package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.models.SettingsItem;
import com.smarthome.iot.models.User;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.SettingsAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.BottomNavSpacingHelper;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends AppCompatActivity {
    private TextView textViewHomeName;
    private TextView textViewUserName;
    private TextView textViewUserEmail;
    private RecyclerView recyclerViewGeneralSettings;
    private RecyclerView recyclerViewSupportSettings;
    private LinearLayout profileSection;
    private LinearLayout logoutSection;
    private BottomNavigationView bottomNavigation;

    private ApiService apiService;
    private AuthManager authManager;
    private SettingsAdapter generalSettingsAdapter;
    private SettingsAdapter supportSettingsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Set status bar color to dark_1
        setStatusBarColor();

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
        setupSettingsLists();
        setupBottomNavigation();
        setupClickListeners();
        loadUserProfile();
        loadPrimaryHome();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        textViewHomeName = findViewById(R.id.textViewHomeName);
        textViewUserName = findViewById(R.id.textViewUserName);
        textViewUserEmail = findViewById(R.id.textViewUserEmail);
        recyclerViewGeneralSettings = findViewById(R.id.recyclerViewGeneralSettings);
        recyclerViewSupportSettings = findViewById(R.id.recyclerViewSupportSettings);
        profileSection = findViewById(R.id.profileSection);
        logoutSection = findViewById(R.id.logoutSection);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupSettingsLists() {
        // General Settings
        List<SettingsItem> generalSettings = new ArrayList<>();
        generalSettings.add(new SettingsItem(getString(R.string.home_management), R.drawable.ic_home_management, "home_management"));
        generalSettings.add(new SettingsItem(getString(R.string.voice_assistants), R.drawable.ic_mic, "voice_assistants"));
        generalSettings.add(new SettingsItem(getString(R.string.notifications), R.drawable.ic_notifications, "notifications"));
        generalSettings.add(new SettingsItem(getString(R.string.account_security), R.drawable.ic_shield, "account_security"));
        generalSettings.add(new SettingsItem(getString(R.string.linked_accounts), R.drawable.ic_linked_accounts, "linked_accounts"));
        generalSettings.add(new SettingsItem(getString(R.string.app_appearance), R.drawable.ic_eye_settings, "app_appearance"));
        generalSettings.add(new SettingsItem(getString(R.string.additional_settings), R.drawable.ic_settings, "additional_settings"));

        generalSettingsAdapter = new SettingsAdapter(generalSettings, this::onSettingClick);
        recyclerViewGeneralSettings.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGeneralSettings.setAdapter(generalSettingsAdapter);

        // Support Settings
        List<SettingsItem> supportSettings = new ArrayList<>();
        supportSettings.add(new SettingsItem(getString(R.string.data_analytics), R.drawable.ic_data_analytics, "data_analytics"));
        supportSettings.add(new SettingsItem(getString(R.string.help_support), R.drawable.ic_help_support, "help_support"));

        supportSettingsAdapter = new SettingsAdapter(supportSettings, this::onSettingClick);
        recyclerViewSupportSettings.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSupportSettings.setAdapter(supportSettingsAdapter);
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            android.util.Log.e("AccountActivity", "BottomNavigation is null!");
            return;
        }

        bottomNavigation.setItemIconTintList(null);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            // Remove active indicator background when selection changes
            BottomNavSpacingHelper.onSelectionChanged(bottomNavigation);
            
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(AccountActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // Don't call finish() immediately - let the activity lifecycle handle it
                return true;
            } else if (itemId == R.id.nav_smart) {
                Intent intent = new Intent(AccountActivity.this, SmartSceneActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_reports) {
                Intent intent = new Intent(AccountActivity.this, ReportsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_account) {
                // Already on account
                return true;
            }
            return false;
        });
        
        // Set account as selected
        bottomNavigation.setSelectedItemId(R.id.nav_account);
        
        // Remove spacing between icon and text
        BottomNavSpacingHelper.removeSpacing(bottomNavigation);
        
        // Disable active indicator (blue background)
        BottomNavSpacingHelper.disableActiveIndicator(bottomNavigation);
    }

    private void setupClickListeners() {
        profileSection.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyProfileActivity.class);
            startActivity(intent);
        });

        logoutSection.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void onSettingClick(SettingsItem item) {
        String action = item.getAction();
        Intent intent;
        switch (action) {
            case "home_management":
                intent = new Intent(this, HomeManagementActivity.class);
                startActivity(intent);
                break;
            case "voice_assistants":
                intent = new Intent(this, VoiceAssistantsActivity.class);
                startActivity(intent);
                break;
            case "notifications":
                intent = new Intent(this, NotificationsActivity.class);
                startActivity(intent);
                break;
            case "account_security":
                intent = new Intent(this, AccountSecurityActivity.class);
                startActivity(intent);
                break;
            case "linked_accounts":
                intent = new Intent(this, LinkedAccountsActivity.class);
                startActivity(intent);
                break;
            case "additional_settings":
                intent = new Intent(this, AdditionalSettingsActivity.class);
                startActivity(intent);
                break;
            case "data_analytics":
                intent = new Intent(this, DataAnalyticsActivity.class);
                startActivity(intent);
                break;
            case "app_appearance":
                intent = new Intent(this, AppAppearanceActivity.class);
                startActivity(intent);
                break;
            case "help_support":
                intent = new Intent(this, HelpSupportActivity.class);
                startActivity(intent);
                break;
            default:
                Toast.makeText(this, item.getTitle() + " - Coming soon", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void loadUserProfile() {
        if (!authManager.isLoggedIn()) {
            navigateToSignIn();
            return;
        }

        Call<ApiResponse<User>> call = apiService.getProfile();
        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user != null) {
                        updateUserProfile(user);
                    }
                } else {
                    // Use cached email if available
                    String email = authManager.getUserEmail();
                    if (email != null) {
                        textViewUserEmail.setText(email);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                // Use cached email if available
                String email = authManager.getUserEmail();
                if (email != null) {
                    textViewUserEmail.setText(email);
                }
            }
        });
    }

    private void updateUserProfile(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            textViewUserName.setText(user.getFirstName() + " " + user.getLastName());
        } else if (user.getFirstName() != null) {
            textViewUserName.setText(user.getFirstName());
        } else {
            textViewUserName.setText("User");
        }
        
        if (user.getEmail() != null) {
            textViewUserEmail.setText(user.getEmail());
        }
    }

    private void showLogoutDialog() {
        try {
            View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_logout, null);
            
            if (bottomSheetView == null) {
                android.util.Log.e("AccountActivity", "Failed to inflate logout dialog layout");
                // Fallback: logout directly if dialog can't be shown
                performLogout();
                return;
            }
            
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
            bottomSheetDialog.setContentView(bottomSheetView);
            
            // Set background to be dimmed for better visual effect
            if (bottomSheetDialog.getWindow() != null) {
                bottomSheetDialog.getWindow().setDimAmount(0.5f);
            }
            
            com.google.android.material.button.MaterialButton buttonCancel = 
                bottomSheetView.findViewById(R.id.buttonCancel);
            com.google.android.material.button.MaterialButton buttonLogout = 
                bottomSheetView.findViewById(R.id.buttonLogout);
            
            if (buttonCancel != null) {
                buttonCancel.setOnClickListener(v -> {
                    try {
                        bottomSheetDialog.dismiss();
                    } catch (Exception e) {
                        android.util.Log.e("AccountActivity", "Error dismissing dialog", e);
                    }
                });
            }
            
            if (buttonLogout != null) {
                buttonLogout.setOnClickListener(v -> {
                    try {
                        bottomSheetDialog.dismiss();
                        performLogout();
                    } catch (Exception e) {
                        android.util.Log.e("AccountActivity", "Error in logout button click", e);
                        // Still try to logout even if dialog dismiss fails
                        performLogout();
                    }
                });
            } else {
                android.util.Log.e("AccountActivity", "Logout button not found in dialog");
            }
            
            bottomSheetDialog.show();
        } catch (Exception e) {
            android.util.Log.e("AccountActivity", "Error showing logout dialog", e);
            // Fallback: logout directly if dialog fails
            performLogout();
        }
    }

    private void performLogout() {
        try {
            // Check if demo user - skip API call for demo users
            boolean isDemoUser = MockDataProvider.isDemoUser(authManager);
            
            if (isDemoUser || !authManager.isLoggedIn()) {
                // For demo users or when not logged in, logout immediately
                // Clear all user data from Globals
                Globals.clearAllUserData();
                authManager.logout();
                navigateToSignIn();
                return;
            }
            
            // For real users, try to call logout API
            try {
                Call<ApiResponse<Void>> call = apiService.logout();
                call.enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        // Clear all user data from Globals
                        Globals.clearAllUserData();
                        // Logout locally regardless of API response
                        authManager.logout();
                        navigateToSignIn();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        // Clear all user data from Globals
                        Globals.clearAllUserData();
                        // Logout locally even if API call fails
                        authManager.logout();
                        navigateToSignIn();
                    }
                });
            } catch (Exception e) {
                // Clear all user data from Globals
                Globals.clearAllUserData();
                // If API call fails due to exception, logout locally anyway
                android.util.Log.e("AccountActivity", "Error calling logout API", e);
                authManager.logout();
                navigateToSignIn();
            }
        } catch (Exception e) {
            // Ensure logout always completes even if there's an unexpected error
            android.util.Log.e("AccountActivity", "Unexpected error during logout", e);
            try {
                // Clear all user data from Globals
                Globals.clearAllUserData();
                authManager.logout();
                navigateToSignIn();
            } catch (Exception ex) {
                android.util.Log.e("AccountActivity", "Critical error during logout", ex);
                // Force navigation to sign in screen
                Intent intent = new Intent(AccountActivity.this, SignInActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }
    }

    private void loadPrimaryHome() {
        if (!authManager.isLoggedIn()) {
            return;
        }

        Call<ApiResponse<Home>> call = apiService.getPrimaryHome();
        call.enqueue(new Callback<ApiResponse<Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<Home>> call, Response<ApiResponse<Home>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Home home = response.body().getData();
                    if (home != null && textViewHomeName != null) {
                        textViewHomeName.setText(home.getName());
                        // Cache primary home in Globals
                        Globals.setPrimaryHome(home);
                    }
                } else {
                    // If no primary home, try to get first home from list
                    loadFirstHome();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Home>> call, Throwable t) {
                android.util.Log.e("AccountActivity", "Error loading primary home", t);
                // Try to load first home as fallback
                loadFirstHome();
            }
        });
    }

    private void loadFirstHome() {
        Call<ApiResponse<List<Home>>> call = apiService.getHomes();
        call.enqueue(new Callback<ApiResponse<List<Home>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Home>>> call, Response<ApiResponse<List<Home>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Home> homes = response.body().getData();
                    if (homes != null && !homes.isEmpty() && textViewHomeName != null) {
                        textViewHomeName.setText(homes.get(0).getName());
                        // Cache first home as primary
                        Globals.setPrimaryHome(homes.get(0));
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Home>>> call, Throwable t) {
                android.util.Log.e("AccountActivity", "Error loading homes", t);
            }
        });
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(AccountActivity.this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

