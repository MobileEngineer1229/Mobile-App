package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
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
import com.smarthome.iot.models.Room;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.RoomAdapter;
import com.smarthome.iot.ui.dialogs.AddRoomBottomSheetDialog;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomManagementActivity extends AppCompatActivity {
    private RecyclerView recyclerViewRooms;
    private MaterialButton buttonAddRoom;
    private ImageButton buttonManage;
    private ProgressBar progressBar;
    private RoomAdapter adapter;
    private List<Room> rooms;
    private int homeId;
    private String homeName;
    private ApiService apiService;
    private AuthManager authManager;
    private boolean needsRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_management);

        setStatusBarColor();
        
        homeId = getIntent().getIntExtra("home_id", -1);
        homeName = getIntent().getStringExtra("home_name");

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
        setupRecyclerView();
        loadRooms();
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
        buttonManage = findViewById(R.id.buttonManage);
        progressBar = findViewById(R.id.progressBar);
        recyclerViewRooms = findViewById(R.id.recyclerViewRooms);
        buttonAddRoom = findViewById(R.id.buttonAddRoom);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rooms = new ArrayList<>();
        adapter = new RoomAdapter(rooms, this::onRoomClick);
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRooms.setAdapter(adapter);
    }

    private void loadRooms() {
        if (homeId == -1) {
            android.util.Log.w("RoomManagement", "Invalid homeId");
            showEmptyState();
            return;
        }

        if (!authManager.isLoggedIn()) {
            android.util.Log.w("RoomManagement", "Not logged in");
            showEmptyState();
            return;
        }

        // Show loading state
        showLoading(true);

        Call<ApiResponse<List<Room>>> call = apiService.getRooms(homeId);
        call.enqueue(new Callback<ApiResponse<List<Room>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Room>>> call, Response<ApiResponse<List<Room>>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Room> apiRooms = response.body().getData();
                    if (apiRooms != null && !apiRooms.isEmpty()) {
                        rooms.clear();
                        rooms.addAll(apiRooms);
                        adapter.notifyDataSetChanged();
                        android.util.Log.d("RoomManagement", "Loaded " + apiRooms.size() + " rooms from API");
                    } else {
                        // No rooms found, show empty state
                        showEmptyState();
                        android.util.Log.d("RoomManagement", "No rooms found from API");
                    }
                } else {
                    android.util.Log.w("RoomManagement", "Failed to load rooms from API");
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Room>>> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("RoomManagement", "Error loading rooms", t);
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        rooms.clear();
        adapter.notifyDataSetChanged();
        // Optionally show a message that no rooms are available
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewRooms != null) {
            recyclerViewRooms.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        buttonAddRoom.setOnClickListener(v -> showAddRoomDialog());
        
        buttonManage.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageRoomsActivity.class);
            intent.putExtra("home_id", homeId);
            intent.putIntegerArrayListExtra("room_ids", getRoomIds());
            intent.putStringArrayListExtra("room_names", getRoomNames());
            startActivityForResult(intent, 100);
        });
    }

    private void onRoomClick(Room room) {
        showEditRoomDialog(room);
    }

    private void showEditRoomDialog(Room room) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_home_name, null);

        TextView textViewTitle = dialogView.findViewById(R.id.textViewTitle);
        com.google.android.material.textfield.TextInputEditText editText =
            dialogView.findViewById(R.id.editTextHomeName);
        MaterialButton buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        MaterialButton buttonSave = dialogView.findViewById(R.id.buttonSave);

        textViewTitle.setText(getString(R.string.edit_room_name));
        editText.setText(room.getName());
        editText.setSelection(room.getName().length());

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            String newName = editText.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateRoomName(room, newName, dialog);
            } else {
                Toast.makeText(this, "Please enter a room name", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void updateRoomName(Room room, String newName, AlertDialog dialog) {
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in to update room", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", newName);

        Call<ApiResponse<Room>> call = apiService.updateRoom(room.getId(), roomData);
        call.enqueue(new Callback<ApiResponse<Room>>() {
            @Override
            public void onResponse(Call<ApiResponse<Room>> call, Response<ApiResponse<Room>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    room.setName(newName);
                    adapter.notifyDataSetChanged();
                    dialog.dismiss();
                    Toast.makeText(RoomManagementActivity.this,
                        getString(R.string.room_updated_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RoomManagementActivity.this,
                        "Failed to update room name", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Room>> call, Throwable t) {
                Toast.makeText(RoomManagementActivity.this,
                    "Error updating room: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ArrayList<Integer> getRoomIds() {
        ArrayList<Integer> ids = new ArrayList<>();
        for (Room room : rooms) {
            ids.add(room.getId());
        }
        return ids;
    }

    private ArrayList<String> getRoomNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Room room : rooms) {
            names.add(room.getName());
        }
        return names;
    }

    private void showAddRoomDialog() {
        AddRoomBottomSheetDialog dialog = new AddRoomBottomSheetDialog(this, roomName -> {
            createRoom(roomName);
        });
        dialog.show();
    }

    private void createRoom(String roomName) {
        if (homeId == -1) {
            android.util.Log.w("RoomManagement", "Invalid homeId, cannot create room");
            android.widget.Toast.makeText(this, "Invalid home ID", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        if (!authManager.isLoggedIn()) {
            android.util.Log.w("RoomManagement", "Not logged in, cannot create room");
            android.widget.Toast.makeText(this, "Please log in to create a room", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        showLoading(true);

        // Prepare room data
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", roomName);
        roomData.put("homeId", homeId);

        Call<ApiResponse<Room>> call = apiService.createRoom(roomData);
        call.enqueue(new Callback<ApiResponse<Room>>() {
            @Override
            public void onResponse(Call<ApiResponse<Room>> call, Response<ApiResponse<Room>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Room newRoom = response.body().getData();
                    if (newRoom != null) {
                        rooms.add(newRoom);
                        adapter.notifyItemInserted(rooms.size() - 1);
                        android.util.Log.d("RoomManagement", "Room created successfully: " + newRoom.getName());
                        android.widget.Toast.makeText(RoomManagementActivity.this, "Room created successfully", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } else {
                    android.util.Log.w("RoomManagement", "Failed to create room via API");
                    android.widget.Toast.makeText(RoomManagementActivity.this, "Failed to create room", android.widget.Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Room>> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("RoomManagement", "Error creating room", t);
                android.widget.Toast.makeText(RoomManagementActivity.this, "Error creating room: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            // Always reload from API after managing rooms to stay in sync
            loadRooms();
        }
    }
}
