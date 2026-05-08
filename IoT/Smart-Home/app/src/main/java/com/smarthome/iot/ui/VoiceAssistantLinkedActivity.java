package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.VoiceAssistant;
import com.smarthome.iot.utils.ThemeHelper;

public class VoiceAssistantLinkedActivity extends AppCompatActivity {
    private ImageView imageViewSmartify;
    private ImageView imageViewAssistant;
    private TextView textViewMessage;
    private MaterialButton buttonOk;
    
    private String assistantName;
    private int assistantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant_linked);

        assistantName = getIntent().getStringExtra("assistantName");
        assistantId = getIntent().getIntExtra("assistantId", 1);
        if (assistantName == null) {
            assistantName = "Google Assistant";
        }

        initializeViews();
        setupClickListeners();
        updateUI();
    }

    private void initializeViews() {
        ImageButton buttonClose = findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(v -> finish());

        imageViewSmartify = findViewById(R.id.imageViewSmartify);
        imageViewAssistant = findViewById(R.id.imageViewAssistant);
        textViewMessage = findViewById(R.id.textViewMessage);
        buttonOk = findViewById(R.id.buttonOk);
    }

    private void setupClickListeners() {
        buttonOk.setOnClickListener(v -> {
            // Navigate back to voice assistants list
            Intent intent = new Intent(this, VoiceAssistantsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void updateUI() {
        // Update assistant icon
        VoiceAssistant assistant = new VoiceAssistant(assistantId, assistantName, true);
        imageViewAssistant.setImageResource(assistant.getIconResId());
        
        // Update message
        String message = "Your Smartify account is now seamlessly linked with " + assistantName + ". Get ready to experience the ultimate hands-free control.";
        textViewMessage.setText(message);
    }
}

