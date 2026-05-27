package com.smarthome.iot.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.smarthome.iot.network.ApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Background service that maintains WebSocket connection to backend for real-time device updates.
 * Receives device status and property changes from backend and broadcasts to the app.
 */
public class WebSocketDeviceStatusService extends Service {
    private static final String TAG = "WebSocketStatusService";

    // Broadcast actions
    public static final String ACTION_DEVICE_STATUS_UPDATED = "com.smarthome.iot.DEVICE_STATUS_UPDATED";
    public static final String ACTION_DEVICE_PROPERTY_UPDATED = "com.smarthome.iot.DEVICE_PROPERTY_UPDATED";
    public static final String ACTION_CONNECTION_STATUS = "com.smarthome.iot.WS_CONNECTION_STATUS";

    // Intent extras
    public static final String EXTRA_PRODUCT_ID = "productId";
    public static final String EXTRA_DEVICE_NUM = "deviceNum";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_PROPERTIES = "properties";
    public static final String EXTRA_CONNECTED = "connected";

    // WebSocket Configuration
    private static final long RECONNECT_DELAY_MS = 5000;
    private static final long HEARTBEAT_INTERVAL_MS = 30000;

    private WebSocket webSocket;
    private OkHttpClient httpClient;
    private String jwtToken;
    private Handler reconnectHandler;
    private Handler heartbeatHandler;
    private boolean isConnecting = false;
    private Runnable heartbeatRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        reconnectHandler = new Handler(Looper.getMainLooper());
        heartbeatHandler = new Handler(Looper.getMainLooper());

        // Configure OkHttpClient with longer timeouts for WebSocket
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // No timeout for WebSocket
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS) // Send ping every 20 seconds
                .build();

        Log.d(TAG, "WebSocket Device Status Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (com.smarthome.iot.utils.MockDataProvider.isMockupMode()) {
            broadcastConnectionStatus(true);
            Log.d(TAG, "WebSocket service mocked as connected");
            return START_STICKY;
        }

        if (intent != null) {
            jwtToken = intent.getStringExtra("token");

            if (jwtToken != null) {
                connectToWebSocket();
                startHeartbeat();
            } else {
                Log.e(TAG, "Missing JWT token");
                stopSelf();
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Connect to backend WebSocket endpoint
     */
    private void connectToWebSocket() {
        if (isConnecting) {
            Log.d(TAG, "Already connecting, skipping");
            return;
        }

        if (webSocket != null) {
            Log.d(TAG, "Already connected");
            return;
        }

        isConnecting = true;

        // Build WebSocket URL with JWT token as query parameter
        String baseUrl = ApiClient.getBaseUrl(); // e.g., "http://172.86.88.76:3003/api/v1/"
        String wsUrl = baseUrl
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                + "devices/stream?token=" + jwtToken;

        Log.d(TAG, "Connecting to WebSocket: " + wsUrl);

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
                isConnecting = false;
                broadcastConnectionStatus(true);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleWebSocketMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                handleWebSocketMessage(bytes.utf8());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.w(TAG, "WebSocket closing: " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.w(TAG, "WebSocket closed: " + reason);
                isConnecting = false;
                broadcastConnectionStatus(false);
                WebSocketDeviceStatusService.this.webSocket = null;
                scheduleReconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error", t);
                isConnecting = false;
                broadcastConnectionStatus(false);
                WebSocketDeviceStatusService.this.webSocket = null;
                scheduleReconnect();
            }
        });
    }

    /**
     * Handle incoming WebSocket messages
     */
    private void handleWebSocketMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");

            Log.d(TAG, "WebSocket message received: " + type);

            switch (type) {
                case "connected":
                    Log.d(TAG, "Connection confirmed: " + json.optString("message"));
                    break;

                case "pong":
                    // Heartbeat response
                    break;

                case "device_status":
                    handleDeviceStatusUpdate(json.getJSONObject("data"));
                    break;

                case "device_property":
                    handleDevicePropertyUpdate(json.getJSONObject("data"));
                    break;

                default:
                    Log.w(TAG, "Unknown message type: " + type);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing WebSocket message", e);
        }
    }

    /**
     * Handle device status update (online/offline)
     */
    private void handleDeviceStatusUpdate(JSONObject data) throws JSONException {
        String productId = data.getString("productId");
        String deviceNum = data.getString("deviceNum");
        String status = data.getString("status");

        Intent intent = new Intent(ACTION_DEVICE_STATUS_UPDATED);
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        intent.putExtra(EXTRA_DEVICE_NUM, deviceNum);
        intent.putExtra(EXTRA_STATUS, status);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Broadcast status update: " + productId + "/" + deviceNum + " -> " + status);
    }

    /**
     * Handle device property update
     */
    private void handleDevicePropertyUpdate(JSONObject data) throws JSONException {
        String productId = data.getString("productId");
        String deviceNum = data.getString("deviceNum");
        JSONObject properties = data.getJSONObject("properties");

        Intent intent = new Intent(ACTION_DEVICE_PROPERTY_UPDATED);
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        intent.putExtra(EXTRA_DEVICE_NUM, deviceNum);
        intent.putExtra(EXTRA_PROPERTIES, properties.toString());

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Broadcast property update: " + productId + "/" + deviceNum);
    }

    /**
     * Broadcast WebSocket connection status
     */
    private void broadcastConnectionStatus(boolean connected) {
        Intent intent = new Intent(ACTION_CONNECTION_STATUS);
        intent.putExtra(EXTRA_CONNECTED, connected);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Broadcast connection status: " + (connected ? "connected" : "disconnected"));
    }

    /**
     * Schedule reconnection attempt
     */
    private void scheduleReconnect() {
        reconnectHandler.postDelayed(() -> {
            Log.d(TAG, "Attempting to reconnect...");
            connectToWebSocket();
        }, RECONNECT_DELAY_MS);
    }

    /**
     * Start heartbeat to keep connection alive
     */
    private void startHeartbeat() {
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (webSocket != null) {
                    try {
                        JSONObject ping = new JSONObject();
                        ping.put("type", "ping");
                        webSocket.send(ping.toString());
                        Log.d(TAG, "Sent heartbeat ping");
                    } catch (JSONException e) {
                        Log.e(TAG, "Error creating heartbeat message", e);
                    }
                }
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        };

        heartbeatHandler.post(heartbeatRunnable);
    }

    /**
     * Stop heartbeat
     */
    private void stopHeartbeat() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");

        stopHeartbeat();

        if (webSocket != null) {
            webSocket.close(1000, "Service stopped");
            webSocket = null;
        }

        if (reconnectHandler != null) {
            reconnectHandler.removeCallbacksAndMessages(null);
        }

        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}
