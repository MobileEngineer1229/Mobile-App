package com.smarthome.iot.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.smarthome.iot.utils.Globals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * WebSocketManager - Manages WebSocket connection for real-time device updates
 *
 * Features:
 * - Auto-reconnection with exponential backoff
 * - JWT token authentication
 * - Device status update broadcasting
 * - Connection state management
 * - Heartbeat/ping handling
 *
 * Usage:
 * 1. Get instance: WebSocketManager.getInstance()
 * 2. Connect: connect(token)
 * 3. Add listener: addDeviceUpdateListener(listener)
 * 4. Disconnect: disconnect()
 */
public class WebSocketManager extends WebSocketListener {
    private static final String TAG = "WebSocketManager";

    // Singleton instance
    private static WebSocketManager instance;

    // WebSocket connection
    private OkHttpClient client;
    private WebSocket webSocket;
    private String authToken;

    // Connection state
    private boolean isConnected = false;
    private boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    // Reconnection delays (exponential backoff)
    private static final long[] RECONNECT_DELAYS = {
        1000,   // 1 second
        2000,   // 2 seconds
        5000,   // 5 seconds
        10000,  // 10 seconds
        30000   // 30 seconds
    };

    // Handlers
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private Runnable reconnectRunnable;

    // Gson for JSON parsing
    private final Gson gson = new Gson();

    // Listeners for device updates
    private final List<DeviceUpdateListener> deviceUpdateListeners = new ArrayList<>();

    // Connection listeners
    private final List<ConnectionListener> connectionListeners = new ArrayList<>();

    /**
     * Device update listener interface
     */
    public interface DeviceUpdateListener {
        /**
         * Called when device status is updated
         * @param deviceData Device status data as JsonObject
         */
        void onDeviceStatusUpdate(JsonObject deviceData);

        /**
         * Called when device property is updated
         * @param propertyData Device property data as JsonObject
         */
        void onDevicePropertyUpdate(JsonObject propertyData);
    }

    /**
     * Connection listener interface
     */
    public interface ConnectionListener {
        /**
         * Called when WebSocket connects successfully
         */
        void onConnected();

        /**
         * Called when WebSocket disconnects
         * @param reason Disconnect reason
         */
        void onDisconnected(String reason);

        /**
         * Called when connection fails
         * @param error Error message
         */
        void onError(String error);
    }

    /**
     * Get singleton instance
     */
    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    /**
     * Private constructor (singleton)
     */
    private WebSocketManager() {
        // Create OkHttp client with long timeouts for WebSocket
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for WebSocket
                .writeTimeout(10, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS) // Send ping every 30 seconds
                .build();
    }

