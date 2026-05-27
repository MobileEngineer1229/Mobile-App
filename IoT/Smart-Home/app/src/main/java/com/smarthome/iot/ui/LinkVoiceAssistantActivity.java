package com.smarthome.iot.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.VoiceAssistant;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LinkVoiceAssistantActivity extends AppCompatActivity {
    private ImageView imageViewIcon;
    private TextView textViewTitle;
    private TextView textViewDescription;
    private MaterialButton buttonLink;
    private Dialog linkingDialog;
    
    private int assistantId;
    private String assistantName;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_voice_assistant);

        assistantId = getIntent().getIntExtra("assistantId", 1);
        assistantName = getIntent().getStringExtra("assistantName");
        if (assistantName == null) {
            assistantName = "Google Assistant";
        }

        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
        setupClickListeners();
        updateUI();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        imageViewIcon = findViewById(R.id.imageViewIcon);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewDescription = findViewById(R.id.textViewDescription);
        buttonLink = findViewById(R.id.buttonLink);
    }

    private void setupClickListeners() {
        buttonLink.setOnClickListener(v -> {
            showLinkingDialog();
            linkVoiceAssistant();
        });
    }

    private void updateUI() {
        textViewTitle.setText(assistantName);
        buttonLink.setText("Link to " + assistantName);
        
        // Update icon based on assistant name
        VoiceAssistant assistant = new VoiceAssistant(assistantId, assistantName, false);
        imageViewIcon.setImageResource(assistant.getIconResId());
    }

    private void showLinkingDialog() {
        linkingDialog = new Dialog(this);
        linkingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        linkingDialog.setContentView(R.layout.dialog_linking);
        linkingDialog.setCancelable(false);
        
        TextView textViewMessage = linkingDialog.findViewById(R.id.textViewMessage);
        if (textViewMessage != null) {
            textViewMessage.setText("Linking your Smartify account to " + assistantName + "…");
        }
        
        linkingDialog.show();
    }

    private void linkVoiceAssistant() {
        if (!authManager.isLoggedIn()) {
            // Demo mode - simulate linking
            new Handler().postDelayed(() -> {
                if (linkingDialog != null && linkingDialog.isShowing()) {
                    linkingDialog.dismiss();
                }
                navigateToSuccess();
            }, 2000);
            return;
        }

        Call<ApiResponse<VoiceAssistant>> call = apiService.linkVoiceAssistant(assistantId);
        call.enqueue(new Callback<ApiResponse<VoiceAssistant>>() {
            @Override
            public void onResponse(Call<ApiResponse<VoiceAssistant>> call, Response<ApiResponse<VoiceAssistant>> response) {
                if (linkingDialog != null && linkingDialog.isShowing()) {
                    linkingDialog.dismiss();
                }
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    navigateToSuccess();
                } else {
                    // Handle error
                    navigateToSuccess(); // For demo purposes
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VoiceAssistant>> call, Throwable t) {
                if (linkingDialog != null && linkingDialog.isShowing()) {
                    linkingDialog.dismiss();
                }
                // For demo purposes, still navigate to success
                navigateToSuccess();
            }
        });
    }

    private void navigateToSuccess() {
        Intent intent = new Intent(this, VoiceAssistantLinkedActivity.class);
        intent.putExtra("assistantName", assistantName);
        intent.putExtra("assistantId", assistantId);
        startActivity(intent);
        finish();
    }
}

