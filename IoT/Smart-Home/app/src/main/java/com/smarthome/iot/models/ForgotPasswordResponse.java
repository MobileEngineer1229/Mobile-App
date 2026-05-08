package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class ForgotPasswordResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("otpSent")
    private Boolean otpSent;

    @SerializedName("expiresInMinutes")
    private Integer expiresInMinutes;

    public ForgotPasswordResponse() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getOtpSent() {
        return otpSent;
    }

    public void setOtpSent(Boolean otpSent) {
        this.otpSent = otpSent;
    }

    public Integer getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public void setExpiresInMinutes(Integer expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }
}

