package com.smarthome.iot.models;

public class HelpSupportItem {
    private String title;
    private int iconResId;
    private String action;

    public HelpSupportItem() {
    }

    public HelpSupportItem(String title, int iconResId, String action) {
        this.title = title;
        this.iconResId = iconResId;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}

