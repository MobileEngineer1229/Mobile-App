package com.smarthome.iot.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.smarthome.iot.R;
import com.smarthome.iot.utils.ThemeHelper;

public class PrivacyPolicyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        TextView textViewLastUpdated = findViewById(R.id.textViewLastUpdated);
        textViewLastUpdated.setText("Last Updated: December 19, 2024");

        TextView textViewContent = findViewById(R.id.textViewContent);
        textViewContent.setText(getString(R.string.privacy_policy_content));
    }
}

