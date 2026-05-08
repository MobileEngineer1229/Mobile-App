package com.smarthome.iot.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.DeviceConsumptionResponse;
import com.smarthome.iot.models.DeviceConsumptionSummary;
import com.smarthome.iot.models.MonthlySummary;
import com.smarthome.iot.models.MonthlySummaryResponse;
import com.smarthome.iot.models.StatisticsDataPoint;
import com.smarthome.iot.models.StatisticsResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.ui.adapters.DeviceConsumptionAdapter;
import com.smarthome.iot.ui.adapters.LocationSelectorAdapter;
import com.smarthome.iot.ui.decorations.GridSpacingItemDecoration;
import com.smarthome.iot.ui.dialogs.ReportsCustomDatePickerDialog;
import com.smarthome.iot.ui.HomeManagementActivity;
import com.smarthome.iot.ui.views.BarChartView;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.Globals;
import com.smarthome.iot.utils.MockDataProvider;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsActivity extends AppCompatActivity {
    private static final String TAG = "ReportsActivity";

    // Views
    private TextView textViewThisMonthKwh;
    private TextView textViewThisMonthCost;
    private TextView textViewPreviousMonthKwh;
    private TextView textViewPreviousMonthCost;
    private TextView textViewDateRange;
    private BarChartView barChartView;
    private RecyclerView recyclerViewDevices;
    private LinearLayout emptyStateContainer;
    private ProgressBar progressBar;

    // Data
    private MonthlySummaryResponse monthlySummary;
    private StatisticsResponse statistics;
    private DeviceConsumptionResponse deviceConsumption;
    private String currentDateRange = "last_6_months";

    // Adapters
    private DeviceConsumptionAdapter deviceConsumptionAdapter;
    private List<DeviceConsumptionSummary> deviceConsumptionList;

    private ApiService apiService;
    private AuthManager authManager;
    
    // Home selection
    private android.widget.PopupWindow locationSelectorPopupWindow;
    private int selectedHomeId = -1;
    private Home currentHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // Set status bar color to dark
        setStatusBarColor();

        apiService = ApiClient.getClient().create(ApiService.class);
        authManager = new AuthManager(this);

        initViews();
        setupBottomNavigation();
        setupRecyclerViews();
        setupDateRangeSelector();

        loadData();
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_3));
        }
    }

    private void initViews() {
        textViewThisMonthKwh = findViewById(R.id.textViewThisMonthKwh);
        textViewThisMonthCost = findViewById(R.id.textViewThisMonthCost);
        textViewPreviousMonthKwh = findViewById(R.id.textViewPreviousMonthKwh);
        textViewPreviousMonthCost = findViewById(R.id.textViewPreviousMonthCost);
        textViewDateRange = findViewById(R.id.textViewDateRange);
        barChartView = findViewById(R.id.barChartView);
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        progressBar = findViewById(R.id.progressBar);
        
        // Setup "My Home" dropdown
        TextView textViewMyHome = findViewById(R.id.textViewMyHome);
        ImageButton buttonDropdown = findViewById(R.id.buttonDropdown);
        
        if (textViewMyHome != null) {
            textViewMyHome.setOnClickListener(v -> showLocationSelector(v));
        }
        
        if (buttonDropdown != null) {
            buttonDropdown.setOnClickListener(v -> showLocationSelector(v));
        }
        
        // Setup calendar button
        ImageButton buttonCalendar = findViewById(R.id.buttonCalendar);
        if (buttonCalendar != null) {
            buttonCalendar.setOnClickListener(v -> showCustomDatePicker());
        }
        
        // Load primary home name
        loadPrimaryHome();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_reports);
        
        // Remove spacing between icon and text
        com.smarthome.iot.utils.BottomNavSpacingHelper.removeSpacing(bottomNavigation);
        
        // Disable active indicator (blue background)
        com.smarthome.iot.utils.BottomNavSpacingHelper.disableActiveIndicator(bottomNavigation);
        
        bottomNavigation.setItemIconTintList(null);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            // Remove active indicator background when selection changes
            com.smarthome.iot.utils.BottomNavSpacingHelper.onSelectionChanged(bottomNavigation);
            
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_smart) {
                Intent intent = new Intent(ReportsActivity.this, SmartSceneActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_reports) {
                // Already on Reports
                return true;
            } else if (itemId == R.id.nav_account) {
                Intent intent = new Intent(ReportsActivity.this, AccountActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupDateRangeSelector() {
        if (textViewDateRange != null) {
            textViewDateRange.setOnClickListener(v -> {
                showDateRangePopupMenu(v);
            });
        }
    }

    private void showDateRangePopupMenu(View anchor) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.dialog_date_range, null);
        
        // Get the anchor button width - use getWidth() if already measured, otherwise measure it
        int buttonWidth = anchor.getWidth();
        if (buttonWidth == 0) {
            anchor.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            buttonWidth = anchor.getMeasuredWidth();
        }
        
        // Measure popup content with the button width to get proper height
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(buttonWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        );
        int popupHeight = popupView.getMeasuredHeight();
        
        // Calculate available space from anchor to bottom navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        int maxAvailableHeight = Integer.MAX_VALUE;
        
        if (bottomNavigation != null && bottomNavigation.getVisibility() == View.VISIBLE) {
            // Get anchor location on screen
            int[] anchorLocation = new int[2];
            anchor.getLocationOnScreen(anchorLocation);
            
            // Get bottom navigation location on screen
            int[] bottomNavLocation = new int[2];
            bottomNavigation.getLocationOnScreen(bottomNavLocation);
            
            // Calculate available space: from bottom of anchor to top of bottom navigation
            // Add some padding (8dp) for spacing
            int spacing = (int) (8 * getResources().getDisplayMetrics().density);
            int anchorBottom = anchorLocation[1] + anchor.getHeight();
            int bottomNavTop = bottomNavLocation[1];
            maxAvailableHeight = bottomNavTop - anchorBottom - spacing;
            
            // Ensure we have at least some minimum height
            int minHeight = (int) (100 * getResources().getDisplayMetrics().density);
            if (maxAvailableHeight < minHeight) {
                maxAvailableHeight = minHeight;
            }
        }
        
        // Limit popup height to available space or a reasonable maximum
        int maxVisibleHeight = (int) (42 * 6 * getResources().getDisplayMetrics().density); // Show ~6 items as fallback
        int finalMaxHeight = Math.min(maxAvailableHeight, maxVisibleHeight);
        
        if (popupHeight > finalMaxHeight) {
            popupHeight = finalMaxHeight;
        }
        
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
            popupView,
            buttonWidth, // Match button width exactly
            popupHeight,
            true
        );
        
        // Set dark background
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_rounded_dark));
        popupWindow.setElevation(8f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        
        // Get all text views
        TextView itemToday = popupView.findViewById(R.id.itemToday);
        TextView itemThisWeek = popupView.findViewById(R.id.itemThisWeek);
        TextView itemLastMonth = popupView.findViewById(R.id.itemLastMonth);
        TextView itemLast3Months = popupView.findViewById(R.id.itemLast3Months);
        TextView itemLast6Months = popupView.findViewById(R.id.itemLast6Months);
        TextView itemThisYear = popupView.findViewById(R.id.itemThisYear);
        TextView itemLastYear = popupView.findViewById(R.id.itemLastYear);
        TextView itemAllTime = popupView.findViewById(R.id.itemAllTime);
        TextView itemCustomRange = popupView.findViewById(R.id.itemCustomRange);
        
        // Set click listeners
        View.OnClickListener dateRangeClickListener = v -> {
            String dateRange = null;
            String displayText = null;
            int viewId = v.getId();
            
            if (viewId == R.id.itemToday) {
                dateRange = "today";
                displayText = "Today";
            } else if (viewId == R.id.itemThisWeek) {
                dateRange = "this_week";
                displayText = "This Week";
            } else if (viewId == R.id.itemLastMonth) {
                dateRange = "last_month";
                displayText = "Last month";
            } else if (viewId == R.id.itemLast3Months) {
                dateRange = "last_3_months";
                displayText = "Last 3 months";
            } else if (viewId == R.id.itemLast6Months) {
                dateRange = "last_6_months";
                displayText = "Last 6 months";
            } else if (viewId == R.id.itemThisYear) {
                dateRange = "this_year";
                displayText = "This year";
            } else if (viewId == R.id.itemLastYear) {
                dateRange = "last_year";
                displayText = "Last year";
            } else if (viewId == R.id.itemAllTime) {
                dateRange = "all_time";
                displayText = "All time";
            } else if (viewId == R.id.itemCustomRange) {
                // TODO: Show custom date range picker
                Toast.makeText(this, "Custom range coming soon", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
                return;
            }
            
            if (dateRange != null && displayText != null) {
                currentDateRange = dateRange;
                textViewDateRange.setText(displayText);
                loadData(); // Reload data with new date range
                popupWindow.dismiss();
            }
        };
        
        if (itemToday != null) itemToday.setOnClickListener(dateRangeClickListener);
        if (itemThisWeek != null) itemThisWeek.setOnClickListener(dateRangeClickListener);
        if (itemLastMonth != null) itemLastMonth.setOnClickListener(dateRangeClickListener);
        if (itemLast3Months != null) itemLast3Months.setOnClickListener(dateRangeClickListener);
        if (itemLast6Months != null) itemLast6Months.setOnClickListener(dateRangeClickListener);
        if (itemThisYear != null) itemThisYear.setOnClickListener(dateRangeClickListener);
        if (itemLastYear != null) itemLastYear.setOnClickListener(dateRangeClickListener);
        if (itemAllTime != null) itemAllTime.setOnClickListener(dateRangeClickListener);
        if (itemCustomRange != null) itemCustomRange.setOnClickListener(dateRangeClickListener);
        
        // Show popup below the anchor view
        popupWindow.showAsDropDown(anchor, 0, 8);
        
        // Scroll to selected item after popup is shown
        popupView.post(() -> {
            // Find the NestedScrollView (it's the root view)
            androidx.core.widget.NestedScrollView scrollView = null;
            if (popupView instanceof androidx.core.widget.NestedScrollView) {
                scrollView = (androidx.core.widget.NestedScrollView) popupView;
            }
            
            // Find and scroll to the currently selected item
            TextView selectedItem = null;
            if (currentDateRange != null) {
                switch (currentDateRange) {
                    case "today":
                        selectedItem = itemToday;
                        break;
                    case "this_week":
                        selectedItem = itemThisWeek;
                        break;
                    case "last_month":
                        selectedItem = itemLastMonth;
                        break;
                    case "last_3_months":
                        selectedItem = itemLast3Months;
                        break;
                    case "last_6_months":
                        selectedItem = itemLast6Months;
                        break;
                    case "this_year":
                        selectedItem = itemThisYear;
                        break;
                    case "last_year":
                        selectedItem = itemLastYear;
                        break;
                    case "all_time":
                        selectedItem = itemAllTime;
                        break;
                }
            }
            
            // Default to "Last 6 months" if no selection or if it's the default
            if (selectedItem == null) {
                selectedItem = itemLast6Months;
            }
            
            // Make final copies for use in nested lambda
            final TextView finalSelectedItem = selectedItem;
            final androidx.core.widget.NestedScrollView finalScrollView = scrollView;
            
            if (finalSelectedItem != null && finalScrollView != null) {
                // Scroll to make the selected item visible at the top
                finalScrollView.post(() -> {
                    int scrollY = finalSelectedItem.getTop() - finalScrollView.getPaddingTop();
                    finalScrollView.smoothScrollTo(0, Math.max(0, scrollY));
                });
            }
        });
    }

    private void showCustomDatePicker() {
        ReportsCustomDatePickerDialog dialog = new ReportsCustomDatePickerDialog(this);
        dialog.setOnDateSelectedListener((startDate, endDate) -> {
            // Handle date selection
            // For now, just update the date range text
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateRangeText = dateFormat.format(startDate);
            if (!startDate.equals(endDate)) {
                dateRangeText += " - " + dateFormat.format(endDate);
            }
            textViewDateRange.setText(dateRangeText);
            currentDateRange = "custom";
            loadData(); // Reload data with custom date range
        });
        dialog.show();
    }

    private void setupRecyclerViews() {
        try {
            // Devices RecyclerView with GridLayoutManager (2 columns)
            if (deviceConsumptionList == null) {
                deviceConsumptionList = new ArrayList<>();
            }
            if (deviceConsumptionAdapter == null) {
                deviceConsumptionAdapter = new DeviceConsumptionAdapter(deviceConsumptionList, device -> {
                    // Handle device click - show details
                    showDeviceDetails(device);
                });
            }
            if (recyclerViewDevices != null) {
                GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
                recyclerViewDevices.setLayoutManager(gridLayoutManager);
                
                // Add spacing between grid items
                int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
                recyclerViewDevices.addItemDecoration(new GridSpacingItemDecoration(2, spacingInPixels, false));
                
                recyclerViewDevices.setAdapter(deviceConsumptionAdapter);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error setting up recycler views", e);
        }
    }

    private void loadData() {
        try {
            showProgress(true);
            // Load from API
            loadMonthlySummary();
            loadStatistics();
            loadDeviceConsumption();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading data", e);
            showProgress(false);
        }
    }

    private void loadMonthlySummary() {
        Call<ApiResponse<MonthlySummaryResponse>> call = apiService.getMonthlySummary();
        call.enqueue(new Callback<ApiResponse<MonthlySummaryResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MonthlySummaryResponse>> call, Response<ApiResponse<MonthlySummaryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    monthlySummary = response.body().getData();
                    updateMonthlySummaryUI();
                } else {
                    android.util.Log.w(TAG, "Failed to load monthly summary from API");
                }
                checkAllDataLoaded();
            }

            @Override
            public void onFailure(Call<ApiResponse<MonthlySummaryResponse>> call, Throwable t) {
                android.util.Log.e(TAG, "Network error loading monthly summary", t);
                checkAllDataLoaded();
            }
        });
    }

    private void loadDemoMonthlySummary() {
        try {
            // Create demo data for monthly summary
            MonthlySummary thisMonth = new MonthlySummary();
            thisMonth.setConsumptionKwh(825.40);
            thisMonth.setCostUsd(123.81);
            thisMonth.setMonth("December 2024");

            MonthlySummary previousMonth = new MonthlySummary();
            previousMonth.setConsumptionKwh(958.75);
            previousMonth.setCostUsd(143.81);
            previousMonth.setMonth("November 2024");

            monthlySummary = new MonthlySummaryResponse();
            monthlySummary.setThisMonth(thisMonth);
            monthlySummary.setPreviousMonth(previousMonth);

            updateMonthlySummaryUI();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading demo monthly summary", e);
        }
    }

    private void loadStatistics() {
        Call<ApiResponse<StatisticsResponse>> call = apiService.getStatistics(currentDateRange, null, null);
        call.enqueue(new Callback<ApiResponse<StatisticsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<StatisticsResponse>> call, Response<ApiResponse<StatisticsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    statistics = response.body().getData();
                    updateStatisticsUI();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<StatisticsResponse>> call, Throwable t) {
                // Silently fail - statistics are not critical
            }
        });
    }

    private void loadDeviceConsumption() {
        Call<ApiResponse<DeviceConsumptionResponse>> call = apiService.getDeviceConsumption(
            currentDateRange, null, null, null, null, null, "device"
        );
        call.enqueue(new Callback<ApiResponse<DeviceConsumptionResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<DeviceConsumptionResponse>> call, Response<ApiResponse<DeviceConsumptionResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    deviceConsumption = response.body().getData();
                    // Only update if API returns actual data, otherwise keep demo data
                    if (deviceConsumption != null && deviceConsumption.getDevices() != null && !deviceConsumption.getDevices().isEmpty()) {
                        updateDeviceConsumptionUI();
                    }
                } else {
                    android.util.Log.w(TAG, "Failed to load device consumption from API");
                }
                checkAllDataLoaded();
            }

            @Override
            public void onFailure(Call<ApiResponse<DeviceConsumptionResponse>> call, Throwable t) {
                android.util.Log.e(TAG, "Network error loading device consumption", t);
                checkAllDataLoaded();
            }
        });
    }

    private void loadDemoDeviceConsumption() {
        try {
            // Create demo data for device consumption (matching Figma design)
            List<DeviceConsumptionSummary> demoDevices = new ArrayList<>();

            DeviceConsumptionSummary device1 = new DeviceConsumptionSummary();
            device1.setDeviceId(1);
            device1.setDeviceName("Smart Lamp");
            device1.setDeviceType("Lighting");
            device1.setRoomId(1);
            device1.setRoomName("Living Room");
            device1.setTotalConsumptionKwh(184.69);
            device1.setTotalCostUsd(27.70);
            device1.setDeviceCount(12);
            demoDevices.add(device1);

            DeviceConsumptionSummary device2 = new DeviceConsumptionSummary();
            device2.setDeviceId(2);
            device2.setDeviceName("Smart V1 CCTV");
            device2.setDeviceType("Camera");
            device2.setRoomId(1);
            device2.setRoomName("Living Room");
            device2.setTotalConsumptionKwh(125.73);
            device2.setTotalCostUsd(18.86);
            device2.setDeviceCount(3);
            demoDevices.add(device2);

            DeviceConsumptionSummary device3 = new DeviceConsumptionSummary();
            device3.setDeviceId(3);
            device3.setDeviceName("Smart Lock");
            device3.setDeviceType("Security");
            device3.setRoomId(2);
            device3.setRoomName("Bedroom");
            device3.setTotalConsumptionKwh(106.45);
            device3.setTotalCostUsd(15.97);
            device3.setDeviceCount(1);
            demoDevices.add(device3);

            DeviceConsumptionSummary device4 = new DeviceConsumptionSummary();
            device4.setDeviceId(4);
            device4.setDeviceName("Smart Plug");
            device4.setDeviceType("Outlet");
            device4.setRoomId(3);
            device4.setRoomName("Kitchen");
            device4.setTotalConsumptionKwh(98.24);
            device4.setTotalCostUsd(14.74);
            device4.setDeviceCount(1);
            demoDevices.add(device4);

            if (deviceConsumptionList == null) {
                deviceConsumptionList = new ArrayList<>();
            }
            if (deviceConsumptionAdapter == null) {
                setupRecyclerViews();
            }

            deviceConsumptionList.clear();
            deviceConsumptionList.addAll(demoDevices);
            if (deviceConsumptionAdapter != null) {
                deviceConsumptionAdapter.notifyDataSetChanged();
            }
            showEmptyState(false);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading demo device consumption", e);
            showEmptyState(true);
        }
    }

    private void updateMonthlySummaryUI() {
        if (monthlySummary == null) return;

        try {
            DecimalFormat df = new DecimalFormat("0.00");
            DecimalFormat costFormat = new DecimalFormat("0.00");

            MonthlySummary thisMonth = monthlySummary.getThisMonth();
            MonthlySummary previousMonth = monthlySummary.getPreviousMonth();

            if (thisMonth != null && textViewThisMonthKwh != null && textViewThisMonthCost != null) {
                Double consumption = thisMonth.getConsumptionKwh();
                Double cost = thisMonth.getCostUsd();
                if (consumption != null) {
                    textViewThisMonthKwh.setText(df.format(consumption));
                }
                if (cost != null) {
                    textViewThisMonthCost.setText("$" + costFormat.format(cost));
                }
            }

            if (previousMonth != null && textViewPreviousMonthKwh != null && textViewPreviousMonthCost != null) {
                Double consumption = previousMonth.getConsumptionKwh();
                Double cost = previousMonth.getCostUsd();
                if (consumption != null) {
                    textViewPreviousMonthKwh.setText(df.format(consumption));
                }
                if (cost != null) {
                    textViewPreviousMonthCost.setText("$" + costFormat.format(cost));
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error updating monthly summary UI", e);
        }
    }

    private void updateStatisticsUI() {
        // TODO: Update bar chart with statistics data
        // For now, this is a placeholder
        if (statistics != null && statistics.getData() != null) {
            // Update chart visualization
        }
    }

    private void updateDeviceConsumptionUI() {
        try {
            if (deviceConsumption == null || deviceConsumption.getDevices() == null || deviceConsumption.getDevices().isEmpty()) {
                // If API returns empty, keep demo data if it exists
                if (deviceConsumptionList == null || deviceConsumptionList.isEmpty()) {
                    showEmptyState(true);
                }
                return;
            }

            if (deviceConsumptionList == null) {
                deviceConsumptionList = new ArrayList<>();
            }
            if (deviceConsumptionAdapter == null) {
                setupRecyclerViews();
            }

            deviceConsumptionList.clear();
            deviceConsumptionList.addAll(deviceConsumption.getDevices());
            if (deviceConsumptionAdapter != null) {
                deviceConsumptionAdapter.notifyDataSetChanged();
            }
            showEmptyState(deviceConsumptionList.isEmpty());
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error updating device consumption UI", e);
            // Don't show empty state if we have demo data
            if (deviceConsumptionList == null || deviceConsumptionList.isEmpty()) {
                showEmptyState(true);
            }
        }
    }

    private void showDeviceDetails(DeviceConsumptionSummary device) {
        // Navigate to device details activity
        Intent intent = new Intent(this, ReportsDeviceDetailActivity.class);
        intent.putExtra("device_name", device.getDeviceName());
        intent.putExtra("device_type", device.getDeviceType());
        intent.putExtra("device_id", device.getDeviceId());
        intent.putExtra("date_range", currentDateRange);
        startActivity(intent);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show) {
        emptyStateContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewDevices.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private int dataLoadCount = 0;
    private void checkAllDataLoaded() {
        dataLoadCount++;
        if (dataLoadCount >= 2) { // Monthly summary + Device consumption
            showProgress(false);
            dataLoadCount = 0;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showLocationSelector(View anchorView) {
        if (!authManager.isLoggedIn()) {
            // Show demo locations if not logged in
            showDemoLocationSelector();
            return;
        }
        
        // Use mock data for demo user
        if (MockDataProvider.isDemoUser(authManager)) {
            List<Home> homes = MockDataProvider.getMockHomes();
            showHomeSelector(homes, anchorView);
            return;
        }
        
        // Check Globals cache first
        if (Globals.isHomesCacheValid()) {
            List<Home> cachedHomes = Globals.getUserHomes();
            if (cachedHomes != null && !cachedHomes.isEmpty()) {
                android.util.Log.d(TAG, "Using cached homes from Globals: " + cachedHomes.size() + " homes");
                showHomeSelector(cachedHomes, anchorView);
                return;
            }
        }
        
        // Load homes from API for non-demo users
        Call<ApiResponse<List<Home>>> call = apiService.getHomes();
        call.enqueue(new Callback<ApiResponse<List<Home>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Home>>> call, 
                                 Response<ApiResponse<List<Home>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Home> homes = response.body().getData();
                    if (homes != null && !homes.isEmpty()) {
                        // Save to Globals
                        Globals.setUserHomes(homes);
                        showHomeSelector(homes, anchorView);
                    } else {
                        showDemoLocationSelector();
                    }
                } else {
                    showDemoLocationSelector();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Home>>> call, Throwable t) {
                showDemoLocationSelector();
            }
        });
    }
    
    private void showHomeSelector(List<Home> homes, View anchorView) {
        // Dismiss existing popup if any
        if (locationSelectorPopupWindow != null && locationSelectorPopupWindow.isShowing()) {
            locationSelectorPopupWindow.dismiss();
            return;
        }
        
        // Find primary home or first home as default selection
        if (selectedHomeId == -1 && homes != null && !homes.isEmpty()) {
            for (Home home : homes) {
                if (home.isPrimary()) {
                    selectedHomeId = home.getId();
                    currentHome = home;
                    break;
                }
            }
            if (selectedHomeId == -1) {
                selectedHomeId = homes.get(0).getId();
                currentHome = homes.get(0);
            }
        }
        
        // Inflate the custom popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.dialog_location_selector, null);
        RecyclerView recyclerViewHomes = popupView.findViewById(R.id.recyclerViewHomes);
        LinearLayout layoutHomeManagement = popupView.findViewById(R.id.layoutHomeManagement);
        
        // Measure the popup view to get its height
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec((int)(248 * getResources().getDisplayMetrics().density), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int popupHeight = popupView.getMeasuredHeight();
        int popupWidth = (int)(248 * getResources().getDisplayMetrics().density);
        
        // Create PopupWindow with fixed width (248dp)
        locationSelectorPopupWindow = new android.widget.PopupWindow(
            popupView,
            popupWidth,
            popupHeight,
            true
        );
        
        // Set background and elevation
        locationSelectorPopupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, android.R.color.transparent));
        locationSelectorPopupWindow.setElevation(8f);
        locationSelectorPopupWindow.setOutsideTouchable(true);
        
        // Ensure homes list is not null
        if (homes == null) {
            homes = new ArrayList<>();
        }
        
        // Setup RecyclerView with popup reference
        LocationSelectorAdapter adapter = new LocationSelectorAdapter(  
            homes, 
            selectedHomeId, 
            home -> {
                selectedHomeId = home.getId();
                currentHome = home;
                updateLocationText(home.getName());
                
                // Update Globals
                Globals.addOrUpdateHome(home);
                
                // Reload data for selected home
                loadData();
                locationSelectorPopupWindow.dismiss();
            }
        );
        
        recyclerViewHomes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHomes.setAdapter(adapter);
        
        // Ensure Home Management is always visible
        if (layoutHomeManagement != null) {
            layoutHomeManagement.setVisibility(View.VISIBLE);
            
            // Home Management click - navigate to HomeManagementActivity
            layoutHomeManagement.setOnClickListener(v -> {
                locationSelectorPopupWindow.dismiss();
                Intent intent = new Intent(this, HomeManagementActivity.class);
                startActivity(intent);
            });
        }
        
        // Position popup below the "My Home" text view, aligned from the left edge
        int[] location = new int[2];
        if (anchorView != null) {
            anchorView.getLocationInWindow(location);
        } else {
            // Fallback: use default position
            location[0] = 24;
            location[1] = 100;
        }
        
        // Convert dp to pixels for spacing and margin
        float density = getResources().getDisplayMetrics().density;
        int spacingPx = (int) (8 * density); // 8dp spacing
        int leftMarginPx = (int) (24 * density); // 24dp left margin from screen edge
        
        // Align popup from the left edge of the screen with 24dp margin
        int x = leftMarginPx;
        int y;
        if (anchorView != null) {
            y = location[1] + anchorView.getHeight() + spacingPx; // Just below the text view
        } else {
            y = location[1] + spacingPx; // Fallback position
        }
        
        // Show popup at calculated position
        locationSelectorPopupWindow.showAtLocation(
            anchorView != null ? anchorView : findViewById(android.R.id.content),
            android.view.Gravity.NO_GRAVITY,
            x,
            y
        );
    }
    
    private void showDemoLocationSelector() {
        // Use mock homes for demo
        List<Home> demoHomes = MockDataProvider.getMockHomes();
        View anchorView = findViewById(R.id.textViewMyHome);
        showHomeSelector(demoHomes, anchorView);
    }
    
    private void updateLocationText(String homeName) {
        TextView textViewMyHome = findViewById(R.id.textViewMyHome);
        if (textViewMyHome != null) {
            textViewMyHome.setText(homeName);
        }
    }
    
    private void loadPrimaryHome() {
        if (apiService == null || !authManager.isLoggedIn()) {
            android.util.Log.w(TAG, "Cannot load primary home: API service not initialized or user not logged in");
            return;
        }
        
        android.util.Log.d(TAG, "Loading primary home from API");
        Call<ApiResponse<Home>> call = apiService.getPrimaryHome();
        call.enqueue(new Callback<ApiResponse<Home>>() {
            @Override
            public void onResponse(Call<ApiResponse<Home>> call, 
                                 Response<ApiResponse<Home>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Home primaryHome = response.body().getData();
                    if (primaryHome != null) {
                        currentHome = primaryHome;
                        selectedHomeId = primaryHome.getId();
                        updateLocationText(primaryHome.getName());
                        android.util.Log.d(TAG, "Primary home loaded: " + primaryHome.getName());
                        
                        // Save to Globals
                        Globals.addOrUpdateHome(primaryHome);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Home>> call, Throwable t) {
                android.util.Log.e(TAG, "Failed to load primary home", t);
            }
        });
    }
}

