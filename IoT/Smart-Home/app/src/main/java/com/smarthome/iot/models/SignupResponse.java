package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

/**
 * Signup response model that matches backend API response
 * Backend returns: { success: true, data: { user: {...}, token: "..." } }
 */
public class SignupResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private SignupData data;

    @SerializedName("error")
    private ApiError error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public SignupData getData() {
        return data;
    }

    public void setData(SignupData data) {
        this.data = data;
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    public static class SignupData {
        @SerializedName("user")
        private User user;

        @SerializedName("token")
        private String token;

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class ApiError {
        @SerializedName("code")
        private String code;

        @SerializedName("message")
        private String message;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
