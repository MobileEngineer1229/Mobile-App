package com.smarthome.iot.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.LinkedAccount;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.LinkedAccountAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinkedAccountsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewLinkedAccounts;
    private LinkedAccountAdapter adapter;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linked_accounts);

        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
        setupRecyclerView();
        loadLinkedAccounts();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerViewLinkedAccounts = findViewById(R.id.recyclerViewLinkedAccounts);
    }

    private void setupRecyclerView() {
        recyclerViewLinkedAccounts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LinkedAccountAdapter(new ArrayList<>(), new LinkedAccountAdapter.OnLinkedAccountClickListener() {
            @Override
            public void onConnectClick(LinkedAccount account) {
                connectAccount(account);
            }

            @Override
            public void onDisconnectClick(LinkedAccount account) {
                disconnectAccount(account);
            }
        });
        recyclerViewLinkedAccounts.setAdapter(adapter);
    }

    private void connectAccount(LinkedAccount account) {
        if (!authManager.isLoggedIn()) {
            // Demo mode
            account.setConnected(true);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, account.getProvider() + " connected", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ApiResponse<LinkedAccount>> call = apiService.linkAccount(account.getProvider());
        call.enqueue(new Callback<ApiResponse<LinkedAccount>>() {
            @Override
            public void onResponse(Call<ApiResponse<LinkedAccount>> call, Response<ApiResponse<LinkedAccount>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    account.setConnected(true);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(LinkedAccountsActivity.this, account.getProvider() + " connected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LinkedAccountsActivity.this, "Failed to connect " + account.getProvider(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LinkedAccount>> call, Throwable t) {
                Toast.makeText(LinkedAccountsActivity.this, "Network error connecting " + account.getProvider(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void disconnectAccount(LinkedAccount account) {
        if (!authManager.isLoggedIn()) {
            account.setConnected(false);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, account.getProvider() + " disconnected", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ApiResponse<Void>> call = apiService.unlinkAccount(account.getId());
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    account.setConnected(false);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(LinkedAccountsActivity.this, account.getProvider() + " disconnected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LinkedAccountsActivity.this, "Failed to disconnect " + account.getProvider(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(LinkedAccountsActivity.this, "Network error disconnecting " + account.getProvider(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLinkedAccounts() {
        if (!authManager.isLoggedIn()) {
            loadDemoLinkedAccounts();
            return;
        }

        Call<ApiResponse<List<LinkedAccount>>> call = apiService.getLinkedAccounts();
        call.enqueue(new Callback<ApiResponse<List<LinkedAccount>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LinkedAccount>>> call, Response<ApiResponse<List<LinkedAccount>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LinkedAccount> accounts = response.body().getData();
                    if (accounts != null && !accounts.isEmpty()) {
                        adapter = new LinkedAccountAdapter(accounts, new LinkedAccountAdapter.OnLinkedAccountClickListener() {
                            @Override
                            public void onConnectClick(LinkedAccount account) {
                                connectAccount(account);
                            }

                            @Override
                            public void onDisconnectClick(LinkedAccount account) {
                                disconnectAccount(account);
                            }
                        });
                        recyclerViewLinkedAccounts.setAdapter(adapter);
                    }
                } else {
                    android.util.Log.w("LinkedAccounts", "Failed to load linked accounts from API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LinkedAccount>>> call, Throwable t) {
                android.util.Log.e("LinkedAccounts", "Network error loading linked accounts", t);
            }
        });
    }

    private void loadDemoLinkedAccounts() {
        List<LinkedAccount> accounts = new ArrayList<>();
        accounts.add(new LinkedAccount(1, "Google", true));
        accounts.add(new LinkedAccount(2, "Apple", true));
        accounts.add(new LinkedAccount(3, "Facebook", false));
        accounts.add(new LinkedAccount(4, "Twitter", false));

        adapter = new LinkedAccountAdapter(accounts, new LinkedAccountAdapter.OnLinkedAccountClickListener() {
            @Override
            public void onConnectClick(LinkedAccount account) {
                connectAccount(account);
            }

            @Override
            public void onDisconnectClick(LinkedAccount account) {
                disconnectAccount(account);
            }
        });
        recyclerViewLinkedAccounts.setAdapter(adapter);
    }
}

