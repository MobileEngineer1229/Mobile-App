package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class VerifyOTPRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("otpCode")
    private String otpCode;

    public VerifyOTPRequest() {
    }

    public VerifyOTPRequest(String email, String otpCode) {
        this.email = email;
        this.otpCode = otpCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}

