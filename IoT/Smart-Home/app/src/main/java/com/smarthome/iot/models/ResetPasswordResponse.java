package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordResponse {
    @SerializedName("success")
    private Boolean success;

    @SerializedName("message")
    private String message;

    public ResetPasswordResponse() {
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

