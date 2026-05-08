package com.smarthome.iot.models;

import java.util.Date;
import java.util.Map;

public class Notification {
    private int id;
    private int userId;
    private String title;
    private String message;
    private String type; // "general", "security", "system", "feature", "reminder", "alert", "info"
    private String icon;
    private boolean isRead;
    private Date readAt;
    private Map<String, Object> metadata;
    private Date createdAt;
    
    // UI helper fields (for display purposes)
    private String category; // "general" or "smart_home" - derived from type
    private String iconType; // "security", "update", "password", etc. - derived from type/icon
    private String badge; // "NEW", "LOCK", etc. - derived from metadata
    private String timestamp; // Formatted timestamp for display

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Date getReadAt() {
        return readAt;
    }

    public void setReadAt(Date readAt) {
        this.readAt = readAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    // UI helper methods
    public String getCategory() {
        if (category != null) return category;
        // Derive category from type
        if (type != null) {
            if (type.equals("security") || type.equals("alert")) {
                return "smart_home";
            }
        }
        return "general";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIconType() {
        if (iconType != null) return iconType;
        // Derive iconType from type
        if (type != null) {
            switch (type) {
                case "security":
                    return "security";
                case "system":
                    return "update";
                case "feature":
                    return "feature";
                case "reminder":
                    return "event";
                default:
                    return "info";
            }
        }
        return "info";
    }

    public void setIconType(String iconType) {
        this.iconType = iconType;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
