package com.smarthome.iot.ui.dialogs;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;

public class DeleteRoomDialog {
    private BottomSheetDialog dialog;
    private OnDeleteListener listener;

    public interface OnDeleteListener {
        void onDelete();
    }

    public DeleteRoomDialog(Activity activity, String roomName, OnDeleteListener listener) {
        this.listener = listener;
        
        View bottomSheetView = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_delete_room, null);
        dialog = new BottomSheetDialog(activity);
        dialog.setContentView(bottomSheetView);
        
        TextView textViewMessage = bottomSheetView.findViewById(R.id.textViewMessage);
        MaterialButton buttonCancel = bottomSheetView.findViewById(R.id.buttonCancel);
        MaterialButton buttonDelete = bottomSheetView.findViewById(R.id.buttonDelete);
        
        textViewMessage.setText(activity.getString(R.string.delete_room_confirmation, roomName));
        
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete();
            }
            dialog.dismiss();
        });
    }

    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
