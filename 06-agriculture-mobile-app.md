# Agriculture Mobile App

## Overview
A mobile app for farmers and agricultural workers to monitor crop health, manage farm operations, access weather forecasts, track resources, and get expert advice — improving productivity and reducing losses.

---

## Features

### Core Features
- **Farm Dashboard** — Overview of farm plots, crop status, and pending tasks
- **Crop Management** — Add and manage crops: type, planting date, expected harvest, notes
- **Weather Forecast** — Location-based 7-day weather forecast with farming impact alerts
- **Soil Monitoring** — Log or receive soil data: pH, moisture, temperature, nutrients
- **Irrigation Management** — Schedule and track irrigation events; smart recommendations
- **Pest & Disease Detection** — Identify pests/diseases from leaf photos using AI
- **Fertilizer & Pesticide Log** — Track what was applied, when, and in what quantity
- **Harvest Tracking** — Log harvest yield per plot; compare against targets

### Additional Features
- **Market Prices** — Live commodity price listings (local market rates)
- **Expert Advisory** — Ask agronomists questions via chat or forum
- **Farm Calendar** — Planting and activity planner with reminders
- **Expense & Income Tracker** — Track farm costs and revenue per season
- **Resource Inventory** — Track seed stock, fertilizer, equipment availability
- **Offline Mode** — Full offline access to logs and basic features
- **Satellite Imagery** — NDVI maps to assess crop health across large plots
- **Multi-Farm Support** — Manage multiple farm locations

---

## Application Logic

### Crop Health Logic
- User photos a leaf → image sent to AI model (CNN trained on PlantVillage dataset)
- Model returns: disease name, confidence score, recommended treatment
- Store detection history per crop/plot

### Weather Alert Logic
- Fetch weather data from OpenWeatherMap or AgWeather API
- Analyze forecast for: frost risk, heavy rain, extreme heat, drought conditions
- Push alert to farmer if critical condition detected (e.g., frost in 24 hours → protect crops)

### Irrigation Scheduling Logic
- Input: crop type, soil type, current soil moisture (manual or sensor)
- Calculate water deficit using evapotranspiration (ET) formula (FAO-56)
- Recommend irrigation amount and next irrigation date
- Optionally trigger IoT irrigation valve via MQTT

### Soil Data Logic
- Manual entry or IoT sensor integration (NPK, pH, moisture sensors)
- Compare values against optimal range for selected crop
- Flag deficiencies and recommend corrective action (e.g., "Apply lime to raise pH")

### Expense Tracker Logic
- Log expense by category: seed, labor, fertilizer, pesticide, fuel, equipment
- Aggregate costs per crop cycle and per plot
- Calculate estimated profit = projected yield × market price − total expenses

---

## Challenges

| Challenge | Description |
|-----------|-------------|
| AI Disease Detection Accuracy | Training/using a model accurate enough to be trusted by farmers |
| Offline-First | Farmers in rural areas have unreliable connectivity |
| IoT Sensor Integration | Diverse sensor hardware with different protocols (MQTT, HTTP, BLE) |
| Low-Tech Users | App must be simple enough for users with minimal smartphone experience |
| Multilingual & Local Language | Support for regional languages and local crop varieties |
| Satellite Image Processing | Fetching and rendering NDVI satellite imagery efficiently |
| Data Accuracy | Manual data entry leads to errors; need simple UX to minimize mistakes |
| Agronomic Knowledge Base | Building or sourcing a comprehensive crop/pest/disease knowledge base |

---

## Recommended Tech Stack
- **Mobile**: Flutter (excellent offline + cross-platform)
- **Backend**: Node.js + PostgreSQL with PostGIS for farm plot geometry
- **AI/ML**: TensorFlow Lite (on-device inference) or REST API to Python Flask model
- **Weather API**: OpenWeatherMap / Tomorrow.io
- **IoT**: MQTT broker (Mosquitto) + Node-RED
- **Offline**: SQLite / Hive
- **Satellite Imagery**: Google Earth Engine API or Sentinel Hub
- **Maps**: Google Maps or Mapbox with polygon drawing tools
