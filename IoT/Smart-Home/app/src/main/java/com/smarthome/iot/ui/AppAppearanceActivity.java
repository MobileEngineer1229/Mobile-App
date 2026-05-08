package com.smarthome.iot.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.LocaleHelper;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppAppearanceActivity extends AppCompatActivity {
    private TextView textViewThemeValue;
    private TextView textViewLanguageValue;
    private LinearLayout layoutTheme;
    private LinearLayout layoutLanguage;
    
    private ApiService apiService;
    private AuthManager authManager;
    private SharedPreferences prefs;
    
    private String currentTheme = "system";
    private String currentLanguage = "en_US";

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply saved locale
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_appearance);

        ApiClient.initialize(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        authManager = new AuthManager(this);
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        initializeViews();
        setupClickListeners();
        loadAppearanceSettings();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        textViewThemeValue = findViewById(R.id.textViewThemeValue);
        textViewLanguageValue = findViewById(R.id.textViewLanguageValue);
        layoutTheme = findViewById(R.id.layoutTheme);
        layoutLanguage = findViewById(R.id.layoutLanguage);
    }

    private void setupClickListeners() {
        layoutTheme.setOnClickListener(v -> showThemeDialog());
        layoutLanguage.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppLanguageActivity.class);
            intent.putExtra("currentLanguage", currentLanguage);
            startActivityForResult(intent, 100);
        });
    }

    private void showThemeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_choose_theme);
        dialog.setCancelable(true);

        RadioGroup radioGroupTheme = dialog.findViewById(R.id.radioGroupTheme);
        RadioButton radioSystemDefault = dialog.findViewById(R.id.radioSystemDefault);
        RadioButton radioLight = dialog.findViewById(R.id.radioLight);
        RadioButton radioDark = dialog.findViewById(R.id.radioDark);
        MaterialButton buttonCancel = dialog.findViewById(R.id.buttonCancel);
        MaterialButton buttonOk = dialog.findViewById(R.id.buttonOk);

        // Set current selection
        switch (currentTheme) {
            case "system":
                radioSystemDefault.setChecked(true);
                break;
            case "light":
                radioLight.setChecked(true);
                break;
            case "dark":
                radioDark.setChecked(true);
                break;
        }

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonOk.setOnClickListener(v -> {
            int selectedId = radioGroupTheme.getCheckedRadioButtonId();
            String newTheme = "system";
            String themeDisplay = getString(R.string.system_default);
            
            if (selectedId == R.id.radioLight) {
                newTheme = "light";
                themeDisplay = getString(R.string.light);
            } else if (selectedId == R.id.radioDark) {
                newTheme = "dark";
                themeDisplay = getString(R.string.dark);
            }
            
            updateTheme(newTheme, themeDisplay);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateTheme(String theme, String themeDisplay) {
        currentTheme = theme;
        textViewThemeValue.setText(themeDisplay);
        
        // Save locally
        prefs.edit().putString("app_theme", theme).apply();
        
        // Apply theme
        switch (theme) {
            case "light":
                ThemeHelper.applyTheme(ThemeHelper.THEME_LIGHT);
                break;
            case "dark":
                ThemeHelper.applyTheme(ThemeHelper.THEME_DARK);
                break;
            default:
                ThemeHelper.applyTheme(ThemeHelper.THEME_SYSTEM);
                break;
        }
        
        // Save to backend
        if (authManager.isLoggedIn()) {
            Map<String, Object> settings = new HashMap<>();
            settings.put("theme", theme);
            
            Call<ApiResponse<Map<String, Object>>> call = apiService.updateAppAppearance(settings);
            call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                    // Theme updated
                }

                @Override
                public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                    // Handle error
                }
            });
        }
        
        // Recreate activity to apply theme
        recreate();
    }

    private void loadAppearanceSettings() {
        // Load from local preferences first
        currentTheme = prefs.getString("app_theme", "system");
        currentLanguage = LocaleHelper.getSavedLanguage(this);
        
        updateThemeDisplay();
        updateLanguageDisplay();
        
        if (!authManager.isLoggedIn()) {
            return;
        }

        Call<ApiResponse<Map<String, Object>>> call = apiService.getAppAppearance();
        call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> data = response.body().getData();
                    if (data != null) {
                        if (data.containsKey("theme")) {
                            currentTheme = (String) data.get("theme");
                            prefs.edit().putString("app_theme", currentTheme).apply();
                        }
                        if (data.containsKey("language")) {
                            currentLanguage = (String) data.get("language");
                            prefs.edit().putString("app_language", currentLanguage).apply();
                        }
                        updateThemeDisplay();
                        updateLanguageDisplay();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                // Use local preferences
            }
        });
    }

    private void updateThemeDisplay() {
        String themeDisplay;
        switch (currentTheme) {
            case "light":
                themeDisplay = getString(R.string.light);
                break;
            case "dark":
                themeDisplay = getString(R.string.dark);
                break;
            default:
                themeDisplay = getString(R.string.system_default);
                break;
        }
        textViewThemeValue.setText(themeDisplay);
    }

    private void updateLanguageDisplay() {
        // Map language code to display name
        String languageDisplay = getLanguageDisplayName(currentLanguage);
        textViewLanguageValue.setText(languageDisplay);
    }

    private String getLanguageDisplayName(String code) {
        switch (code) {
            case "en_US":
                return getString(R.string.english_us);
            case "ko_KR":
                return getString(R.string.korean);
            case "en_GB":
                return getString(R.string.english_uk);
            case "zh_CN":
                return getString(R.string.mandarin);
            case "es_ES":
                return getString(R.string.spanish);
            case "hi_IN":
                return getString(R.string.hindi);
            case "fr_FR":
                return getString(R.string.french);
            case "ar_SA":
                return getString(R.string.arabic);
            case "ru_RU":
                return getString(R.string.russian);
            case "ja_JP":
                return getString(R.string.japanese);
            default:
                return getString(R.string.english_us);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String selectedLanguage = data.getStringExtra("selectedLanguage");
            if (selectedLanguage != null) {
                currentLanguage = selectedLanguage;
                
                // Save language preference
                LocaleHelper.saveLanguage(this, currentLanguage);
                prefs.edit().putString("app_language", currentLanguage).apply();
                
                // Apply locale immediately
                LocaleHelper.setLocale(this, currentLanguage);
                
                updateLanguageDisplay();
                
                // Save to backend
                if (authManager.isLoggedIn()) {
                    Map<String, Object> settings = new HashMap<>();
                    settings.put("language", currentLanguage);
                    
                    Call<ApiResponse<Map<String, Object>>> call = apiService.updateAppAppearance(settings);
                    call.enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                            // Language updated
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                            // Handle error
                        }
                    });
                }
                
                // Recreate activity to apply new locale
                recreate();
            }
        }
    }
}

