package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class VerifyOTPResponse {
    @SerializedName("valid")
    private Boolean valid;

    @SerializedName("message")
    private String message;

    public VerifyOTPResponse() {
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

