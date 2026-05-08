package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StatisticsResponse {
    @SerializedName("dateRange")
    private String dateRange;

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("data")
    private List<StatisticsDataPoint> data;

    public StatisticsResponse() {
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<StatisticsDataPoint> getData() {
        return data;
    }

    public void setData(List<StatisticsDataPoint> data) {
        this.data = data;
    }
}

