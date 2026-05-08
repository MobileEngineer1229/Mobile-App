package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class OnboardingStep1Activity extends AppCompatActivity {

    private ImageButton buttonBack;
    private TextView textViewProgress;
    private TextView textViewTitle;
    private TextView textViewSubtitle;
    private TextInputEditText editTextSearch;
    private ListView listViewCountries;
    private MaterialButton buttonSkip;
    private MaterialButton buttonContinue;

    private List<String> allCountries;
    private List<String> filteredCountries;
    private ArrayAdapter<String> adapter;
    private String selectedCountry = null;
    private int userId;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_step1);

        // Set status bar color to dark
        setStatusBarColor();

        // Get data from previous activity
        userId = getIntent().getIntExtra("userId", -1);
        email = getIntent().getStringExtra("email");

        initializeViews();
        setupTitleText();
        setupCountriesList();
        setupListeners();
        setupSearch();
    }
    
    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }
    
    private void setupTitleText() {
        String fullText = getString(R.string.select_country_of_origin);
        String highlightText = "Country";
        
        SpannableString spannableString = new SpannableString(fullText);
        int startIndex = fullText.indexOf(highlightText);
        
        if (startIndex != -1) {
            int endIndex = startIndex + highlightText.length();
            int primaryColor = ContextCompat.getColor(this, R.color.primary);
            spannableString.setSpan(
                new ForegroundColorSpan(primaryColor),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            textViewTitle.setText(spannableString);
        }
    }

    private void initializeViews() {
        buttonBack = findViewById(R.id.buttonBack);
        textViewProgress = findViewById(R.id.textViewProgress);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewSubtitle = findViewById(R.id.textViewSubtitle);
        editTextSearch = findViewById(R.id.editTextSearch);
        listViewCountries = findViewById(R.id.listViewCountries);
        buttonSkip = findViewById(R.id.buttonSkip);
        buttonContinue = findViewById(R.id.buttonContinue);

        // Set progress indicator
        textViewProgress.setText("1/4");
    }

    private void setupCountriesList() {
        // Get all country names
        allCountries = new ArrayList<>();
        String[] locales = Locale.getISOCountries();
        for (String countryCode : locales) {
            Locale locale = new Locale("", countryCode);
            String countryName = locale.getDisplayCountry();
            if (!countryName.isEmpty() && !allCountries.contains(countryName)) {
                allCountries.add(countryName);
            }
        }
        
        // Sort countries alphabetically
        String[] countriesArray = allCountries.toArray(new String[0]);
        Arrays.sort(countriesArray);
        allCountries = new ArrayList<>(Arrays.asList(countriesArray));

        filteredCountries = new ArrayList<>(allCountries);
        adapter = new ArrayAdapter<String>(this, R.layout.item_country_list, filteredCountries) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = getLayoutInflater().inflate(R.layout.item_country_list, parent, false);
                }
                
                TextView textViewCountryName = view.findViewById(R.id.textViewCountryName);
                ImageView imageViewCheck = view.findViewById(R.id.imageViewCheck);
                
                String country = filteredCountries.get(position);
                textViewCountryName.setText(country);
                
                // Reset to default state
                textViewCountryName.setTextColor(getResources().getColor(R.color.white, null));
                view.setBackground(null);
                imageViewCheck.setVisibility(android.view.View.GONE);
                
                // Show outline with primary color and checkmark when country is selected
                if (selectedCountry != null && country.equals(selectedCountry)) {
                    view.setBackgroundResource(R.drawable.country_item_selected);
                    imageViewCheck.setVisibility(android.view.View.VISIBLE);
                    imageViewCheck.setColorFilter(getResources().getColor(R.color.primary, null));
                }
                
                return view;
            }
        };
        listViewCountries.setAdapter(adapter);

        listViewCountries.setOnItemClickListener((parent, view, position, id) -> {
            selectedCountry = filteredCountries.get(position);
            updateContinueButton();
            // Refresh adapter to update highlighting
            adapter.notifyDataSetChanged();
        });
    }

    private void setupSearch() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCountries(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterCountries(String query) {
        filteredCountries.clear();
        if (query.isEmpty()) {
            filteredCountries.addAll(allCountries);
        } else {
            String lowerQuery = query.toLowerCase();
            for (String country : allCountries) {
                if (country.toLowerCase().contains(lowerQuery)) {
                    filteredCountries.add(country);
                }
            }
        }
        adapter.notifyDataSetChanged();
        // Keep selected country if it's still in filtered list
        if (selectedCountry != null && !filteredCountries.contains(selectedCountry)) {
            selectedCountry = null;
        }
        updateContinueButton();
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());

        buttonSkip.setOnClickListener(v -> {
            // Skip country selection and go to next step
            navigateToNextStep(null);
        });

        buttonContinue.setOnClickListener(v -> {
            if (selectedCountry != null) {
                navigateToNextStep(selectedCountry);
            }
        });
    }

    private void updateContinueButton() {
        buttonContinue.setEnabled(selectedCountry != null);
    }

    private void navigateToNextStep(String country) {
        Intent intent = new Intent(this, OnboardingStep2Activity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("email", email);
        if (country != null) {
            intent.putExtra("country", country);
        }
        startActivity(intent);
    }
}

