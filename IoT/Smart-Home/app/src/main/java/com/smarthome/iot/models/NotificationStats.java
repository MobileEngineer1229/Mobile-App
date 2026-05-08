package com.smarthome.iot.models;

import java.util.Map;

public class NotificationStats {
    private int total;
    private int unread;
    private Map<String, Integer> byType;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getUnread() {
        return unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }

    public Map<String, Integer> getByType() {
        return byType;
    }

    public void setByType(Map<String, Integer> byType) {
        this.byType = byType;
    }
}
