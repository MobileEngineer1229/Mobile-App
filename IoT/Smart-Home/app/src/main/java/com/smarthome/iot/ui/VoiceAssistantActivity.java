package com.smarthome.iot.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.smarthome.iot.R;
import com.smarthome.iot.utils.ThemeHelper;

public class VoiceAssistantActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant);

        initializeViews();
    }

    private void initializeViews() {
        ImageButton buttonClose = findViewById(R.id.buttonClose);
        TextView textViewCommand = findViewById(R.id.textViewCommand);

        buttonClose.setOnClickListener(v -> finish());

        // Get command from intent if available
        String command = getIntent().getStringExtra("command");
        if (command != null && !command.isEmpty()) {
            textViewCommand.setText("\"" + command + "\"");
        }
    }
}

