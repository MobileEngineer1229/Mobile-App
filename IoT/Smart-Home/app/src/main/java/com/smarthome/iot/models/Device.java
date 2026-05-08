package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Device {
    @SerializedName("id")
    private Integer id;

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private String type;

    @SerializedName("status")
    private String status;

    @SerializedName("room_id")
    private Integer roomId;

    @SerializedName("mac_address")
    private String macAddress;

    @SerializedName("ip_address")
    private String ipAddress;

    @SerializedName("metadata")
    private Map<String, Object> metadata;

    // UI state (not from API)
    private boolean isOn;
    private boolean isOnline;

    public Device() {
    }

    public Device(Integer id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.isOn = false;
        this.isOnline = false;
        this.status = "offline";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        // Update isOnline based on status
        if (status != null) {
            this.isOnline = "online".equalsIgnoreCase(status);
        } else {
            this.isOnline = false;
        }
    }

    // Helper method to sync status with isOnline
    public void syncStatus() {
        if (status != null) {
            this.isOnline = "online".equalsIgnoreCase(status);
        } else {
            this.isOnline = false;
            this.status = "offline";
        }
    }

    // Helper method to sync power state from metadata
    public void syncPowerFromMetadata() {
        if (metadata != null && metadata.containsKey("power")) {
            Object powerValue = metadata.get("power");
            if (powerValue instanceof Boolean) {
                this.isOn = (Boolean) powerValue;
            } else if (powerValue instanceof String) {
                this.isOn = "true".equalsIgnoreCase((String) powerValue);
            }
        }
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

