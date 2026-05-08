package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class MonthlySummaryResponse {
    @SerializedName("thisMonth")
    private MonthlySummary thisMonth;

    @SerializedName("previousMonth")
    private MonthlySummary previousMonth;

    public MonthlySummaryResponse() {
    }

    public MonthlySummary getThisMonth() {
        return thisMonth;
    }

    public void setThisMonth(MonthlySummary thisMonth) {
        this.thisMonth = thisMonth;
    }

    public MonthlySummary getPreviousMonth() {
        return previousMonth;
    }

    public void setPreviousMonth(MonthlySummary previousMonth) {
        this.previousMonth = previousMonth;
    }
}

