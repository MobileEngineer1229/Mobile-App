package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.models.HomeMember;
import com.smarthome.iot.ui.adapters.HomeMemberAdapter;
import com.smarthome.iot.ui.dialogs.SuccessDialog;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeDetailActivity extends AppCompatActivity {
    private TextView textViewHomeName;
    private TextView textViewCurrentHomeName;
    private TextView textViewRoomCount;
    private TextView textViewDeviceCount;
    private TextView textViewLocation;
    private TextView textViewMembersTitle;
    private LinearLayout layoutHomeName;
    private LinearLayout layoutRoomManagement;
    private RecyclerView recyclerViewMembers;
    private ImageButton buttonAddMember;
    private MaterialButton buttonDeleteHome;
    
    private int homeId;
    private String homeName;
    private String homeAddress;
    private int roomCount = 0;
    private int deviceCount = 0;
    private int memberCount = 4;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_detail);

        setStatusBarColor();
        
        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);
        
        // Get home data from intent
        homeId = getIntent().getIntExtra("home_id", -1);
        homeName = getIntent().getStringExtra("home_name");
        homeAddress = getIntent().getStringExtra("home_address");
        
        if (homeName == null) {
            homeName = "My Home";
        }

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadHomeData();
        updateUI();
    }
    
    private void loadHomeData() {
        if (homeId == -1) {
            android.util.Log.w("HomeDetailActivity", "Invalid home ID");
            roomCount = 0;
            deviceCount = 0;
            return;
        }

        if (!authManager.isLoggedIn()) {
            android.util.Log.w("HomeDetailActivity", "Not logged in");
            roomCount = 0;
            deviceCount = 0;
            return;
        }

        // Load home details from API
        Call<ApiResponse<com.smarthome.iot.models.Home>> homeCall = apiService.getHomeById(homeId);
        homeCall.enqueue(new Callback<ApiResponse<com.smarthome.iot.models.Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.smarthome.iot.models.Home>> call, Response<ApiResponse<com.smarthome.iot.models.Home>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.smarthome.iot.models.Home home = response.body().getData();
                    if (home != null) {
                        homeName = home.getName();
                        homeAddress = home.getAddress();
                        updateUI();
                    }
                }
                // Load rooms and devices counts
                loadRoomsAndDevicesCounts();
            }

            @Override
            public void onFailure(Call<ApiResponse<com.smarthome.iot.models.Home>> call, Throwable t) {
                android.util.Log.e("HomeDetailActivity", "Error loading home details", t);
                roomCount = 0;
                deviceCount = 0;
                updateUI();
            }
        });
    }

    private void loadRoomsAndDevicesCounts() {
        // Load rooms count
        Call<ApiResponse<List<com.smarthome.iot.models.Room>>> roomsCall = apiService.getRooms(homeId);
        roomsCall.enqueue(new Callback<ApiResponse<List<com.smarthome.iot.models.Room>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<com.smarthome.iot.models.Room>>> call, Response<ApiResponse<List<com.smarthome.iot.models.Room>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.smarthome.iot.models.Room> rooms = response.body().getData();
                    roomCount = rooms != null ? rooms.size() : 0;
                } else {
                    roomCount = 0;
                }
                // Load devices count
                loadDevicesCount();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<com.smarthome.iot.models.Room>>> call, Throwable t) {
                android.util.Log.e("HomeDetailActivity", "Error loading rooms", t);
                roomCount = 0;
                loadDevicesCount();
            }
        });
    }

    private void loadDevicesCount() {
        // Load devices count
        Call<ApiResponse<List<com.smarthome.iot.models.Device>>> devicesCall = apiService.getDevices(null, homeId, null, null, null, null);
        devicesCall.enqueue(new Callback<ApiResponse<List<com.smarthome.iot.models.Device>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<com.smarthome.iot.models.Device>>> call, Response<ApiResponse<List<com.smarthome.iot.models.Device>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.smarthome.iot.models.Device> devices = response.body().getData();
                    deviceCount = devices != null ? devices.size() : 0;
                } else {
                    deviceCount = 0;
                }
                updateUI();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<com.smarthome.iot.models.Device>>> call, Throwable t) {
                android.util.Log.e("HomeDetailActivity", "Error loading devices", t);
                deviceCount = 0;
                updateUI();
            }
        });
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
        textViewHomeName = findViewById(R.id.textViewHomeName);
        textViewCurrentHomeName = findViewById(R.id.textViewCurrentHomeName);
        textViewRoomCount = findViewById(R.id.textViewRoomCount);
        textViewDeviceCount = findViewById(R.id.textViewDeviceCount);
        textViewLocation = findViewById(R.id.textViewLocation);
        textViewMembersTitle = findViewById(R.id.textViewMembersTitle);
        layoutHomeName = findViewById(R.id.layoutHomeName);
        layoutRoomManagement = findViewById(R.id.layoutRoomManagement);
        recyclerViewMembers = findViewById(R.id.recyclerViewMembers);
        buttonAddMember = findViewById(R.id.buttonAddMember);
        buttonDeleteHome = findViewById(R.id.buttonDeleteHome);

        buttonBack.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
        buttonMore.setOnClickListener(v -> {
            Toast.makeText(this, "More options coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(this));
        loadHomeMembers();
    }

    private void loadHomeMembers() {
        if (homeId == -1) {
            android.util.Log.w("HomeDetailActivity", "Invalid homeId, cannot load members");
            showEmptyMembersState();
            return;
        }

        if (!authManager.isLoggedIn()) {
            android.util.Log.w("HomeDetailActivity", "Not logged in, cannot load members");
            showEmptyMembersState();
            return;
        }

        // Call API to get home members
        Call<ApiResponse<List<HomeMember>>> call = apiService.getHomeMembers(homeId);
        call.enqueue(new Callback<ApiResponse<List<HomeMember>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<HomeMember>>> call, Response<ApiResponse<List<HomeMember>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<HomeMember> members = response.body().getData();
                    if (members != null && !members.isEmpty()) {
                        displayMembers(members);
                        android.util.Log.d("HomeDetailActivity", "Loaded " + members.size() + " members from API");
                    } else {
                        // No members found - show empty state
                        showEmptyMembersState();
                        android.util.Log.d("HomeDetailActivity", "No members found from API");
                    }
                } else {
                    android.util.Log.w("HomeDetailActivity", "Failed to load members from API");
                    showEmptyMembersState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<HomeMember>>> call, Throwable t) {
                android.util.Log.e("HomeDetailActivity", "Error loading members", t);
                showEmptyMembersState();
            }
        });
    }

    private void displayMembers(List<HomeMember> apiMembers) {
        List<HomeMemberAdapter.MemberItem> memberItems = new ArrayList<>();
        
        for (HomeMember member : apiMembers) {
            memberItems.add(new HomeMemberAdapter.MemberItem(
                member.getId(),
                member.getFullName(),
                member.getDisplayEmail(),
                member.getRoleDisplayText(),
                member.isOwner()
            ));
        }
        
        memberCount = memberItems.size();
        updateUI();
        
        HomeMemberAdapter adapter = new HomeMemberAdapter(memberItems, member -> {
            Intent intent = new Intent(this, MemberDetailActivity.class);
            intent.putExtra("home_id", homeId);
            intent.putExtra("member_id", member.getId());
            intent.putExtra("member_name", member.getName());
            intent.putExtra("member_email", member.getEmail());
            intent.putExtra("member_role", member.getRole());
            startActivityForResult(intent, 201);
        });
        recyclerViewMembers.setAdapter(adapter);
    }

    private void showEmptyMembersState() {
        // Show empty state - just the current user as owner
        List<HomeMemberAdapter.MemberItem> memberItems = new ArrayList<>();
        // Add current user as owner
        String currentUserName = authManager.getUserName();
        String currentUserEmail = authManager.getUserEmail();
        if (currentUserName == null || currentUserName.isEmpty()) {
            currentUserName = "You";
        }
        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            currentUserEmail = "your.email@example.com";
        }
        memberItems.add(new HomeMemberAdapter.MemberItem(
            0,
            currentUserName,
            currentUserEmail.length() > 25 ? currentUserEmail.substring(0, 22) + "..." : currentUserEmail,
            "Owner",
            true
        ));
        
        memberCount = memberItems.size();
        updateUI();
        
        HomeMemberAdapter adapter = new HomeMemberAdapter(memberItems, member -> {
            Intent intent = new Intent(this, MemberDetailActivity.class);
            intent.putExtra("home_id", homeId);
            intent.putExtra("member_id", member.getId());
            intent.putExtra("member_name", member.getName());
            intent.putExtra("member_email", member.getEmail());
            intent.putExtra("member_role", member.getRole());
            startActivityForResult(intent, 201);
        });
        recyclerViewMembers.setAdapter(adapter);
    }

    private void setupClickListeners() {
        layoutHomeName.setOnClickListener(v -> showEditHomeNameDialog());
        
        layoutRoomManagement.setOnClickListener(v -> {
            Intent intent = new Intent(this, RoomManagementActivity.class);
            intent.putExtra("home_id", homeId);
            intent.putExtra("home_name", homeName);
            startActivityForResult(intent, 100);
        });
        
        buttonAddMember.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMemberActivity.class);
            intent.putExtra("home_id", homeId);
            startActivityForResult(intent, 200);
        });
        
        buttonDeleteHome.setOnClickListener(v -> {
            com.smarthome.iot.ui.dialogs.DeleteHomeDialog dialog = 
                new com.smarthome.iot.ui.dialogs.DeleteHomeDialog(this, () -> {
                    deleteHome();
                });
            dialog.show();
        });
    }

    private void updateUI() {
        textViewHomeName.setText(homeName);
        textViewCurrentHomeName.setText(homeName);
        textViewRoomCount.setText(roomCount + " " + getString(R.string.rooms));
        textViewDeviceCount.setText(deviceCount + " " + getString(R.string.devices));
        
        if (homeAddress != null && !homeAddress.isEmpty()) {
            textViewLocation.setText(homeAddress);
        } else {
            textViewLocation.setText("701 7th Ave...");
        }
        
        textViewMembersTitle.setText(getString(R.string.home_members) + " (" + memberCount + ")");
    }

    private void showEditHomeNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_home_name, null);
        
        TextView textViewTitle = dialogView.findViewById(R.id.textViewTitle);
        com.google.android.material.textfield.TextInputEditText editTextHomeName = 
            dialogView.findViewById(R.id.editTextHomeName);
        MaterialButton buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        MaterialButton buttonSave = dialogView.findViewById(R.id.buttonSave);
        
        textViewTitle.setText(getString(R.string.edit_home_name));
        editTextHomeName.setText(homeName);
        editTextHomeName.setSelection(homeName.length());
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            String newName = editTextHomeName.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateHomeName(newName, dialog);
            } else {
                Toast.makeText(this, "Please enter a home name", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Update room count from result
            ArrayList<String> roomNames = data.getStringArrayListExtra("room_names");
            if (roomNames != null) {
                roomCount = roomNames.size();
                updateUI();
            } else {
                // Reload from API
                loadHomeData();
            }
        } else if (requestCode == 200 && resultCode == RESULT_OK) {
            // Member added - reload members from API
            loadHomeMembers();
        } else if (requestCode == 201 && resultCode == RESULT_OK && data != null) {
            // Member removed - reload members from API
            if (data.getBooleanExtra("member_removed", false)) {
                loadHomeMembers();
            }
        }
    }

    private void deleteHome() {
        if (homeId == -1) {
            Toast.makeText(this, "Invalid home ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in to delete this home", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call API to delete home
        Call<ApiResponse<Void>> call = apiService.deleteHome(homeId);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Show success dialog
                    SuccessDialog successDialog = new SuccessDialog(HomeDetailActivity.this, 
                        getString(R.string.home_deleted_success, homeName));
                    successDialog.show();
                    
                    // Return result and finish
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("home_deleted", true);
                    resultIntent.putExtra("home_id", homeId);
                    setResult(RESULT_OK, resultIntent);
                    
                    // Finish after delay
                    buttonDeleteHome.postDelayed(() -> finish(), 2000);
                    android.util.Log.d("HomeDetailActivity", "Home deleted successfully");
                } else {
                    android.util.Log.w("HomeDetailActivity", "Failed to delete home via API");
                    Toast.makeText(HomeDetailActivity.this, 
                        "Failed to delete home", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                android.util.Log.e("HomeDetailActivity", "Error deleting home", t);
                Toast.makeText(HomeDetailActivity.this, 
                    "Error deleting home: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateHomeName(String newName, AlertDialog dialog) {
        if (homeId == -1) {
            Toast.makeText(this, "Invalid home ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in to update the home name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare update data
        Map<String, Object> homeData = new HashMap<>();
        homeData.put("name", newName);

        // Call API to update home
        Call<ApiResponse<com.smarthome.iot.models.Home>> call = apiService.updateHome(homeId, homeData);
        call.enqueue(new Callback<ApiResponse<com.smarthome.iot.models.Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.smarthome.iot.models.Home>> call, Response<ApiResponse<com.smarthome.iot.models.Home>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.smarthome.iot.models.Home updatedHome = response.body().getData();
                    if (updatedHome != null) {
                        homeName = updatedHome.getName();
                        homeAddress = updatedHome.getAddress();
                        updateUI();
                        dialog.dismiss();
                        Toast.makeText(HomeDetailActivity.this, "Home name updated", Toast.LENGTH_SHORT).show();
                        android.util.Log.d("HomeDetailActivity", "Home name updated successfully");
                    }
                } else {
                    android.util.Log.w("HomeDetailActivity", "Failed to update home name via API");
                    Toast.makeText(HomeDetailActivity.this, "Failed to update home name", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.smarthome.iot.models.Home>> call, Throwable t) {
                android.util.Log.e("HomeDetailActivity", "Error updating home name", t);
                Toast.makeText(HomeDetailActivity.this, "Error updating home name: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
