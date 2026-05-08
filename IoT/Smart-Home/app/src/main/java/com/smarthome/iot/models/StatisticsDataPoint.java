package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class StatisticsDataPoint {
    @SerializedName("period")
    private String period;

    @SerializedName("consumptionKwh")
    private Double consumptionKwh;

    @SerializedName("costUsd")
    private Double costUsd;

    public StatisticsDataPoint() {
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
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

