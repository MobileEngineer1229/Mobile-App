package com.smarthome.iot.models;

public class AccountAction {
    private String title;
    private String subtitle;
    private String action;
    private boolean isDangerous;

    public AccountAction() {
    }

    public AccountAction(String title, String subtitle, String action, boolean isDangerous) {
        this.title = title;
        this.subtitle = subtitle;
        this.action = action;
        this.isDangerous = isDangerous;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isDangerous() {
        return isDangerous;
    }

    public void setDangerous(boolean dangerous) {
        isDangerous = dangerous;
    }
}

