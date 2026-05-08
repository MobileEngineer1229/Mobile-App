# WebSocket Real-Time Device Status Integration

## Complete Implementation Guide

### Backend Setup ✅

**Files Created/Modified**:
1. ✅ `src/services/websocket/websocket-server.ts` - WebSocket server implementation
2. ✅ `src/services/mqtt/mqtt-message-handler.ts` - Updated to broadcast to WebSocket
3. ✅ `src/app.ts` - Initialize WebSocket server
4. ✅ `package.json` - Added `ws` and `@types/ws` dependencies

**WebSocket Endpoint**: `ws://172.86.88.76:3003/api/v1/devices/stream`

**Authentication**: JWT token via query parameter: `?token=<jwt_token>`

### Mobile App Setup ✅

**Files Created/Modified**:
1. ✅ `services/WebSocketDeviceStatusService.java` - WebSocket client service
2. ✅ `AndroidManifest.xml` - Service registration
3. ⏳ `ui/MainActivity.java` - Integration needed

---

## MainActivity Integration Instructions

Add the following code to `MainActivity.java`:

### 1. Add Imports

```java
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.smarthome.iot.services.WebSocketDeviceStatusService;
import org.json.JSONObject;
```

### 2. Add Fields (in MainActivity class, around line 91)

```java
private BroadcastReceiver deviceStatusReceiver;
```

### 3. Start WebSocket Service (in onCreate, after line 124)

```java
// Connect WebSocket for real-time device status
if (authManager.isLoggedIn() && !MockDataProvider.isDemoUser(authManager)) {
    startWebSocketService();
}
```

### 4. Add Helper Methods (at end of MainActivity class)

```java
/**
 * Start WebSocket service for real-time device updates
 */
private void startWebSocketService() {
    String token = authManager.getToken();
    if (token != null) {
        Intent intent = new Intent(this, WebSocketDeviceStatusService.class);
        intent.putExtra("token", token);
        startService(intent);
        android.util.Log.d("MainActivity", "WebSocket service started");
    }
}

/**
 * Stop WebSocket service
 */
private void stopWebSocketService() {
    Intent intent = new Intent(this, WebSocketDeviceStatusService.class);
    stopService(intent);
}

/**
 * Register broadcast receiver for WebSocket updates
 */
private void registerDeviceStatusReceiver() {
    deviceStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case WebSocketDeviceStatusService.ACTION_DEVICE_STATUS_UPDATED:
                    handleDeviceStatusUpdate(intent);
                    break;

                case WebSocketDeviceStatusService.ACTION_DEVICE_PROPERTY_UPDATED:
                    handleDevicePropertyUpdate(intent);
                    break;

                case WebSocketDeviceStatusService.ACTION_CONNECTION_STATUS:
                    handleWebSocketConnectionStatus(intent);
                    break;
            }
        }
    };

    IntentFilter filter = new IntentFilter();
    filter.addAction(WebSocketDeviceStatusService.ACTION_DEVICE_STATUS_UPDATED);
    filter.addAction(WebSocketDeviceStatusService.ACTION_DEVICE_PROPERTY_UPDATED);
    filter.addAction(WebSocketDeviceStatusService.ACTION_CONNECTION_STATUS);

    LocalBroadcastManager.getInstance(this).registerReceiver(deviceStatusReceiver, filter);
    android.util.Log.d("MainActivity", "Device status receiver registered");
}

/**
 * Unregister broadcast receiver
 */
private void unregisterDeviceStatusReceiver() {
    if (deviceStatusReceiver != null) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceStatusReceiver);
        deviceStatusReceiver = null;
    }
}

/**
 * Handle device status update (online/offline)
 */
private void handleDeviceStatusUpdate(Intent intent) {
    String productId = intent.getStringExtra(WebSocketDeviceStatusService.EXTRA_PRODUCT_ID);
    String deviceNum = intent.getStringExtra(WebSocketDeviceStatusService.EXTRA_DEVICE_NUM);
    String status = intent.getStringExtra(WebSocketDeviceStatusService.EXTRA_STATUS);

    android.util.Log.d("MainActivity", "Device status update: " + productId + "/" + deviceNum + " -> " + status);

    // Update device in local list
    if (allDevicesList != null) {
        for (Device device : allDevicesList) {
            if (isMatchingDevice(device, productId, deviceNum)) {
                device.setStatus(status);
                runOnUiThread(() -> {
                    deviceAdapter.notifyDataSetChanged();
                });
                break;
            }
        }
    }
}

/**
 * Handle device property update
 */
private void handleDevicePropertyUpdate(Intent intent) {
    String productId = intent.getStringExtra(WebSocketDeviceStatusService.EXTRA_PRODUCT_ID);
    String deviceNum = intent.getStringExtra(WebSocketDeviceStatusService.EXTRA_DEVICE_NUM);
    String propertiesJson = intent.getStringExtra(WebSocketDeviceStatusService.EXTRA_PROPERTIES);

    android.util.Log.d("MainActivity", "Device property update: " + productId + "/" + deviceNum);

    try {
        JSONObject properties = new JSONObject(propertiesJson);

        // Update device properties in local list
        if (allDevicesList != null) {
            for (Device device : allDevicesList) {
                if (isMatchingDevice(device, productId, deviceNum)) {
                    // Update device metadata with new properties
                    Map<String, Object> metadata = device.getMetadata();
                    if (metadata == null) {
                        metadata = new HashMap<>();
                        device.setMetadata(metadata);
                    }
                    metadata.put("lastProperties", properties.toString());

                    runOnUiThread(() -> {
                        deviceAdapter.notifyDataSetChanged();
                    });
                    break;
                }
            }
        }
    } catch (Exception e) {
        android.util.Log.e("MainActivity", "Error parsing property update", e);
    }
}

/**
 * Handle WebSocket connection status
 */
private void handleWebSocketConnectionStatus(Intent intent) {
    boolean connected = intent.getBooleanExtra(WebSocketDeviceStatusService.EXTRA_CONNECTED, false);
    android.util.Log.d("MainActivity", "WebSocket connection status: " + (connected ? "connected" : "disconnected"));

    // Optional: Show toast or update UI indicator
    if (!connected) {
        // Could show a small indicator that real-time updates are unavailable
    }
}

/**
 * Check if device matches MQTT productId/deviceNum
 */
private boolean isMatchingDevice(Device device, String productId, String deviceNum) {
    Map<String, Object> metadata = device.getMetadata();
    if (metadata == null) return false;

    Object deviceProductId = metadata.get("productId");
    Object deviceDeviceNum = metadata.get("deviceNum");

    return productId.equals(deviceProductId) && deviceNum.equals(deviceDeviceNum);
}
```

