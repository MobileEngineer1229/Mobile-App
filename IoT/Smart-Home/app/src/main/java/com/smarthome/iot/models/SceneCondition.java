package com.smarthome.iot.models;

import java.io.Serializable;

public class SceneCondition implements Serializable {
    private Integer id;
    private String type; // "temperature", "humidity", "weather", "sunrise_sunset", "wind_speed", "location", "time", "device_status", "arm_mode", "alarm"
    private String operator; // "<", "=", ">", "dry", "comfortable", "moist", "sunny", "cloudy", "rainy", "snowy", "hazy", "sunrise", "sunset"
    private Double value;
    private String unit; // "celsius", "fahrenheit", "m/s", "km/h"
    private String location;
    private String deviceId;
    private String deviceStatus;
    private String armMode; // "disarmed", "arm_stay", "arm_away"

    public SceneCondition() {
    }

    public SceneCondition(String type, String operator, Double value, String location) {
        this.type = type;
        this.operator = operator;
        this.value = value;
        this.location = location;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceStatus() { return deviceStatus; }
    public void setDeviceStatus(String deviceStatus) { this.deviceStatus = deviceStatus; }

    public String getArmMode() { return armMode; }
    public void setArmMode(String armMode) { this.armMode = armMode; }

    public String getDisplayText() {
        switch (type) {
            case "tap_to_run":
                return "Launch Tap-to-Run";
            case "location_arrive_at":
                return "Arrive at";
            case "location_leave":
                return "Leave";
            case "schedule_time":
                if (value != null) {
                    int timeValue = value.intValue();
                    int hour = timeValue / 100;
                    int minute = timeValue % 100;
                    String period = hour >= 12 ? "PM" : "AM";
                    int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
                    return "Schedule Time: " + String.format("%d:%02d %s", displayHour, minute, period);
                }
                return "Schedule Time";
            case "temperature":
                return "Temperature: " + operator + " " + value + "°" + (unit != null ? unit.toUpperCase().charAt(0) : "C");
            case "humidity":
                return "Humidity: " + operator;
            case "weather":
                return "Weather: " + operator;
            case "sunrise_sunset":
                return "Sunrise / Sunset: " + operator;
            case "wind_speed":
                return "Wind Speed: " + operator + " " + value + " " + unit;
            default:
                return type + ": " + operator;
        }
    }
    
    public String getDescriptionText() {
        if ("tap_to_run".equals(type)) {
            return "One tap to execute commands";
        }
        if (type != null && type.startsWith("location_")) {
            return location != null ? location : "";
        }
        if ("schedule_time".equals(type)) {
            return operator != null ? operator.replace("_", " ") : "Every Day";
        }
        return location != null ? location : "";
    }
}