    /**
     * Connect to WebSocket server with JWT token
     * @param token JWT authentication token
     */
    public void connect(String token) {
        if (com.smarthome.iot.utils.MockDataProvider.isMockupMode()) {
            this.authToken = token;
            shouldReconnect = false;
            isConnected = true;
            Globals.setWebSocketConnected(true);
            mainHandler.post(() -> {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnected();
                }
            });
            Log.d(TAG, "WebSocket mocked as connected");
            return;
        }

        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Cannot connect: token is null or empty");
            notifyConnectionError("Authentication token is missing");
            return;
        }

        this.authToken = token;
        shouldReconnect = true;
        reconnectAttempts = 0;

        Log.d(TAG, "Initiating WebSocket connection...");
        connectInternal();
    }

    /**
     * Internal connection method
     */
    private void connectInternal() {
        if (webSocket != null) {
            Log.d(TAG, "Closing existing WebSocket connection before reconnecting");
            webSocket.close(1000, "Reconnecting");
            webSocket = null;
        }

        try {
            String wsUrl = Globals.getWebSocketUrl(authToken);
            Log.d(TAG, "Connecting to: " + wsUrl);

            Request request = new Request.Builder()
                    .url(wsUrl)
                    .addHeader("Authorization", "Bearer " + authToken)
                    .build();

            webSocket = client.newWebSocket(request, this);

        } catch (Exception e) {
            Log.e(TAG, "Error creating WebSocket connection", e);
            notifyConnectionError("Failed to create WebSocket: " + e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * Disconnect from WebSocket server
     */
    public void disconnect() {
        Log.d(TAG, "Disconnecting WebSocket");
        shouldReconnect = false;
        reconnectAttempts = 0;

        // Cancel any pending reconnect attempts
        if (reconnectRunnable != null) {
            reconnectHandler.removeCallbacks(reconnectRunnable);
            reconnectRunnable = null;
        }

        if (webSocket != null) {
            webSocket.close(1000, "User disconnected");
            webSocket = null;
        }

        isConnected = false;
        Globals.setWebSocketConnected(false);
    }

    /**
     * Send a message through WebSocket
     * @param message Message to send (will be converted to JSON string)
     */
    public void sendMessage(Object message) {
        if (webSocket == null || !isConnected) {
            Log.w(TAG, "Cannot send message: WebSocket not connected");
            return;
        }

        try {
            String json = gson.toJson(message);
            boolean success = webSocket.send(json);
            if (success) {
                Log.d(TAG, "Message sent: " + json);
            } else {
                Log.e(TAG, "Failed to send message");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
        }
    }

    /**
     * Check if WebSocket is connected
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Add device update listener
     */
    public void addDeviceUpdateListener(DeviceUpdateListener listener) {
        if (listener != null && !deviceUpdateListeners.contains(listener)) {
            deviceUpdateListeners.add(listener);
            Log.d(TAG, "Device update listener added. Total listeners: " + deviceUpdateListeners.size());
        }
    }

    /**
     * Remove device update listener
     */
    public void removeDeviceUpdateListener(DeviceUpdateListener listener) {
        deviceUpdateListeners.remove(listener);
        Log.d(TAG, "Device update listener removed. Total listeners: " + deviceUpdateListeners.size());
    }

    /**
     * Add connection listener
     */
    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null && !connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
        }
    }

    /**
     * Remove connection listener
     */
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    /**
     * Clear all listeners
     */
    public void clearListeners() {
        deviceUpdateListeners.clear();
        connectionListeners.clear();
        Log.d(TAG, "All listeners cleared");
    }

    // ========== WebSocketListener Callbacks ==========

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.d(TAG, "WebSocket connected successfully!");
        isConnected = true;
        reconnectAttempts = 0;
        Globals.setWebSocketConnected(true);

        // Notify connection listeners on main thread
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onConnected();
            }
        });
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.d(TAG, "WebSocket message received: " + text);

        try {
            JsonObject message = gson.fromJson(text, JsonObject.class);
            handleMessage(message);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing WebSocket message", e);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.d(TAG, "WebSocket binary message received: " + bytes.hex());
        // Handle binary messages if needed (currently not used)
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "WebSocket closing: code=" + code + ", reason=" + reason);
        isConnected = false;
        Globals.setWebSocketConnected(false);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.d(TAG, "WebSocket closed: code=" + code + ", reason=" + reason);
        isConnected = false;
        Globals.setWebSocketConnected(false);

        // Notify connection listeners on main thread
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onDisconnected(reason);
            }
        });

        // Schedule reconnect if needed
        if (shouldReconnect && code != 1000) { // 1000 = normal closure
            scheduleReconnect();
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.e(TAG, "WebSocket connection failed", t);
        isConnected = false;
        Globals.setWebSocketConnected(false);

        String errorMessage = t.getMessage();
        if (response != null) {
            errorMessage = "HTTP " + response.code() + ": " + response.message();
        }

        final String finalErrorMessage = errorMessage;

        // Notify connection listeners on main thread
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onError(finalErrorMessage);
            }
        });

        // Schedule reconnect if needed
        if (shouldReconnect) {
            scheduleReconnect();
        }
    }

    // ========== Message Handling ==========

    /**
     * Handle incoming WebSocket message
     */
    private void handleMessage(JsonObject message) {
        if (!message.has("type")) {
            Log.w(TAG, "Message missing 'type' field");
            return;
        }

        String type = message.get("type").getAsString();
        JsonObject data = message.has("data") ? message.getAsJsonObject("data") : null;

        if (data == null) {
            Log.w(TAG, "Message missing 'data' field");
            return;
        }

        // Handle different message types
        switch (type) {
            case "device_status":
                notifyDeviceStatusUpdate(data);
                break;

            case "device_property":
                notifyDevicePropertyUpdate(data);
                break;

            case "ping":
                // Handle ping/keep-alive
                Log.d(TAG, "Received ping from server");
                break;

            default:
                Log.w(TAG, "Unknown message type: " + type);
                break;
        }
    }

    /**
     * Notify device status update to all listeners
     */
    private void notifyDeviceStatusUpdate(JsonObject data) {
        Log.d(TAG, "Device status update: " + data.toString());
        mainHandler.post(() -> {
            for (DeviceUpdateListener listener : deviceUpdateListeners) {
                listener.onDeviceStatusUpdate(data);
            }
        });
    }

    /**
     * Notify device property update to all listeners
     */
    private void notifyDevicePropertyUpdate(JsonObject data) {
        Log.d(TAG, "Device property update: " + data.toString());
        mainHandler.post(() -> {
            for (DeviceUpdateListener listener : deviceUpdateListeners) {
                listener.onDevicePropertyUpdate(data);
            }
        });
    }

    /**
     * Notify connection error to all listeners
     */
    private void notifyConnectionError(String error) {
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onError(error);
            }
        });
    }

    // ========== Reconnection Logic ==========

    /**
     * Schedule reconnection with exponential backoff
     */
    private void scheduleReconnect() {
        if (!shouldReconnect) {
            Log.d(TAG, "Reconnection disabled, not scheduling");
            return;
        }

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts reached (" + MAX_RECONNECT_ATTEMPTS + "), giving up");
            notifyConnectionError("Failed to reconnect after " + MAX_RECONNECT_ATTEMPTS + " attempts");
            return;
        }

        // Get delay for current attempt (with exponential backoff)
        int delayIndex = Math.min(reconnectAttempts, RECONNECT_DELAYS.length - 1);
        long delay = RECONNECT_DELAYS[delayIndex];

        reconnectAttempts++;
        Log.d(TAG, "Scheduling reconnect attempt " + reconnectAttempts + " in " + (delay / 1000) + " seconds");

        // Cancel any existing reconnect attempt
        if (reconnectRunnable != null) {
            reconnectHandler.removeCallbacks(reconnectRunnable);
        }

        // Schedule new reconnect attempt
        reconnectRunnable = this::connectInternal;
        reconnectHandler.postDelayed(reconnectRunnable, delay);
    }
}
