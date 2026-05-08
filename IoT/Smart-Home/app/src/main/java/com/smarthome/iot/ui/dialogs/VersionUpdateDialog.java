package com.smarthome.iot.ui.dialogs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.smarthome.iot.R;
import com.smarthome.iot.models.VersionCheckResponse;

public class VersionUpdateDialog {
    private final Activity activity;
    private AlertDialog dialog;

    public VersionUpdateDialog(Activity activity) {
        this.activity = activity;
    }

    public void show(VersionCheckResponse versionResponse) {
        if (versionResponse == null) {
            return;
        }

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_version_update, null);

        TextView textViewTitle = dialogView.findViewById(R.id.textViewTitle);
        TextView textViewMessage = dialogView.findViewById(R.id.textViewMessage);
        TextView textViewReleaseNotes = dialogView.findViewById(R.id.textViewReleaseNotes);
        android.widget.Button buttonLater = dialogView.findViewById(R.id.buttonLater);
        android.widget.Button buttonUpdate = dialogView.findViewById(R.id.buttonUpdate);

        // Set title based on whether update is required
        if (versionResponse.isUpdateRequired()) {
            textViewTitle.setText(activity.getString(R.string.update_required));
            textViewMessage.setText(versionResponse.getMessage() != null 
                ? versionResponse.getMessage() 
                : activity.getString(R.string.update_required_message));
            // Hide "Later" button if update is required
            buttonLater.setVisibility(View.GONE);
        } else {
            textViewTitle.setText(activity.getString(R.string.update_available));
            textViewMessage.setText(versionResponse.getMessage() != null 
                ? versionResponse.getMessage() 
                : activity.getString(R.string.update_message));
            buttonLater.setVisibility(View.VISIBLE);
        }

        // Show release notes if available
        if (versionResponse.getReleaseNotes() != null && !versionResponse.getReleaseNotes().isEmpty()) {
            textViewReleaseNotes.setText(versionResponse.getReleaseNotes());
            textViewReleaseNotes.setVisibility(View.VISIBLE);
        }

        // Update button click
        buttonUpdate.setOnClickListener(v -> {
            openUpdateUrl(versionResponse.getUpdateUrl());
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        // Later button click (only if update is not required)
        buttonLater.setOnClickListener(v -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);
        builder.setCancelable(!versionResponse.isUpdateRequired()); // Can't cancel if update is required

        dialog = builder.create();
        dialog.show();
    }

    private void openUpdateUrl(String updateUrl) {
        if (updateUrl == null || updateUrl.isEmpty()) {
            // Default to Play Store if no URL provided
            String packageName = activity.getPackageName();
            updateUrl = "https://play.google.com/store/apps/details?id=" + packageName;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
            activity.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("VersionUpdateDialog", "Error opening update URL", e);
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
