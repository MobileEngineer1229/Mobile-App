package com.smarthome.iot.models;

public class AdditionalSetting {
    private String title;
    private String value;
    private String action;

    public AdditionalSetting() {
    }

    public AdditionalSetting(String title, String value, String action) {
        this.title = title;
        this.value = value;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}

