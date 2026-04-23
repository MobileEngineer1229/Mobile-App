package com.talentbaby.app.activities;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.talentbaby.app.R;
import com.talentbaby.app.ui.growth.GrowthChartView;

public class GrowthTrackerActivity extends AppCompatActivity {

    private static final String[] AGE_RANGES = {
            "0 Month - 6 Month",
            "6 Month - 12 Month",
            "12 Month - 18 Month"
    };

    private boolean isWeight = true;
    private int ageRangeIndex = 0;

    private TextView tabWeight, tabHeight;
    private TextView textChartTitle, textAgeRange;
    private GrowthChartView growthChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_growth_tracker);

        applyHeaderGradient();
        bindViews();
        setupListeners();
        refreshUI();
    }

    private void applyHeaderGradient() {
        GradientDrawable grad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFF9D0B8, 0xFFF08B6B});
        findViewById(R.id.headerGrowth).setBackground(grad);
    }

    private void bindViews() {
        tabWeight      = findViewById(R.id.tabWeight);
        tabHeight      = findViewById(R.id.tabHeight);
        textChartTitle = findViewById(R.id.textChartTitle);
        textAgeRange   = findViewById(R.id.textAgeRange);
        growthChart    = findViewById(R.id.growthChart);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tabWeight.setOnClickListener(v -> setActiveTab(true));
        tabHeight.setOnClickListener(v -> setActiveTab(false));

        findViewById(R.id.btnRangePrev).setOnClickListener(v -> {
            if (ageRangeIndex > 0) { ageRangeIndex--; refreshUI(); }
        });
        findViewById(R.id.btnRangeNext).setOnClickListener(v -> {
            if (ageRangeIndex < AGE_RANGES.length - 1) { ageRangeIndex++; refreshUI(); }
        });

        findViewById(R.id.btnSI).setOnClickListener(v ->
                Toast.makeText(this, "SI units", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnIMP).setOnClickListener(v ->
                Toast.makeText(this, "Imperial units", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnAddData).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.add_data), Toast.LENGTH_SHORT).show());
    }

    private void setActiveTab(boolean weight) {
        isWeight = weight;
        // Active tab: white bg + coralDeep text
        tabWeight.setBackgroundResource(weight ? R.drawable.bg_tab_white_pill : android.R.color.transparent);
        tabWeight.setTextColor(weight ? 0xFFD96E4B : 0xFFFFFFFF);
        tabHeight.setBackgroundResource(weight ? android.R.color.transparent : R.drawable.bg_tab_white_pill);
        tabHeight.setTextColor(weight ? 0xFFFFFFFF : 0xFFD96E4B);
        refreshUI();
    }

    private void refreshUI() {
        textChartTitle.setText(isWeight ? getString(R.string.weight_chart) : getString(R.string.height_chart));
        textAgeRange.setText(AGE_RANGES[ageRangeIndex]);
        growthChart.setMode(isWeight, ageRangeIndex);
    }
}
