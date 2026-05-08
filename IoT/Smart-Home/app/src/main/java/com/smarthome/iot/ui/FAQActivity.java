package com.smarthome.iot.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.FAQItem;
import com.smarthome.iot.ui.adapters.FAQAdapter;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class FAQActivity extends AppCompatActivity {
    private RecyclerView recyclerViewFAQ;
    private FAQAdapter adapter;
    private EditText editTextSearch;
    private MaterialButton buttonTabGeneral;
    private MaterialButton buttonTabAccount;
    private MaterialButton buttonTabServices;
    private MaterialButton buttonTabHome;
    
    private List<FAQItem> allFAQItems;
    private String currentCategory = "general";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        initializeViews();
        setupRecyclerView();
        setupTabs();
        setupSearch();
        loadFAQItems();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerViewFAQ = findViewById(R.id.recyclerViewFAQ);
        editTextSearch = findViewById(R.id.editTextSearch);
        buttonTabGeneral = findViewById(R.id.buttonTabGeneral);
        buttonTabAccount = findViewById(R.id.buttonTabAccount);
        buttonTabServices = findViewById(R.id.buttonTabServices);
        buttonTabHome = findViewById(R.id.buttonTabHome);
    }

    private void setupRecyclerView() {
        recyclerViewFAQ.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FAQAdapter(new ArrayList<>());
        recyclerViewFAQ.setAdapter(adapter);
    }

    private void setupTabs() {
        buttonTabGeneral.setOnClickListener(v -> switchCategory("general"));
        buttonTabAccount.setOnClickListener(v -> switchCategory("account"));
        buttonTabServices.setOnClickListener(v -> switchCategory("services"));
        buttonTabHome.setOnClickListener(v -> switchCategory("home"));
    }

    private void switchCategory(String category) {
        currentCategory = category;
        updateTabButtons();
        filterFAQItems();
    }

    private void updateTabButtons() {
        int activeColor = getResources().getColor(R.color.primary);
        int inactiveColor = getResources().getColor(R.color.dark_4);
        
        buttonTabGeneral.setBackgroundTintList(getColorStateList(
            currentCategory.equals("general") ? R.color.primary : R.color.dark_4));
        buttonTabAccount.setBackgroundTintList(getColorStateList(
            currentCategory.equals("account") ? R.color.primary : R.color.dark_4));
        buttonTabServices.setBackgroundTintList(getColorStateList(
            currentCategory.equals("services") ? R.color.primary : R.color.dark_4));
        buttonTabHome.setBackgroundTintList(getColorStateList(
            currentCategory.equals("home") ? R.color.primary : R.color.dark_4));
    }

    private void setupSearch() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFAQItems();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterFAQItems() {
        String searchQuery = editTextSearch.getText().toString().toLowerCase();
        List<FAQItem> filtered = new ArrayList<>();
        
        for (FAQItem item : allFAQItems) {
            if (item.getCategory().equals(currentCategory)) {
                if (searchQuery.isEmpty() || 
                    item.getQuestion().toLowerCase().contains(searchQuery) ||
                    item.getAnswer().toLowerCase().contains(searchQuery)) {
                    filtered.add(item);
                }
            }
        }
        
        adapter = new FAQAdapter(filtered);
        recyclerViewFAQ.setAdapter(adapter);
    }

    private void loadFAQItems() {
        allFAQItems = new ArrayList<>();
        
        // General FAQs
        allFAQItems.add(new FAQItem(
            getString(R.string.faq_what_is_smartify),
            getString(R.string.faq_what_is_smartify_answer),
            "general"
        ));
        allFAQItems.add(new FAQItem(
            getString(R.string.faq_add_device),
            getString(R.string.faq_add_device_answer),
            "general"
        ));
        allFAQItems.add(new FAQItem(
            getString(R.string.faq_create_automation),
            getString(R.string.faq_create_automation_answer),
            "general"
        ));
        allFAQItems.add(new FAQItem(
            getString(R.string.faq_tap_to_run),
            getString(R.string.faq_tap_to_run_answer),
            "general"
        ));
        allFAQItems.add(new FAQItem(
            getString(R.string.faq_offline_use),
            getString(R.string.faq_offline_use_answer),
            "general"
        ));
        
        // Account FAQs
        allFAQItems.add(new FAQItem(
            getString(R.string.faq_reset_password),
            getString(R.string.faq_reset_password_answer),
            "account"
        ));
        
        // Services FAQs (placeholder)
        allFAQItems.add(new FAQItem(
            "How do I subscribe to premium services?",
            "Navigate to Settings > Services to view and subscribe to premium features.",
            "services"
        ));
        
        // Home FAQs (placeholder)
        allFAQItems.add(new FAQItem(
            "How do I add a new room?",
            "Go to Home Management, tap Add Room, and follow the setup instructions.",
            "home"
        ));
        
        filterFAQItems();
    }
}

