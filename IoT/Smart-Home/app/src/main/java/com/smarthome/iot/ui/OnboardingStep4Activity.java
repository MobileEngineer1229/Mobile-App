package com.smarthome.iot.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OnboardingStep4Activity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ImageButton buttonBack;
    private TextView textViewProgress;
    private TextView textViewTitle;
    private MaterialButton buttonSkip;
    private MaterialButton buttonContinue;
    private View dividerAboveButtons;
    private AlertDialog locationDialog;

    private int userId;
    private String email;
    private String country;
    private String homeName;
    private ArrayList<String> rooms;
    private String address = null;
    private double latitude = 0;
    private double longitude = 0;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_step4);

        // Set status bar color
        setStatusBarColor();

        // Get data from previous activity
        userId = getIntent().getIntExtra("userId", -1);
        email = getIntent().getStringExtra("email");
        country = getIntent().getStringExtra("country");
        homeName = getIntent().getStringExtra("homeName");
        rooms = getIntent().getStringArrayListExtra("rooms");

        initializeViews();
        setupListeners();

        // Show location dialog first (second image)
        showLocationPermissionDialog();
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        buttonBack = findViewById(R.id.buttonBack);
        textViewProgress = findViewById(R.id.textViewProgress);
        textViewTitle = findViewById(R.id.textViewTitle);
        buttonSkip = findViewById(R.id.buttonSkip);
        buttonContinue = findViewById(R.id.buttonContinue);
        dividerAboveButtons = findViewById(R.id.dividerAboveButtons);

        textViewProgress.setText("4/4");
        
        // Setup title text with "Location" highlighted in primary color
        setupTitleText();
    }

    private void setupTitleText() {
        String title = getString(R.string.set_home_location);
        SpannableString spannableString = new SpannableString(title);
        
        // Find "Location" in the title and highlight it with primary color
        int locationIndex = title.indexOf("Location");
        if (locationIndex != -1) {
            int primaryColor = ContextCompat.getColor(this, R.color.primary);
            spannableString.setSpan(
                new ForegroundColorSpan(primaryColor),
                locationIndex,
                locationIndex + "Location".length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        textViewTitle.setText(spannableString);
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());

        buttonSkip.setOnClickListener(v -> {
            navigateToCompletion();
        });

        buttonContinue.setOnClickListener(v -> {
            if (address != null) {
                // Location already set, go to completion
                navigateToCompletion();
            } else {
                // Show location permission dialog first
                showLocationPermissionDialog();
            }
        });
    }
    
    private void showLocationPermissionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_enable_location, null);
        
        MaterialButton buttonEnableLocation = dialogView.findViewById(R.id.buttonEnableLocation);
        MaterialButton buttonNotNow = dialogView.findViewById(R.id.buttonNotNow);
        
        locationDialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        // Set dialog background to transparent for rounded corners
        if (locationDialog.getWindow() != null) {
            locationDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        buttonEnableLocation.setOnClickListener(v -> {
            locationDialog.dismiss();
            requestLocationPermission();
        });
        
        buttonNotNow.setOnClickListener(v -> {
            locationDialog.dismiss();
            navigateToCompletion();
        });
        
        locationDialog.show();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, get location
            getLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        if (locationManager == null) {
            // LocationManager not available, use mock location
            useMockLocation();
            return;
        }

        // Check if GPS provider is enabled
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && 
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
            // Use mock location as fallback
            useMockLocation();
            return;
        }

        // Create location listener
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                
                // Get address from coordinates using Geocoder
                getAddressFromCoordinates(latitude, longitude);
                
                // Remove location updates to save battery
                if (locationManager != null && locationListener != null) {
                    locationManager.removeUpdates(locationListener);
                }
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };

        // Request location updates
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                // Try GPS first, then network
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0,
                        0,
                        locationListener
                    );
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0,
                        0,
                        locationListener
                    );
                } else {
                    // No provider available, use mock location
                    useMockLocation();
                }

                // Also try to get last known location immediately
                Location lastKnownLocation = null;
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (lastKnownLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (lastKnownLocation != null) {
                    // Use last known location immediately
                    latitude = lastKnownLocation.getLatitude();
                    longitude = lastKnownLocation.getLongitude();
                    getAddressFromCoordinates(latitude, longitude);
                } else {
                    // No last known location, wait for updates (with timeout)
                    // Set a timeout to use mock location if no location received
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (address == null) {
                            // No location received within timeout, use mock location
                            useMockLocation();
                        }
                    }, 10000); // 10 second timeout
                }
            } else {
                // Permission not granted, use mock location
                useMockLocation();
            }
        } catch (SecurityException e) {
            android.util.Log.e("OnboardingStep4", "Error requesting location", e);
            // Use mock location as fallback
            useMockLocation();
        }
    }

    /**
     * Get address from coordinates using Geocoder
     */
    private void getAddressFromCoordinates(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addressObj = addresses.get(0);
                StringBuilder addressBuilder = new StringBuilder();
                
                // Build address string
                for (int i = 0; i <= addressObj.getMaxAddressLineIndex(); i++) {
                    if (i > 0) {
                        addressBuilder.append(", ");
                    }
                    addressBuilder.append(addressObj.getAddressLine(i));
                }
                
                address = addressBuilder.toString();
                
                // Show location prompt success
                showLocationSuccess();
            } else {
                // No address found, use coordinates as address
                address = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lon);
                showLocationSuccess();
            }
        } catch (IOException e) {
            android.util.Log.e("OnboardingStep4", "Error getting address from coordinates", e);
            // Use coordinates as address if geocoding fails
            address = String.format(Locale.getDefault(), "%.6f, %.6f", lat, lon);
            showLocationSuccess();
        }
    }

    /**
     * Use mock location as fallback
     */
    private void useMockLocation() {
        latitude = 40.7128; // New York coordinates
        longitude = -74.0060;
        address = "701 7th Ave, New York, 10036, USA";

        // Show location prompt success
        showLocationSuccess();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove location updates to save battery
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                android.util.Log.e("OnboardingStep4", "Error removing location updates", e);
            }
        }
    }

    private void showLocationSuccess() {
        // Show map view
        View cardMap = findViewById(R.id.cardMap);
        if (cardMap != null) {
            cardMap.setVisibility(View.VISIBLE);
        }

        // Show address section
        View addressSection = findViewById(R.id.addressSection);
        if (addressSection != null) {
            addressSection.setVisibility(View.VISIBLE);
        }

        // Update address text
        TextView textViewAddress = findViewById(R.id.textViewAddress);
        if (textViewAddress != null) {
            textViewAddress.setText(address);
        }

        // Show divider
        if (dividerAboveButtons != null) {
            dividerAboveButtons.setVisibility(View.VISIBLE);
        }
        
        // Enable continue button
        buttonContinue.setEnabled(true);
    }

    private void navigateToCompletion() {
        Intent intent = new Intent(this, OnboardingCompleteActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("email", email);
        if (country != null) {
            intent.putExtra("country", country);
        }
        if (homeName != null) {
            intent.putExtra("homeName", homeName);
        }
        if (rooms != null) {
            intent.putStringArrayListExtra("rooms", rooms);
        }
        if (address != null) {
            intent.putExtra("address", address);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
        }
        startActivity(intent);
        finish();
    }
}

