# MQTTX Testing Guide for Smartify IoT

This guide helps you test the MQTT integration using MQTTX as a virtual device simulator.

## Prerequisites

1. **EMQX Broker** running at `172.86.88.76:1883`
2. **MQTTX** installed ([Download](https://mqttx.app/))
3. **Backend** running (`npm run dev` in Smart-Home-Backend)
4. **Android App** running (or use another MQTTX instance to simulate)

---

## Connection Setup in MQTTX

Create a new connection:
- **Name:** Virtual Smart Lamp
- **Host:** 172.86.88.76
- **Port:** 1883
- **Client ID:** virtual-lamp-001
- **Username:** (leave empty or use configured credentials)
- **Password:** (leave empty or use configured credentials)

---

## Test Scenario 1: Device Online/Offline Status

### Subscribe to ALL device topics (for monitoring):
```
/fastbee/#
```

### Simulate device coming online:
**Topic:** `/fastbee/1001/lamp-living-room/status`
**Payload (JSON):**
```json
{
  "status": "online",
  "rssi": -45,
  "uptime": 3600,
  "firmware": "1.2.3"
}
```

### Simulate device going offline:
**Topic:** `/fastbee/1001/lamp-living-room/status`
**Payload (JSON):**
```json
{
  "status": "offline"
}
```

**Expected Backend Behavior:**
- Logs: `Device online: 1001/lamp-living-room`
- Database: Updates device status in `devices` table
- Automation: Triggers any `device_status` scene conditions

---

## Test Scenario 2: Receive Commands FROM App

### Subscribe to receive commands:
```
/fastbee/1001/lamp-living-room/property/set
```

### When user taps "Power ON" in the app, you should receive:
```json
{
  "command": "power",
  "params": {
    "state": "ON"
  },
  "timestamp": "2026-01-28T10:30:00.000Z",
  "requestId": "uuid-here"
}
```

### When user adjusts brightness slider, you should receive:
```json
{
  "command": "lamp_control",
  "params": {
    "brightness": 75,
    "mode": "white"
  }
}
```

---

## Test Scenario 3: Device Reports State (Property Post)

### Publish device state:
**Topic:** `/fastbee/1001/lamp-living-room/property/post`
**Payload (JSON):**
```json
{
  "power": "ON",
  "brightness": 85,
  "color": "#FF6B35",
  "colorTemperature": 4000,
  "mode": "color",
  "timestamp": "2026-01-28T10:31:00.000Z"
}
```

**Expected Backend Behavior:**
- Logs: `MQTT message received: property/post`
- Database: Updates `metadata.lastProperties` in device record
- Automation: Evaluates any automation conditions (temperature, etc.)

---

## Test Scenario 4: Temperature Sensor (Trigger Automation)

### Publish temperature reading:
**Topic:** `/fastbee/2001/sensor-temp-01/property/post`
**Payload (JSON):**
```json
{
  "temperature": 16.5,
  "humidity": 65,
  "battery": 85,
  "timestamp": "2026-01-28T10:32:00.000Z"
}
```

**Expected Backend Behavior:**
- If you have an automation scene with condition `temperature < 18`:
  - Scene should trigger automatically
  - Check logs for: `Automation triggered: "Scene Name"`

---

## Test Scenario 5: Full Round-Trip Test

1. **In MQTTX - Subscribe to:**
   ```
   /fastbee/1001/test-lamp/property/set
   ```

2. **In MQTTX - Publish device online:**
   ```
   Topic: /fastbee/1001/test-lamp/status
   Payload: {"status": "online"}
   ```

3. **In Android App:**
   - Create a device with metadata:
     - `productId`: `1001`
     - `deviceNum`: `test-lamp`
   - Go to device control and tap power ON

4. **In MQTTX - You should receive:**
   ```json
   {
     "command": "power",
     "params": {"state": "ON"}
   }
   ```

5. **In MQTTX - Respond with confirmation:**
   ```
   Topic: /fastbee/1001/test-lamp/property/post
   Payload: {"power": "ON", "brightness": 100}
   ```

6. **Verify:**
   - Backend logs show the message received
   - Database has updated device state

---

## Test Scenario 6: Scene Automation Trigger

### Setup:
1. Create a scene in the app with:
   - **Condition:** Temperature < 20
   - **Task:** Control device (turn on heater)

2. Subscribe in MQTTX to see the heater command:
   ```
   /fastbee/+/+/property/set
   ```

### Trigger:
Publish a temperature reading below threshold:
```
Topic: /fastbee/2001/temp-sensor/property/post
Payload: {"temperature": 18}
```

### Expected:
1. Backend evaluates the automation condition
2. Condition met → executes scene tasks
3. MQTTX receives command for the heater device

---

## Quick Reference: Topic Structure

| Direction | Topic Pattern | Example |
|-----------|---------------|---------|
| Device → Server | `/fastbee/{pid}/{dev}/property/post` | `/fastbee/1001/lamp-01/property/post` |
| Device → Server | `/fastbee/{pid}/{dev}/status` | `/fastbee/1001/lamp-01/status` |
| Device → Server | `/fastbee/{pid}/{dev}/info/post` | `/fastbee/1001/lamp-01/info/post` |
| Server → Device | `/fastbee/{pid}/{dev}/property/set` | `/fastbee/1001/lamp-01/property/set` |

---

## Common Product IDs (Example)

| Product ID | Device Type |
|------------|-------------|
| 1001 | Smart Lamp |
| 1002 | Smart Plug |
| 2001 | Temperature Sensor |
| 2002 | Humidity Sensor |
| 3001 | Camera |
| 4001 | Speaker |
| 5001 | Air Conditioner |
| 6001 | Smart Lock |

---

## Troubleshooting

### Backend not receiving messages?
1. Check EMQX dashboard: `http://172.86.88.76:18083`
2. Verify client connections
3. Check backend logs for MQTT connection status

### App not receiving updates?
1. Check if `MqttManager.isConnected()` returns true
2. Verify Globals cache is being updated
3. **Note:** UI real-time refresh is not yet implemented (see README)

### Commands not reaching MQTTX?
1. Verify you're subscribed to the correct topic
2. Check the device's `metadata.productId` and `metadata.deviceNum` in database
3. Ensure device is registered and belongs to the logged-in user

---

## Sample Payloads for Copy-Paste

### Lamp State Report
```json
{"power":"ON","brightness":85,"color":"#FFFFFF","mode":"white","colorTemperature":4000}
```

### AC State Report
```json
{"power":"ON","temperature":24,"mode":"cooling","windSpeed":2,"eco":false}
```

### Camera State Report
```json
{"power":"ON","privateMode":false,"nightMode":"auto","recording":true}
```

### Speaker State Report
```json
{"power":"ON","volume":65,"isPlaying":true,"source":"spotify"}
```

### Sensor Reading
```json
{"temperature":22.5,"humidity":55,"pressure":1013.25,"battery":85}
```

### Device Info Report
```json
{"firmware":"1.2.3","hardware":"v2","model":"SL-100","manufacturer":"Smartify"}
```
