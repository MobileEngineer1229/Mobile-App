# Smartify IoT — EMQX Integration Workflow

> A beginner-friendly, end-to-end guide covering device discovery, registration, real-time control, and smart automation — all powered by **EMQX** as the MQTT broker.

---

## Table of Contents

1. [Big Picture — How Everything Connects](#1-big-picture--how-everything-connects)
2. [EMQX Broker Setup](#2-emqx-broker-setup)
3. [Backend ↔ EMQX Integration](#3-backend--emqx-integration)
4. [Mobile App ↔ EMQX Integration](#4-mobile-app--emqx-integration)
5. [Workflow A — Search, Add & Save a Device](#5-workflow-a--search-add--save-a-device)
6. [Workflow B — Real-Time Device Control](#6-workflow-b--real-time-device-control)
7. [Workflow C — Smart Features (Conditions & Automation)](#7-workflow-c--smart-features-conditions--automation)
8. [Workflow D — Scheduled Scenes](#8-workflow-d--scheduled-scenes)
9. [MQTT Topic Design (FastBee Convention)](#9-mqtt-topic-design-fastbee-convention)
10. [Message Payload Reference](#10-message-payload-reference)
11. [Error Handling & Reconnection](#11-error-handling--reconnection)
12. [Security](#12-security)
13. [Testing Checklist](#13-testing-checklist)

---

## 1. Big Picture — How Everything Connects

```
┌──────────────┐       MQTT (pub/sub)       ┌──────────────┐
│  IoT Device  │ ◄────────────────────────► │  EMQX Broker │
│  (ESP32,     │   topic: /fastbee/...      │  (port 1883) │
│   Zigbee GW, │                            │              │
│   BLE GW)    │                            └──────┬───────┘
└──────────────┘                                   │
                                          MQTT     │    MQTT
                                     ┌─────────────┼──────────────┐
                                     ▼             ▼              ▼
                              ┌────────────┐ ┌──────────┐ ┌────────────┐
                              │  Backend   │ │ Mobile   │ │ Other      │
                              │  Server    │ │ App      │ │ Clients    │
                              │ (Express)  │ │ (Android)│ │ (Web, etc) │
                              └─────┬──────┘ └──────────┘ └────────────┘
                                    │
                                    ▼
                              ┌────────────┐
                              │ PostgreSQL │
                              │ Database   │
                              └────────────┘
```

### Key Concepts for Beginners

| Term | What It Means |
|------|---------------|
| **MQTT** | A lightweight messaging protocol designed for IoT. Devices "publish" messages to "topics" and "subscribe" to topics to receive messages. Think of it like a chat room — you join a channel (topic) and can send/receive messages. |
| **EMQX** | An open-source MQTT broker (the server that routes MQTT messages between devices and apps). It's like a post office that delivers messages to the right recipients. |
| **Topic** | A hierarchical path like `/fastbee/1001/device-abc/property/set`. Clients subscribe to topics to receive messages published on them. |
| **Publish** | Sending a message to a topic. |
| **Subscribe** | Listening for messages on a topic. |
| **QoS** | Quality of Service — 0 (fire-and-forget), 1 (delivered at least once), 2 (delivered exactly once). We use QoS 1 for commands. |
| **Broker** | The EMQX server that sits between publishers and subscribers, routing messages. |
| **FastBee** | The topic naming convention this project uses: `/fastbee/{productId}/{deviceNum}/...` |

---

## 2. EMQX Broker Setup

### 2.1 Install EMQX

**Option A — Docker (Recommended for Development)**
```bash
docker run -d --name emqx \
  -p 1883:1883 \
  -p 8083:8083 \
  -p 8084:8084 \
  -p 8883:8883 \
  -p 18083:18083 \
  emqx/emqx:latest
```

| Port | Purpose |
|------|---------|
| 1883 | MQTT (plain TCP) — devices & backend connect here |
| 8083 | MQTT over WebSocket — web clients |
| 8084 | MQTT over Secure WebSocket |
| 8883 | MQTT over TLS |
| 18083 | EMQX Dashboard (admin UI) |

**Option B — Native Install**
```bash
# Ubuntu/Debian
curl -s https://assets.emqx.com/scripts/install-emqx-deb.sh | sudo bash
sudo apt-get install emqx
sudo systemctl start emqx
```

### 2.2 Access the Dashboard

Open `http://localhost:18083` in your browser.
- Default credentials: `admin` / `public`
- Change the password immediately.

### 2.3 Configure Authentication (Important)

In the EMQX Dashboard:
1. Go to **Access Control → Authentication**
2. Add a **Built-in Database** authenticator
3. Create credentials for:
   - Backend server: `username=smartify-backend`, `password=<strong-password>`
   - Mobile app: `username=smartify-android`, `password=<strong-password>`
   - IoT devices: one per device or a shared device credential

### 2.4 Configure ACL (Access Control Lists)

Restrict which clients can publish/subscribe to which topics:

| Client | Can Publish | Can Subscribe |
|--------|-------------|---------------|
| IoT Device `{deviceNum}` | `/fastbee/+/{deviceNum}/property/post`, `/fastbee/+/{deviceNum}/status`, `/fastbee/+/{deviceNum}/info/post` | `/fastbee/+/{deviceNum}/property/set` |
| Backend Server | `/fastbee/+/+/property/set` | `/fastbee/#` (all topics) |
| Mobile App | `/fastbee/+/+/property/set` (for direct control) | `/fastbee/+/+/property/post`, `/fastbee/+/+/status` |

### 2.5 Backend `.env` Configuration

```env
# MQTT / EMQX Configuration
MQTT_BROKER_URL=mqtt://localhost:1883
MQTT_USERNAME=smartify-backend
MQTT_PASSWORD=<your-password>
MQTT_CLIENT_ID=smartify-backend-001
MQTT_RECONNECT_PERIOD=5000
MQTT_CONNECT_TIMEOUT=10000
```

---

## 3. Backend ↔ EMQX Integration

### 3.1 How the Backend Connects

The backend uses the MQTT client service (`src/services/mqtt/mqtt-client.ts`) to maintain a persistent connection to EMQX.

```
Server Startup
    │
    ├── 1. Connect to PostgreSQL
    ├── 2. Run auto-migrations (if AUTO_MIGRATE=true)
    ├── 3. Connect to EMQX broker via MQTT
    │       ├── Authenticate with username/password
    │       ├── Set client ID: "smartify-backend-{instance}"
    │       ├── Enable auto-reconnect (5s interval)
    │       └── Set keep-alive: 60s
    ├── 4. Subscribe to device topics:
    │       ├── /fastbee/+/+/property/post   (device property updates)
    │       ├── /fastbee/+/+/status          (device online/offline)
    │       └── /fastbee/+/+/info/post       (device info reports)
    ├── 5. Register MQTT message handler
    └── 6. Start Express HTTP server on port 3003
```

### 3.2 Message Flow — Backend as MQTT Client

```
                   SUBSCRIBES TO                    PUBLISHES TO
                   (listens for)                    (sends commands)
                        │                                │
                        ▼                                ▼
Backend ──► /fastbee/+/+/property/post    /fastbee/{pid}/{dev}/property/set
            /fastbee/+/+/status
            /fastbee/+/+/info/post
```

### 3.3 What the Backend Does When It Receives MQTT Messages

**File**: `src/services/mqtt/mqtt-message-handler.ts`

```
MQTT Message Received
    │
    ├── Parse topic → extract productId, deviceNum
    │
    ├── If topic ends with "/property/post":
    │   ├── Device is reporting its current state (e.g., lamp brightness=80)
    │   ├── Update device metadata in PostgreSQL
    │   ├── Check if any automation scene conditions are now met
    │   └── If conditions met → execute scene tasks
    │
    ├── If topic ends with "/status":
    │   ├── Device is reporting online/offline
    │   ├── Update device status in PostgreSQL
    │   ├── Check "device_status" scene conditions
    │   └── Send push notification if device goes offline unexpectedly
    │
    └── If topic ends with "/info/post":
        ├── Device is reporting its firmware/hardware info
        └── Update device metadata in PostgreSQL
```

---

## 4. Mobile App ↔ EMQX Integration

### 4.1 How the Android App Connects

**File**: `MqttManager.java` (singleton)

```
App Launch / User Login
    │
    ├── MqttManager.getInstance().connect()
    │       ├── Broker: tcp://172.86.88.76:1883  (or your EMQX IP)
    │       ├── Client ID: "smartify-android-{serial}-{timestamp}"
    │       ├── Clean Session: false  (broker remembers subscriptions)
    │       ├── Keep-Alive: 30s
    │       ├── Auto-Reconnect: true
    │       └── Connection Timeout: 10s
    │
    ├── On Connected:
    │   ├── Subscribe to /fastbee/+/+/property/post  (QoS 1)
    │   ├── Subscribe to /fastbee/+/+/status         (QoS 1)
    │   └── Subscribe to /fastbee/+/+/info/post      (QoS 0)
    │
    └── On Message Received:
        ├── MqttMessageHandler parses topic
        ├── Extracts productId and deviceNum
        ├── Updates cached device in Globals.java
        └── Broadcasts LocalBroadcast: ACTION_DEVICE_UPDATED
            └── UI Activities receive broadcast → refresh their views
```

### 4.2 Real-Time UI Updates via MQTT

```
IoT Device changes state (e.g., lamp turned on physically)
    │
    ▼
Device publishes to /fastbee/1001/lamp-abc/property/post
    │
    ▼
EMQX routes message to all subscribers
    │
    ├──► Backend receives → updates PostgreSQL
    │
    └──► Mobile App receives
         ├── MqttMessageHandler updates Globals cache
         └── Sends LocalBroadcast(ACTION_DEVICE_UPDATED)
              │
              ▼
         MainActivity / DeviceControlDetailActivity
              └── BroadcastReceiver triggers UI refresh
                  └── Device card shows "ON" with updated brightness
```

---

## 5. Workflow A — Search, Add & Save a Device

This is the complete flow from the user tapping "Add Device" to the device appearing on their dashboard.

### Step-by-Step

```
┌─────────────────────────────────────────────────────────────────────┐
│                        USER OPENS APP                               │
│                                                                     │
│  MainActivity → taps "+" button → AddDeviceActivity                 │
│                                                                     │
│  Two tabs:                                                          │
│  ┌──────────────────────┐  ┌──────────────────────┐                │
│  │ Tab 1: Nearby Devices│  │ Tab 2: Add Manually  │                │
│  │ (Auto-Discovery)     │  │ (QR Code / Manual)   │                │
│  └──────────┬───────────┘  └──────────┬───────────┘                │
│             │                          │                             │
│             ▼                          ▼                             │
│  NearbyDevicesFragment        AddManualFragment                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.1 Auto-Discovery (Nearby Devices Tab)

```
User taps "Scan for Devices"
    │
    ▼
App calls: GET /api/v1/devices/discover
    │
    ▼
Backend performs network scan:
    ├── mDNS discovery (Bonjour/Avahi)
    ├── UPnP/SSDP broadcast
    └── Network ARP scan on local subnet
    │
    ▼
Backend returns list of discovered devices:
    [
      {
        "name": "Smart Lamp",
        "type": "lamp",
        "macAddress": "AA:BB:CC:DD:EE:FF",
        "ipAddress": "192.168.1.50",
        "protocol": "mqtt",
        "productId": "1001"
      },
      ...
    ]
    │
    ▼
App displays list in RecyclerView
    │
    ▼
User taps a discovered device → goes to device setup screen
```

### 5.2 QR Code Scan (Manual Tab)

```
User taps "Scan QR Code"
    │
    ▼
ScanDeviceActivity opens camera (ZXing library)
    │
    ▼
QR code contains device info JSON:
    {
      "productId": "1001",
      "deviceNum": "lamp-abc-123",
      "macAddress": "AA:BB:CC:DD:EE:FF",
      "type": "lamp",
      "name": "Smart Lamp Pro"
    }
    │
    ▼
App parses QR data → pre-fills device setup form
```

### 5.3 Device Registration (Save)

```
User fills in device details:
    ├── Name: "Living Room Lamp"
    ├── Type: lamp (auto-detected or selected)
    ├── Room: Living Room (selected from dropdown)
    ├── Home: My Home (auto-selected)
    └── Confirms
    │
    ▼
App calls: POST /api/v1/devices
    Body: {
      "name": "Living Room Lamp",
      "type": "lamp",
      "macAddress": "AA:BB:CC:DD:EE:FF",
      "ipAddress": "192.168.1.50",
      "roomId": 5,
      "homeId": 1,
      "metadata": {
        "protocol": "mqtt",
        "productId": "1001",
        "deviceNum": "lamp-abc-123",
        "connection": {
          "brokerUrl": "mqtt://your-emqx:1883"
        }
      }
    }
    │
    ▼
Backend DeviceService:
    ├── Validates input (unique MAC address per user)
    ├── Saves device to PostgreSQL
    ├── Returns device with assigned ID
    │
    ▼
Backend MQTT Setup for New Device:
    ├── Subscribe to /fastbee/1001/lamp-abc-123/property/post
    ├── Subscribe to /fastbee/1001/lamp-abc-123/status
    ├── Subscribe to /fastbee/1001/lamp-abc-123/info/post
    └── (Already covered by wildcard subscription /fastbee/+/+/...)
    │
    ▼
App receives success response:
    ├── Adds device to Globals cache
    ├── Navigates back to MainActivity
    └── Device card appears in the device grid
    │
    ▼
Meanwhile, the physical IoT device:
    ├── Connects to EMQX with its own credentials
    ├── Subscribes to /fastbee/1001/lamp-abc-123/property/set
    │   (listens for commands from backend/app)
    ├── Publishes to /fastbee/1001/lamp-abc-123/status
    │   payload: { "status": "online" }
    └── Backend receives status → updates DB → app gets real-time update
```

### 5.4 Complete Registration Sequence Diagram

```
  Mobile App              Backend API           PostgreSQL        EMQX Broker        IoT Device
      │                       │                     │                  │                  │
      │  POST /devices        │                     │                  │                  │
      │──────────────────────►│                     │                  │                  │
      │                       │  INSERT device      │                  │                  │
      │                       │────────────────────►│                  │                  │
      │                       │  OK (device id=42)  │                  │                  │
      │                       │◄────────────────────│                  │                  │
      │  201 Created          │                     │                  │                  │
      │  { id: 42, ... }      │                     │                  │                  │
      │◄──────────────────────│                     │                  │                  │
      │                       │                     │                  │                  │
      │  (device powers on and connects to EMQX)    │                  │                  │
      │                       │                     │                  │  CONNECT         │
      │                       │                     │                  │◄─────────────────│
      │                       │                     │                  │  CONNACK         │
      │                       │                     │                  │─────────────────►│
      │                       │                     │                  │                  │
      │                       │                     │                  │  SUB: .../set    │
      │                       │                     │                  │◄─────────────────│
      │                       │                     │                  │                  │
      │                       │                     │                  │  PUB: .../status  │
      │                       │                     │                  │  {"status":"online"}
      │                       │                     │                  │◄─────────────────│
      │                       │                     │                  │                  │
      │  (EMQX forwards status to backend & app)    │                  │                  │
      │                       │  MQTT: .../status    │                 │                  │
      │                       │◄───────────────────────────────────────│                  │
      │                       │  UPDATE status=online│                 │                  │
      │                       │────────────────────►│                  │                  │
      │                       │                     │                  │                  │
      │  MQTT: .../status     │                     │                  │                  │
      │◄────────────────────────────────────────────────────────────── │                  │
      │  (UI shows device     │                     │                  │                  │
      │   as "online")        │                     │                  │                  │
```

---

## 6. Workflow B — Real-Time Device Control

### 6.1 Power ON/OFF (Simple Example)

```
User taps power toggle on "Living Room Lamp"
    │
    ▼
App calls: POST /api/v1/devices/42/control/power
    Body: { "state": "ON" }
    │
    ▼
Backend DeviceControlService.controlPower():
    ├── Validates device belongs to user
    ├── Looks up device metadata → finds productId + deviceNum
    ├── Constructs MQTT command:
    │   Topic:   /fastbee/1001/lamp-abc-123/property/set
    │   Payload: {
    │     "command": "power",
    │     "params": { "state": "ON" },
    │     "timestamp": "2026-01-28T10:30:00Z",
    │     "requestId": "req-uuid-123"
    │   }
    │   QoS: 1
    ├── Publishes message to EMQX
    ├── Updates device state in PostgreSQL
    └── Returns success to app
    │
    ▼
EMQX delivers message to IoT device
    │
    ▼
IoT Device:
    ├── Receives on /fastbee/1001/lamp-abc-123/property/set
    ├── Parses command → turns relay ON
    ├── Confirms by publishing:
    │   Topic:   /fastbee/1001/lamp-abc-123/property/post
    │   Payload: { "power": "ON", "timestamp": "..." }
    │
    ▼
EMQX forwards confirmation to Backend & App
    │
    ├── Backend: updates DB, confirms state
    └── App: MqttMessageHandler updates Globals cache
             → LocalBroadcast → UI shows lamp as ON
```

### 6.2 Smart Lamp Control (Complex Example)

```
User opens DeviceControlDetailActivity for lamp
    │
    ├── Adjusts brightness slider to 75%
    ├── Picks color: #FF6B35 (orange)
    ├── Sets mode: "color"
    │
    ▼
App calls: POST /api/v1/devices/42/control/lamp
    Body: {
      "brightness": 75,
      "color": "#FF6B35",
      "mode": "color"
    }
    │
    ▼
Backend:
    ├── Validates brightness 0-100, color hex format
    ├── Publishes MQTT:
    │   Topic:   /fastbee/1001/lamp-abc-123/property/set
    │   Payload: {
    │     "command": "lamp_control",
    │     "params": {
    │       "brightness": 75,
    │       "color": "#FF6B35",
    │       "mode": "color"
    │     },
    │     "timestamp": "2026-01-28T10:31:00Z"
    │   }
    └── Returns 200 OK
    │
    ▼
IoT Device applies settings → confirms via /property/post
```

### 6.3 Device Control for All Supported Types

| Device Type | Control Endpoint | MQTT Command Fields |
|-------------|-----------------|---------------------|
| **Lamp** | `POST /devices/:id/control/lamp` | brightness, color, colorTemperature, mode (white/color/scene) |
| **Camera** | `POST /devices/:id/control/camera` | action (playback/snapshot/record/speak), privateMode, nightMode, pan, tilt |
| **Speaker** | `POST /devices/:id/control/speaker` | volume, action (play/pause/next/previous), service (spotify/apple/youtube) |
| **AC** | `POST /devices/:id/control/ac` | temperature, mode (cooling/heating/purifying), windSpeed, windDirection, eco, sleep |
| **Lock** | `POST /devices/:id/command` | command: "lock"/"unlock", params: { pin } |
| **Any** | `POST /devices/:id/command` | command: string, params: object (generic interface) |

### 6.4 Direct MQTT Control from App (Optional Fast Path)

For latency-sensitive controls, the app can also publish directly to EMQX without going through the REST API:

```
User adjusts brightness slider (real-time, many updates)
    │
    ▼
MqttManager.publish(
    topic: "/fastbee/1001/lamp-abc-123/property/set",
    payload: { "command": "lamp_control", "params": { "brightness": 75 } },
    qos: 0  // fire-and-forget for slider updates
)
    │
    ▼
EMQX → IoT Device (near-instant, <100ms)
    │
    ▼
Device confirms → /property/post → Backend saves to DB
```

> **When to use direct MQTT vs REST API:**
> - **REST API**: For state-changing actions that need validation, logging, and database persistence (power on/off, mode changes). This is the standard path.
> - **Direct MQTT**: For real-time slider adjustments (brightness, volume, temperature) where low latency matters and intermediate values don't need database persistence.

---

## 7. Workflow C — Smart Features (Conditions & Automation)

This is the core "smart" functionality. Users create **scenes** with **conditions** (triggers) and **tasks** (actions).

### 7.1 Scene Types

| Type | Description | Example |
|------|-------------|---------|
| **Automation** | Triggered automatically when conditions are met | "Turn on lights at sunset" |
| **Tap-to-Run** | Triggered manually by the user with one tap | "Movie mode" (dim lights, turn on TV, close curtains) |

### 7.2 Creating an Automation Scene (Full Flow)

**Example**: "When temperature drops below 18°C, turn on the heater and send me a notification"

```
User opens SmartSceneActivity → taps "+" → CreateSceneActivity
    │
    ▼
Step 1: CHOOSE SCENE TYPE
    ├── [Automation] ← selected
    └── [Tap-to-Run]
    │
    ▼
Step 2: SET CONDITIONS (SceneBuilderActivity)
    │
    ├── User taps "Add Condition"
    │   ├── Temperature
    │   ├── Humidity
    │   ├── Weather
    │   ├── Sunrise/Sunset
    │   ├── Wind Speed
    │   ├── Location (arrive/leave)
    │   ├── Schedule Time
    │   ├── Device Status (online/offline)
    │   └── Arm Mode
    │
    ├── User selects "Temperature"
    │   └── TemperatureConditionActivity opens:
    │       ├── Operator: < (less than)
    │       ├── Value: 18
    │       └── Unit: celsius
    │
    ├── Condition Logic: "All" (AND) or "Any" (OR)
    │   └── User selects "All"
    │
    ▼
Step 3: SET TASKS (Actions)
    │
    ├── User taps "Add Task"
    │   ├── Control Device
    │   ├── Run Another Scene
    │   ├── Change Arm Mode
    │   ├── Send Notification
    │   └── Add Delay
    │
    ├── Task 1: "Control Device"
    │   ├── Select device: "Bedroom Heater" (type: appliance)
    │   └── Function: "ON"
    │
    ├── Task 2: "Send Notification"
    │   └── Message: "Heater turned on — temperature below 18°C"
    │
    ├── Task order: [1. Turn on heater, 2. Send notification]
    │
    ▼
Step 4: NAME & SAVE
    ├── Name: "Auto Heater"
    ├── Icon: thermostat
    ├── Color: #FF5722
    └── Confirm
    │
    ▼
App calls: POST /api/v1/scenes
    Body: {
      "name": "Auto Heater",
      "type": "automation",
      "homeId": 1,
      "conditionLogic": "all",
      "icon": "thermostat",
      "color": "#FF5722",
      "isEnabled": true,
      "conditions": [
        {
          "type": "temperature",
          "operator": "<",
          "value": "18",
          "unit": "celsius"
        }
      ],
      "tasks": [
        {
          "type": "control_device",
          "deviceId": 15,
          "deviceName": "Bedroom Heater",
          "roomName": "Bedroom",
          "function": "ON",
          "orderIndex": 0
        },
        {
          "type": "send_notification",
          "notificationMessage": "Heater turned on — temperature below 18°C",
          "orderIndex": 1
        }
      ]
    }
    │
    ▼
Backend SceneService:
    ├── Saves scene to smart_scenes table
    ├── Saves conditions to scene_conditions table
    ├── Saves tasks to scene_tasks table (ordered by orderIndex)
    └── Returns scene with ID
```

### 7.3 How Automation Conditions Are Evaluated (EMQX Integration)

This is where EMQX is critical. The backend continuously listens for device data via MQTT and evaluates conditions.

```
                    EMQX BROKER
                         │
    ┌────────────────────┼────────────────────┐
    │                    │                    │
    ▼                    ▼                    ▼
/property/post      /status             External APIs
(sensor data)    (device online/       (weather, sunset,
                  offline)              location)

    │                    │                    │
    └────────────────────┼────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │  Condition Evaluator │
              │  (Backend Service)   │
              │                     │
              │  For each enabled   │
              │  automation scene:  │
              │                     │
              │  Check all/any      │
              │  conditions met?    │
              └─────────┬───────────┘
                        │
                  YES   │   NO
              ┌─────────┴─────────┐
              ▼                   ▼
    Execute Scene Tasks      Do nothing
    (in order)               (wait for next
                              data update)
```

#### Detailed Condition Evaluation Flow

```
Temperature sensor publishes to EMQX:
    Topic:   /fastbee/2001/sensor-temp-01/property/post
    Payload: { "temperature": 16.5, "humidity": 65 }
    │
    ▼
EMQX → Backend MQTT Message Handler receives message
    │
    ▼
Handler extracts: productId=2001, deviceNum=sensor-temp-01
    │
    ▼
Handler updates device metadata in PostgreSQL:
    UPDATE devices SET metadata = jsonb_set(metadata, '{lastReading}',
      '{"temperature": 16.5, "humidity": 65}')
    WHERE device_num = 'sensor-temp-01'
    │
    ▼
Handler triggers condition evaluation:
    │
    ├── Query: SELECT * FROM smart_scenes
    │          WHERE type = 'automation'
    │          AND is_enabled = true
    │          AND home_id = <device's home>
    │
    ├── For each automation scene:
    │   ├── Load conditions from scene_conditions
    │   ├── Evaluate each condition:
    │   │
    │   │   Condition: temperature < 18°C
    │   │   Current value: 16.5°C
    │   │   Result: TRUE ✓
    │   │
    │   ├── Check condition_logic:
    │   │   "all" → ALL conditions must be true
    │   │   "any" → ANY condition must be true
    │   │
    │   └── If conditions met AND not recently triggered (debounce):
    │       └── EXECUTE SCENE TASKS
    │
    ▼
Scene Task Execution (in order_index sequence):
    │
    ├── Task 0: control_device (Bedroom Heater → ON)
    │   ├── Lookup device → get productId, deviceNum
    │   ├── Publish MQTT:
    │   │   Topic:   /fastbee/1002/heater-bedroom/property/set
    │   │   Payload: { "command": "power", "params": { "state": "ON" } }
    │   └── Update device state in DB
    │
    ├── Task 1: send_notification
    │   ├── Create notification record in DB
    │   └── Send push notification to user's mobile app
    │
    └── Log execution:
        INSERT INTO scene_logs (scene_id, executed_at, triggered_by,
          execution_time_ms, status)
        VALUES (scene_id, NOW(), 'temperature_condition', 150, 'success')
```

### 7.4 All Supported Condition Types — How Each Works

#### Temperature Condition
```
Trigger:  IoT temperature sensor publishes reading via MQTT
Topic:    /fastbee/{pid}/{sensorId}/property/post
Payload:  { "temperature": 16.5 }
Check:    value <operator> threshold  (e.g., 16.5 < 18 → true)
```

#### Humidity Condition
```
Trigger:  IoT humidity sensor publishes reading via MQTT
Topic:    /fastbee/{pid}/{sensorId}/property/post
Payload:  { "humidity": 75 }
Check:    Mapped to categories: dry (<40), comfortable (40-70), moist (>70)
```

#### Weather Condition
```
Trigger:  Backend periodically fetches weather API (e.g., OpenWeatherMap)
Check:    Current weather matches operator (sunny, cloudy, rainy, snowy, hazy)
Interval: Every 15-30 minutes
```

#### Sunrise / Sunset Condition
```
Trigger:  Backend calculates sunrise/sunset times for user's location
Check:    Current time matches sunrise or sunset (±minutes offset)
Source:   Astronomical calculation or weather API
```

#### Wind Speed Condition
```
Trigger:  Weather API data
Check:    wind speed <operator> value (in m/s or km/h)
```

#### Location Condition (Geofencing)
```
Trigger:  Mobile app reports location changes to backend
Check:    User "arrive_at" or "leave_from" a specific location
Source:   Android LocationManager / Fused Location Provider
API:      App periodically sends: POST /api/v1/users/location
          Body: { "lat": 37.5665, "lng": 126.9780 }
```

#### Schedule Time Condition
```
Trigger:  Backend cron job / scheduler checks every minute
Check:    Current time matches schedule (e.g., 0730 = 7:30 AM)
Options:  every_day, weekdays, weekends, specific days
```

#### Device Status Condition
```
Trigger:  Device publishes status to EMQX
Topic:    /fastbee/{pid}/{deviceNum}/status
Payload:  { "status": "online" } or { "status": "offline" }
Check:    Device status matches condition (online/offline)
```

#### Arm Mode Condition
```
Trigger:  User changes security arm mode via app
Check:    Current mode matches: disarmed, arm_stay, arm_away
```

### 7.5 All Supported Task Types

| Task Type | What It Does | MQTT Involved? |
|-----------|-------------|----------------|
| `control_device` | Sends command to a device (ON/OFF, brightness, etc.) | YES — publishes to `/fastbee/.../property/set` |
| `select_scene` | Triggers another scene (chaining) | Indirectly — the chained scene may control devices |
| `change_arm_mode` | Changes security system mode | No — updates DB + notifies app |
| `send_notification` | Sends push notification to user | No — uses push notification service |
| `delay` | Waits N seconds before next task | No — backend timer |

### 7.6 Complex Automation Example — "Good Morning" Scene

```json
{
  "name": "Good Morning Routine",
  "type": "automation",
  "conditionLogic": "all",
  "conditions": [
    {
      "type": "schedule_time",
      "value": "0700",
      "operator": "weekdays"
    },
    {
      "type": "sunrise_sunset",
      "operator": "sunrise"
    }
  ],
  "tasks": [
    {
      "type": "control_device",
      "deviceName": "Bedroom Lamp",
      "function": "ON",
      "orderIndex": 0,
      "metadata": { "brightness": 30, "colorTemperature": 2700 }
    },
    {
      "type": "delay",
      "delaySeconds": 300,
      "orderIndex": 1
    },
    {
      "type": "control_device",
      "deviceName": "Bedroom Lamp",
      "function": "ON",
      "orderIndex": 2,
      "metadata": { "brightness": 100, "colorTemperature": 5000 }
    },
    {
      "type": "control_device",
      "deviceName": "Kitchen Coffee Maker",
      "function": "ON",
      "orderIndex": 3
    },
    {
      "type": "send_notification",
      "notificationMessage": "Good morning! Lights are on, coffee is brewing.",
      "orderIndex": 4
    }
  ]
}
```

**Execution**: At 7:00 AM on weekdays (if after sunrise):
1. Turn bedroom lamp on at 30% brightness, warm tone
2. Wait 5 minutes
3. Increase brightness to 100%, cool daylight tone
4. Turn on coffee maker
5. Send notification

---

## 8. Workflow D — Scheduled Scenes

### 8.1 How Scheduling Works with EMQX

```
┌──────────────────────────────────────────────────────────┐
│                    BACKEND SERVER                        │
│                                                          │
│  ┌──────────────┐    ┌──────────────────┐                │
│  │  Scheduler   │    │  Scene Evaluator │                │
│  │  (cron-like) │───►│                  │                │
│  │              │    │  Checks schedule │                │
│  │  Runs every  │    │  conditions for  │                │
│  │  minute      │    │  all enabled     │                │
│  │              │    │  automations     │                │
│  └──────────────┘    └────────┬─────────┘                │
│                               │                          │
│                    Conditions met?                       │
│                         │ YES                            │
│                         ▼                                │
│                 ┌──────────────┐                         │
│                 │ Task Executor│                         │
│                 │              │                         │
│                 │ For each task│──── MQTT PUBLISH ──────►│ EMQX
│                 │ in order:    │    to device topics     │
│                 └──────────────┘                         │
└──────────────────────────────────────────────────────────┘
```

### 8.2 Schedule Implementation in Backend

The backend needs a scheduler that runs every minute:

```typescript
// Conceptual flow (runs in backend)

// Every 60 seconds:
async function evaluateScheduledScenes() {
  const now = new Date();
  const currentTime = formatTime(now);       // e.g., "0730"
  const currentDay = getDayType(now);        // "weekday" | "weekend" | "monday" etc.

  // Find all enabled automation scenes with schedule_time conditions
  const scenes = await sceneRepository.findEnabledAutomations();

  for (const scene of scenes) {
    const conditions = await sceneConditionRepository.findBySceneId(scene.id);
    const allMet = evaluateConditions(conditions, { currentTime, currentDay });

    if (allMet && !recentlyTriggered(scene.id)) {
      await executeSceneTasks(scene.id);
      await logSceneExecution(scene.id, 'schedule');
    }
  }
}
```

### 8.3 User Creates a Scheduled Automation

```
User in SceneBuilderActivity:
    │
    ├── Adds condition: "Schedule Time"
    │   └── ScheduleTimeActivity:
    │       ├── Time: 22:00 (10 PM)
    │       └── Repeat: Every Day
    │
    ├── Adds tasks:
    │   ├── Task 1: Turn off all lights
    │   ├── Task 2: Lock front door
    │   └── Task 3: Set thermostat to 20°C
    │
    └── Saves as "Bedtime" automation
    │
    ▼
Every night at 10 PM, backend:
    ├── Scheduler matches time condition
    ├── Executes tasks:
    │   ├── MQTT → /fastbee/.../property/set → lights OFF
    │   ├── MQTT → /fastbee/.../property/set → lock LOCK
    │   └── MQTT → /fastbee/.../property/set → thermostat 20°C
    └── Logs execution in scene_logs table
```

---

## 9. MQTT Topic Design (FastBee Convention)

### 9.1 Topic Structure

```
/fastbee/{productId}/{deviceNum}/{category}/{action}
```

| Segment | Description | Examples |
|---------|-------------|---------|
| `productId` | Product type identifier (assigned by manufacturer/platform) | `1001` (lamp), `2001` (sensor), `3001` (camera) |
| `deviceNum` | Unique device identifier (serial number or custom ID) | `lamp-abc-123`, `sensor-temp-01` |
| `category` | Message category | `property`, `status`, `info` |
| `action` | Direction of communication | `set` (server→device), `post` (device→server) |

### 9.2 All Topics Used

| Topic | Direction | Purpose | QoS |
|-------|-----------|---------|-----|
| `/fastbee/{pid}/{dev}/property/set` | Server → Device | Send commands (power, brightness, etc.) | 1 |
| `/fastbee/{pid}/{dev}/property/post` | Device → Server | Report current state (sensor readings, status) | 1 |
| `/fastbee/{pid}/{dev}/status` | Device → Server | Report online/offline status | 1 |
| `/fastbee/{pid}/{dev}/info/post` | Device → Server | Report device info (firmware, hardware) | 0 |

### 9.3 Wildcard Subscriptions

```
Backend subscribes to:
    /fastbee/+/+/property/post   → receives ALL device property updates
    /fastbee/+/+/status          → receives ALL device status changes
    /fastbee/+/+/info/post       → receives ALL device info reports

Where:
    + = single-level wildcard (matches any single segment)
    # = multi-level wildcard (matches remaining segments) — not used here
```

---

## 10. Message Payload Reference

### 10.1 Command Payload (Server → Device)

```json
{
  "command": "power",
  "params": {
    "state": "ON"
  },
  "timestamp": "2026-01-28T10:30:00.000Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 10.2 Property Report (Device → Server)

```json
{
  "power": "ON",
  "brightness": 75,
  "color": "#FF6B35",
  "mode": "color",
  "timestamp": "2026-01-28T10:30:01.000Z"
}
```

### 10.3 Status Report (Device → Server)

```json
{
  "status": "online",
  "rssi": -45,
  "uptime": 3600,
  "firmware": "1.2.3"
}
```

### 10.4 Sensor Data Report

```json
{
  "temperature": 22.5,
  "humidity": 55,
  "pressure": 1013.25,
  "battery": 85,
  "timestamp": "2026-01-28T10:30:00.000Z"
}
```

### 10.5 Device-Specific Command Examples

**Lamp**:
```json
{
  "command": "lamp_control",
  "params": { "brightness": 80, "color": "#FFFFFF", "mode": "white" }
}
```

**AC**:
```json
{
  "command": "ac_control",
  "params": { "temperature": 24, "mode": "cooling", "windSpeed": 2 }
}
```

**Camera**:
```json
{
  "command": "camera_control",
  "params": { "action": "snapshot" }
}
```

**Speaker**:
```json
{
  "command": "speaker_control",
  "params": { "volume": 50, "action": "play", "service": "spotify" }
}
```

**Lock**:
```json
{
  "command": "lock_control",
  "params": { "action": "lock" }
}
```

---

## 11. Error Handling & Reconnection

### 11.1 MQTT Connection Loss

```
Connection Lost
    │
    ├── Backend (mqtt-client.ts):
    │   ├── Auto-reconnect enabled (5s interval)
    │   ├── Logs warning: "MQTT connection lost, reconnecting..."
    │   ├── On reconnect: re-subscribes to all wildcard topics
    │   └── Device commands queue until reconnected
    │
    └── Mobile App (MqttManager.java):
        ├── Auto-reconnect enabled
        ├── Keep-alive: 30s (broker detects dead client after 1.5× = 45s)
        ├── On reconnect: re-subscribes to all topics
        ├── Clean session: false → broker delivers missed messages
        └── UI shows connection indicator
```

### 11.2 Device Offline Detection

```
Device disconnects from EMQX (power loss, network issue)
    │
    ▼
EMQX detects via keep-alive timeout (or LWT message)
    │
    ▼
EMQX publishes Last Will and Testament (LWT):
    Topic:   /fastbee/{pid}/{dev}/status
    Payload: { "status": "offline" }
    │
    ▼
Backend receives:
    ├── Updates device status in PostgreSQL
    ├── Evaluates "device_status: offline" conditions
    └── Triggers any automation scenes that match
    │
    ▼
App receives:
    └── UI shows device as "offline" (greyed out)
```

### 11.3 Command Delivery Failure

```
Backend publishes command to device
    │
    ├── QoS 1: Broker acknowledges receipt (PUBACK)
    │   ├── If PUBACK received → command is in broker, will be delivered
    │   └── If no PUBACK → retry up to 3 times
    │
    ├── Device is offline:
    │   ├── EMQX queues message (if persistent session)
    │   └── Delivered when device reconnects
    │
    └── Command timeout (no response from device):
        ├── Backend retries after 5s, 10s, 30s
        ├── After 3 retries → mark command as failed
        └── Notify user: "Device not responding"
```

---

## 12. Security

### 12.1 Authentication Chain

```
Mobile App
    │
    ├── Login: POST /api/v1/auth/login → JWT token
    │
    ├── REST API calls: Authorization: Bearer <JWT>
    │   └── Backend validates JWT via AuthMiddleware
    │
    └── MQTT connection: username/password to EMQX
        └── EMQX validates via built-in DB or HTTP auth plugin
```

### 12.2 Security Best Practices

| Layer | Practice |
|-------|----------|
| EMQX Auth | Use per-device credentials or certificate-based auth (mTLS) |
| EMQX ACL | Restrict topics — devices can only publish/subscribe to their own topics |
| EMQX TLS | Use port 8883 (MQTTS) in production |
| Backend | Validate device ownership before sending commands |
| App | Store MQTT credentials in Android Keystore, not SharedPreferences |
| Payloads | Never include sensitive data (passwords, tokens) in MQTT payloads |

### 12.3 EMQX Authentication Plugin (Recommended for Production)

Configure EMQX to authenticate MQTT clients against your backend:

```
EMQX → HTTP Auth Plugin → POST http://backend:3003/api/v1/mqtt/auth
    Body: { "username": "device-lamp-abc", "password": "..." }
    Backend validates → 200 OK or 401 Unauthorized
```

---

## 13. Testing Checklist

### 13.1 EMQX Connection

- [ ] EMQX Docker container running on port 1883
- [ ] Dashboard accessible at `http://localhost:18083`
- [ ] Backend connects successfully (check logs)
- [ ] Mobile app connects successfully (check MqttManager logs)

### 13.2 Device Lifecycle

- [ ] Discover device via `/devices/discover`
- [ ] Register device via `POST /devices`
- [ ] Simulate device coming online (MQTT publish to `/status`)
- [ ] Verify device shows as "online" in app
- [ ] Simulate device going offline (disconnect from EMQX)
- [ ] Verify LWT triggers and device shows "offline"

### 13.3 Device Control

- [ ] Send power ON command → verify MQTT message published
- [ ] Verify IoT device (or simulator) receives command
- [ ] Verify device confirms state via `/property/post`
- [ ] Verify app UI updates in real-time

### 13.4 Smart Automation

- [ ] Create automation scene with temperature condition
- [ ] Publish fake sensor data via MQTT:
  ```bash
  mosquitto_pub -h localhost -t "/fastbee/2001/sensor-01/property/post" \
    -m '{"temperature": 15}' -q 1
  ```
- [ ] Verify condition evaluator triggers scene
- [ ] Verify tasks execute in correct order
- [ ] Verify scene_logs table records execution
- [ ] Verify delay tasks work correctly

### 13.5 MQTT Testing Tools

```bash
# Subscribe to all topics (monitor everything)
mosquitto_sub -h localhost -t "/fastbee/#" -v

# Publish a test device status
mosquitto_pub -h localhost -t "/fastbee/1001/test-device/status" \
  -m '{"status":"online"}'

# Publish test sensor data
mosquitto_pub -h localhost -t "/fastbee/2001/sensor-01/property/post" \
  -m '{"temperature":22.5,"humidity":55}'

# Simulate a command to a device
mosquitto_pub -h localhost -t "/fastbee/1001/lamp-01/property/set" \
  -m '{"command":"power","params":{"state":"ON"}}'
```

### 13.6 Demo Mode

Use demo login (`demo@smartify.com` / `demo123456`) to test UI flows without EMQX or backend. MockDataProvider simulates all API responses locally on the Android device.

---

## Quick Reference — Complete Data Flow

```
                        ┌──────────────────────────────┐
                        │        EMQX BROKER           │
                        │    (Message Router Hub)      │                        └─────┬──────┬──────┬──────────┘
                              │      │      │
              ┌───────────────┘      │      └───────────────┐
              │                      │                      │
              ▼                      ▼                      ▼
    ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
    │   IoT Devices   │   │    Backend      │   │   Mobile App    │
    │                 │   │                 │   │                 │
    │ • Connect to    │   │ • Subscribes    │   │ • Subscribes    │
    │   EMQX on boot  │   │   to all topics │   │   to all topics │
    │ • Subscribe to  │   │ • Saves state   │   │ • Updates UI    │
    │   .../set       │   │   to PostgreSQL │   │   in real-time  │
    │ • Publish to    │   │ • Evaluates     │   │ • Sends commands│
    │   .../post      │   │   automation    │   │   via REST API  │
    │   .../status    │   │   conditions    │   │   (or direct    │
    │                 │   │ • Executes      │   │    MQTT)        │
    │                 │   │   scene tasks   │   │                 │
    │                 │   │ • Runs          │   │                 │
    │                 │   │   scheduler     │   │                 │
    └─────────────────┘   └─────────────────┘   └─────────────────┘

REST API Flow:
    App → POST /devices/:id/control/power → Backend → MQTT → Device

MQTT Real-Time Flow:
    Device → MQTT → EMQX → Backend (save) + App (UI update)

Automation Flow:
    Device data → EMQX → Backend evaluates conditions → MQTT → Target devices
```
