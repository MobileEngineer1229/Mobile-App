# FastBee Bluetooth Provisioning Implementation

## Overview
Replaced backend MQTT provisioning with **direct Bluetooth Low Energy (BLE)** provisioning using the FastBee protocol.

## Architecture Change

### Previous Flow (Incorrect)
```
Mobile → Backend API → MQTT Broker → IoT Device
```
- Mobile called `/mqtt/provision-device` endpoint
- Backend published credentials to MQTT topic
- **Problem**: IoT devices have 4G/5G, not WiFi. They can't receive MQTT during setup.

### New Flow (Correct)
```
Mobile → Bluetooth (FastBee) → IoT Device
                                    ↓
                          Device connects to MQTT via 4G/5G
```

## Implementation Details

### 1. Added Bluetooth Permissions
**File**: `AndroidManifest.xml`

```xml
<!-- Bluetooth permissions for FastBee provisioning -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```

### 2. Created FastBee Bluetooth Service
**File**: `app/src/main/java/com/smarthome/iot/services/FastBeeBluetoothService.java`

**Key Features**:
- BLE scanning for FastBee devices
- GATT connection management
- JSON credential payload transmission
- Callback-based provisioning flow

**FastBee Device Naming Convention**:
```
FastBee_{productId}_{deviceNum}
```
Examples:
- `FastBee_1001_lamp-01` (Lamp product)
- `FastBee_2001_sensor-living` (Sensor product)

**BLE Service UUIDs** (update these with actual FastBee firmware UUIDs):
```java
SERVICE_UUID = "0000FFE0-0000-1000-8000-00805F9B34FB"
CHARACTERISTIC_WRITE_UUID = "0000FFE1-0000-1000-8000-00805F9B34FB"
CHARACTERISTIC_NOTIFY_UUID = "0000FFE2-0000-1000-8000-00805F9B34FB"
```

**Credential Payload Format**:
```json
{
  "broker": "mqtt://172.86.88.76:1883",
  "username": "device_1001_lamp-01",
  "password": "generated_password",
  "clientId": "fastbee_1001_lamp-01"
}
```

### 3. Updated DeviceDetectedActivity
**File**: `app/src/main/java/com/smarthome/iot/ui/DeviceDetectedActivity.java`

**Changes**:
1. Replaced `provisionIoTDevice()` method to use Bluetooth instead of backend API
2. Added runtime permission handling for Android 12+ (BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
3. Implemented BLE scanning and connection flow
4. Added cleanup in `onDestroy()`

**Provisioning Flow**:
```java
1. Get MQTT credentials from backend `/mqtt/register-device`
2. Scan for FastBee device by name (FastBee_{productId}_{deviceNum})
3. Connect via BLE GATT
4. Send JSON credentials to write characteristic
5. Device receives, saves credentials, and reconnects to MQTT via 4G/5G
6. Mobile completes progress animation
```

### 4. Device Status & Control
Mobile app gets device status from **backend REST API**, not MQTT:

```
GET /api/v1/devices → Returns all devices with status
GET /api/v1/devices/:id → Returns single device status
POST /api/v1/mqtt/command → Send control commands (backend publishes to MQTT)
```

Backend maintains MQTT connection to broker and handles:
- Publishing commands to devices
- Receiving state updates from devices
- Storing state in PostgreSQL
- Exposing state via REST API to mobile

## Complete Provisioning Sequence

```
1. User scans QR code or enters setup code (format: productId:deviceNum)
   ↓
2. Mobile registers device via POST /api/v1/mqtt/register-device
   - Backend creates device record in PostgreSQL
   - Backend creates MQTT credentials in EMQX
   - Returns credentials to mobile
   ↓
3. Mobile scans for BLE device "FastBee_{productId}_{deviceNum}"
   - Timeout: 10 seconds
   ↓
4. Mobile connects to device via BLE GATT
   ↓
5. Mobile writes JSON credentials to BLE characteristic
   ↓
6. IoT device receives credentials via Bluetooth
   - Saves credentials to flash memory
   - Disconnects from BLE
   ↓
7. IoT device connects to MQTT broker via 4G/5G
   - Uses received credentials
   - Subscribes to topic: /fastbee/{productId}/{deviceNum}/#
   - Publishes state updates
   ↓
8. Mobile refreshes device list from backend API
   - Device appears with online status
```

## IoT Device Firmware Requirements

The IoT device firmware must:

1. **Advertise BLE with correct name**: `FastBee_{productId}_{deviceNum}`
2. **Expose GATT service** with FastBee UUIDs
3. **Listen for credential write** on characteristic FFE1
4. **Parse JSON payload** and extract MQTT credentials
5. **Save credentials** to non-volatile storage
6. **Connect to MQTT** using 4G/5G with saved credentials
7. **Subscribe to command topic**: `/fastbee/{productId}/{deviceNum}/command`
8. **Publish state updates** to: `/fastbee/{productId}/{deviceNum}/state`

## Testing

### Prerequisites
- IoT device in pairing mode (BLE advertising)
- Device name matches FastBee convention
- Bluetooth enabled on mobile
- Permissions granted (Android 12+)

### Test Procedure
1. Open app and go to "Add Device"
2. Select "Enter Setup Code"
3. Enter code: `1001:lamp-01`
4. Tap "Connect"
5. Observe logs for:
   - "Scanning for FastBee device: FastBee_1001_lamp-01"
   - "Device found"
   - "Connected to device"
   - "Credentials sent via Bluetooth"
6. Check device connects to MQTT broker
7. Verify device appears in main screen

### Troubleshooting

**Device not found**:
- Check BLE advertising is active
- Verify device name matches exactly
- Check Bluetooth is enabled
- Ensure within range (<10 meters)

**Connection fails**:
- Check GATT service UUIDs match firmware
- Verify characteristic permissions allow write
- Check device not already connected to another phone

**Credentials not received**:
- Verify characteristic supports write operation
- Check payload size (<512 bytes)
- Ensure device firmware is parsing JSON correctly

## Next Steps

1. **Update BLE UUIDs** in `FastBeeBluetoothService.java` to match actual device firmware
2. **Test with real IoT hardware**
3. **Add retry logic** for failed BLE connections
4. **Implement device pairing UI** showing BLE scan progress
5. **Add BLE signal strength indicator** during scanning
6. **Handle multiple devices** found during scan (show list to user)

## Backend Changes (Optional)

The `/mqtt/provision-device` endpoint is **no longer used** by the mobile app. You can:
- Keep it for future web dashboard provisioning
- Remove it to simplify the codebase
- Document it as deprecated

Mobile now handles provisioning independently via Bluetooth.
