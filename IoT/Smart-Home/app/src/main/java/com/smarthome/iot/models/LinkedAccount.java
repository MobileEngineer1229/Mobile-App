package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class LinkedAccount {
    @SerializedName("id")
    private int id;

    @SerializedName("provider")
    private String provider;

    @SerializedName("isConnected")
    private boolean isConnected;

    @SerializedName("iconResId")
    private int iconResId; // Set on client side

    public LinkedAccount() {
    }

    public LinkedAccount(int id, String provider, boolean isConnected) {
        this.id = id;
        this.provider = provider;
        this.isConnected = isConnected;
        this.iconResId = getIconResIdByProvider(provider);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
        this.iconResId = getIconResIdByProvider(provider);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    private int getIconResIdByProvider(String provider) {
        if (provider == null) {
            return com.smarthome.iot.R.drawable.ic_google;
        }
        
        switch (provider.toLowerCase()) {
            case "google":
                return com.smarthome.iot.R.drawable.ic_google;
            case "apple":
                return com.smarthome.iot.R.drawable.ic_apple;
            case "facebook":
                return com.smarthome.iot.R.drawable.ic_facebook;
            case "twitter":
                return com.smarthome.iot.R.drawable.ic_twitter;
            default:
                return com.smarthome.iot.R.drawable.ic_google;
        }
    }
}

