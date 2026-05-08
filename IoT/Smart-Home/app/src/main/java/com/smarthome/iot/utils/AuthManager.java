package com.smarthome.iot.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.smarthome.iot.network.AuthInterceptor;

public class AuthManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_SAVED_EMAIL = "saved_email";
    private static final String KEY_AUTO_LOGIN_ENABLED = "auto_login_enabled";
    private static final String KEY_USER_NAME = "user_name";

    private Context context;
    private SharedPreferences prefs;

    public AuthManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
        AuthInterceptor.saveToken(context, token);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUserInfo(int userId, String email) {
        prefs.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public void saveUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    /**
     * Save email for auto-login on the same device
     */
    public void saveLoginEmail(String email) {
        prefs.edit()
                .putString(KEY_SAVED_EMAIL, email)
                .putBoolean(KEY_AUTO_LOGIN_ENABLED, true)
                .apply();
    }

    /**
     * Get saved email for auto-login
     */
    public String getSavedEmail() {
        return prefs.getString(KEY_SAVED_EMAIL, null);
    }

    /**
     * Check if auto-login is enabled
     */
    public boolean isAutoLoginEnabled() {
        return prefs.getBoolean(KEY_AUTO_LOGIN_ENABLED, false) && getSavedEmail() != null && isLoggedIn();
    }

    /**
     * Clear auto-login data (when user unchecks "Remember Me")
     */
    public void clearAutoLogin() {
        prefs.edit()
                .remove(KEY_SAVED_EMAIL)
                .putBoolean(KEY_AUTO_LOGIN_ENABLED, false)
                .apply();
    }

    public void logout() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_NAME)
                .remove(KEY_SAVED_EMAIL)
                .putBoolean(KEY_AUTO_LOGIN_ENABLED, false)
                .apply();
        AuthInterceptor.clearToken(context);
    }
}

