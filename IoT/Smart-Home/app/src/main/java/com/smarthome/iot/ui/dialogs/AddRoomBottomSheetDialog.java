package com.smarthome.iot.ui.dialogs;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;
import com.smarthome.iot.ui.adapters.SuggestedRoomAdapter;

import java.util.ArrayList;
import java.util.List;

public class AddRoomBottomSheetDialog {
    private BottomSheetDialog dialog;
    private OnRoomAddedListener listener;
    private TextInputEditText editTextRoomName;

    public interface OnRoomAddedListener {
        void onRoomAdded(String roomName);
    }

    public AddRoomBottomSheetDialog(Activity activity, OnRoomAddedListener listener) {
        this.listener = listener;
        
        View bottomSheetView = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_add_room, null);
        dialog = new BottomSheetDialog(activity);
        dialog.setContentView(bottomSheetView);
        
        editTextRoomName = bottomSheetView.findViewById(R.id.editTextRoomName);
        RecyclerView recyclerViewSuggested = bottomSheetView.findViewById(R.id.recyclerViewSuggestedRooms);
        MaterialButton buttonCancel = bottomSheetView.findViewById(R.id.buttonCancel);
        MaterialButton buttonSave = bottomSheetView.findViewById(R.id.buttonSave);
        
        // Setup suggested rooms
        List<String> suggestedRooms = new ArrayList<>();
        suggestedRooms.add(activity.getString(R.string.study_room));
        suggestedRooms.add(activity.getString(R.string.kids_room));
        suggestedRooms.add(activity.getString(R.string.library));
        suggestedRooms.add(activity.getString(R.string.balcony));
        suggestedRooms.add(activity.getString(R.string.workshop));
        suggestedRooms.add(activity.getString(R.string.rooftop));
        
        SuggestedRoomAdapter adapter = new SuggestedRoomAdapter(suggestedRooms, roomName -> {
            editTextRoomName.setText(roomName);
        });
        recyclerViewSuggested.setLayoutManager(new GridLayoutManager(activity, 3));
        recyclerViewSuggested.setAdapter(adapter);
        
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            String roomName = editTextRoomName.getText().toString().trim();
            if (!roomName.isEmpty()) {
                if (listener != null) {
                    listener.onRoomAdded(roomName);
                }
                dialog.dismiss();
            } else {
                Toast.makeText(activity, "Please enter a room name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
