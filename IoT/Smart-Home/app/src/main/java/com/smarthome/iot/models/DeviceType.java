package com.smarthome.iot.models;

public class DeviceType {
    private String name;
    private int iconResId;
    private String category;

    public DeviceType(String name, int iconResId, String category) {
        this.name = name;
        this.iconResId = iconResId;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getCategory() {
        return category;
    }
}

