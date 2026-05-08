package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.ui.adapters.OnboardingPagerAdapter;
import com.smarthome.iot.utils.ThemeHelper;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private OnboardingPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        initializeViews();
        setupViewPager();
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        adapter = new OnboardingPagerAdapter();
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Use post with delay to ensure views are fully laid out
                viewPager.postDelayed(() -> {
                    setupButtonsForCurrentPage(position);
                    updatePaginationDots(position);
                }, 100);
            }
        });

        // Setup buttons for initial page after layout with delay
        viewPager.postDelayed(() -> {
            setupButtonsForCurrentPage(0);
            updatePaginationDots(0);
        }, 200);
    }

    private void setupButtonsForCurrentPage(int position) {
        // Find the RecyclerView inside ViewPager2
        android.view.View recyclerView = viewPager.getChildAt(0);
        if (recyclerView instanceof androidx.recyclerview.widget.RecyclerView) {
            androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) recyclerView;
            // Find the ViewHolder for current position
            androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder = rv.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                View itemView = viewHolder.itemView;
                setupButtonsInView(itemView, position);
            }
        }
        
        // Also setup for adjacent pages to ensure buttons work when swiping
        setupButtonsForPage(position - 1);
        setupButtonsForPage(position + 1);
    }
    
    private void setupButtonsForPage(int position) {
        if (position < 0 || position >= adapter.getItemCount()) {
            return;
        }
        
        android.view.View recyclerView = viewPager.getChildAt(0);
        if (recyclerView instanceof androidx.recyclerview.widget.RecyclerView) {
            androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) recyclerView;
            androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder = rv.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                View itemView = viewHolder.itemView;
                setupButtonsInView(itemView, position);
            }
        }
    }
    
    private void setupButtonsInView(View itemView, int position) {
        // Find buttons in the current page layout
        MaterialButton buttonSkip = itemView.findViewById(R.id.buttonSkip);
        MaterialButton buttonContinue = itemView.findViewById(R.id.buttonContinue);

        if (buttonSkip != null) {
            // Remove any existing listeners
            buttonSkip.setOnClickListener(null);
            buttonSkip.setOnClickListener(v -> navigateToWelcome());
        }

        if (buttonContinue != null) {
            // Remove any existing listeners
            buttonContinue.setOnClickListener(null);
            buttonContinue.setOnClickListener(v -> {
                int currentItem = viewPager.getCurrentItem();
                if (currentItem < adapter.getItemCount() - 1) {
                    // Go to next page
                    viewPager.setCurrentItem(currentItem + 1, true);
                } else {
                    // Last page - navigate to welcome
                    navigateToWelcome();
                }
            });
        }
    }

    private void updatePaginationDots(int position) {
        // Find the RecyclerView inside ViewPager2
        android.view.View recyclerView = viewPager.getChildAt(0);
        if (recyclerView instanceof androidx.recyclerview.widget.RecyclerView) {
            androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) recyclerView;
            // Find the ViewHolder for current position
            androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder = rv.findViewHolderForAdapterPosition(position);
            if (viewHolder != null) {
                View itemView = viewHolder.itemView;
                
                View dot1 = itemView.findViewById(R.id.dot1);
                View dot2 = itemView.findViewById(R.id.dot2);
                View dot3 = itemView.findViewById(R.id.dot3);

                if (dot1 != null) {
                    dot1.setBackground(getDrawable(position == 0 ? R.drawable.dot_active : R.drawable.dot_inactive));
                }
                if (dot2 != null) {
                    dot2.setBackground(getDrawable(position == 1 ? R.drawable.dot_active : R.drawable.dot_inactive));
                }
                if (dot3 != null) {
                    dot3.setBackground(getDrawable(position == 2 ? R.drawable.dot_active : R.drawable.dot_inactive));
                }
            }
        }
    }

    private void navigateToWelcome() {
        Intent intent = new Intent(OnboardingActivity.this, WelcomeActivity.class);
        startActivity(intent);
        finish();
    }
}
