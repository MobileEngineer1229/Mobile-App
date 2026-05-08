package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.models.Room;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.RoomSelectionAdapter;
import com.smarthome.iot.ui.dialogs.AddRoomBottomSheetDialog;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateHomeActivity extends AppCompatActivity {
    private TextInputEditText editTextHomeName;
    private LinearLayout layoutLocation;
    private ImageButton buttonAddRoom;
    private RecyclerView recyclerViewRooms;
    private MaterialButton buttonSave;
    
    private RoomSelectionAdapter adapter;
    private List<RoomSelectionAdapter.RoomItem> allRooms;
    private Set<Integer> selectedRoomIds;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_home);

        setStatusBarColor();

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        authManager = new AuthManager(this);

        initializeViews();
        setupRecyclerView();
        loadAvailableRooms();
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
        ImageButton buttonMore = findViewById(R.id.buttonMore);
        editTextHomeName = findViewById(R.id.editTextHomeName);
        layoutLocation = findViewById(R.id.layoutLocation);
        buttonAddRoom = findViewById(R.id.buttonAddRoom);
        recyclerViewRooms = findViewById(R.id.recyclerViewRooms);
        buttonSave = findViewById(R.id.buttonSave);

        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            // TODO: Show more options
        });
    }

    private void setupRecyclerView() {
        allRooms = new ArrayList<>();
        selectedRoomIds = new HashSet<>();
        adapter = new RoomSelectionAdapter(allRooms, selectedRoomIds, this::onRoomToggle);
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRooms.setAdapter(adapter);
    }

    private void loadAvailableRooms() {
        // Load available room types
        allRooms.clear();
        allRooms.add(new RoomSelectionAdapter.RoomItem(1, getString(R.string.living_room), true));
        allRooms.add(new RoomSelectionAdapter.RoomItem(2, getString(R.string.bedroom), true));
        allRooms.add(new RoomSelectionAdapter.RoomItem(3, getString(R.string.bathroom), true));
        allRooms.add(new RoomSelectionAdapter.RoomItem(4, getString(R.string.kitchen), true));
        allRooms.add(new RoomSelectionAdapter.RoomItem(5, getString(R.string.study_room), true));
        allRooms.add(new RoomSelectionAdapter.RoomItem(6, getString(R.string.dining_room), false));
        
        selectedRoomIds.add(1);
        selectedRoomIds.add(2);
        selectedRoomIds.add(3);
        selectedRoomIds.add(4);
        selectedRoomIds.add(5);
        
        adapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        layoutLocation.setOnClickListener(v -> {
            Toast.makeText(this, "Location selection - Coming soon", Toast.LENGTH_SHORT).show();
        });
        
        buttonAddRoom.setOnClickListener(v -> {
            AddRoomBottomSheetDialog dialog = new AddRoomBottomSheetDialog(this, roomName -> {
                // Add new room to list
                int newId = allRooms.isEmpty() ? 1 : allRooms.get(allRooms.size() - 1).getId() + 1;
                allRooms.add(new RoomSelectionAdapter.RoomItem(newId, roomName, true));
                selectedRoomIds.add(newId);
                adapter.notifyItemInserted(allRooms.size() - 1);
            });
            dialog.show();
        });
        
        buttonSave.setOnClickListener(v -> {
            String homeName = editTextHomeName.getText().toString().trim();
            if (homeName.isEmpty()) {
                editTextHomeName.setError(getString(R.string.home_name) + " is required");
                return;
            }
            
            createHome(homeName);
        });
    }

    private void onRoomToggle(int roomId, boolean isSelected) {
        if (isSelected) {
            selectedRoomIds.add(roomId);
        } else {
            selectedRoomIds.remove(roomId);
        }
    }

    private void createHome(String homeName) {
        if (!authManager.isLoggedIn()) {
            // If not logged in, return result without API call
            Intent resultIntent = new Intent();
            resultIntent.putExtra("home_name", homeName);
            resultIntent.putExtra("rooms_count", selectedRoomIds.size());
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }

        // Show loading state
        showLoading(true);

        // Prepare home data
        Map<String, Object> homeData = new HashMap<>();
        homeData.put("name", homeName);
        
        // Add location data if available from intent
        Intent intent = getIntent();
        if (intent != null) {
            String address = intent.getStringExtra("address");
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            String country = intent.getStringExtra("country");
            
            if (address != null && !address.trim().isEmpty()) {
                homeData.put("address", address);
            }
            if (latitude != 0) {
                homeData.put("latitude", latitude);
            }
            if (longitude != 0) {
                homeData.put("longitude", longitude);
            }
            if (country != null && !country.trim().isEmpty()) {
                homeData.put("country", country);
            }
        }

        Call<ApiResponse<Home>> call = apiService.createHome(homeData);
        call.enqueue(new Callback<ApiResponse<Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<Home>> call, Response<ApiResponse<Home>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Home createdHome = response.body().getData();
                    if (createdHome != null) {
                        // Create rooms for the new home
                        createRoomsForHome(createdHome.getId(), createdHome);
                    } else {
                        // Home created but no data returned, return result
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("home_name", homeName);
                        resultIntent.putExtra("rooms_count", selectedRoomIds.size());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                } else {
                    android.util.Log.w("CreateHome", "Failed to create home via API");
                    // Return result anyway
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("home_name", homeName);
                    resultIntent.putExtra("rooms_count", selectedRoomIds.size());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Home>> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("CreateHome", "Error creating home", t);
                // Return result anyway
                Intent resultIntent = new Intent();
                resultIntent.putExtra("home_name", homeName);
                resultIntent.putExtra("rooms_count", selectedRoomIds.size());
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void createRoomsForHome(int homeId, Home createdHome) {
        if (selectedRoomIds.isEmpty()) {
            // No rooms to create, return result
            Intent resultIntent = new Intent();
            resultIntent.putExtra("home_name", createdHome.getName());
            resultIntent.putExtra("rooms_count", 0);
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }

        // Create rooms one by one
        List<String> roomNames = new ArrayList<>();
        for (RoomSelectionAdapter.RoomItem item : allRooms) {
            if (selectedRoomIds.contains(item.getId())) {
                roomNames.add(item.getName());
            }
        }

        createRoomsSequentially(homeId, createdHome, roomNames, 0);
    }

    private void createRoomsSequentially(int homeId, Home createdHome, List<String> roomNames, int index) {
        if (index >= roomNames.size()) {
            // All rooms created, return result
            Intent resultIntent = new Intent();
            resultIntent.putExtra("home_name", createdHome.getName());
            resultIntent.putExtra("rooms_count", roomNames.size());
            setResult(RESULT_OK, resultIntent);
            finish();
            return;
        }

        String roomName = roomNames.get(index);
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("name", roomName);
        roomData.put("homeId", homeId);

        Call<ApiResponse<Room>> call = apiService.createRoom(roomData);
        call.enqueue(new Callback<ApiResponse<Room>>() {
            @Override
            public void onResponse(Call<ApiResponse<Room>> call, Response<ApiResponse<Room>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("CreateHome", "Room created: " + roomName);
                }
                // Continue with next room
                createRoomsSequentially(homeId, createdHome, roomNames, index + 1);
            }

            @Override
            public void onFailure(Call<ApiResponse<Room>> call, Throwable t) {
                android.util.Log.e("CreateHome", "Error creating room: " + roomName, t);
                // Continue with next room even if this one failed
                createRoomsSequentially(homeId, createdHome, roomNames, index + 1);
            }
        });
    }

    private void showLoading(boolean show) {
        // TODO: Implement loading indicator if needed
        // For now, just log
        if (show) {
            android.util.Log.d("CreateHome", "Creating home...");
        }
    }
}
