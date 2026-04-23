package com.talentbaby.app;

import android.app.Application;
import android.content.Context;

public class TalentBabyApplication extends Application {
    private static TalentBabyApplication instance;
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        appContext = getApplicationContext();
    }

    public static TalentBabyApplication getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return appContext;
    }
}
