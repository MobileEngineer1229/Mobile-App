package com.smarthome.iot.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.smarthome.iot.models.Device;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.Globals;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/**
 * SSE client that connects to the backend's /api/v1/devices/stream endpoint.
 * Receives real-time device updates pushed from the backend.
 * Updates the Globals cache and broadcasts events for UI refresh.
 */
public class DeviceSSEClient {
    private static final String TAG = "DeviceSSEClient";
    private static final String SSE_URL = "http://172.86.88.76:3003/api/v1/devices/stream";
    private static final long RECONNECT_DELAY_INITIAL_MS = 5000;
    private static final long RECONNECT_DELAY_MAX_MS = 60000;

    public static final String ACTION_DEVICE_UPDATED = "com.smarthome.iot.DEVICE_UPDATED";
    public static final String EXTRA_PRODUCT_ID = "product_id";
    public static final String EXTRA_DEVICE_NUM = "device_num";

    private static DeviceSSEClient instance;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private EventSource eventSource;
    private boolean running = false;
    private long currentReconnectDelay = RECONNECT_DELAY_INITIAL_MS;
    private int reconnectAttempts = 0;

    private DeviceSSEClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized DeviceSSEClient getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceSSEClient(context);
        }
        return instance;
    }

    public void connect() {
        if (com.smarthome.iot.utils.MockDataProvider.isMockupMode()) {
            running = true;
            Log.i(TAG, "SSE mocked as connected");
            return;
        }

        if (running) {
            Log.d(TAG, "Already connected");
            return;
        }

        AuthManager authManager = new AuthManager(context);
        String token = authManager.getToken();
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "No auth token — cannot connect SSE");
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // No read timeout for SSE
                .build();

        Request request = new Request.Builder()
                .url(SSE_URL)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "text/event-stream")
                .build();

        EventSource.Factory factory = EventSources.createFactory(client);
        eventSource = factory.newEventSource(request, new EventSourceListener() {
            @Override
            public void onOpen(@NonNull EventSource source, @NonNull Response response) {
                running = true;
                currentReconnectDelay = RECONNECT_DELAY_INITIAL_MS;
                reconnectAttempts = 0;
                Log.i(TAG, "SSE connected");
            }

            @Override
            public void onEvent(@NonNull EventSource source, @Nullable String id,
                                @Nullable String type, @NonNull String data) {
                if ("device-update".equals(type)) {
                    handleDeviceUpdate(data);
                }
            }

            @Override
            public void onClosed(@NonNull EventSource source) {
                running = false;
                Log.w(TAG, "SSE closed — reconnecting");
                scheduleReconnect();
            }

            @Override
            public void onFailure(@NonNull EventSource source, @Nullable Throwable t,
                                  @Nullable Response response) {
                running = false;
                reconnectAttempts++;
                if (reconnectAttempts <= 3) {
                    Log.e(TAG, "SSE failure — reconnecting (attempt " + reconnectAttempts + ")", t);
                } else if (reconnectAttempts % 10 == 0) {
                    Log.w(TAG, "SSE still failing after " + reconnectAttempts + " attempts");
                }
                scheduleReconnect();
            }
        });
    }

    public void disconnect() {
        running = false;
        if (eventSource != null) {
            eventSource.cancel();
            eventSource = null;
        }
        mainHandler.removeCallbacksAndMessages(null);
        Log.i(TAG, "SSE disconnected");
    }

    public boolean isConnected() {
        return running;
    }

    private void scheduleReconnect() {
        if (com.smarthome.iot.utils.MockDataProvider.isMockupMode()) {
            return;
        }
        mainHandler.postDelayed(this::connect, currentReconnectDelay);
        currentReconnectDelay = Math.min(currentReconnectDelay * 2, RECONNECT_DELAY_MAX_MS);
    }

    private void handleDeviceUpdate(String jsonData) {
        try {
            JSONObject event = new JSONObject(jsonData);
            String productId = event.getString("productId");
            String deviceNum = event.getString("deviceNum");
            String type = event.getString("type");
            JSONObject data = event.optJSONObject("data");

            Log.d(TAG, "Device update: " + type + " " + productId + "/" + deviceNum);

            // Update cached device status if it's a status event
            if ("status".equals(type) && data != null) {
                boolean online = data.optInt("status", 0) == 1
                        || "online".equals(data.optString("status"));
                Device device = findCachedDevice(productId, deviceNum);
                if (device != null) {
                    device.setStatus(online ? "online" : "offline");
                }
            }

            // Broadcast for UI refresh
            Intent intent = new Intent(ACTION_DEVICE_UPDATED);
            intent.putExtra(EXTRA_PRODUCT_ID, productId);
            intent.putExtra(EXTRA_DEVICE_NUM, deviceNum);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse SSE device update", e);
        }
    }

    private Device findCachedDevice(String productId, String deviceNum) {
        List<Device> devices = Globals.getCachedDevices();
        if (devices == null) return null;
        for (Device d : devices) {
            if (d.getMetadata() != null) {
                Object pid = d.getMetadata().get("productId");
                Object dnum = d.getMetadata().get("deviceNum");
                if (productId.equals(String.valueOf(pid)) && deviceNum.equals(String.valueOf(dnum))) {
                    return d;
                }
            }
        }
        return null;
    }
}
