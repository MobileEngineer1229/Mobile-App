package com.smarthome.iot.ui.dialogs;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.smarthome.iot.R;

public class SuccessDialog {
    private BottomSheetDialog dialog;

    public SuccessDialog(Activity activity, String message) {
        View bottomSheetView = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_success, null);
        dialog = new BottomSheetDialog(activity);
        dialog.setContentView(bottomSheetView);
        
        TextView textViewMessage = bottomSheetView.findViewById(R.id.textViewMessage);
        if (textViewMessage != null) {
            textViewMessage.setText(message);
        }
        
        // Auto-dismiss after 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 2000);
    }

    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}
