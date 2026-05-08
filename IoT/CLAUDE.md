# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Smartify** — Full-stack IoT smart home system with two components:
- **`Smart-Home/`** — Native Android app (Java 8, Gradle, API 24–33)
- **`Smart-Home-Backend/`** — REST API (Express.js, TypeScript 5.3, PostgreSQL)

## Build & Development Commands

### Backend (`Smart-Home-Backend/`)
```bash
npm install              # Install dependencies
npm run dev              # Dev server with nodemon (auto-reload)
npm run build            # Compile TypeScript
npm start                # Production start (node dist/app.js)
npm run migrate          # Run database migrations (dev)
npm run migrate:prod     # Run database migrations (prod)
npm run lint             # ESLint
npm run format           # Prettier
```

### Frontend (`Smart-Home/`)
```bash
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease      # Release APK (ProGuard enabled)
./gradlew test                 # Unit tests (JUnit 4)
./gradlew connectedAndroidTest # Instrumented tests (Espresso)
./gradlew clean                # Clean build
```

APK output: `app/build/outputs/apk/` (named `Smartify_<version>.apk`)

### Database Setup
```bash
createdb -U postgres smart_home_db
psql -U postgres -d smart_home_db -f database/schema.sql
```
Migrations auto-run on server start when `AUTO_MIGRATE=true` in `.env`.

## Architecture

### Backend — Layered Architecture
```
Routes → Controllers → Services → Repositories → PostgreSQL
```
- Constructor-based dependency injection
- Repository pattern with interfaces in `src/domain/repositories/`
- JWT authentication via middleware; `AuthInterceptor` on frontend
- Swagger UI at `/api-docs`
- Winston logging to `logs/` (date-based subdirs)

### Frontend — Activity-based MVC
```
Activity → ApiService (Retrofit) → OkHttp + AuthInterceptor → Backend
Response → Gson → Model → Globals (cache) → UI update
```
- 60+ Activities, 43+ Adapters, no MVVM — activities manage own state
- `Globals.java` singleton cache: homes (10min TTL), rooms/devices (5min), health (1min)
- `AuthManager.java` handles JWT/session via SharedPreferences
- `ApiClient.java` configures base URL, timeouts (30s API, 5s health check)
- `MainActivity.java` (~111KB) is the central hub for device grid, room filtering, bottom nav

### API Configuration
- Backend port: `3003` (configurable via `.env`)
- API base path: `/api/v1/`
- Frontend base URL hardcoded in `ApiClient.java`
- Emulator uses `http://10.0.2.2:3003`, physical devices use LAN IP

### Adding a New Feature (end-to-end)
1. **Backend**: route → controller → service → repository → validator → migration SQL → Swagger docs
2. **Frontend**: `ApiService.java` method → model classes → Activity/UI → `MockDataProvider` for demo mode

## Key Conventions

### Backend
- Prettier: single quotes, semicolons, 100-char width, 2-space indent
- ESLint: `@typescript-eslint/no-explicit-any` is warn, unused vars with `_` prefix allowed
- TypeScript strict mode enabled
- Parameterized SQL queries (no string interpolation)

### Frontend
- Java 8, camelCase methods/variables, PascalCase classes
- All strings in `res/values/strings.xml` (900+ entries, English + Korean)
- Dark theme primary: colors defined in `colors.xml` with `values-night/` overrides
- Custom font: Urbanist (in `res/font/`)
- Figma reference: 430×888 (Pixel 8 Pro)
- Network security config allows cleartext HTTP for dev servers

## Demo Mode
Login with `demo@smartify.com` / `demo123456` to use `MockDataProvider` for offline testing without backend.

## IoT Communication Stack
- **Mobile ↔ Backend**: REST API (Retrofit) + WebSocket for real-time device status
- **Backend ↔ Devices**: MQTT via EMQX broker at `tcp://172.86.88.76:1883`; FastBee topic convention `/fastbee/{productId}/{deviceNum}/{action}`
- **SSE**: Server-Sent Events for backend push notifications
- **Bluetooth**: `FastBeeBluetoothService` for BLE device provisioning (WiFi-less pairing)
- Mobile does **not** connect to EMQX directly — all device communication goes through the backend

## Backend Database Schema
Main tables and relationships:
```
Users ──1:N──> Homes ──1:N──> Rooms ──1:N──> Devices
Users ──1:N──> Notifications
Users ──1:N──> Chatbot Messages
Users ──1:1──> Notification Preferences / Security Settings / Profile Metadata
```
- JSONB columns for flexible device metadata
- ENUM types for device status (`online`/`offline`/`unknown`) and type (`sensor`/`actuator`/`controller`)
- Migrations tracked in `migrations` table; add new SQL files to `database/` and register in `src/utils/migrations.ts`

## Backend Utility Scripts
Located in `Smart-Home-Backend/scripts/`:
- `create-test-user.js` — seed a test user
- `reset-password.js` — reset user password
- `insert-sample-energy-data.js` — insert sample data

## Figma-to-Android Conversion
Design basis is 430×888 (Pixel 8 Pro, xxhdpi/480 DPI). Key conversion:
- **dp = px / 3** (480 DPI / 160 = 3x scale factor)
- Prefer percentage-based constraints: `constraintWidth_percent = px / 430`, `constraintHeight_percent = px / 888`
- Status bar (44px) is system UI — excluded from design area calculations

## Additional Documentation
- `Smart-Home/CLAUDE.md` — detailed Android-specific guidance
- `Smart-Home-Backend/README.md` — backend setup and API endpoints
- `.cursorrules` files (root, backend, frontend) — detailed architecture, UI patterns, and coding rules
- `DEVICE_CONTROL_ARCHITECTURE.md`, `BLUETOOTH_GATEWAY_ARCHITECTURE.md` — IoT-specific architecture
- `Smart-Home/MQTT_EMQX_ARCHITECTURE.md` — MQTT topic structure and data flow
- `Smart-Home/I18N_SETUP.md` — internationalization (English + Korean)
