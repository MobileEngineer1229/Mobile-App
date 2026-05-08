package com.smarthome.iot;

import android.app.Application;
import android.content.Context;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.HealthCheckService;
import com.smarthome.iot.utils.LocaleHelper;

public class App extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        // Apply saved locale before attaching base context
        super.attachBaseContext(LocaleHelper.applySavedLocale(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize API client with application context
        ApiClient.initialize(this);
        
        // Check API health on app start
        android.util.Log.d("App", "Checking API health on app start...");
        HealthCheckService.getInstance().checkHealth();
    }
}

