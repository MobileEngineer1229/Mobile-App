package com.smarthome.iot.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Notification;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.NotificationItemAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.ThemeHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewNotifications;
    private NotificationItemAdapter adapter;
    private com.google.android.material.button.MaterialButton buttonTabGeneral;
    private com.google.android.material.button.MaterialButton buttonTabSmartHome;
    private ProgressBar progressBar;
    private List<Notification> allNotifications = new ArrayList<>();
    private String currentCategory = "general";
    private List<NotificationItemAdapter.NotificationItem> notificationItems;
    private ApiService apiService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        initializeViews();
        setupRecyclerView();
        setupTabs();
        loadNotifications();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        ImageButton buttonSettings = findViewById(R.id.buttonSettings);
        buttonBack.setOnClickListener(v -> finish());
        if (buttonSettings != null) {
            buttonSettings.setOnClickListener(v -> {
                // Open notification settings
            });
        }

        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        buttonTabGeneral = findViewById(R.id.buttonTabGeneral);
        buttonTabSmartHome = findViewById(R.id.buttonTabSmartHome);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        if (recyclerViewNotifications == null) return;
        
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationItems = new ArrayList<>();
        adapter = new NotificationItemAdapter(notificationItems, this::onNotificationClick);
        recyclerViewNotifications.setAdapter(adapter);
    }

    private void setupTabs() {
        if (buttonTabGeneral != null && buttonTabSmartHome != null) {
            buttonTabGeneral.setOnClickListener(v -> switchCategory("general"));
            buttonTabSmartHome.setOnClickListener(v -> switchCategory("smart_home"));
            switchCategory("general"); // Default to general
        }
    }

    private void switchCategory(String category) {
        currentCategory = category;
        
        // Update tab appearance - use new tab button style
        if (buttonTabGeneral != null && buttonTabSmartHome != null) {
            // Clear background tint to ensure our drawables are used (no stroke)
            buttonTabGeneral.setBackgroundTintList(null);
            buttonTabGeneral.setElevation(0f);
            buttonTabSmartHome.setBackgroundTintList(null);
            buttonTabSmartHome.setElevation(0f);
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                buttonTabGeneral.setStateListAnimator(null);
                buttonTabSmartHome.setStateListAnimator(null);
            }
            
            if ("general".equals(category)) {
                buttonTabGeneral.post(() -> {
                    buttonTabGeneral.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_selected_background));
                    buttonTabGeneral.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                    buttonTabGeneral.invalidate();
                });
                buttonTabSmartHome.post(() -> {
                    buttonTabSmartHome.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_unselected_background));
                    buttonTabSmartHome.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                    buttonTabSmartHome.invalidate();
                });
            } else {
                buttonTabSmartHome.post(() -> {
                    buttonTabSmartHome.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_selected_background));
                    buttonTabSmartHome.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                    buttonTabSmartHome.invalidate();
                });
                buttonTabGeneral.post(() -> {
                    buttonTabGeneral.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.tab_unselected_background));
                    buttonTabGeneral.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                    buttonTabGeneral.invalidate();
                });
            }
        }
        
        // Filter and display notifications
        filterNotifications();
    }

    private void filterNotifications() {
        notificationItems.clear();
        
        // Check if allNotifications is null or empty
        if (allNotifications == null || allNotifications.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }
        
        // Filter notifications by category
        List<Notification> filtered = new ArrayList<>();
        for (Notification notification : allNotifications) {
            if (notification != null && notification.getCategory() != null && 
                notification.getCategory().equals(currentCategory)) {
                filtered.add(notification);
            }
        }
        
        // Group by date and add section headers
        List<Notification> todayNotifications = new ArrayList<>();
        List<Notification> yesterdayNotifications = new ArrayList<>();
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);
        
        // Simple grouping: notifications with "AM" or early times are today, "PM" or late times are yesterday
        for (Notification notification : filtered) {
            if (notification == null) continue;
            String timestamp = notification.getTimestamp();
            if (timestamp != null && (timestamp.contains("AM") || timestamp.contains("09:") || timestamp.contains("08:") || timestamp.contains("07:"))) {
                todayNotifications.add(notification);
            } else {
                yesterdayNotifications.add(notification);
            }
        }
        
        // Add "Today" section
        if (!todayNotifications.isEmpty()) {
            notificationItems.add(new NotificationItemAdapter.NotificationItem(getString(R.string.today)));
            for (Notification notification : todayNotifications) {
                if (notification != null) {
                    notificationItems.add(new NotificationItemAdapter.NotificationItem(notification));
                }
            }
        }
        
        // Add "Yesterday" section
        if (!yesterdayNotifications.isEmpty()) {
            notificationItems.add(new NotificationItemAdapter.NotificationItem(getString(R.string.yesterday)));
            for (Notification notification : yesterdayNotifications) {
                if (notification != null) {
                    notificationItems.add(new NotificationItemAdapter.NotificationItem(notification));
                }
            }
        }
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void onNotificationClick(Notification notification) {
        if (notification == null) return;
        
        // Mark as read via API
        if (!notification.isRead() && authManager.isLoggedIn() && notification.getId() > 0) {
            Call<ApiResponse<Notification>> call = apiService.markNotificationAsRead(notification.getId());
            call.enqueue(new Callback<ApiResponse<Notification>>() {
                @Override
                public void onResponse(Call<ApiResponse<Notification>> call, Response<ApiResponse<Notification>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        notification.setRead(true);
                        filterNotifications();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Notification>> call, Throwable t) {
                    // Silently fail
                }
            });
        }
        // Could navigate to notification detail screen here
    }
    
    private void loadNotifications() {
        showLoading(true);
        
        // Always use API - load all notifications (both categories)
        Call<ApiResponse<List<Notification>>> call = apiService.getNotifications(null, 100, null, null, null);
        call.enqueue(new Callback<ApiResponse<List<Notification>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Notification>>> call, Response<ApiResponse<List<Notification>>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Notification> notifications = response.body().getData();
                    if (notifications != null && !notifications.isEmpty()) {
                        allNotifications = mapApiNotificationsToUI(notifications);
                        filterNotifications();
                        android.util.Log.d("NotificationsActivity", "Loaded " + notifications.size() + " notifications from API");
                    } else {
                        // No notifications from API, show empty state (not demo data)
                        allNotifications.clear();
                        filterNotifications();
                        android.util.Log.d("NotificationsActivity", "No notifications found from API");
                    }
                } else {
                    // API error - log and show empty state
                    android.util.Log.w("NotificationsActivity", "API error loading notifications");
                    allNotifications.clear();
                    filterNotifications();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Notification>>> call, Throwable t) {
                showLoading(false);
                android.util.Log.e("NotificationsActivity", "Failed to load notifications from API", t);
                // Network error - show empty state (not demo data)
                allNotifications.clear();
                filterNotifications();
            }
        });
    }
    
    private List<Notification> mapApiNotificationsToUI(List<Notification> apiNotifications) {
        List<Notification> uiNotifications = new ArrayList<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        String todayStr = dateFormat.format(today.getTime());
        
        for (Notification apiNotif : apiNotifications) {
            Notification uiNotif = new Notification();
            uiNotif.setId(apiNotif.getId());
            uiNotif.setUserId(apiNotif.getUserId());
            uiNotif.setTitle(apiNotif.getTitle());
            uiNotif.setMessage(apiNotif.getMessage()); // This will be used as description in UI
            uiNotif.setType(apiNotif.getType());
            uiNotif.setIcon(apiNotif.getIcon());
            uiNotif.setRead(apiNotif.isRead());
            uiNotif.setReadAt(apiNotif.getReadAt());
            uiNotif.setCreatedAt(apiNotif.getCreatedAt());
            
            // Map type to category
            String category = "general";
            if (apiNotif.getType() != null) {
                if (apiNotif.getType().equals("security") || apiNotif.getType().equals("alert")) {
                    category = "smart_home";
                }
            }
            uiNotif.setCategory(category);
            
            // Map type to iconType
            String iconType = "info";
            if (apiNotif.getType() != null) {
                switch (apiNotif.getType()) {
                    case "security":
                        iconType = "security";
                        break;
                    case "system":
                        iconType = "update";
                        break;
                    case "feature":
                        iconType = "feature";
                        break;
                    case "reminder":
                        iconType = "event";
                        break;
                }
            }
            uiNotif.setIconType(iconType);
            
            // Format timestamp
            if (apiNotif.getCreatedAt() != null) {
                String dateStr = dateFormat.format(apiNotif.getCreatedAt());
                String timeStr = timeFormat.format(apiNotif.getCreatedAt());
                
                if (dateStr.equals(todayStr)) {
                    uiNotif.setTimestamp(timeStr);
                } else {
                    Calendar notifDate = Calendar.getInstance();
                    notifDate.setTime(apiNotif.getCreatedAt());
                    Calendar yesterday = Calendar.getInstance();
                    yesterday.add(Calendar.DAY_OF_YEAR, -1);
                    
                    if (dateFormat.format(notifDate.getTime()).equals(dateFormat.format(yesterday.getTime()))) {
                        uiNotif.setTimestamp(timeStr);
                    } else {
                        uiNotif.setTimestamp(timeStr);
                    }
                }
            }
            
            // Set badge from metadata if available
            if (apiNotif.getMetadata() != null) {
                Object badgeObj = apiNotif.getMetadata().get("badge");
                if (badgeObj != null) {
                    uiNotif.setBadge(badgeObj.toString());
                }
            }
            
            uiNotifications.add(uiNotif);
        }
        
        return uiNotifications;
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void loadDemoNotifications() {
        if (allNotifications == null) {
            allNotifications = new ArrayList<>();
        } else {
            allNotifications.clear();
        }
        Calendar cal = Calendar.getInstance();
        
        // General Notifications - Today
        Notification notif1 = new Notification();
        notif1.setId(1);
        notif1.setTitle("Account Security Alert");
        notif1.setMessage("We've noticed some unusual activity on your account. Please review your recent logins and update your password if necessary.");
        notif1.setType("security");
        notif1.setCategory("general");
        notif1.setIcon("security");
        notif1.setRead(false);
        notif1.setTimestamp("09:41 AM");
        notif1.setBadge("LOCK");
        notif1.setCreatedAt(cal.getTime());
        allNotifications.add(notif1);
        
        Notification notif2 = new Notification();
        notif2.setId(2);
        notif2.setTitle("System Update Available");
        notif2.setMessage("A new system update is ready for installation. It includes performance improvements and bug fixes.");
        notif2.setType("system");
        notif2.setCategory("general");
        notif2.setIcon("update");
        notif2.setRead(false);
        notif2.setTimestamp("08:46 AM");
        notif2.setCreatedAt(cal.getTime());
        allNotifications.add(notif2);
        
        // General Notifications - Yesterday
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Notification notif3 = new Notification();
        notif3.setId(3);
        notif3.setTitle("Password Reset Successful");
        notif3.setMessage("Your password has been successfully reset. If you didn't request this change, please contact support immediately.");
        notif3.setType("security");
        notif3.setCategory("general");
        notif3.setIcon("password");
        notif3.setRead(true);
        notif3.setTimestamp("20:30 PM");
        notif3.setCreatedAt(cal.getTime());
        allNotifications.add(notif3);
        
        Notification notif4 = new Notification();
        notif4.setId(4);
        notif4.setTitle("Exciting New Feature");
        notif4.setMessage("We've just launched a new feature that will enhance your user experience. Check it out now!");
        notif4.setType("feature");
        notif4.setCategory("general");
        notif4.setIcon("feature");
        notif4.setRead(true);
        notif4.setTimestamp("16:29 PM");
        notif4.setBadge("NEW");
        notif4.setCreatedAt(cal.getTime());
        allNotifications.add(notif4);
        
        Notification notif5 = new Notification();
        notif5.setId(5);
        notif5.setTitle("Event Reminder");
        notif5.setMessage("Don't forget about your scheduled event tomorrow at 2:00 PM.");
        notif5.setType("reminder");
        notif5.setCategory("general");
        notif5.setIcon("event");
        notif5.setRead(false);
        notif5.setTimestamp("17:00 PM");
        notif5.setCreatedAt(cal.getTime());
        allNotifications.add(notif5);
        
        // Smart Home Notifications
        cal = Calendar.getInstance();
        Notification notif6 = new Notification();
        notif6.setId(6);
        notif6.setTitle("Device Status Alert");
        notif6.setMessage("Your living room light has been turned on. Is this expected?");
        notif6.setType("alert");
        notif6.setCategory("smart_home");
        notif6.setIcon("device");
        notif6.setRead(false);
        notif6.setTimestamp("09:30 AM");
        notif6.setCreatedAt(cal.getTime());
        allNotifications.add(notif6);
        
        Notification notif7 = new Notification();
        notif7.setId(7);
        notif7.setTitle("Energy Consumption Alert");
        notif7.setMessage("Your energy usage today is 15% higher than usual. Check your devices.");
        notif7.setType("alert");
        notif7.setCategory("smart_home");
        notif7.setIcon("energy");
        notif7.setRead(false);
        notif7.setTimestamp("08:00 AM");
        notif7.setCreatedAt(cal.getTime());
        allNotifications.add(notif7);
        
        Notification notif8 = new Notification();
        notif8.setId(8);
        notif8.setTitle("Device Maintenance Reminder");
        notif8.setMessage("It's time to check your air conditioner filter. Last maintenance was 3 months ago.");
        notif8.setType("reminder");
        notif8.setCategory("smart_home");
        notif8.setIcon("device");
        notif8.setRead(false);
        notif8.setTimestamp("07:45 AM");
        notif8.setCreatedAt(cal.getTime());
        allNotifications.add(notif8);
        
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Notification notif9 = new Notification();
        notif9.setId(9);
        notif9.setTitle("Automation Triggered");
        notif9.setMessage("Your 'Good Morning' automation has been activated successfully.");
        notif9.setType("feature");
        notif9.setCategory("smart_home");
        notif9.setIcon("device");
        notif9.setRead(true);
        notif9.setTimestamp("06:30 AM");
        notif9.setCreatedAt(cal.getTime());
        allNotifications.add(notif9);
        
        Notification notif10 = new Notification();
        notif10.setId(10);
        notif10.setTitle("Security Alert");
        notif10.setMessage("Motion detected in your backyard at 11:23 PM. Check your security camera.");
        notif10.setType("security");
        notif10.setCategory("smart_home");
        notif10.setIcon("security");
        notif10.setRead(false);
        notif10.setTimestamp("23:25 PM");
        notif10.setCreatedAt(cal.getTime());
        allNotifications.add(notif10);
        
        filterNotifications();
    }
}

