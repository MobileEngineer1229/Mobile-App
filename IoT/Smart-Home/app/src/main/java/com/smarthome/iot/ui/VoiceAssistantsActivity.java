package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.VoiceAssistant;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.VoiceAssistantAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceAssistantsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewVoiceAssistants;
    private VoiceAssistantAdapter adapter;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistants);

        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
        setupRecyclerView();
        loadVoiceAssistants();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerViewVoiceAssistants = findViewById(R.id.recyclerViewVoiceAssistants);
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerViewVoiceAssistants.setLayoutManager(layoutManager);
        
        adapter = new VoiceAssistantAdapter(new ArrayList<>(), this::onVoiceAssistantClick);
        recyclerViewVoiceAssistants.setAdapter(adapter);
    }

    private void onVoiceAssistantClick(VoiceAssistant assistant) {
        if (assistant.isLinked()) {
            // TODO: Show options to unlink or manage
            Toast.makeText(this, assistant.getName() + " is already linked", Toast.LENGTH_SHORT).show();
        } else {
            // Navigate to linking screen
            Intent intent = new Intent(this, LinkVoiceAssistantActivity.class);
            intent.putExtra("assistantId", assistant.getId());
            intent.putExtra("assistantName", assistant.getName());
            startActivity(intent);
        }
    }

    private void loadVoiceAssistants() {
        Call<ApiResponse<List<VoiceAssistant>>> call = apiService.getVoiceAssistants();
        call.enqueue(new Callback<ApiResponse<List<VoiceAssistant>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<VoiceAssistant>>> call, Response<ApiResponse<List<VoiceAssistant>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<VoiceAssistant> assistants = response.body().getData();
                    if (assistants != null && !assistants.isEmpty()) {
                        adapter = new VoiceAssistantAdapter(assistants, VoiceAssistantsActivity.this::onVoiceAssistantClick);
                        recyclerViewVoiceAssistants.setAdapter(adapter);
                    }
                } else {
                    android.util.Log.w("VoiceAssistants", "Failed to load voice assistants from API");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<VoiceAssistant>>> call, Throwable t) {
                android.util.Log.e("VoiceAssistants", "Network error loading voice assistants", t);
            }
        });
    }

    private void loadDemoVoiceAssistants() {
        List<VoiceAssistant> assistants = new ArrayList<>();
        assistants.add(new VoiceAssistant(1, "Google Assistant", false));
        assistants.add(new VoiceAssistant(2, "Amazon Alexa", true));
        assistants.add(new VoiceAssistant(3, "Microsoft Cortana", true));
        assistants.add(new VoiceAssistant(4, "Samsung Bixby", false));
        assistants.add(new VoiceAssistant(5, "Naver Clova", false));
        assistants.add(new VoiceAssistant(6, "Apple Siri", false));
        
        adapter = new VoiceAssistantAdapter(assistants, this::onVoiceAssistantClick);
        recyclerViewVoiceAssistants.setAdapter(adapter);
    }
}

