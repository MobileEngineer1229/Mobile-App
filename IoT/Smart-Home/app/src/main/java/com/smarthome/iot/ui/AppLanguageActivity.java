package com.smarthome.iot.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.Language;
import com.smarthome.iot.ui.adapters.LanguageAdapter;
import com.smarthome.iot.utils.LocaleHelper;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class AppLanguageActivity extends AppCompatActivity {
    private RecyclerView recyclerViewLanguages;
    private LanguageAdapter adapter;
    private String currentLanguageCode;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply saved locale
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_language);

        currentLanguageCode = getIntent().getStringExtra("currentLanguage");
        if (currentLanguageCode == null) {
            currentLanguageCode = "en_US";
        }

        initializeViews();
        setupRecyclerView();
        loadLanguages();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerViewLanguages = findViewById(R.id.recyclerViewLanguages);
    }

    private void setupRecyclerView() {
        recyclerViewLanguages.setLayoutManager(new LinearLayoutManager(this));
        // Don't create adapter here - will be created in loadLanguages()
    }

    private void onLanguageClick(Language language) {
        android.util.Log.d("AppLanguageActivity", "Language clicked: " + language.getCode() + " - " + language.getName());
        currentLanguageCode = language.getCode();
        adapter.setSelectedLanguage(currentLanguageCode);
        
        // Return selected language to previous activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedLanguage", currentLanguageCode);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void loadLanguages() {
        List<Language> languages = new ArrayList<>();
        languages.add(new Language("en_US", getString(R.string.english_us), R.drawable.ic_google)); // Placeholder flag
        languages.add(new Language("ko_KR", getString(R.string.korean), R.drawable.ic_google));
        languages.add(new Language("en_GB", getString(R.string.english_uk), R.drawable.ic_google));
        languages.add(new Language("zh_CN", getString(R.string.mandarin), R.drawable.ic_google));
        languages.add(new Language("es_ES", getString(R.string.spanish), R.drawable.ic_google));
        languages.add(new Language("hi_IN", getString(R.string.hindi), R.drawable.ic_google));
        languages.add(new Language("fr_FR", getString(R.string.french), R.drawable.ic_google));
        languages.add(new Language("ar_SA", getString(R.string.arabic), R.drawable.ic_google));
        languages.add(new Language("ru_RU", getString(R.string.russian), R.drawable.ic_google));
        languages.add(new Language("ja_JP", getString(R.string.japanese), R.drawable.ic_google));

        // Create adapter only once with the languages list
        adapter = new LanguageAdapter(languages, currentLanguageCode, this::onLanguageClick);
        recyclerViewLanguages.setAdapter(adapter);
        
        android.util.Log.d("AppLanguageActivity", "Loaded " + languages.size() + " languages, current: " + currentLanguageCode);
    }
}

