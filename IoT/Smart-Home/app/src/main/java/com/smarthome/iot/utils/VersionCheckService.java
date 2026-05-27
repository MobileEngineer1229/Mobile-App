package com.smarthome.iot.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.VersionCheckResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VersionCheckService {
    private static final String TAG = "VersionCheckService";
    private final Context context;
    private final ApiService apiService;

    public interface VersionCheckCallback {
        void onVersionCheckComplete(VersionCheckResponse response);
        void onVersionCheckError(String error);
    }

    public VersionCheckService(Context context) {
        this.context = context;
        this.apiService = ApiClient.getApiService();
    }

    /**
     * Get current app version info
     */
    public AppVersionInfo getCurrentVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(),
                0
            );
            return new AppVersionInfo(
                packageInfo.versionName != null ? packageInfo.versionName : "1.0",
                packageInfo.versionCode
            );
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version", e);
            return new AppVersionInfo("1.0", 1);
        }
    }

    /**
     * Check app version with server
     */
    public void checkVersion(VersionCheckCallback callback) {
        AppVersionInfo currentVersion = getCurrentVersion();

        Map<String, Object> versionData = new HashMap<>();
        versionData.put("platform", "android");
        versionData.put("versionName", currentVersion.versionName);
        versionData.put("versionCode", currentVersion.versionCode);

        Call<ApiResponse<VersionCheckResponse>> call = apiService.checkAppVersion(versionData);
        call.enqueue(new Callback<ApiResponse<VersionCheckResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<VersionCheckResponse>> call, Response<ApiResponse<VersionCheckResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    VersionCheckResponse versionResponse = response.body().getData();
                    if (versionResponse != null) {
                        callback.onVersionCheckComplete(versionResponse);
                    } else {
                        callback.onVersionCheckError("Invalid response data");
                    }
                } else {
                    String errorMessage = "Failed to check version";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMessage = response.body().getError().getMessage();
                    }
                    callback.onVersionCheckError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VersionCheckResponse>> call, Throwable t) {
                Log.e(TAG, "Version check failed", t);
                callback.onVersionCheckError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /**
     * App version info class
     */
    public static class AppVersionInfo {
        public final String versionName;
        public final int versionCode;

        public AppVersionInfo(String versionName, int versionCode) {
            this.versionName = versionName;
            this.versionCode = versionCode;
        }
    }
}
