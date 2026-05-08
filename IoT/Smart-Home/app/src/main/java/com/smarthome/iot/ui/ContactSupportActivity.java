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
import com.smarthome.iot.models.ContactOption;
import com.smarthome.iot.ui.adapters.ContactOptionAdapter;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class ContactSupportActivity extends AppCompatActivity {
    private RecyclerView recyclerViewContactOptions;
    private ContactOptionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_support);

        initializeViews();
        setupRecyclerView();
        loadContactOptions();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerViewContactOptions = findViewById(R.id.recyclerViewContactOptions);
    }

    private void setupRecyclerView() {
        recyclerViewContactOptions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactOptionAdapter(new ArrayList<>(), this::onOptionClick);
        recyclerViewContactOptions.setAdapter(adapter);
    }

    private void onOptionClick(ContactOption option) {
        String action = option.getAction();
        
        switch (action) {
            case "customer_support":
                openEmail("support@smartify.com");
                break;
            case "website":
                openWebsite("https://smartify.com");
                break;
            case "whatsapp":
                openWhatsApp();
                break;
            case "facebook":
                openFacebook();
                break;
            case "twitter":
                openTwitter();
                break;
            case "instagram":
                openInstagram();
                break;
            default:
                Toast.makeText(this, option.getTitle() + " - Coming soon", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void openEmail(String email) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request");
        try {
            startActivity(Intent.createChooser(intent, "Send email"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWebsite(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void openWhatsApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/1234567890"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFacebook() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/smartify"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open Facebook", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTwitter() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/smartify"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open Twitter", Toast.LENGTH_SHORT).show();
        }
    }

    private void openInstagram() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/smartify"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open Instagram", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadContactOptions() {
        List<ContactOption> options = new ArrayList<>();
        options.add(new ContactOption(getString(R.string.customer_support), R.drawable.ic_help_support, "customer_support"));
        options.add(new ContactOption(getString(R.string.website), R.drawable.ic_help_support, "website"));
        options.add(new ContactOption(getString(R.string.whatsapp), R.drawable.ic_help_support, "whatsapp"));
        options.add(new ContactOption(getString(R.string.facebook), R.drawable.ic_help_support, "facebook"));
        options.add(new ContactOption(getString(R.string.twitter), R.drawable.ic_help_support, "twitter"));
        options.add(new ContactOption(getString(R.string.instagram), R.drawable.ic_help_support, "instagram"));

        adapter = new ContactOptionAdapter(options, this::onOptionClick);
        recyclerViewContactOptions.setAdapter(adapter);
    }
}

