package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class MonthlySummary {
    @SerializedName("month")
    private String month;

    @SerializedName("consumptionKwh")
    private Double consumptionKwh;

    @SerializedName("costUsd")
    private Double costUsd;

    public MonthlySummary() {
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Double getConsumptionKwh() {
        return consumptionKwh;
    }

    public void setConsumptionKwh(Double consumptionKwh) {
        this.consumptionKwh = consumptionKwh;
    }

    public Double getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(Double costUsd) {
        this.costUsd = costUsd;
    }
}

