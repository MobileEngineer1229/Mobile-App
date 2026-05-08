package com.smarthome.iot.models;

public class DiscoveredDevice {
    private String name;
    private String type;
    private int iconResId;
    private boolean isSelected;

    public DiscoveredDevice(String name, String type, int iconResId) {
        this.name = name;
        this.type = type;
        this.iconResId = iconResId;
        this.isSelected = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}

