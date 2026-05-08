package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class NotificationPreference {
    @SerializedName("type")
    private String type;

    @SerializedName("enabled")
    private boolean enabled;

    public NotificationPreference() {
    }

    public NotificationPreference(String type, boolean enabled) {
        this.type = type;
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

