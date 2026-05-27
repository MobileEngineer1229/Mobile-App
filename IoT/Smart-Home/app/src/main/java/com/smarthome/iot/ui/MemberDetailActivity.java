package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.dialogs.DeleteMemberDialog;
import com.smarthome.iot.ui.dialogs.SuccessDialog;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MemberDetailActivity extends AppCompatActivity {
    private ImageView imageViewAvatar;
    private TextView textViewName;
    private TextView textViewEmail;
    private TextView textViewRole;
    private LinearLayout layoutRole;
    private MaterialButton buttonRemoveMember;
    
    private int homeId;
    private int memberId;
    private String memberName;
    private String memberEmail;
    private String memberRole;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_detail);

        setStatusBarColor();
        
        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);
        
        // Get member data from intent
        homeId = getIntent().getIntExtra("home_id", -1);
        memberId = getIntent().getIntExtra("member_id", -1);
        memberName = getIntent().getStringExtra("member_name");
        memberEmail = getIntent().getStringExtra("member_email");
        memberRole = getIntent().getStringExtra("member_role");
        
        if (memberName == null) {
            memberName = "Member";
        }
        if (memberEmail == null) {
            memberEmail = "email@example.com";
        }
        if (memberRole == null) {
            memberRole = "Member";
        }

        initializeViews();
        setupClickListeners();
        updateUI();
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
        ImageButton buttonMore = findViewById(R.id.buttonMore);
        imageViewAvatar = findViewById(R.id.imageViewAvatar);
        textViewName = findViewById(R.id.textViewName);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewRole = findViewById(R.id.textViewRole);
        layoutRole = findViewById(R.id.layoutRole);
        buttonRemoveMember = findViewById(R.id.buttonRemoveMember);

        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            // TODO: Show more options menu
        });
    }

    private void setupClickListeners() {
        layoutRole.setOnClickListener(v -> {
            // TODO: Show role selection dialog
        });
        
        buttonRemoveMember.setOnClickListener(v -> {
            DeleteMemberDialog dialog = new DeleteMemberDialog(this, memberName, () -> {
                removeMember();
            });
            dialog.show();
        });
    }

    private void updateUI() {
        textViewName.setText(memberName);
        textViewEmail.setText(memberEmail);
        textViewRole.setText(memberRole);
        
        // TODO: Load profile picture
        imageViewAvatar.setImageResource(R.drawable.ic_account_circle);
    }

    private void removeMember() {
        if (homeId == -1 || memberId == -1) {
            Toast.makeText(this, "Invalid home or member ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!authManager.isLoggedIn() || MockDataProvider.isDemoUser(authManager)) {
            // Demo user - just show success
            SuccessDialog successDialog = new SuccessDialog(this, 
                getString(R.string.member_removed_success, memberName));
            successDialog.show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("member_removed", true);
            resultIntent.putExtra("member_id", memberId);
            setResult(RESULT_OK, resultIntent);
            buttonRemoveMember.postDelayed(() -> finish(), 2000);
            return;
        }

        // Call API to remove member
        Call<ApiResponse<Void>> call = apiService.removeHomeMember(homeId, memberId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("MemberDetailActivity", "Member removed successfully: " + memberName);
                    
                    // Show success dialog
                    SuccessDialog successDialog = new SuccessDialog(MemberDetailActivity.this, 
                        getString(R.string.member_removed_success, memberName));
                    successDialog.show();
                    
                    // Return result to HomeDetailActivity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("member_removed", true);
                    resultIntent.putExtra("member_id", memberId);
                    setResult(RESULT_OK, resultIntent);
                    
                    // Finish after a delay to show success message
                    buttonRemoveMember.postDelayed(() -> finish(), 2000);
                } else {
                    android.util.Log.w("MemberDetailActivity", "Failed to remove member via API");
                    String errorMessage = "Failed to remove member";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMessage = response.body().getError().getMessage();
                    }
                    Toast.makeText(MemberDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                android.util.Log.e("MemberDetailActivity", "Error removing member", t);
                Toast.makeText(MemberDetailActivity.this, 
                    "Error removing member: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
