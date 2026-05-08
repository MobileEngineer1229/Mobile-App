package com.smarthome.iot.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.HelpSupportItem;
import com.smarthome.iot.ui.adapters.HelpSupportAdapter;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class HelpSupportActivity extends AppCompatActivity {
    private RecyclerView recyclerViewHelpSupport;
    private HelpSupportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        initializeViews();
        setupRecyclerView();
        loadHelpSupportItems();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerViewHelpSupport = findViewById(R.id.recyclerViewHelpSupport);
    }

    private void setupRecyclerView() {
        recyclerViewHelpSupport.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HelpSupportAdapter(new ArrayList<>(), this::onItemClick);
        recyclerViewHelpSupport.setAdapter(adapter);
    }

    private void onItemClick(HelpSupportItem item) {
        String action = item.getAction();
        Intent intent;
        
        switch (action) {
            case "faq":
                intent = new Intent(this, FAQActivity.class);
                startActivity(intent);
                break;
            case "contact_support":
                intent = new Intent(this, ContactSupportActivity.class);
                startActivity(intent);
                break;
            case "privacy_policy":
                intent = new Intent(this, PrivacyPolicyActivity.class);
                startActivity(intent);
                break;
            case "terms_of_service":
                intent = new Intent(this, TermsOfServiceActivity.class);
                startActivity(intent);
                break;
            case "partner":
            case "job_vacancy":
            case "accessibility":
            case "feedback":
            case "about_us":
                Toast.makeText(this, item.getTitle() + " - Coming soon", Toast.LENGTH_SHORT).show();
                break;
            case "rate_us":
                openPlayStore();
                break;
            case "visit_website":
                openWebsite();
                break;
            case "social_media":
                Toast.makeText(this, "Social media - Coming soon", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, item.getTitle() + " - Coming soon", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void openPlayStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
            startActivity(intent);
        }
    }

    private void openWebsite() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://smartify.com"));
        startActivity(intent);
    }

    private void loadHelpSupportItems() {
        List<HelpSupportItem> items = new ArrayList<>();
        items.add(new HelpSupportItem(getString(R.string.faq), R.drawable.ic_help_support, "faq"));
        items.add(new HelpSupportItem(getString(R.string.contact_support), R.drawable.ic_help_support, "contact_support"));
        items.add(new HelpSupportItem(getString(R.string.privacy_policy), R.drawable.ic_help_support, "privacy_policy"));
        items.add(new HelpSupportItem(getString(R.string.terms_of_service), R.drawable.ic_help_support, "terms_of_service"));
        items.add(new HelpSupportItem(getString(R.string.partner), R.drawable.ic_help_support, "partner"));
        items.add(new HelpSupportItem(getString(R.string.job_vacancy), R.drawable.ic_help_support, "job_vacancy"));
        items.add(new HelpSupportItem(getString(R.string.accessibility), R.drawable.ic_help_support, "accessibility"));
        items.add(new HelpSupportItem(getString(R.string.feedback), R.drawable.ic_help_support, "feedback"));
        items.add(new HelpSupportItem(getString(R.string.about_us), R.drawable.ic_help_support, "about_us"));
        items.add(new HelpSupportItem(getString(R.string.rate_us), R.drawable.ic_help_support, "rate_us"));
        items.add(new HelpSupportItem(getString(R.string.visit_our_website), R.drawable.ic_help_support, "visit_website"));
        items.add(new HelpSupportItem(getString(R.string.follow_us_on_social_media), R.drawable.ic_help_support, "social_media"));

        adapter = new HelpSupportAdapter(items, this::onItemClick);
        recyclerViewHelpSupport.setAdapter(adapter);
    }
}

