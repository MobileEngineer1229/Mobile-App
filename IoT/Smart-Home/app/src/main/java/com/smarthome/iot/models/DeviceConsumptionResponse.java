package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DeviceConsumptionResponse {
    @SerializedName("devices")
    private List<DeviceConsumptionSummary> devices;

    @SerializedName("totalConsumptionKwh")
    private Double totalConsumptionKwh;

    @SerializedName("totalCostUsd")
    private Double totalCostUsd;

    public DeviceConsumptionResponse() {
    }

    public List<DeviceConsumptionSummary> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceConsumptionSummary> devices) {
        this.devices = devices;
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
}

