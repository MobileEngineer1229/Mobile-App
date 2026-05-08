package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;

public class OnboardingStep2Activity extends AppCompatActivity {

    private ImageButton buttonBack;
    private TextView textViewProgress;
    private TextView textViewTitle;
    private TextInputEditText editTextHomeName;
    private MaterialButton buttonSkip;
    private MaterialButton buttonContinue;

    private int userId;
    private String email;
    private String country;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_step2);

        // Set status bar color
        setStatusBarColor();

        // Get data from previous activity
        userId = getIntent().getIntExtra("userId", -1);
        email = getIntent().getStringExtra("email");
        country = getIntent().getStringExtra("country");

        initializeViews();
        setupListeners();
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        buttonBack = findViewById(R.id.buttonBack);
        textViewProgress = findViewById(R.id.textViewProgress);
        textViewTitle = findViewById(R.id.textViewTitle);
        editTextHomeName = findViewById(R.id.editTextHomeName);
        buttonSkip = findViewById(R.id.buttonSkip);
        buttonContinue = findViewById(R.id.buttonContinue);

        textViewProgress.setText("2/4");
        editTextHomeName.setText(getString(R.string.my_home));
        
        // Setup title text with "Home" highlighted in primary color
        setupTitleText();

        editTextHomeName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateContinueButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Initially disable continue button
        updateContinueButton();
    }

    private void setupTitleText() {
        String title = getString(R.string.add_home_name);
        SpannableString spannableString = new SpannableString(title);
        
        // Find "Home" in the title and highlight it with primary color
        int homeIndex = title.indexOf("Home");
        if (homeIndex != -1) {
            int primaryColor = ContextCompat.getColor(this, R.color.primary);
            spannableString.setSpan(
                new ForegroundColorSpan(primaryColor),
                homeIndex,
                homeIndex + "Home".length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        textViewTitle.setText(spannableString);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());

        buttonSkip.setOnClickListener(v -> {
            navigateToNextStep(null);
        });

        buttonContinue.setOnClickListener(v -> {
            String homeName = editTextHomeName.getText() != null 
                ? editTextHomeName.getText().toString().trim() 
                : getString(R.string.my_home);
            navigateToNextStep(homeName);
        });
    }

    private void updateContinueButton() {
        String homeName = editTextHomeName.getText() != null 
            ? editTextHomeName.getText().toString().trim() 
            : "";
        buttonContinue.setEnabled(!homeName.isEmpty());
    }

    private void navigateToNextStep(String homeName) {
        Intent intent = new Intent(this, OnboardingStep3Activity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("email", email);
        if (country != null) {
            intent.putExtra("country", country);
        }
        if (homeName != null) {
            intent.putExtra("homeName", homeName);
        }
        startActivity(intent);
    }
}

