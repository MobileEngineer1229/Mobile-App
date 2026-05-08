package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.smarthome.iot.R;
import com.smarthome.iot.ui.fragments.AddManualFragment;
import com.smarthome.iot.ui.fragments.NearbyDevicesFragment;
import com.smarthome.iot.utils.ThemeHelper;

public class AddDeviceActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private com.google.android.material.button.MaterialButton buttonNearbyDevices;
    private com.google.android.material.button.MaterialButton buttonAddManual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        // Set status bar color to dark
        setStatusBarColor();

        initializeViews();
        setupViewPager();
        setupTabButtons();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_1));
        }
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        ImageButton buttonShare = findViewById(R.id.buttonShare);

        buttonBack.setOnClickListener(v -> finish());

        buttonShare.setOnClickListener(v -> {
            // Launch QR code scanner for FastBee/MQTT device registration
            Intent intent = new Intent(this, ScanDeviceActivity.class);
            startActivity(intent);
        });

        viewPager = findViewById(R.id.viewPager);
        buttonNearbyDevices = findViewById(R.id.buttonNearbyDevices);
        buttonAddManual = findViewById(R.id.buttonAddManual);
    }

    private void setupViewPager() {
        AddDevicePagerAdapter adapter = new AddDevicePagerAdapter(this);
        viewPager.setAdapter(adapter);
        
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabButtons(position);
            }
        });
    }

    private void setupTabButtons() {
        buttonNearbyDevices.setOnClickListener(v -> {
            viewPager.setCurrentItem(0, true);
            updateTabButtons(0);
        });

        buttonAddManual.setOnClickListener(v -> {
            viewPager.setCurrentItem(1, true);
            updateTabButtons(1);
        });

        // Set initial state
        updateTabButtons(0);
    }

    private void updateTabButtons(int selectedPosition) {
        // Use new tab button style - works like a switch: one selected, one unselected
        // Clear background tint to ensure our drawables are used (no stroke)
        buttonNearbyDevices.setBackgroundTintList(null);
        buttonNearbyDevices.setElevation(0f);
        buttonAddManual.setBackgroundTintList(null);
        buttonAddManual.setElevation(0f);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            buttonNearbyDevices.setStateListAnimator(null);
            buttonAddManual.setStateListAnimator(null);
        }
        
        if (selectedPosition == 0) {
            // Nearby Devices selected, Add Manual unselected
            buttonNearbyDevices.post(() -> {
                buttonNearbyDevices.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_selected_background));
                buttonNearbyDevices.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                buttonNearbyDevices.invalidate();
            });
            buttonAddManual.post(() -> {
                buttonAddManual.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_unselected_background));
                buttonAddManual.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                buttonAddManual.invalidate();
            });
        } else {
            // Nearby Devices unselected, Add Manual selected
            buttonNearbyDevices.post(() -> {
                buttonNearbyDevices.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_unselected_background));
                buttonNearbyDevices.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                buttonNearbyDevices.invalidate();
            });
            buttonAddManual.post(() -> {
                buttonAddManual.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_selected_background));
                buttonAddManual.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                buttonAddManual.invalidate();
            });
        }
    }

    private static class AddDevicePagerAdapter extends FragmentStateAdapter {

        public AddDevicePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new NearbyDevicesFragment();
            } else {
                return new AddManualFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}

