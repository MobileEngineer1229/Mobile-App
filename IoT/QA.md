# Smartify Q&A

## Q1: How to search/discover devices?

### Android (Mobile)

The entry point is **`AddDeviceActivity.java`** which has two tabs:

**Tab 1 — Nearby Devices (auto-discovery)**
- Fragment: `NearbyDevicesFragment.java`
- Calls `apiService.discoverDevices()` → `GET /api/v1/devices/discover`
- Backend returns a list of mock nearby devices (Smart Lamp, Smart V1 CCTV, Wi-Fi Router, Stereo Speaker)
- User taps a discovered device → opens `DeviceDetectedActivity` to connect it

**Tab 2 — Manual Selection**
- Fragment: `AddManualFragment.java`
- Shows hardcoded device categories: Popular, Lightning, Camera, Electronics
- No API call — device types are defined in the fragment
- User taps a device type → opens `DeviceDetectedActivity` to connect it

### Backend

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/devices/discover` | GET | Returns mock discovered nearby devices |
| `/api/v1/devices/nearby` | GET | Alias for discover |
| `/api/v1/devices/types` | GET | Returns device types by category |

**Files:**
- Route: `Smart-Home-Backend/src/routes/device-discovery-routes.ts`
- Controller: `Smart-Home-Backend/src/controllers/device-discovery-controller.ts`

> **Note:** Discovery currently returns mock data. Real device discovery would use network scanning, mDNS, or MQTT device announcements.

---

## Q2: How to get the device ID?

The device ID is an **auto-incremented integer** assigned by PostgreSQL when a device is created.

### When creating a device

`DeviceDetectedActivity.java` calls:
```java
apiService.createDevice(deviceData);  // POST /api/v1/devices
```

The response includes the device with its assigned `id`:
```json
{
  "success": true,
  "data": {
    "id": 42,
    "name": "Smart Lamp",
    "type": "lamp",
    "status": "offline",
    ...
  }
}
```

### When listing devices

`MainActivity.java` loads all devices for the current home:
```java
apiService.getDevices(roomId, homeId, status, type, page, limit);
// GET /api/v1/devices?homeId=1
```

Each device in the response array contains its `id`.

### When getting a single device

```java
apiService.getDeviceById(deviceId);  // GET /api/v1/devices/{id}
```

### All backend endpoints that return device IDs

| Endpoint | Method | Returns |
|----------|--------|---------|
| `GET /api/v1/devices` | GET | List of devices (each has `id`) |
| `GET /api/v1/devices/{id}` | GET | Single device by ID |
| `POST /api/v1/devices` | POST | Newly created device (with assigned `id`) |
| `GET /api/v1/devices/home/{homeId}` | GET | Devices filtered by home |
| `GET /api/v1/devices/category/{category}` | GET | Devices filtered by category |

**Files:**
- Route: `Smart-Home-Backend/src/routes/device-routes.ts`
- Controller: `Smart-Home-Backend/src/controllers/device-controller.ts`
- Service: `Smart-Home-Backend/src/services/device-service.ts`
- Repository: `Smart-Home-Backend/src/repositories/device-repository.ts`

### Android cache

Once loaded, devices are cached in `Globals.java`:
```java
Globals.setCachedDevices(devices, homeId);   // store
Globals.getCachedDevices(homeId);            // retrieve (5-min TTL)
```

---

## Q3: How to get device data?

"Device data" can mean two things: **device metadata/state** or **device energy/usage data**.

### A. Device state & metadata

**Get current state (real-time):**
```java
apiService.getDeviceState(deviceId);  // GET /api/v1/devices/{id}/control/state
```

Returns the device's `metadata` JSONB field from PostgreSQL, which includes:
- `power` (boolean)
- `brightness`, `color`, `temperature` (for lamps)
- `lastControlTime`
- `lastProperties` (updated via MQTT)
- `mqttOnline` (updated via MQTT status topic)
- `protocol`, `productId`, `deviceNum` (for MQTT devices)

**Get full device object:**
```java
apiService.getDeviceById(deviceId);  // GET /api/v1/devices/{id}
```

Returns: `id`, `name`, `type`, `status`, `roomId`, `macAddress`, `ipAddress`, `metadata`

### B. Device energy consumption data

**Submit energy data (device → backend):**

| Endpoint | Method | Description |
|----------|--------|-------------|
| `POST /api/v1/devices/{deviceId}/energy` | POST | Submit single energy record |
| `POST /api/v1/devices/{deviceId}/energy/batch` | POST | Submit multiple energy records |
| `POST /api/v1/devices/webhook/energy` | POST | Submit via device token (no JWT) |

Request body:
```json
{
  "consumptionKwh": 2.5,
  "date": "2024-12-03",
  "costUsd": 0.375
}
```

**Files:**
- Route: `Smart-Home-Backend/src/routes/device-data-routes.ts`
- Controller: `Smart-Home-Backend/src/controllers/device-data-controller.ts`
- Service: `Smart-Home-Backend/src/services/device-data-service.ts`

### C. Real-time data via MQTT

Devices report data by publishing to MQTT topics:

| Topic | Direction | Purpose |
|-------|-----------|---------|
| `/fastbee/{productId}/{deviceNum}/property/post` | Device → Server | Report properties (brightness, temp, etc.) |
| `/fastbee/{productId}/{deviceNum}/status` | Device → Server | Online/offline status |
| `/fastbee/{productId}/{deviceNum}/info/post` | Device → Server | Firmware, model info |

The backend `MqttMessageHandler` (`Smart-Home-Backend/src/services/mqtt/mqtt-message-handler.ts`) listens on these topics and writes the data into the device's `metadata` JSONB column automatically.

On Android, `MqttMessageHandler.java` receives these messages and updates the `Globals` device cache in real time, then sends a `LocalBroadcast` (`ACTION_DEVICE_UPDATED`) so the UI can refresh.

---

## Q4: How to send a command to a device?

### Android (Mobile)

Commands are sent via REST API from control fragments:

```java
// Power on/off
apiService.controlDevicePower(deviceId, Map.of("power", true));
// POST /api/v1/devices/{id}/control/power

