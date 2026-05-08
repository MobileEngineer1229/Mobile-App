package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smarthome.iot.R;
import com.smarthome.iot.models.SecuritySetting;

import java.util.List;

public class SecurityToggleAdapter extends RecyclerView.Adapter<SecurityToggleAdapter.ViewHolder> {
    private List<SecuritySetting> securitySettings;
    private OnSecurityToggleListener listener;

    public interface OnSecurityToggleListener {
        void onSecurityToggle(SecuritySetting setting, boolean enabled);
    }

    public SecurityToggleAdapter(List<SecuritySetting> securitySettings, OnSecurityToggleListener listener) {
        this.securitySettings = securitySettings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_security_toggle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SecuritySetting setting = securitySettings.get(position);
        holder.textViewTitle.setText(getSecurityTitle(setting.getType(), holder.itemView.getContext()));
        holder.switchSecurity.setChecked(setting.isEnabled());

        holder.switchSecurity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setting.setEnabled(isChecked);
            if (listener != null) {
                listener.onSecurityToggle(setting, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return securitySettings != null ? securitySettings.size() : 0;
    }

    private String getSecurityTitle(String type, android.content.Context context) {
        switch (type) {
            case "biometric_id":
                return context.getString(R.string.biometric_id);
            case "face_id":
                return context.getString(R.string.face_id);
            case "sms_authenticator":
                return context.getString(R.string.sms_authenticator);
            case "google_authenticator":
                return context.getString(R.string.google_authenticator);
            default:
                return type;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textViewTitle;
        SwitchMaterial switchSecurity;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            switchSecurity = itemView.findViewById(R.id.switchSecurity);
            // Apply consistent switch styling
            if (switchSecurity != null) {
                com.smarthome.iot.utils.SwitchHelper.applySwitchStyle(itemView.getContext(), switchSecurity);
            }
        }
    }
}

