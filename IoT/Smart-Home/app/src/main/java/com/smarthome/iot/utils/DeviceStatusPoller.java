package com.smarthome.iot.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DeviceStatusPoller - Polls device status periodically
 *
 * IMPORTANT: This poller should ONLY be used in device control pages (DeviceControlDetailActivity)
 * where real-time status updates are critical. Do NOT use in MainActivity to avoid excessive API calls.
 *
 * Features:
 * - Configurable polling interval (default 3 seconds from Globals.DEVICE_POLLING_INTERVAL)
 * - Auto-start/stop based on activity lifecycle
 * - Device status update callbacks
 * - Handles errors gracefully
 *
 * Usage in DeviceControlDetailActivity:
 * ```java
 * // In onCreate() or onResume():
 * DeviceStatusPoller poller = new DeviceStatusPoller(apiService, homeId);
 * poller.setDeviceStatusListener(new DeviceStatusPoller.DeviceStatusListener() {
 *     @Override
 *     public void onDevicesUpdated(List<Device> devices) {
 *         // Update UI with latest device status
 *         updateDeviceUI(devices);
 *     }
 *
 *     @Override
 *     public void onError(String error) {
 *         Log.e("DeviceControl", "Polling error: " + error);
 *     }
 * });
 * poller.start();
 *
 * // In onPause() or onDestroy():
 * poller.stop();
 * ```
 *
 * Flow:
 * 1. User toggles device control on DeviceControlDetailActivity
 * 2. API call is made to backend
 * 3. Backend publishes command to MQTT/EMQX
 * 4. IoT device receives command and executes
 * 5. Device publishes status back to EMQX
 * 6. Backend receives status and broadcasts via WebSocket
 * 7. Poller also fetches status every 3 seconds as backup
 * 8. UI updates with latest device state
 */
public class DeviceStatusPoller {
    private static final String TAG = "DeviceStatusPoller";

    // API service
    private final ApiService apiService;
    private final Integer homeId;

    // Handler for periodic polling
    private final Handler handler;
    private Runnable pollingRunnable;

    // Polling state
    private boolean isPolling = false;
    private long pollingInterval;

    // Listener for device status updates
    private DeviceStatusListener listener;

    /**
     * Device status listener interface
     */
    public interface DeviceStatusListener {
        /**
         * Called when device list is updated
         * @param devices Updated list of devices
         */
        void onDevicesUpdated(List<Device> devices);

        /**
         * Called when polling encounters an error
         * @param error Error message
         */
        void onError(String error);
    }

    /**
     * Constructor
     * @param apiService API service for making requests
     * @param homeId Home ID to poll devices for
     */
    public DeviceStatusPoller(ApiService apiService, Integer homeId) {
        this.apiService = apiService;
        this.homeId = homeId;
        this.handler = new Handler(Looper.getMainLooper());
        this.pollingInterval = Globals.getDevicePollingInterval(); // 3 seconds from Globals
        Log.d(TAG, "DeviceStatusPoller created for homeId: " + homeId + ", polling interval: " + pollingInterval + "ms");
    }

    /**
     * Constructor with custom polling interval
     * @param apiService API service for making requests
     * @param homeId Home ID to poll devices for
     * @param pollingInterval Custom polling interval in milliseconds
     */
    public DeviceStatusPoller(ApiService apiService, Integer homeId, long pollingInterval) {
        this.apiService = apiService;
        this.homeId = homeId;
        this.handler = new Handler(Looper.getMainLooper());
        this.pollingInterval = pollingInterval;
        Log.d(TAG, "DeviceStatusPoller created with custom interval: " + pollingInterval + "ms");
    }

    /**
     * Set device status listener
     * @param listener Listener to receive updates
     */
    public void setDeviceStatusListener(DeviceStatusListener listener) {
        this.listener = listener;
        Log.d(TAG, "Device status listener set");
    }

    /**
     * Start polling for device status updates
     */
    public void start() {
        if (isPolling) {
            Log.d(TAG, "Polling already started, ignoring start request");
            return;
        }

        if (apiService == null) {
            Log.e(TAG, "Cannot start polling: apiService is null");
            notifyError("API service not available");
            return;
        }

        isPolling = true;
        Log.d(TAG, "Starting device status polling (interval: " + pollingInterval + "ms)");

        // Create polling runnable
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPolling) {
                    pollDeviceStatus();
                    // Schedule next poll
                    handler.postDelayed(this, pollingInterval);
                }
            }
        };

        // Start polling immediately
        handler.post(pollingRunnable);
    }

    /**
     * Stop polling
     */
    public void stop() {
        if (!isPolling) {
            Log.d(TAG, "Polling not started, ignoring stop request");
            return;
        }

        isPolling = false;
        Log.d(TAG, "Stopping device status polling");

        // Remove callbacks
        if (pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }

    /**
     * Check if polling is active
     * @return true if polling is active
     */
    public boolean isPolling() {
        return isPolling;
    }

    /**
     * Update polling interval
     * @param intervalMs New polling interval in milliseconds
     */
    public void setPollingInterval(long intervalMs) {
        this.pollingInterval = intervalMs;
        Log.d(TAG, "Polling interval updated to: " + intervalMs + "ms");

        // Restart polling if active
        if (isPolling) {
            stop();
            start();
        }
    }

    /**
     * Poll device status from API
     */
    private void pollDeviceStatus() {
        Log.d(TAG, "Polling device status for homeId: " + homeId);

        try {
            // Call API to get devices
            Call<ApiResponse<List<Device>>> call = apiService.getDevices(
                    null,       // roomId (null for all rooms)
                    homeId,     // homeId
                    null,       // status (null for all statuses)
                    null,       // type (null for all types)
                    null,       // page
                    null        // limit
            );

            call.enqueue(new Callback<ApiResponse<List<Device>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Device>>> call, Response<ApiResponse<List<Device>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<Device> devices = response.body().getData();
                        if (devices == null) {
                            devices = new ArrayList<>();
                        }
                        Log.d(TAG, "Device status poll successful: " + devices.size() + " devices");
                        notifyDevicesUpdated(devices);
                    } else {
                        String errorMsg = "Failed to poll device status: HTTP " + response.code();
                        Log.w(TAG, errorMsg);
                        notifyError(errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Device>>> call, Throwable t) {
                    String errorMsg = "Device status poll failed: " + t.getMessage();
                    Log.e(TAG, errorMsg, t);
                    notifyError(errorMsg);
                }
            });

        } catch (Exception e) {
            String errorMsg = "Error polling device status: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            notifyError(errorMsg);
        }
    }

    /**
     * Notify listener of device updates
     * @param devices Updated device list
     */
    private void notifyDevicesUpdated(List<Device> devices) {
        if (listener != null) {
            handler.post(() -> listener.onDevicesUpdated(devices));
        }
    }

    /**
     * Notify listener of error
     * @param error Error message
     */
    private void notifyError(String error) {
        if (listener != null) {
            handler.post(() -> listener.onError(error));
        }
    }
}
