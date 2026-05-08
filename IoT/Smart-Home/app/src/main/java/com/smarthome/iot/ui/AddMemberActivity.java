package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.dialogs.SuccessDialog;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.HashMap;
import java.util.Map;

import android.os.Handler;
import android.os.Looper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMemberActivity extends AppCompatActivity {
    private MaterialButton buttonInviteEmail;
    private MaterialButton buttonInviteQR;
    private TextInputEditText editTextEmail;
    private RadioGroup radioGroupRole;
    private RadioButton radioAdmin;
    private RadioButton radioMember;
    private MaterialButton buttonSendInvite;
    
    private int homeId;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        setStatusBarColor();
        
        homeId = getIntent().getIntExtra("home_id", -1);

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        authManager = new AuthManager(this);

        initializeViews();
        setupClickListeners();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonInviteEmail = findViewById(R.id.buttonInviteEmail);
        buttonInviteQR = findViewById(R.id.buttonInviteQR);
        editTextEmail = findViewById(R.id.editTextEmail);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        radioAdmin = findViewById(R.id.radioAdmin);
        radioMember = findViewById(R.id.radioMember);
        buttonSendInvite = findViewById(R.id.buttonSendInvite);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        buttonInviteEmail.setOnClickListener(v -> {
            // Already on email mode
        });

        buttonInviteQR.setOnClickListener(v -> {
            // Switch to QR mode
            Intent intent = new Intent(this, AddMemberQRActivity.class);
            intent.putExtra("home_id", homeId);
            startActivity(intent);
            finish();
        });

        buttonSendInvite.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            if (email.isEmpty()) {
                editTextEmail.setError(getString(R.string.email) + " is required");
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editTextEmail.setError("Invalid email address");
                return;
            }

            // Get selected role
            String role = radioMember.isChecked() ? "Member" : "Admin";
            
            sendInvitation(email, role);
        });
    }

    private void sendInvitation(String email, String role) {
        if (homeId == -1) {
            Toast.makeText(this, "Invalid home ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            // Demo user - just show success
            String message = getString(R.string.invitation_sent_success, email);
            SuccessDialog successDialog = new SuccessDialog(this, message);
            successDialog.show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("member_added", true);
            setResult(RESULT_OK, resultIntent);
            new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 2000);
            return;
        }

        // Prepare invitation data
        Map<String, Object> invitationData = new HashMap<>();
        invitationData.put("email", email);
        invitationData.put("role", role);

        // Call API to send invitation
        Call<ApiResponse<Map<String, Object>>> call = apiService.sendHomeInvitation(homeId, invitationData);
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("AddMemberActivity", "Invitation sent successfully to: " + email);
                    
                    String message = getString(R.string.invitation_sent_success, email);
                    SuccessDialog successDialog = new SuccessDialog(AddMemberActivity.this, message);
                    successDialog.show();
                    
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("member_added", true);
                    setResult(RESULT_OK, resultIntent);
                    
                    new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 2000);
                } else {
                    android.util.Log.w("AddMemberActivity", "Failed to send invitation via API");
                    String errorMessage = "Failed to send invitation";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMessage = response.body().getError().getMessage();
                    }
                    Toast.makeText(AddMemberActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                android.util.Log.e("AddMemberActivity", "Error sending invitation", t);
                Toast.makeText(AddMemberActivity.this, 
                    "Error sending invitation: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
