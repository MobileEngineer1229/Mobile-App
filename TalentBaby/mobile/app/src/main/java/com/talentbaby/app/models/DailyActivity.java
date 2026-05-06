package com.talentbaby.app.models;

import com.google.gson.annotations.SerializedName;

public class DailyActivity {
    @SerializedName("daily_id")
    private int dailyId;

    @SerializedName("slot")
    private int slot;

    @SerializedName("is_completed")
    private boolean completed;

    @SerializedName("completed_at")
    private String completedAt;

    @SerializedName("activity")
    private Activity activity;

    public int getDailyId() { return dailyId; }
    public int getSlot() { return slot; }
    public boolean isCompleted() { return completed; }
    public String getCompletedAt() { return completedAt; }
    public Activity getActivity() { return activity; }
}
