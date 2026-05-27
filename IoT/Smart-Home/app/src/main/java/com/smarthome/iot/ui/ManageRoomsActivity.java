package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.ManageRoomsAdapter;
import com.smarthome.iot.ui.dialogs.DeleteRoomDialog;
import com.smarthome.iot.ui.dialogs.SuccessDialog;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageRoomsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewRooms;
    private ManageRoomsAdapter adapter;
    private List<ManageRoomsAdapter.RoomItem> rooms;
    private ItemTouchHelper itemTouchHelper;
    private ApiService apiService;
    private AuthManager authManager;
    private int homeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_rooms);

        setStatusBarColor();
        
        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);
        homeId = getIntent().getIntExtra("home_id", -1);
        
        initializeViews();
        setupRecyclerView();
        loadRooms();
        setupItemTouchHelper();
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
        recyclerViewRooms = findViewById(R.id.recyclerViewRooms);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rooms = new ArrayList<>();
        adapter = new ManageRoomsAdapter(rooms, this::onRoomDelete, this::onDragStart);
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRooms.setAdapter(adapter);
    }

    private void loadRooms() {
        ArrayList<Integer> roomIds = getIntent().getIntegerArrayListExtra("room_ids");
        ArrayList<String> roomNames = getIntent().getStringArrayListExtra("room_names");

        rooms.clear();
        if (roomIds != null && roomNames != null && roomIds.size() == roomNames.size()) {
            for (int i = 0; i < roomIds.size(); i++) {
                rooms.add(new ManageRoomsAdapter.RoomItem(roomIds.get(i), roomNames.get(i)));
            }
            adapter.notifyDataSetChanged();
        } else if (homeId != -1 && authManager.isLoggedIn()) {
            // Fallback: fetch rooms from API
            loadRoomsFromApi();
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void loadRoomsFromApi() {
        Call<ApiResponse<List<com.smarthome.iot.models.Room>>> call = apiService.getRooms(homeId);
        call.enqueue(new Callback<ApiResponse<List<com.smarthome.iot.models.Room>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<com.smarthome.iot.models.Room>>> call,
                                   Response<ApiResponse<List<com.smarthome.iot.models.Room>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.smarthome.iot.models.Room> apiRooms = response.body().getData();
                    if (apiRooms != null) {
                        rooms.clear();
                        for (com.smarthome.iot.models.Room r : apiRooms) {
                            rooms.add(new ManageRoomsAdapter.RoomItem(r.getId(), r.getName()));
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<com.smarthome.iot.models.Room>>> call, Throwable t) {
                android.util.Log.e("ManageRooms", "Error loading rooms from API", t);
                Toast.makeText(ManageRoomsActivity.this, "Failed to load rooms", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN,
            0
        ) {
            @Override
            public boolean onMove(androidx.recyclerview.widget.RecyclerView recyclerView,
                                  androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder,
                                  androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(rooms, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used
            }
        };
        
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewRooms);
    }

    private void onRoomDelete(ManageRoomsAdapter.RoomItem room) {
        DeleteRoomDialog dialog = new DeleteRoomDialog(this, room.getName(), () -> {
            // Check if logged in
            if (!authManager.isLoggedIn()) {
                Toast.makeText(this, "Please log in to delete rooms", Toast.LENGTH_SHORT).show();
                return;
            }

            // Call API to delete room
            Call<ApiResponse<Void>> call = apiService.deleteRoom(room.getId());
            call.enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        // Remove from local list
                        int position = -1;
                        for (int i = 0; i < rooms.size(); i++) {
                            if (rooms.get(i).getId() == room.getId()) {
                                position = i;
                                break;
                            }
                        }
                        
                        if (position >= 0) {
                            rooms.remove(position);
                            adapter.notifyItemRemoved(position);
                            
                            // Show success dialog
                            SuccessDialog successDialog = new SuccessDialog(ManageRoomsActivity.this, 
                                getString(R.string.room_deleted_success, room.getName()));
                            successDialog.show();
                            
                            // Return result
                            Intent resultIntent = new Intent();
                            resultIntent.putIntegerArrayListExtra("room_ids", getRoomIds());
                            resultIntent.putStringArrayListExtra("room_names", getRoomNames());
                            setResult(RESULT_OK, resultIntent);
                        }
                        android.util.Log.d("ManageRoomsActivity", "Room deleted successfully: " + room.getName());
                    } else {
                        android.util.Log.w("ManageRoomsActivity", "Failed to delete room via API");
                        Toast.makeText(ManageRoomsActivity.this, "Failed to delete room", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    android.util.Log.e("ManageRoomsActivity", "Error deleting room", t);
                    Toast.makeText(ManageRoomsActivity.this, "Error deleting room: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    private void onDragStart(androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null) {
            itemTouchHelper.startDrag(viewHolder);
        }
    }

    private ArrayList<Integer> getRoomIds() {
        ArrayList<Integer> ids = new ArrayList<>();
        for (ManageRoomsAdapter.RoomItem room : rooms) {
            ids.add(room.getId());
        }
        return ids;
    }

    private ArrayList<String> getRoomNames() {
        ArrayList<String> names = new ArrayList<>();
        for (ManageRoomsAdapter.RoomItem room : rooms) {
            names.add(room.getName());
        }
        return names;
    }
}
