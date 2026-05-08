package com.smarthome.iot.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String DEFAULT_LANGUAGE = "en_US";

    /**
     * Set the app locale based on language code
     * @param context Application context
     * @param languageCode Language code (e.g., "en_US", "ko_KR")
     * @return Context with updated locale
     */
    public static Context setLocale(Context context, String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            languageCode = DEFAULT_LANGUAGE;
        }

        // Save language preference
        saveLanguage(context, languageCode);

        // Apply locale
        return updateResources(context, languageCode);
    }

    /**
     * Get the saved language code
     */
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    /**
     * Save language preference
     */
    public static void saveLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    /**
     * Apply saved locale to context
     */
    public static Context applySavedLocale(Context context) {
        String languageCode = getSavedLanguage(context);
        return updateResources(context, languageCode);
    }

    /**
     * Update resources with new locale
     */
    private static Context updateResources(Context context, String languageCode) {
        Locale locale = getLocaleFromCode(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }

    /**
     * Convert language code to Locale object
     */
    private static Locale getLocaleFromCode(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return Locale.US; // Default to English US
        }

        // Parse language code (e.g., "en_US" -> language="en", country="US")
        String[] parts = languageCode.split("_");
        String language = parts[0].toLowerCase();
        String country = parts.length > 1 ? parts[1].toUpperCase() : "";

        if (country.isEmpty()) {
            return new Locale(language);
        } else {
            return new Locale(language, country);
        }
    }

    /**
     * Get display name for language code
     */
    public static String getLanguageDisplayName(Context context, String languageCode) {
        Locale locale = getLocaleFromCode(languageCode);
        return locale.getDisplayName(locale);
    }
}
