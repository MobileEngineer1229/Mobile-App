package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
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
import com.smarthome.iot.models.ResetPasswordRequest;
import com.smarthome.iot.models.ResetPasswordResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.utils.ThemeHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateNewPasswordActivity extends AppCompatActivity {

    private TextInputEditText editTextNewPassword;
    private TextInputEditText editTextConfirmPassword;
    private MaterialButton buttonSavePassword;
    private ImageButton buttonBack;
    private ImageButton buttonToggleNewPassword;
    private ImageButton buttonToggleConfirmPassword;
    private ProgressBar progressBar;

    private String email;
    private String otpCode;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    
    private ApiService apiService;
    private boolean isResetting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_password);

        // Set status bar color to dark_1
        setStatusBarColor();

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        email = getIntent().getStringExtra("email");
        otpCode = getIntent().getStringExtra("otpCode");

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
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSavePassword = findViewById(R.id.buttonSavePassword);
        buttonBack = findViewById(R.id.buttonBack);
        buttonToggleNewPassword = findViewById(R.id.buttonToggleNewPassword);
        buttonToggleConfirmPassword = findViewById(R.id.buttonToggleConfirmPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> onBackPressed());

        buttonToggleNewPassword.setOnClickListener(v -> togglePasswordVisibility(editTextNewPassword, buttonToggleNewPassword, true));
        buttonToggleConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(editTextConfirmPassword, buttonToggleConfirmPassword, false));

        buttonSavePassword.setOnClickListener(v -> {
            String newPassword = editTextNewPassword.getText() != null ? editTextNewPassword.getText().toString() : "";
            String confirmPassword = editTextConfirmPassword.getText() != null ? editTextConfirmPassword.getText().toString() : "";
            
            if (validatePasswords(newPassword, confirmPassword) && !isResetting) {
                resetPassword(newPassword);
            }
        });
    }

    private void setupTextWatchers() {
        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editTextNewPassword.addTextChangedListener(passwordWatcher);
        editTextConfirmPassword.addTextChangedListener(passwordWatcher);
    }

    private void togglePasswordVisibility(TextInputEditText editText, ImageButton button, boolean isNew) {
        if (isNew) {
            isNewPasswordVisible = !isNewPasswordVisible;
            editText.setInputType(isNewPasswordVisible 
                ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD 
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            button.setImageResource(isNewPasswordVisible ? R.drawable.ic_eye : R.drawable.ic_eye_off);
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            editText.setInputType(isConfirmPasswordVisible 
                ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD 
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            button.setImageResource(isConfirmPasswordVisible ? R.drawable.ic_eye : R.drawable.ic_eye_off);
        }
        editText.setSelection(editText.getText() != null ? editText.getText().length() : 0);
    }

    private void updateButtonState() {
        String newPassword = editTextNewPassword.getText() != null ? editTextNewPassword.getText().toString() : "";
        String confirmPassword = editTextConfirmPassword.getText() != null ? editTextConfirmPassword.getText().toString() : "";
        buttonSavePassword.setEnabled(!newPassword.isEmpty() && !confirmPassword.isEmpty() && newPassword.length() >= 6);
    }

    private boolean validatePasswords(String newPassword, String confirmPassword) {
        if (newPassword.isEmpty()) {
            editTextNewPassword.setError("Password is required");
            return false;
        }

        if (newPassword.length() < 6) {
            editTextNewPassword.setError("Password must be at least 6 characters");
            return false;
        }

        if (confirmPassword.isEmpty()) {
            editTextConfirmPassword.setError("Please confirm your password");
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void resetPassword(String newPassword) {
        isResetting = true;
        showProgress(true);
        buttonSavePassword.setEnabled(false);
        buttonBack.setEnabled(false);

        ResetPasswordRequest request = new ResetPasswordRequest(email, otpCode, newPassword);

        Call<ApiResponse<ResetPasswordResponse>> call = apiService.resetPassword(request);
        call.enqueue(new Callback<ApiResponse<ResetPasswordResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ResetPasswordResponse>> call, Response<ApiResponse<ResetPasswordResponse>> response) {
                isResetting = false;
                showProgress(false);
                buttonSavePassword.setEnabled(true);
                buttonBack.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Password reset successfully
                    Toast.makeText(CreateNewPasswordActivity.this, "Password reset successfully", Toast.LENGTH_SHORT).show();
                    navigateToSuccess();
                } else {
                    String errorMsg = "Failed to reset password";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMsg = response.body().getError().getMessage();
                    }
                    Toast.makeText(CreateNewPasswordActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ResetPasswordResponse>> call, Throwable t) {
                isResetting = false;
                showProgress(false);
                buttonSavePassword.setEnabled(true);
                buttonBack.setEnabled(true);
                android.util.Log.e("CreateNewPassword", "Error resetting password", t);
                Toast.makeText(CreateNewPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void navigateToSuccess() {
        Intent intent = new Intent(CreateNewPasswordActivity.this, PasswordResetSuccessActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
