package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.VerifyOTPRequest;
import com.smarthome.iot.models.VerifyOTPResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.utils.ThemeHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EnterOTPActivity extends AppCompatActivity {

    private TextInputEditText[] otpInputs = new TextInputEditText[4];
    private ImageButton buttonBack;
    private TextView textViewResendTimer;
    private TextView textViewResendCode;
    private ProgressBar progressBar;
    
    // Keypad views
    private TextView[] keypadNumbers = new TextView[10]; // 0-9
    private TextView keypadAsterisk;
    private TextView keypadBackspace;

    private String email;
    private int expiresInMinutes = 15;
    private CountDownTimer resendTimer;
    private static final long RESEND_COOLDOWN_MS = 60000; // 60 seconds
    private int currentFocusIndex = 0;
    
    private ApiService apiService;
    private boolean isVerifying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_otp);

        // Set status bar color to dark_1
        setStatusBarColor();

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        email = getIntent().getStringExtra("email");
        expiresInMinutes = getIntent().getIntExtra("expiresInMinutes", 15);

        initViews();
        setupClickListeners();
        setupOTPInputs();
        setupKeypad();
        startResendTimer();
        
        // Focus first input
        otpInputs[0].requestFocus();
        updateInputBorder(0);
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initViews() {
        otpInputs[0] = findViewById(R.id.editTextOTP1);
        otpInputs[1] = findViewById(R.id.editTextOTP2);
        otpInputs[2] = findViewById(R.id.editTextOTP3);
        otpInputs[3] = findViewById(R.id.editTextOTP4);
        buttonBack = findViewById(R.id.buttonBack);
        textViewResendTimer = findViewById(R.id.textViewResendTimer);
        textViewResendCode = findViewById(R.id.textViewResendCode);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> onBackPressed());

        textViewResendCode.setOnClickListener(v -> {
            if (textViewResendCode.isEnabled()) {
                resendOTP();
            }
        });
    }

    private void setupOTPInputs() {
        for (int i = 0; i < otpInputs.length; i++) {
            final int index = i;
            
            // Focus change listener
            otpInputs[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    currentFocusIndex = index;
                    updateInputBorder(index);
                }
            });
            
            // Text change listener
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        // Update border for filled input
                        updateInputBorder(index);
                        
                        // Move to next input if not last
                        if (index < otpInputs.length - 1) {
                            otpInputs[index + 1].requestFocus();
                            currentFocusIndex = index + 1;
                        } else {
                            // All inputs filled, navigate forward
                            checkAndNavigate();
                        }
                    } else if (s.length() == 0) {
                        // Clear border when empty
                        updateInputBorder(index);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupKeypad() {
        // Initialize number keypads (0-9)
        keypadNumbers[1] = findViewById(R.id.keypad1);
        keypadNumbers[2] = findViewById(R.id.keypad2);
        keypadNumbers[3] = findViewById(R.id.keypad3);
        keypadNumbers[4] = findViewById(R.id.keypad4);
        keypadNumbers[5] = findViewById(R.id.keypad5);
        keypadNumbers[6] = findViewById(R.id.keypad6);
        keypadNumbers[7] = findViewById(R.id.keypad7);
        keypadNumbers[8] = findViewById(R.id.keypad8);
        keypadNumbers[9] = findViewById(R.id.keypad9);
        keypadNumbers[0] = findViewById(R.id.keypad0);
        
        keypadAsterisk = findViewById(R.id.keypadAsterisk);
        keypadBackspace = findViewById(R.id.keypadBackspace);

        // Set click listeners for number keys
        for (int i = 0; i <= 9; i++) {
            final int number = i;
            keypadNumbers[i].setOnClickListener(v -> {
                if (currentFocusIndex < otpInputs.length) {
                    otpInputs[currentFocusIndex].setText(String.valueOf(number));
                    // Focus will move automatically via TextWatcher
                }
            });
        }

        // Backspace
        keypadBackspace.setOnClickListener(v -> {
            if (currentFocusIndex < otpInputs.length) {
                TextInputEditText currentInput = otpInputs[currentFocusIndex];
                if (currentInput.getText() != null && currentInput.getText().length() > 0) {
                    currentInput.setText("");
                } else if (currentFocusIndex > 0) {
                    // Move to previous input and clear it
                    currentFocusIndex--;
                    otpInputs[currentFocusIndex].requestFocus();
                    otpInputs[currentFocusIndex].setText("");
                }
            }
        });

        // Asterisk - no action needed for now
        keypadAsterisk.setOnClickListener(v -> {
            // No action for asterisk
        });
    }

    private void updateInputBorder(int index) {
        for (int i = 0; i < otpInputs.length; i++) {
            TextInputEditText input = otpInputs[i];
            boolean isFocused = input.hasFocus();
            
            // Show border only if focused (not when filled)
            if (isFocused && i == index) {
                input.setBackgroundResource(R.drawable.otp_input_background_active);
            } else {
                input.setBackgroundResource(R.drawable.otp_input_background);
            }
        }
    }

    private String getOTPCode() {
        StringBuilder code = new StringBuilder();
        for (TextInputEditText input : otpInputs) {
            String text = input.getText() != null ? input.getText().toString() : "";
            code.append(text);
        }
        return code.toString();
    }

    private void checkAndNavigate() {
        String otpCode = getOTPCode();
        if (otpCode.length() == 4 && !isVerifying) {
            verifyOTP(otpCode);
        }
    }

    private void verifyOTP(String otpCode) {
        isVerifying = true;
        showProgress(true);
        
        // Disable all inputs
        for (TextInputEditText input : otpInputs) {
            input.setEnabled(false);
        }

        VerifyOTPRequest request = new VerifyOTPRequest(email, otpCode);

        Call<ApiResponse<VerifyOTPResponse>> call = apiService.verifyOTP(request);
        call.enqueue(new Callback<ApiResponse<VerifyOTPResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<VerifyOTPResponse>> call, Response<ApiResponse<VerifyOTPResponse>> response) {
                isVerifying = false;
                showProgress(false);
                
                // Re-enable inputs
                for (TextInputEditText input : otpInputs) {
                    input.setEnabled(true);
                }

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // OTP verified successfully
                    navigateToNextScreen(otpCode);
                } else {
                    String errorMsg = "Invalid or expired OTP code";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMsg = response.body().getError().getMessage();
                    }
                    android.widget.Toast.makeText(EnterOTPActivity.this, errorMsg, android.widget.Toast.LENGTH_SHORT).show();
                    
                    // Clear OTP inputs
                    for (TextInputEditText input : otpInputs) {
                        input.setText("");
                    }
                    otpInputs[0].requestFocus();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VerifyOTPResponse>> call, Throwable t) {
                isVerifying = false;
                showProgress(false);
                
                // Re-enable inputs
                for (TextInputEditText input : otpInputs) {
                    input.setEnabled(true);
                }
                
                android.util.Log.e("EnterOTP", "Error verifying OTP", t);
                android.widget.Toast.makeText(EnterOTPActivity.this, "Network error: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void navigateToNextScreen(String otpCode) {
        try {
            Intent intent = new Intent(EnterOTPActivity.this, CreateNewPasswordActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("otpCode", otpCode);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("EnterOTPActivity", "Error navigating to next screen", e);
            finish();
        }
    }

    private void resendOTP() {
        // Navigate back to forgot password to resend
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }

    private void startResendTimer() {
        resendTimer = new CountDownTimer(RESEND_COOLDOWN_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                String text = "You can resend the code in " + seconds + " seconds";
                SpannableString spannableString = new SpannableString(text);
                
                // Find the position of the number
                int numberStart = text.indexOf(String.valueOf(seconds));
                int numberEnd = numberStart + String.valueOf(seconds).length();
                
                // Apply blue color to the number
                int primaryColor = getResources().getColor(R.color.primary);
                spannableString.setSpan(new ForegroundColorSpan(primaryColor), 
                    numberStart, numberEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                textViewResendTimer.setText(spannableString);
                textViewResendCode.setEnabled(false);
            }

            @Override
            public void onFinish() {
                textViewResendTimer.setText("");
                textViewResendCode.setEnabled(true);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
