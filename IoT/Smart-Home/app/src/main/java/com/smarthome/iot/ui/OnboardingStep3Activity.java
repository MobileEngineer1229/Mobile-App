package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.ui.adapters.OnboardingRoomSelectionAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnboardingStep3Activity extends AppCompatActivity {

    private ImageButton buttonBack;
    private TextView textViewProgress;
    private TextView textViewTitle;
    private GridView gridViewRooms;
    private MaterialButton buttonSkip;
    private MaterialButton buttonContinue;

    private OnboardingRoomSelectionAdapter adapter;
    private Set<String> selectedRooms = new HashSet<>();
    private List<String> availableRooms;

    private int userId;
    private String email;
    private String country;
    private String homeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_step3);

        // Set status bar color
        setStatusBarColor();

        // Get data from previous activity
        userId = getIntent().getIntExtra("userId", -1);
        email = getIntent().getStringExtra("email");
        country = getIntent().getStringExtra("country");
        homeName = getIntent().getStringExtra("homeName");

        initializeRooms();
        initializeViews();
        setupListeners();
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeRooms() {
        availableRooms = new ArrayList<>();
        availableRooms.add(getString(R.string.living_room));
        availableRooms.add(getString(R.string.bedroom));
        availableRooms.add(getString(R.string.bathroom));
        availableRooms.add(getString(R.string.kitchen));
        availableRooms.add(getString(R.string.dining_room));
        availableRooms.add(getString(R.string.backyard));
        availableRooms.add(getString(R.string.study_room));
        availableRooms.add(getString(R.string.add_room));
    }

    private void initializeViews() {
        buttonBack = findViewById(R.id.buttonBack);
        textViewProgress = findViewById(R.id.textViewProgress);
        textViewTitle = findViewById(R.id.textViewTitle);
        gridViewRooms = findViewById(R.id.gridViewRooms);
        buttonSkip = findViewById(R.id.buttonSkip);
        buttonContinue = findViewById(R.id.buttonContinue);

        textViewProgress.setText("3/4");
        
        // Setup title text with "Rooms" highlighted in primary color
        setupTitleText();

        adapter = new OnboardingRoomSelectionAdapter(this, availableRooms, selectedRooms, room -> {
            // Handle "Add Room" special case
            if (room.equals(getString(R.string.add_room))) {
                // TODO: Show dialog or navigate to add custom room
                Toast.makeText(this, "Add custom room feature coming soon", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedRooms.contains(room)) {
                selectedRooms.remove(room);
            } else {
                selectedRooms.add(room);
            }
            adapter.notifyDataSetChanged();
            updateContinueButton();
        });

        gridViewRooms.setAdapter(adapter);
        
        // Initially disable continue button
        updateContinueButton();
    }

    private void setupTitleText() {
        String title = getString(R.string.add_rooms);
        SpannableString spannableString = new SpannableString(title);
        
        // Find "Rooms" in the title and highlight it with primary color
        int roomsIndex = title.indexOf("Rooms");
        if (roomsIndex != -1) {
            int primaryColor = ContextCompat.getColor(this, R.color.primary);
            spannableString.setSpan(
                new ForegroundColorSpan(primaryColor),
                roomsIndex,
                roomsIndex + "Rooms".length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        textViewTitle.setText(spannableString);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());

        buttonSkip.setOnClickListener(v -> {
            navigateToNextStep(new ArrayList<>());
        });

        buttonContinue.setOnClickListener(v -> {
            navigateToNextStep(new ArrayList<>(selectedRooms));
        });
    }

    private void updateContinueButton() {
        buttonContinue.setEnabled(!selectedRooms.isEmpty());
    }

    private void navigateToNextStep(List<String> rooms) {
        Intent intent = new Intent(this, OnboardingStep4Activity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("email", email);
        if (country != null) {
            intent.putExtra("country", country);
        }
        if (homeName != null) {
            intent.putExtra("homeName", homeName);
        }
        intent.putStringArrayListExtra("rooms", new ArrayList<>(rooms));
        startActivity(intent);
    }
}

