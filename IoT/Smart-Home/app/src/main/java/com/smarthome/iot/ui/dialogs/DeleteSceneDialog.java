package com.smarthome.iot.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.smarthome.iot.R;

public class DeleteSceneDialog extends BottomSheetDialog {
    private String sceneName;
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete();
    }

    public DeleteSceneDialog(@NonNull Context context, String sceneName, OnDeleteListener deleteListener) {
        super(context);
        this.sceneName = sceneName;
        this.deleteListener = deleteListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_delete_scene);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView textViewMessage = findViewById(R.id.textViewMessage);
        MaterialButton buttonCancel = findViewById(R.id.buttonCancel);
        MaterialButton buttonDelete = findViewById(R.id.buttonDelete);

        if (textViewMessage != null) {
            String message = getContext().getString(R.string.delete_scene_confirmation, sceneName);
            textViewMessage.setText(message);
        }

        if (buttonCancel != null) {
            buttonCancel.setOnClickListener(v -> dismiss());
        }

        if (buttonDelete != null) {
            buttonDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete();
                }
                dismiss();
            });
        }
    }
}
