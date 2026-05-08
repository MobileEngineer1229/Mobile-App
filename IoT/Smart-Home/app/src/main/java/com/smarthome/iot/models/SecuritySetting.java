package com.smarthome.iot.models;

public class SecuritySetting {
    private String type;
    private boolean enabled;

    public SecuritySetting() {
    }

    public SecuritySetting(String type, boolean enabled) {
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

