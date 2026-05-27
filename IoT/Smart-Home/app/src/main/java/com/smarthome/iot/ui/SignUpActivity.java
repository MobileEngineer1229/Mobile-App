package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;
import com.smarthome.iot.models.SignupRequest;
import com.smarthome.iot.models.SignupResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.utils.AuthManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private ImageButton buttonTogglePassword;
    private CheckBox checkBoxTerms;
    private MaterialButton buttonSignUp;
    private FrameLayout loadingOverlay;
    private ImageButton buttonBack;
    private TextView textViewSignIn;
    private TextView textViewLoadingMessage;
    
    // Alert message views
    private View emailAlertMessage;
    private View passwordAlertMessage;
    private TextView emailAlertText;
    private TextView passwordAlertText;
    private ImageView emailAlertIcon;
    private ImageView passwordAlertIcon;
    
    // Input containers for border changes
    private android.widget.LinearLayout emailInputContainer;
    private android.widget.LinearLayout passwordInputContainer;

    private ApiService apiService;
    private AuthManager authManager;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Set status bar color to primary (blue) to match Figma
        setStatusBarColor();

        // Initialize API client and AuthManager
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
        setupListeners();
        setupTextWatchers();
    }
    
    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        buttonBack = findViewById(R.id.buttonBack);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonTogglePassword = findViewById(R.id.buttonTogglePassword);
        checkBoxTerms = findViewById(R.id.checkBoxTerms);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        textViewSignIn = findViewById(R.id.textViewSignIn);
        textViewLoadingMessage = loadingOverlay.findViewById(R.id.textViewLoadingMessage);
        
        // Set loading message for sign up
        if (textViewLoadingMessage != null) {
            textViewLoadingMessage.setText(R.string.sign_up_loading);
        }
        
        // Alert message views
        emailAlertMessage = findViewById(R.id.emailAlertMessage);
        passwordAlertMessage = findViewById(R.id.passwordAlertMessage);
        if (emailAlertMessage != null) {
            emailAlertText = emailAlertMessage.findViewById(R.id.textViewAlertMessage);
            emailAlertIcon = emailAlertMessage.findViewById(R.id.imageViewAlertIcon);
        }
        if (passwordAlertMessage != null) {
            passwordAlertText = passwordAlertMessage.findViewById(R.id.textViewAlertMessage);
            passwordAlertIcon = passwordAlertMessage.findViewById(R.id.imageViewAlertIcon);
        }
        
        // Input containers
        emailInputContainer = findViewById(R.id.emailInputContainer);
        passwordInputContainer = findViewById(R.id.passwordInputContainer);
        
        // Set primary color for "Terms & Conditions" in checkbox
        setCheckboxTermsText();
        
        // Set primary color for "Sign in" in TextView
        setSignInTextColor();
    }
    
    private void setCheckboxTermsText() {
        String fullText = getString(R.string.i_agree_terms);
        String highlightText = "Terms & Conditions";
        
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf(highlightText);
        
        if (startIndex != -1) {
            int endIndex = startIndex + highlightText.length();
            int primaryColor = ContextCompat.getColor(this, R.color.primary);
            spannableString.setSpan(
                new ForegroundColorSpan(primaryColor),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            checkBoxTerms.setText(spannableString);
        }
        
        // Set letter spacing (0.2px converted to em units for 18sp font ≈ 0.01)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            checkBoxTerms.setLetterSpacing(0.01f);
        }
    }
    
    private void setSignInTextColor() {
        String fullText = getString(R.string.already_have_account);
        String highlightText = "Sign in";
        
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf(highlightText);
        
        if (startIndex != -1) {
            int endIndex = startIndex + highlightText.length();
            int primaryColor = ContextCompat.getColor(this, R.color.primary);
            spannableString.setSpan(
                new ForegroundColorSpan(primaryColor),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            textViewSignIn.setText(spannableString);
        }
        
        // Set letter spacing (0.2px converted to em units for 18sp font ≈ 0.01)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textViewSignIn.setLetterSpacing(0.01f);
        }
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());

        buttonTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        textViewSignIn.setOnClickListener(v -> {
            finish(); // Go back to SignInActivity
        });

        buttonSignUp.setOnClickListener(v -> {
            if (validateInputs()) {
                performSignup();
            }
        });
    }

    private void setupTextWatchers() {
        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSignUpButtonState();
                // Clear alert when user starts typing
                String email = s.toString().trim();
                if (email.isEmpty()) {
                    hideEmailAlert();
                } else if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showEmailSuccess();
                } else {
                    // Don't show error while typing, only on blur or submit
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSignUpButtonState();
                // Clear alert when user starts typing
                String password = s.toString();
                if (password.isEmpty()) {
                    hidePasswordAlert();
                } else if (password.length() >= 8) {
                    showPasswordSuccess();
                } else {
                    // Don't show error while typing, only on blur or submit
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        checkBoxTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateSignUpButtonState();
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            editTextPassword.setTransformationMethod(new PasswordTransformationMethod());
            buttonTogglePassword.setImageResource(R.drawable.ic_eye_off);
            isPasswordVisible = false;
        } else {
            editTextPassword.setTransformationMethod(null);
            buttonTogglePassword.setImageResource(R.drawable.ic_eye);
            isPasswordVisible = true;
        }
        // Move cursor to end
        editTextPassword.setSelection(editTextPassword.getText().length());
    }

    private void updateSignUpButtonState() {
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null ? editTextPassword.getText().toString() : "";
        boolean termsAccepted = checkBoxTerms.isChecked();

        buttonSignUp.setEnabled(!email.isEmpty() && !password.isEmpty() && termsAccepted);
    }

    private boolean validateInputs() {
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null ? editTextPassword.getText().toString() : "";
        
        boolean isValid = true;
        
        // Validate Email
        if (email.isEmpty()) {
            showEmailError("Please enter your email");
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showEmailError("Please enter a valid email address");
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
            showEmailSuccess();
        }
        
        // Validate Password
        if (password.isEmpty()) {
            showPasswordError("Please enter your password");
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if (password.length() < 8) {
            showPasswordError("Password must be at least 8 characters");
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
            showPasswordSuccess();
        }
        
        // Validate Terms
        if (!checkBoxTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms & Conditions", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }
    
    private void showEmailError(String message) {
        if (emailInputContainer != null) {
            emailInputContainer.setBackgroundResource(R.drawable.sign_in_input_background_error);
        }
        if (emailAlertMessage != null && emailAlertText != null && emailAlertIcon != null) {
            emailAlertMessage.setVisibility(android.view.View.VISIBLE);
            emailAlertText.setText(message);
            emailAlertText.setTextColor(getResources().getColor(R.color.error));
            emailAlertIcon.setImageResource(R.drawable.ic_error);
            emailAlertIcon.setColorFilter(getResources().getColor(R.color.error));
            emailAlertMessage.setBackgroundResource(R.drawable.bg_alert_error);
        }
    }
    
    private void showEmailSuccess() {
        if (emailInputContainer != null) {
            emailInputContainer.setBackgroundResource(R.drawable.sign_in_input_background_success);
        }
        if (emailAlertMessage != null && emailAlertText != null && emailAlertIcon != null) {
            emailAlertMessage.setVisibility(android.view.View.VISIBLE);
            emailAlertText.setText("Email is valid");
            emailAlertText.setTextColor(getResources().getColor(R.color.success));
            emailAlertIcon.setImageResource(R.drawable.ic_check);
            emailAlertIcon.setColorFilter(getResources().getColor(R.color.success));
            emailAlertMessage.setBackgroundResource(R.drawable.bg_alert_success);
        }
    }
    
    private void hideEmailAlert() {
        if (emailInputContainer != null) {
            emailInputContainer.setBackgroundResource(R.drawable.sign_in_input_background);
        }
        if (emailAlertMessage != null) {
            emailAlertMessage.setVisibility(android.view.View.GONE);
        }
    }
    
    private void showPasswordError(String message) {
        if (passwordInputContainer != null) {
            passwordInputContainer.setBackgroundResource(R.drawable.sign_in_input_background_error);
        }
        if (passwordAlertMessage != null && passwordAlertText != null && passwordAlertIcon != null) {
            passwordAlertMessage.setVisibility(android.view.View.VISIBLE);
            passwordAlertText.setText(message);
            passwordAlertText.setTextColor(getResources().getColor(R.color.error));
            passwordAlertIcon.setImageResource(R.drawable.ic_error);
            passwordAlertIcon.setColorFilter(getResources().getColor(R.color.error));
            passwordAlertMessage.setBackgroundResource(R.drawable.bg_alert_error);
        }
    }
    
    private void showPasswordSuccess() {
        if (passwordInputContainer != null) {
            passwordInputContainer.setBackgroundResource(R.drawable.sign_in_input_background_success);
        }
        if (passwordAlertMessage != null && passwordAlertText != null && passwordAlertIcon != null) {
            passwordAlertMessage.setVisibility(android.view.View.VISIBLE);
            passwordAlertText.setText("Password is valid");
            passwordAlertText.setTextColor(getResources().getColor(R.color.success));
            passwordAlertIcon.setImageResource(R.drawable.ic_check);
            passwordAlertIcon.setColorFilter(getResources().getColor(R.color.success));
            passwordAlertMessage.setBackgroundResource(R.drawable.bg_alert_success);
        }
    }
    
    private void hidePasswordAlert() {
        if (passwordInputContainer != null) {
            passwordInputContainer.setBackgroundResource(R.drawable.sign_in_input_background);
        }
        if (passwordAlertMessage != null) {
            passwordAlertMessage.setVisibility(android.view.View.GONE);
        }
    }

    private void performSignup() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        // Extract first and last name from email
        // In a real app, you might want to add name fields to the UI
        String[] emailParts = email.split("@");
        String firstName = emailParts[0].split("\\.")[0];
        String lastName = emailParts.length > 0 && emailParts[0].contains(".") 
            ? emailParts[0].substring(emailParts[0].indexOf(".") + 1) 
            : "User";

        // Capitalize first letter
        if (firstName.length() > 0) {
            firstName = firstName.substring(0, 1).toUpperCase() + 
                       (firstName.length() > 1 ? firstName.substring(1) : "");
        }
        if (lastName.length() > 0 && !lastName.equals(firstName)) {
            lastName = lastName.substring(0, 1).toUpperCase() + 
                      (lastName.length() > 1 ? lastName.substring(1) : "");
        } else {
            lastName = "User";
        }

        showLoading(true);
        buttonSignUp.setEnabled(false);

        SignupRequest request = new SignupRequest(email, password, firstName, lastName);

        Call<SignupResponse> call = apiService.signup(request);
        call.enqueue(new Callback<SignupResponse>() {
            @Override
            public void onResponse(Call<SignupResponse> call, Response<SignupResponse> response) {
                showLoading(false);
                buttonSignUp.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    SignupResponse signupResponse = response.body();
                    if (signupResponse.isSuccess() && signupResponse.getData() != null) {
                        SignupResponse.SignupData data = signupResponse.getData();
                        
                        // Save token if provided (backend may return token in signup response)
                        if (data.getToken() != null && !data.getToken().isEmpty()) {
                            authManager.saveToken(data.getToken());
                        }
                        
                        // Save user info
                        if (data.getUser() != null) {
                            authManager.saveUserInfo(data.getUser().getId(), data.getUser().getEmail());
                            authManager.saveUserName(data.getUser().getFullName());
                        }
                        
                        // Signup successful - navigate to onboarding
                        Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to onboarding step 1
                        Intent intent = new Intent(SignUpActivity.this, OnboardingStep1Activity.class);
                        if (data.getUser() != null) {
                            intent.putExtra("userId", data.getUser().getId());
                        }
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = signupResponse.getError() != null 
                            ? signupResponse.getError().getMessage() 
                            : "Signup failed";
                        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Try to parse as ApiResponse<User> if SignupResponse fails (backward compatibility)
                    String errorMessage = "Signup failed. Please try again.";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("SignUp", "Error response: " + errorBody);
                            // Try to extract error message from JSON
                            if (errorBody.contains("\"message\"")) {
                                int messageIndex = errorBody.indexOf("\"message\"");
                                if (messageIndex != -1) {
                                    int start = errorBody.indexOf("\"", messageIndex + 10) + 1;
                                    int end = errorBody.indexOf("\"", start);
                                    if (start > 0 && end > start) {
                                        errorMessage = errorBody.substring(start, end);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("SignUp", "Error parsing error response", e);
                        }
                    }
                    Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SignupResponse> call, Throwable t) {
                showLoading(false);
                buttonSignUp.setEnabled(true);
                
                String errorMessage;
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("Failed to connect") || t.getMessage().contains("failed to connect")) {
                        errorMessage = "Cannot connect to server.\n\nPlease ensure:\n1. Backend server is running\n2. Server is accessible at http://172.86.88.76:3003";
                    } else if (t.getMessage().contains("timeout") || t.getMessage().contains("Timeout")) {
                        errorMessage = "Connection timeout. Please check your network connection.";
                    } else {
                        errorMessage = "Network error: " + t.getMessage();
                    }
                } else {
                    errorMessage = "Network error occurred. Please try again.";
                }
                
                Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                android.util.Log.e("SignUp", "Signup error", t);
            }
        });
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
