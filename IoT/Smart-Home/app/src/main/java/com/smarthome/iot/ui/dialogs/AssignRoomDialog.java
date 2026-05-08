package com.smarthome.iot.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.smarthome.iot.R;
import com.smarthome.iot.models.Room;
import java.util.ArrayList;
import java.util.List;

public class AssignRoomDialog extends DialogFragment {
    public interface OnRoomSelectedListener {
        void onRoomSelected(Room room);
        void onNoRoomSelected();
    }

    private List<Room> rooms;
    private OnRoomSelectedListener listener;
    private Room currentRoom;

    public static AssignRoomDialog newInstance(List<Room> rooms, Room currentRoom) {
        AssignRoomDialog dialog = new AssignRoomDialog();
        dialog.rooms = rooms != null ? new ArrayList<>(rooms) : new ArrayList<>();
        dialog.currentRoom = currentRoom;
        return dialog;
    }

    public void setOnRoomSelectedListener(OnRoomSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Create list of room names with "No Room" option
        List<String> roomNames = new ArrayList<>();
        roomNames.add("No Room");
        for (Room room : rooms) {
            roomNames.add(room.getName());
        }

        // Find current selection index
        int selectedIndex = 0;
        if (currentRoom != null) {
            for (int i = 0; i < rooms.size(); i++) {
                if (rooms.get(i).getId() == currentRoom.getId()) {
                    selectedIndex = i + 1; // +1 because "No Room" is at index 0
                    break;
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Assign to Room")
                .setSingleChoiceItems(
                        roomNames.toArray(new String[0]),
                        selectedIndex,
                        (dialog, which) -> {
                            if (which == 0) {
                                // "No Room" selected
                                if (listener != null) {
                                    listener.onNoRoomSelected();
                                }
                            } else {
                                // Room selected
                                Room selectedRoom = rooms.get(which - 1);
                                if (listener != null) {
                                    listener.onRoomSelected(selectedRoom);
                                }
                            }
                            dialog.dismiss();
                        })
                .setNegativeButton("Cancel", null);

        return builder.create();
    }
}

