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
import com.smarthome.iot.models.NotificationPreference;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<NotificationPreference> notifications;
    private OnNotificationToggleListener listener;

    public interface OnNotificationToggleListener {
        void onNotificationToggle(NotificationPreference preference, boolean enabled);
    }

    public NotificationAdapter(List<NotificationPreference> notifications, OnNotificationToggleListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_setting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationPreference preference = notifications.get(position);
        holder.textViewTitle.setText(getNotificationTitle(preference.getType(), holder.itemView.getContext()));
        holder.switchNotification.setChecked(preference.isEnabled());

        holder.switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preference.setEnabled(isChecked);
            if (listener != null) {
                listener.onNotificationToggle(preference, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    private String getNotificationTitle(String type) {
        // This will be set in onBindViewHolder using context
        return type;
    }

    public String getNotificationTitle(String type, android.content.Context context) {
        switch (type) {
            case "device_status_alerts":
                return context.getString(R.string.device_status_alerts);
            case "energy_consumption_alerts":
                return context.getString(R.string.energy_consumption_alerts);
            case "bill_reminders":
                return context.getString(R.string.bill_reminders);
            case "automation_updates":
                return context.getString(R.string.automation_updates);
            case "device_maintenance_reminders":
                return context.getString(R.string.device_maintenance_reminders);
            case "security_alerts":
                return context.getString(R.string.security_alerts);
            case "weather_based_suggestions":
                return context.getString(R.string.weather_based_suggestions);
            case "community_updates":
                return context.getString(R.string.community_updates);
            case "home_invitations":
                return context.getString(R.string.home_invitations);
            case "user_access_alerts":
                return context.getString(R.string.user_access_alerts);
            case "customer_support_updates":
                return context.getString(R.string.customer_support_updates);
            case "feedback_updates":
                return context.getString(R.string.feedback_updates);
            default:
                return type;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textViewTitle;
        SwitchMaterial switchNotification;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            switchNotification = itemView.findViewById(R.id.switchNotification);
            // Apply consistent switch styling
            if (switchNotification != null) {
                com.smarthome.iot.utils.SwitchHelper.applySwitchStyle(itemView.getContext(), switchNotification);
            }
        }
    }
}

