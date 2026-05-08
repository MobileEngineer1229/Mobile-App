package com.smarthome.iot.ui.dialogs;

import android.app.Activity;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;

public class DeleteHomeDialog {
    private BottomSheetDialog dialog;
    private OnDeleteListener listener;

    public interface OnDeleteListener {
        void onDelete();
    }

    public DeleteHomeDialog(Activity activity, OnDeleteListener listener) {
        this.listener = listener;
        
        View bottomSheetView = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_delete_home, null);
        dialog = new BottomSheetDialog(activity);
        dialog.setContentView(bottomSheetView);
        
        MaterialButton buttonCancel = bottomSheetView.findViewById(R.id.buttonCancel);
        MaterialButton buttonDelete = bottomSheetView.findViewById(R.id.buttonDelete);
        
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
