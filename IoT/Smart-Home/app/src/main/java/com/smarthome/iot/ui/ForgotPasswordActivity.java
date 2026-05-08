package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.ForgotPasswordRequest;
import com.smarthome.iot.models.ForgotPasswordResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.utils.ThemeHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail;
    private MaterialButton buttonSendOTP;
    private ImageButton buttonBack;
    private ProgressBar progressBar;

    private ApiService apiService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Set status bar color to dark_1
        setStatusBarColor();

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        initViews();
        setupClickListeners();
        setupTextWatchers();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        buttonSendOTP = findViewById(R.id.buttonSendOTP);
        buttonBack = findViewById(R.id.buttonBack);
        progressBar = findViewById(R.id.progressBar);

        // Pre-fill email if passed from SignInActivity
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            editTextEmail.setText(email);
        }
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> onBackPressed());

        buttonSendOTP.setOnClickListener(v -> {
            String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
            if (validateEmail(email)) {
                sendOTP(email);
            }
        });
    }

    private void setupTextWatchers() {
        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateButtonState() {
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        buttonSendOTP.setEnabled(!email.isEmpty() && EMAIL_PATTERN.matcher(email).matches());
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            editTextEmail.setError("Invalid email format");
            return false;
        }

        return true;
    }

    private void sendOTP(String email) {
        showProgress(true);
        buttonSendOTP.setEnabled(false);

        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        Call<ApiResponse<ForgotPasswordResponse>> call = apiService.forgotPassword(request);
        call.enqueue(new Callback<ApiResponse<ForgotPasswordResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ForgotPasswordResponse>> call, Response<ApiResponse<ForgotPasswordResponse>> response) {
                showProgress(false);
                buttonSendOTP.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // OTP sent successfully
                    Toast.makeText(ForgotPasswordActivity.this, "OTP code sent to your email", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to OTP screen
                    Intent intent = new Intent(ForgotPasswordActivity.this, EnterOTPActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("expiresInMinutes", 15);
                    startActivity(intent);
                } else {
                    String errorMsg = "Failed to send OTP code";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMsg = response.body().getError().getMessage();
                    }
                    Toast.makeText(ForgotPasswordActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ForgotPasswordResponse>> call, Throwable t) {
                showProgress(false);
                buttonSendOTP.setEnabled(true);
                android.util.Log.e("ForgotPassword", "Error sending OTP", t);
                Toast.makeText(ForgotPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonSendOTP.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}

