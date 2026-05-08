package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class HealthResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("environment")
    private String environment;

    @SerializedName("mqtt")
    private String mqtt;

    public HealthResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getMqtt() {
        return mqtt;
    }

    public void setMqtt(String mqtt) {
        this.mqtt = mqtt;
    }

    public boolean isHealthy() {
        return "ok".equalsIgnoreCase(status);
    }

    public boolean isMqttConnected() {
        return "connected".equalsIgnoreCase(mqtt);
    }
}
