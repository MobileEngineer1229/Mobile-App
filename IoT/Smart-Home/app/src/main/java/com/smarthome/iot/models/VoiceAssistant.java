package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class VoiceAssistant {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("isLinked")
    private boolean isLinked;

    @SerializedName("iconResId")
    private int iconResId; // This will be set on the client side based on name

    public VoiceAssistant() {
    }

    public VoiceAssistant(int id, String name, boolean isLinked) {
        this.id = id;
        this.name = name;
        this.isLinked = isLinked;
        this.iconResId = getIconResIdByName(name);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.iconResId = getIconResIdByName(name);
    }

    public boolean isLinked() {
        return isLinked;
    }

    public void setLinked(boolean linked) {
        isLinked = linked;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    private int getIconResIdByName(String name) {
        if (name == null) {
            return com.smarthome.iot.R.drawable.ic_google_assistant;
        }
        
        switch (name.toLowerCase()) {
            case "google assistant":
                return com.smarthome.iot.R.drawable.ic_google_assistant;
            case "amazon alexa":
                return com.smarthome.iot.R.drawable.ic_alexa;
            case "microsoft cortana":
                return com.smarthome.iot.R.drawable.ic_cortana;
            case "samsung bixby":
                return com.smarthome.iot.R.drawable.ic_bixby;
            case "naver clova":
                return com.smarthome.iot.R.drawable.ic_clova;
            case "apple siri":
                return com.smarthome.iot.R.drawable.ic_siri;
            default:
                return com.smarthome.iot.R.drawable.ic_google_assistant;
        }
    }
}