### 5. Update onResume (add after existing code, around line 200)

```java
@Override
protected void onResume() {
    super.onResume();
    // ... existing code ...

    // Register WebSocket receiver for real-time updates
    registerDeviceStatusReceiver();
}
```

### 6. Update onPause (add after existing code)

```java
@Override
protected void onPause() {
    super.onPause();
    // ... existing code ...

    // Unregister WebSocket receiver
    unregisterDeviceStatusReceiver();
}
```

### 7. Update onDestroy (add after existing code)

```java
@Override
protected void onDestroy() {
    super.onDestroy();

    // Stop WebSocket service
    stopWebSocketService();
}
```

---

## Testing

### 1. Start Backend

```bash
cd Smart-Home-Backend
npm install
npm run dev
```

**Expected output**:
```
🔌 WebSocket server initialized at /api/v1/devices/stream
🌐 API Base URL: http://172.86.88.76:3003/api/v1/
```

### 2. Build Mobile App

```bash
cd Smart-Home
./gradlew assembleDebug
```

### 3. Test Real-Time Updates

1. **Install and open app** on Android device/emulator
2. **Login** with a real user (not demo@smartify.com)
3. **Check logs** for WebSocket connection:
   ```
   WebSocketStatusService: Connecting to WebSocket: ws://172.86.88.76:3003/api/v1/devices/stream?token=...
   WebSocketStatusService: WebSocket connected
   MainActivity: Device status receiver registered
   ```

4. **Simulate device status change**:
   - IoT device publishes to MQTT: `/fastbee/1001/lamp-01/status`
   - Backend receives and broadcasts to WebSocket
   - Mobile receives update and refreshes device card

5. **Check MainActivity logs**:
   ```
   MainActivity: Device status update: 1001/lamp-01 -> online
   MainActivity: Device property update: 1001/lamp-01
   ```

---

## Message Flow Diagram

```
IoT Device (4G/5G)
    ↓
    Publishes to: /fastbee/1001/lamp-01/property/post
    ↓
NanoMQ Broker (172.86.88.76:1883)
    ↓
Backend MQTT Client (subscribed to /fastbee/+/+/property/post)
    ↓
MqttMessageHandler.handlePropertyReport()
    ↓
WebSocketServer.broadcastDeviceProperty(userId, data)
    ↓
Mobile WebSocket Client (connected with JWT)
    ↓
WebSocketDeviceStatusService.handleWebSocketMessage()
    ↓
LocalBroadcastManager.sendBroadcast(ACTION_DEVICE_PROPERTY_UPDATED)
    ↓
MainActivity.deviceStatusReceiver.onReceive()
    ↓
Update device list & refresh UI
```

---

## Troubleshooting

**WebSocket not connecting:**
- Check JWT token is valid
- Verify backend is running on correct IP/port
- Check firewall allows WebSocket connections
- Look for backend logs showing connection attempts

**No real-time updates:**
- Verify IoT device is publishing to MQTT
- Check backend MQTT handler logs
- Confirm device user_id matches logged-in user
- Check mobile logs for broadcast messages

**Battery drain:**
- WebSocket uses persistent connection (more efficient than polling)
- Heartbeat interval set to 30 seconds (adjustable)
- Service automatically reconnects on failure

---

## Next Steps

1. ✅ Integrate code into MainActivity.java
2. Test with real IoT device
3. Add UI indicator for WebSocket connection status
4. Implement reconnection handling in UI
5. Add battery optimization exemption if needed

---

## Advantages of This Approach

✅ **Centralized Security** - Backend validates all access
✅ **Real-Time Updates** - Sub-second latency via WebSocket
✅ **Battery Efficient** - Single persistent connection
✅ **Scalable** - Backend handles MQTT complexity
✅ **Simple Mobile Code** - No MQTT client library needed
✅ **Automatic Filtering** - Backend only sends user's devices

