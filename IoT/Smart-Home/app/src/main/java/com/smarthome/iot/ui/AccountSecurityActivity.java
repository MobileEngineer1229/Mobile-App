package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.AccountAction;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.SecuritySetting;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.AccountActionAdapter;
import com.smarthome.iot.ui.adapters.SecurityToggleAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountSecurityActivity extends AppCompatActivity {
    private RecyclerView recyclerViewSecurityToggles;
    private RecyclerView recyclerViewAccountActions;
    private SecurityToggleAdapter securityAdapter;
    private AccountActionAdapter actionAdapter;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_security);

        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
        setupRecyclerViews();
        loadSecuritySettings();
        setupAccountActions();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerViewSecurityToggles = findViewById(R.id.recyclerViewSecurityToggles);
        recyclerViewAccountActions = findViewById(R.id.recyclerViewAccountActions);
    }

    private void setupRecyclerViews() {
        recyclerViewSecurityToggles.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAccountActions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadSecuritySettings() {
        List<SecuritySetting> settings = new ArrayList<>();
        settings.add(new SecuritySetting("biometric_id", false));
        settings.add(new SecuritySetting("face_id", false));
        settings.add(new SecuritySetting("sms_authenticator", false));
        settings.add(new SecuritySetting("google_authenticator", false));

        securityAdapter = new SecurityToggleAdapter(settings, this::onSecurityToggle);
        recyclerViewSecurityToggles.setAdapter(securityAdapter);
    }

    private void onSecurityToggle(SecuritySetting setting, boolean enabled) {
        if (!authManager.isLoggedIn()) {
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("type", setting.getType());
        updateData.put("enabled", enabled);

        Call<ApiResponse<Map<String, Object>>> call = apiService.updateSecuritySetting(setting.getType(), updateData);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                // Setting updated
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                // Handle error
            }
        });
    }

    private void setupAccountActions() {
        List<AccountAction> actions = new ArrayList<>();
        actions.add(new AccountAction(
            getString(R.string.change_password),
            "",
            "change_password",
            false
        ));
        actions.add(new AccountAction(
            getString(R.string.device_management),
            getString(R.string.device_management_subtitle),
            "device_management",
            false
        ));
        actions.add(new AccountAction(
            getString(R.string.deactivate_account),
            getString(R.string.deactivate_account_subtitle),
            "deactivate_account",
            false
        ));
        actions.add(new AccountAction(
            getString(R.string.delete_account),
            getString(R.string.delete_account_subtitle),
            "delete_account",
            true
        ));

        actionAdapter = new AccountActionAdapter(actions, this::onAccountActionClick);
        recyclerViewAccountActions.setAdapter(actionAdapter);
    }

    private void onAccountActionClick(AccountAction action) {
        switch (action.getAction()) {
            case "change_password":
                // TODO: Navigate to change password screen
                Toast.makeText(this, "Change password coming soon", Toast.LENGTH_SHORT).show();
                break;
            case "device_management":
                // TODO: Navigate to device management screen
                Toast.makeText(this, "Device management coming soon", Toast.LENGTH_SHORT).show();
                break;
            case "deactivate_account":
                showDeactivateAccountDialog();
                break;
            case "delete_account":
                showDeleteAccountDialog();
                break;
        }
    }

    private void showDeactivateAccountDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.deactivate_account)
            .setMessage("Are you sure you want to deactivate your account? You can reactivate it later.")
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                deactivateAccount();
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_account)
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            .setPositiveButton(R.string.delete_account, (dialog, which) -> {
                deleteAccount();
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }

    private void deactivateAccount() {
        if (!authManager.isLoggedIn()) {
            return;
        }

        Call<ApiResponse<Void>> call = apiService.deactivateAccount();
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    // Clear all user data from Globals
                    Globals.clearAllUserData();
                    authManager.logout();
                    navigateToSignIn();
                } else {
                    Toast.makeText(AccountSecurityActivity.this, "Failed to deactivate account", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(AccountSecurityActivity.this, "Error deactivating account", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteAccount() {
        if (!authManager.isLoggedIn()) {
            return;
        }

        Call<ApiResponse<Void>> call = apiService.deleteAccount();
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    // Clear all user data from Globals
                    Globals.clearAllUserData();
                    authManager.logout();
                    navigateToSignIn();
                } else {
                    Toast.makeText(AccountSecurityActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(AccountSecurityActivity.this, "Error deleting account", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

