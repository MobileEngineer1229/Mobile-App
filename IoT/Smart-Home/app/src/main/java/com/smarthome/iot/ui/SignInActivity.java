package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;
import com.smarthome.iot.models.LoginRequest;
import com.smarthome.iot.models.LoginResponse;
import com.smarthome.iot.models.User;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignInActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    
    private MaterialButton buttonSignIn;
    private ImageButton buttonBack;
    private ImageButton buttonTogglePassword;
    private FrameLayout loadingOverlay;
    private TextView textViewForgotPassword;
    private TextView textViewSignUp;
    private CheckBox checkBoxRememberMe;
    private boolean isPasswordVisible = false;
    private AuthManager authManager;
    private ApiService apiService;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Set status bar color to dark
        setStatusBarColor();

        // Initialize AuthManager
        authManager = new AuthManager(this);
        
        // Initialize API client
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();

        initializeViews();
        prefillTestCredentials();
        setupTextWatchers();
        setupSignInButton();
        setupClickListeners();
    }

    private void initializeViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        
        buttonSignIn = findViewById(R.id.buttonSignIn);
        buttonBack = findViewById(R.id.buttonBack);
        buttonTogglePassword = findViewById(R.id.buttonTogglePassword);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        textViewSignUp = findViewById(R.id.textViewSignUp);
        checkBoxRememberMe = findViewById(R.id.checkBoxRememberMe);
        
        // Set loading message for sign in (default is already set in component)
        // The component already has sign_in_loading as default, so no need to change
        
        // Set primary color for "Sign up" in TextView
        if (textViewSignUp != null) {
            setSignUpTextColor();
        }
    }
    
    private void setSignUpTextColor() {
        String fullText = getString(R.string.dont_have_account);
        String highlightText = "Sign up";
        
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
            textViewSignUp.setText(spannableString);
        }
        
        // Set letter spacing (0.2px converted to em units for 18sp font ≈ 0.01)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textViewSignUp.setLetterSpacing(0.01f);
        }
    }

    private void prefillTestCredentials() {
        if (MockDataProvider.isMockupMode()) {
            editTextEmail.setText("demo@smartify.com");
            editTextPassword.setText("demo123456");
            checkBoxRememberMe.setChecked(true);
            return;
        }

        // Prefill with saved email if "Remember Me" was checked previously
        String savedEmail = authManager.getSavedEmail();
        if (savedEmail != null && !savedEmail.isEmpty()) {
            editTextEmail.setText(savedEmail);
        }
    }

    private void setupTextWatchers() {
        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePassword();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSignInButton() {
        buttonSignIn.setOnClickListener(v -> {
            if (validateAllFields()) {
                performLogin();
            }
        });
    }

    private void performLogin() {
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null ? editTextPassword.getText().toString() : "";

        showLoadingState();

        LoginRequest loginRequest = new LoginRequest(email, password);
        Call<LoginResponse> call = apiService.login(loginRequest);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                hideLoadingState();

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    
                    if (loginResponse.isSuccess() && loginResponse.getData() != null) {
                        // Save token and user info
                        String token = loginResponse.getData().getToken();
                        if (token != null && !token.isEmpty()) {
                            authManager.saveToken(token);
                            
                            if (loginResponse.getData().getUser() != null) {
                                User user = loginResponse.getData().getUser();
                                authManager.saveUserInfo(
                                    user.getId(),
                                    user.getEmail()
                                );
                                authManager.saveUserName(user.getFullName());
                                
                                // Save user to Globals
                                com.smarthome.iot.utils.Globals.setCurrentUser(user);
                                
                                // Save email for auto-login only if "Remember Me" is checked
                                if (checkBoxRememberMe.isChecked()) {
                                    authManager.saveLoginEmail(user.getEmail());
                                } else {
                                    // Clear saved email and disable auto-login if checkbox is not checked
                                    authManager.clearAutoLogin();
                                }
                            }

                            // Show success message
                            Toast.makeText(SignInActivity.this, "Signed in successfully!", Toast.LENGTH_SHORT).show();

                            // Navigate to main activity
                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SignInActivity.this, "Login failed: No token received", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Handle error from response
                        String errorMessage = "Login failed";
                        if (loginResponse.getError() != null) {
                            errorMessage = loginResponse.getError().getMessage();
                        }
                        Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Handle HTTP error
                    String errorMessage = "Login failed";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMessage = response.body().getError().getMessage();
                    } else {
                        // Try to parse error response
                        if (response.errorBody() != null) {
                            try {
                                android.util.Log.e("SignIn", "Error response: " + response.errorBody().string());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        errorMessage = "Invalid email or password";
                    }
                    Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                hideLoadingState();
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
                
                Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                android.util.Log.e("SignIn", "Login error", t);
            }
        });
    }

    private void setupClickListeners() {
        buttonBack.setOnClickListener(v -> finish());

        buttonTogglePassword.setOnClickListener(v -> {
            togglePasswordVisibility();
        });

        textViewForgotPassword.setOnClickListener(v -> {
            String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
            Intent intent = new Intent(SignInActivity.this, ForgotPasswordActivity.class);
            if (!email.isEmpty()) {
                intent.putExtra("email", email);
            }
            startActivity(intent);
        });
        
        if (textViewSignUp != null) {
            textViewSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(intent);
            });
        }
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        
        if (isPasswordVisible) {
            editTextPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            buttonTogglePassword.setImageResource(R.drawable.ic_eye);
        } else {
            editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            buttonTogglePassword.setImageResource(R.drawable.ic_eye_off);
        }
        
        // Move cursor to end
        editTextPassword.setSelection(editTextPassword.getText() != null ? editTextPassword.getText().length() : 0);
    }

    private boolean validateEmail() {
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            return false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }
        return true;
    }

    private boolean validatePassword() {
        String password = editTextPassword.getText() != null ? editTextPassword.getText().toString() : "";
        if (password.isEmpty()) {
            return false;
        } else if (password.length() < 6) {
            return false;
        }
        return true;
    }

    private boolean validateAllFields() {
        boolean isValid = true;
        isValid &= validateEmail();
        isValid &= validatePassword();
        
        if (!isValid) {
            Toast.makeText(this, "Please enter valid email and password", Toast.LENGTH_SHORT).show();
        }
        
        return isValid;
    }

    private void showLoadingState() {
        loadingOverlay.setVisibility(View.VISIBLE);
        buttonSignIn.setEnabled(false);
        
        // Disable all input fields
        editTextEmail.setEnabled(false);
        editTextPassword.setEnabled(false);
    }

    private void hideLoadingState() {
        loadingOverlay.setVisibility(View.GONE);
        buttonSignIn.setEnabled(true);
        
        // Enable all input fields
        editTextEmail.setEnabled(true);
        editTextPassword.setEnabled(true);
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }
}
