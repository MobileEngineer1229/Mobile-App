package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class DeviceConsumptionSummary {
    @SerializedName("deviceId")
    private Integer deviceId;

    @SerializedName("deviceName")
    private String deviceName;

    @SerializedName("deviceType")
    private String deviceType;

    @SerializedName("roomId")
    private Integer roomId;

    @SerializedName("roomName")
    private String roomName;

    @SerializedName("totalConsumptionKwh")
    private Double totalConsumptionKwh;

    @SerializedName("totalCostUsd")
    private Double totalCostUsd;

    @SerializedName("deviceCount")
    private Integer deviceCount;

    public DeviceConsumptionSummary() {
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Double getTotalConsumptionKwh() {
        return totalConsumptionKwh;
    }

    public void setTotalConsumptionKwh(Double totalConsumptionKwh) {
        this.totalConsumptionKwh = totalConsumptionKwh;
    }

    public Double getTotalCostUsd() {
        return totalCostUsd;
    }

    public void setTotalCostUsd(Double totalCostUsd) {
        this.totalCostUsd = totalCostUsd;
    }

    public Integer getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(Integer deviceCount) {
        this.deviceCount = deviceCount;
    }
}

