package com.smarthome.iot.models;

public class DataAnalyticsOption {
    private String title;
    private String subtitle;
    private String action;

    public DataAnalyticsOption() {
    }

    public DataAnalyticsOption(String title, String subtitle, String action) {
        this.title = title;
        this.subtitle = subtitle;
        this.action = action;
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
}