// Lamp settings
apiService.controlLamp(deviceId, Map.of("brightness", 75, "color", "#FF0000"));
// POST /api/v1/devices/{id}/control/lamp

// Camera
apiService.controlCamera(deviceId, Map.of("action", "snapshot"));
// POST /api/v1/devices/{id}/control/camera

// Speaker
apiService.controlSpeaker(deviceId, Map.of("volume", 50, "action", "play"));
// POST /api/v1/devices/{id}/control/speaker

// Air Conditioner
apiService.controlAC(deviceId, Map.of("temperature", 24, "mode", "cooling"));
// POST /api/v1/devices/{id}/control/ac

// Unified command (any device type)
apiService.executeDeviceCommand(deviceId, Map.of("command", "power", "power", true));
// POST /api/v1/devices/{id}/command
```

**UI entry points:**
- `MainActivity.java` — quick power toggle from device grid
- `DeviceControlDetailActivity.java` — loads type-specific fragment:
  - `LampControlFragment` — brightness, color wheel, temperature
  - `CameraControlFragment` — playback, snapshot, PTZ
  - `SpeakerControlFragment` — volume, play/pause/next
  - `AirConditionerControlFragment` — temperature, mode, fan

### Backend command flow

```
POST /api/v1/devices/{id}/control/power
  → DeviceControlController.controlPower()
    → DeviceControlService.controlPower(deviceId, userId, power)
      → getProtocolAdapter(device)
         ├─ metadata.protocol === "mqtt"
         │   → MqttAdapter.sendCommand(device, "power", {power: true})
         │   → publishes to /fastbee/{productId}/{deviceNum}/property/set
         │   → EMQX broker → physical device
         │
         ├─ metadata.protocol === "bluetooth"
         │   → BluetoothGatewayAdapter.sendCommand(device, "power", {power: true})
         │   → HTTP POST to gateway backend → BLE → physical device
         │
         └─ no adapter configured
             → logs command only (no physical device)
      → updates device status + metadata in PostgreSQL
```

**Files:**
- Route: `Smart-Home-Backend/src/routes/device-control-routes.ts`
- Controller: `Smart-Home-Backend/src/controllers/device-control-controller.ts`
- Service: `Smart-Home-Backend/src/services/device-control-service.ts`
- MQTT adapter: `Smart-Home-Backend/src/services/protocols/mqtt-adapter.ts`
- Bluetooth adapter: `Smart-Home-Backend/src/services/protocols/bluetooth-gateway-adapter.ts`

### All device control endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `POST /devices/{id}/control/power` | POST | Power on/off |
| `POST /devices/{id}/control/lamp` | POST | Lamp brightness/color/temperature |
| `POST /devices/{id}/control/camera` | POST | Camera actions |
| `GET  /devices/{id}/control/camera/stream` | GET | Get camera stream URL |
| `POST /devices/{id}/control/speaker` | POST | Speaker volume/playback |
| `POST /devices/{id}/control/ac` | POST | AC temperature/mode/fan |
| `GET  /devices/{id}/control/state` | GET | Get current device state |
| `POST /devices/{id}/command` | POST | Unified command interface |
