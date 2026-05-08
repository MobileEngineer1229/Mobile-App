package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

public class VersionCheckResponse {
    @SerializedName("currentVersion")
    private VersionInfo currentVersion;

    @SerializedName("minimumRequiredVersion")
    private String minimumRequiredVersion;

    @SerializedName("updateAvailable")
    private boolean updateAvailable;

    @SerializedName("updateRequired")
    private boolean updateRequired;

    @SerializedName("updateUrl")
    private String updateUrl;

    @SerializedName("releaseNotes")
    private String releaseNotes;

    @SerializedName("message")
    private String message;

    public static class VersionInfo {
        @SerializedName("versionName")
        private String versionName;

        @SerializedName("versionCode")
        private int versionCode;

        public String getVersionName() {
            return versionName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }

        public int getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }
    }

    public VersionInfo getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(VersionInfo currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getMinimumRequiredVersion() {
        return minimumRequiredVersion;
    }

    public void setMinimumRequiredVersion(String minimumRequiredVersion) {
        this.minimumRequiredVersion = minimumRequiredVersion;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public void setUpdateAvailable(boolean updateAvailable) {
        this.updateAvailable = updateAvailable;
    }

    public boolean isUpdateRequired() {
        return updateRequired;
    }

    public void setUpdateRequired(boolean updateRequired) {
        this.updateRequired = updateRequired;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(String releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
