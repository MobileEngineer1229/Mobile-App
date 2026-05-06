package com.talentbaby.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.talentbaby.app.MainActivity;
import com.talentbaby.app.activities.AddBabyActivity;
import com.talentbaby.app.R;
import com.talentbaby.app.models.ApiResponse;
import com.talentbaby.app.models.AuthResponse;
import com.talentbaby.app.models.Baby;
import com.talentbaby.app.network.ApiService;
import com.talentbaby.app.utils.ApiClient;
import com.talentbaby.app.utils.TokenManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_REMEMBER = "remember_me_prefs";
    private static final String KEY_SAVED_EMAIL = "saved_email";
    private static final String KEY_REMEMBER = "remember_checked";

    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private MaterialCheckBox checkboxRememberMe;
    private TextView buttonLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TokenManager.isLoggedIn(this)) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkboxRememberMe = findViewById(R.id.checkboxRememberMe);
        buttonLogin = findViewById(R.id.buttonLogin);
        progressBar = findViewById(R.id.progressBar);

        // Back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        // Forgot password
        findViewById(R.id.textForgotPassword).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.forgot_password), Toast.LENGTH_SHORT).show());

        // Sign up link
        findViewById(R.id.textViewSignUp).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        buttonLogin.setOnClickListener(v -> login());

        restoreSavedEmail();
    }

    private void restoreSavedEmail() {
        SharedPreferences prefs = getSharedPreferences(PREFS_REMEMBER, Context.MODE_PRIVATE);
        boolean remembered = prefs.getBoolean(KEY_REMEMBER, false);
        if (remembered) {
            String savedEmail = prefs.getString(KEY_SAVED_EMAIL, "");
            editTextEmail.setText(savedEmail);
            checkboxRememberMe.setChecked(true);
        }
    }

    private void login() {
        String email = editTextEmail.getText() != null
                ? editTextEmail.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null
                ? editTextPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.field_required), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.login(credentials).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    AuthResponse.AuthData data = response.body().getData();
                    TokenManager.saveToken(LoginActivity.this, data.getToken());
                    if (data.getUser() != null) {
                        TokenManager.saveUserId(LoginActivity.this, data.getUser().getId());
                    }
                    saveRememberMe(email);
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                    checkBabiesAndNavigate();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Login failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveRememberMe(String email) {
        SharedPreferences prefs = getSharedPreferences(PREFS_REMEMBER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (checkboxRememberMe.isChecked()) {
            editor.putBoolean(KEY_REMEMBER, true);
            editor.putString(KEY_SAVED_EMAIL, email);
        } else {
            editor.putBoolean(KEY_REMEMBER, false);
            editor.remove(KEY_SAVED_EMAIL);
        }
        editor.apply();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!loading);
        buttonLogin.setAlpha(loading ? 0.6f : 1f);
    }

    private void checkBabiesAndNavigate() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getBabies().enqueue(new Callback<ApiResponse<List<Baby>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Baby>>> call,
                                   Response<ApiResponse<List<Baby>>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null
                        && !response.body().getData().isEmpty()) {
                    navigateToMain();
                } else {
                    navigateToAddBaby();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Baby>>> call, Throwable t) {
                navigateToMain();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToAddBaby() {
        Intent intent = new Intent(this, AddBabyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
