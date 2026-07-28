# Mobile App Structure Rules

Reusable conventions for building native **Android (Java)** apps that talk to a REST backend, extracted from the Smartify Smart-Home Android client. Apply these rules to any new Android app that should share the same shape.

Stack assumptions:
- **Language**: Java 8 (no Kotlin in this codebase)
- **Min/target SDK**: API 24 (Android 7.0) – API 33 (Android 13)
- **Build**: Gradle (no KTS, plain `.gradle`)
- **Architecture**: Activity-based MVC (no MVVM/ViewModel)
- **Network**: Retrofit 2 + OkHttp 4 + Gson
- **Real-time**: Server-Sent Events + WebSocket (raw OkHttp), BLE for provisioning
- **State**: Static singleton (`Globals`) + SharedPreferences (`AuthManager`)
- **Demo mode**: `MockDataProvider` for offline use

For build/env setup, see `MOBILE_ENVIRONMENT_GUIDE.md` in this folder.

---

## 1. Package Layout

Root package: `com.<org>.<app>` (e.g. `com.smarthome.iot`).

```
com.smarthome.iot/
├── App.java                  # Application class — locale + ApiClient init + health check
├── ui/                       # Activities, Adapters, Fragments, Dialogs (one screen = one Activity)
├── network/                  # Retrofit clients, interceptors, real-time clients
│   ├── ApiClient.java
│   ├── ApiService.java
│   ├── AuthInterceptor.java
│   ├── HealthCheckService.java
│   ├── DeviceSSEClient.java
│   └── WebSocketManager.java
├── models/                   # POJO data classes for request/response (Gson-serialized)
├── services/                 # Long-running Android Services (BLE, foreground WebSocket)
├── utils/                    # Singletons + helpers (Globals, AuthManager, *Helper)
└── views/                    # Custom Views and ViewGroups
```

**Rule:** Don't add new top-level packages without a clear reason. If something doesn't fit `ui/network/models/services/utils/views`, the boundary is probably wrong.

**Rule:** All UI classes go in `ui/`. Don't create `ui/activities/`, `ui/adapters/`, `ui/dialogs/` subpackages — flat is fine for ~100 classes and keeps imports short. If `ui/` exceeds ~150 classes, group by **feature** (e.g. `ui/devices/`, `ui/scenes/`), not by **type** (no `ui/adapters/`).

---

## 2. Architecture: Activity-Based MVC

**No MVVM, no ViewModel, no LiveData, no DataBinding.** Activities are the controller; they hold state, call the API directly, and update views via `findViewById`.

```
                ┌─────────────────────────────────────────┐
User action ──► │  Activity                                │
                │   ├── findViewById, setOnClickListener   │
                │   ├── ApiClient.getClient()              │
                │   │     .create(ApiService.class)        │
                │   │     .someEndpoint(...)               │
                │   │     .enqueue(callback)               │
                │   ├── Globals.set*  /  Globals.get*      │
                │   └── runOnUiThread { update views }     │
                └─────────────────────────────────────────┘
                                   │
                                   ▼
                ┌─────────────────────────────────────────┐
                │  ApiService (Retrofit interface)         │
                └─────────────────────────────────────────┘
                                   │
                                   ▼
                ┌─────────────────────────────────────────┐
                │  OkHttp ── AuthInterceptor ── Backend    │
                └─────────────────────────────────────────┘
                                   │
                                   ▼
                          JSON ── Gson ── Model
```

**Why:** small team, no shared screens, fast iteration. The cost of an MVVM migration is high and the win marginal at this size.

**Rules:**
- Activities own their state. No global ViewModel.
- Network calls happen *in the Activity*, not in a service or repository.
- Multi-activity workflows (wizards) pass primitives through `Intent` extras — no shared in-memory blob.
- Long-running ops (BLE pairing, WebSocket) live in `services/` as Android `Service`s; Activities `bind()` to them.

---

## 3. Naming Conventions

| Kind                | Convention                            | Example                          |
|---------------------|---------------------------------------|----------------------------------|
| Activity            | `<Feature>Activity`                   | `DeviceControlActivity`          |
| Adapter             | `<Item>Adapter`                       | `DeviceListAdapter`              |
| Custom view         | `<Name>View`                          | `QrScanOverlayView`              |
| Helper / utility    | `<Domain>Helper`                      | `IconifyHelper`, `ThemeHelper`   |
| Singleton manager   | `<Domain>Manager`                     | `AuthManager`                    |
| Background service  | `<Purpose>Service`                    | `WebSocketDeviceStatusService`   |
| Network client      | `<Channel>Client` / `<Channel>Manager`| `DeviceSSEClient`, `WebSocketManager` |
| Model / DTO         | `<Entity>` / `<Entity>Request` / `<Entity>Response` | `Device`, `LoginRequest` |
| Layout file         | `<scope>_<feature>.xml`               | `activity_main.xml`, `item_device.xml`, `dialog_confirm.xml` |
| Drawable            | `ic_<name>` (icon), `bg_<name>` (background), `selector_<name>` | `ic_lightbulb`, `bg_card_dark` |
| String resource     | `<screen>_<element>` (snake_case)     | `device_control_title`           |
| Color resource      | semantic name in `colors.xml`          | `dark_1`, `primary`              |

**Java style:**
- Classes: `PascalCase`
- Methods/variables/fields: `camelCase` — no `m`/`s` prefixes (no `mContext`, just `context`)
- Constants: `UPPER_SNAKE_CASE`
- Static singletons: instance method `getInstance()`

---

## 4. Network Layer

### 4.1 Retrofit singleton (`ApiClient`)

One `Retrofit` instance per base URL. Lazily built. Initialized with app context at `Application.onCreate`.

```java
public class ApiClient {
    private static final String BASE_URL = "http://172.86.88.76:3003/api/v1/";
    private static Retrofit retrofit = null;
    private static Context appContext = null;

    public static void initialize(Context ctx) { appContext = ctx.getApplicationContext(); }

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }
}
```

**Rules:**
- Health-check endpoints get a **separate Retrofit instance** (`getHealthClient()`) with shorter 5s timeouts and no auth interceptor — never share a 30s-timeout client with health probing.
- API timeouts: **30s** connect/read/write for normal API.
- Health timeouts: **5s** connect/read/write.
- Base URL **NOT** hardcoded once you have more than one environment — push it to `BuildConfig` (see `MOBILE_ENVIRONMENT_GUIDE.md` §5).

### 4.2 `ApiService` interface

One Retrofit interface for the entire app. Group endpoints by resource using comments. Every method returns `Call<ApiResponse<T>>`.

```java
public interface ApiService {
    // ===== Auth =====
    @POST("users/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest body);

    // ===== Homes =====
    @GET("homes")
    Call<ApiResponse<List<Home>>> getHomes();

    @POST("homes")
    Call<ApiResponse<Home>> createHome(@Body CreateHomeRequest body);

    // ===== Devices =====
    @GET("devices")
    Call<ApiResponse<List<Device>>> getDevices(@Query("homeId") int homeId);
}
```

### 4.3 `ApiResponse<T>` envelope

Mirrors the backend response shape exactly:

```java
public class ApiResponse<T> {
    public boolean success;
    public T data;
    public ApiError error;        // present when success == false
    public Meta meta;

    public static class ApiError { public String code; public String message; }
    public static class Meta     { public String timestamp; }
}
```

### 4.4 Calling pattern

Always async via `enqueue`. Always check `response.isSuccessful()` AND `body.success`:

```java
api.getDevices(homeId).enqueue(new Callback<ApiResponse<List<Device>>>() {
    @Override public void onResponse(Call<ApiResponse<List<Device>>> call,
                                     Response<ApiResponse<List<Device>>> response) {
        if (response.isSuccessful() && response.body() != null && response.body().success) {
            List<Device> devices = response.body().data;
            Globals.setCachedDevices(homeId, devices);
            runOnUiThread(() -> renderDevices(devices));
        } else {
            showError(response.body() != null ? response.body().error.message : "Request failed");
        }
    }
    @Override public void onFailure(Call<ApiResponse<List<Device>>> call, Throwable t) {
        showError("Network error: " + t.getMessage());
    }
});
```

**Never** use `.execute()` (synchronous) on the main thread.

### 4.5 `AuthInterceptor`

Reads JWT from SharedPreferences (`auth_prefs/auth_token`) and adds `Authorization: Bearer <token>`.

**Skip rules** — token must be omitted for endpoints that issue/refresh credentials or run before login:
- `health` (any health endpoint)
- `users/login`
- `users/signup`
- `users/forgot-password`
- onboarding endpoints

Implement skip via URL substring match in the interceptor.

### 4.6 Real-time channels

| Channel    | Class                              | Backend endpoint                 | Use                       |
|------------|------------------------------------|----------------------------------|---------------------------|
| SSE        | `DeviceSSEClient`                  | `GET /api/v1/devices/stream`     | Device status push        |
| WebSocket  | `WebSocketManager` + `WebSocketDeviceStatusService` | `ws://.../api/v1/devices/stream` | Bidirectional updates    |
| BLE        | `FastBeeBluetoothService`          | n/a                              | Wi-Fi-less device pairing |

**Rule:** Mobile **never** connects to MQTT directly. All device traffic goes through the backend. SSE/WebSocket are read-only consumers of backend pushes; control commands go via REST.

---

## 5. State Management

### 5.1 `Globals` (in-memory cache singleton)

A class with `static` fields and `static` getters/setters. Holds TTL'd caches keyed by `homeId` where applicable. Cleared on logout.

| Cache            | TTL    | Key                | Notes                                |
|------------------|--------|--------------------|--------------------------------------|
| API health       | 1 min  | -                  | Backend reachable + MQTT broker up   |
| User profile     | -      | -                  | Set at login, cleared at logout      |
| Homes            | 10 min | -                  | List of `Home`                       |
| Rooms            | 5 min  | `homeId`           | Refetch when `cachedRoomsHomeId` changes |
| Devices          | 5 min  | `homeId`           | Refetch when `cachedHomeId` changes  |
| Scenes           | 5 min  | `homeId` + `type`  | `automation` or `tap_to_run`         |

**Rules:**
- Always check cache validity (`now - lastLoadTime < TTL` AND key matches) before re-fetching.
- Provide a `clearAll()` for logout.
- No business logic in `Globals` — only get/set/has-valid-cache.

### 5.2 `AuthManager` (persistent)

Wraps `SharedPreferences("auth_prefs")` for:
- `auth_token` (JWT) — also pushed to `AuthInterceptor` as a static fallback
- `user_id` (int)
- `user_email` (string)
- `user_name` (string)
- `saved_email` (last entered, for autofill)
- `auto_login_enabled` (bool)

**Rules:**
- One `AuthManager` instance per Activity (cheap — just wraps prefs).
- `isLoggedIn()` is the single source of truth for guarding screens.
- On logout: `clear()` prefs + `Globals.clearAll()` + redirect to `LoginActivity`.

### 5.3 Demo mode

Triggered by login with the demo email/password (`demo@smartify.com` / `demo123456`). When active, all API calls are short-circuited to `MockDataProvider`, which returns canned `ApiResponse<T>` synchronously.

**Rule:** Demo mode is a **first-class branch** in every Activity that fetches data:
```java
if (AuthManager.isDemoMode()) {
    renderDevices(MockDataProvider.getDevices());
} else {
    api.getDevices(homeId).enqueue(...);
}
```
Don't try to mock at the Retrofit layer — too brittle.

---

## 6. Models / DTOs

- All models live in `models/` as flat POJOs.
- Public fields (Java) — no getters/setters unless logic is needed. Gson reads/writes fields directly.
- Dates are `String` (ISO-8601) at the wire layer, parsed at the call site if needed.
- Request bodies and response payloads each get their own class: `LoginRequest`, `LoginResponse`, `CreateHomeRequest`, etc.
- Don't reuse a single `User` class for both request and response — diverging fields are inevitable.

```java
public class Device {
    public int id;
    public int homeId;
    public int roomId;
    public String name;
    public String type;          // "sensor" | "actuator" | "controller"
    public String status;        // "online" | "offline" | "unknown"
    public String createdAt;
    public String updatedAt;
}
```

---

## 7. UI Conventions

### 7.1 Activity per screen

One Activity per logical screen. Navigate via `Intent` + extras. Don't reuse one Activity with switching layouts — split.

### 7.2 RecyclerView + Adapter

Standard pattern:
- `RecyclerView` in the Activity layout.
- `<Item>Adapter extends RecyclerView.Adapter<<Item>Adapter.ViewHolder>` with inner `ViewHolder`.
- Click handling via a constructor-passed listener interface, not via the Activity calling `getAdapter()`.

### 7.3 Bottom navigation

5 fixed tabs: **Home, Rooms, Smart Scenes, Notifications, Account**. Each tab launches a separate Activity (or replaces fragments inside `MainActivity`). Use `@style/CustomBottomNavigationView`, `42dp` height, `14dp` icon, `dark_1` background.

### 7.4 Tab buttons / pill toggles

Material defaults clash with the dark theme. When using `MaterialButton`/`Button` as a tab:
- Clear `backgroundTintList`
- `setStateListAnimator(null)` to kill the elevation animation
- Toggle between `tab_selected_background` (primary) and `tab_unselected_background` (dark gray) via `view.post { ... }` callbacks (so the toggle survives layout pass)

### 7.5 Status bar

Set programmatically to `dark_1` (`#181A20`) in every Activity that has a custom appbar. Don't rely on theme alone — Android resets it on some OEMs.

### 7.6 Fonts

**Every** `TextView`, `Button`, `EditText` must declare `android:fontFamily="@font/urbanist"`. No system fonts. Enforce in code review.

### 7.7 Layout file conventions

- `activity_*.xml` for top-level screens
- `fragment_*.xml` for fragments (rare in this codebase)
- `item_*.xml` for RecyclerView rows
- `dialog_*.xml` for custom dialogs
- `view_*.xml` for reusable include layouts

---

## 8. Resources

```
res/
├── values/                # Default (English-equivalent + light theme)
│   ├── colors.xml         # 144+ semantic colors
│   ├── strings.xml        # 900+ entries — ALL user-facing strings live here
│   ├── styles.xml         # 20+ text style variants
│   └── themes.xml
├── values-night/          # Dark theme overrides — primary theme of this app
├── font/                  # Urbanist .ttf files
├── drawable/              # VectorDrawables (converted from Iconify SVGs)
├── layout/                # Portrait layouts
├── layout-land/           # Landscape variants (only when needed)
├── menu/                  # Bottom nav, popup menus
├── mipmap-*dpi/           # App icon densities
└── xml/
    └── network_security_config.xml   # Cleartext HTTP allow-list for dev
```

**Rules:**
- **No hard-coded strings in layouts or Java code.** Every label goes in `strings.xml`. Lint rule should fail on `tools:ignore="HardcodedText"` abuse.
- **No hard-coded colors.** Use `@color/<semantic_name>` so dark/light theming works.
- English is the current UI language. Add every label to `values/strings.xml`.

---

## 9. Localization & Theming

### 9.1 `LocaleHelper`

Applies the saved locale before each Activity's base context attaches:

```java
public class App extends Application {
    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(base));
    }
}
```

Every Activity that supports runtime locale switching must override `attachBaseContext` similarly.

### 9.2 `ThemeHelper`

Central place to flip between light/dark/system. The app primarily ships **dark theme** (`values-night/` is the design source of truth); light is a follow-up.

### 9.3 Custom font

Urbanist is the only font. Declared in `res/font/urbanist.xml` (font family with weights). Every text view sets `android:fontFamily="@font/urbanist"`.

---

## 10. Figma → Android Conversion

Design baseline: **430 × 888 px** (Pixel 8 Pro, xxhdpi/480 DPI). Status bar (44px) is excluded.

| Figma px           | Android equivalent                                                       |
|--------------------|--------------------------------------------------------------------------|
| Width              | `app:layout_constraintWidth_percent = px / 430`                          |
| Height             | `app:layout_constraintHeight_percent = px / 888`                         |
| Margin / padding   | `<margin> = px / 3` dp (480 DPI / 160 baseline)                          |
| Font size          | `<size> = px / 3` sp                                                     |
| Corner radius      | `<r> = px / 3` dp                                                        |

**Rule:** Prefer percentage constraints over fixed dp for any element that should scale to other devices. Use ConstraintLayout chains and guidelines to mirror Figma auto-layout.

---

## 11. Background Services

Two long-lived services under `services/`:

| Service                          | Purpose                                                  | Type          |
|----------------------------------|----------------------------------------------------------|---------------|
| `WebSocketDeviceStatusService`   | Maintains WebSocket to backend for live device updates   | Foreground    |
| `FastBeeBluetoothService`        | BLE provisioning (Wi-Fi credentials over Bluetooth)      | Started+bound |

**Rules:**
- Long-running connections live in services, never in Activities.
- Foreground services need a notification channel + `FOREGROUND_SERVICE` permission (declared in manifest).
- Activities `bind()` to receive callbacks; service survives Activity rotation/death.
- Broadcast updates with `LocalBroadcastManager` so multiple Activities can listen.

---

## 12. Logging

- Use `android.util.Log.d/i/w/e(TAG, message)`.
- `TAG` is a `private static final String` per class, `<= 23 chars` (Android limit pre-API 24 — easier to keep the rule than relax it).
- **Never** log JWTs, passwords, full request bodies for auth endpoints, or PII at `info` level.
- HTTP logging via `HttpLoggingInterceptor.Level.BODY` — debug only. Strip down for release builds.
- For release: prefer `BuildConfig.DEBUG` gates around verbose logging instead of removing logs entirely.

---

## 13. Testing

- **Unit tests**: JUnit 4 in `app/src/test/`.
- **Instrumented tests**: Espresso in `app/src/androidTest/`.
- Run: `./gradlew test` (unit), `./gradlew connectedAndroidTest` (instrumented, needs device/emulator).
- Activity-based MVC is hard to unit-test — pull testable logic into helpers (`*Helper`) and test those, not the Activity.

---

## 14. Anti-Patterns to Reject

- ❌ Synchronous `.execute()` on a Retrofit `Call` from the main thread.
- ❌ Hard-coded strings in Java or XML (use `strings.xml`).
- ❌ Hard-coded colors / hex values in layouts (use `colors.xml`).
- ❌ Direct `SharedPreferences` access outside `AuthManager` for auth data.
- ❌ Network calls inside Adapters or custom Views — Activities call the API and pass data down.
- ❌ Multiple `Retrofit.Builder()` calls scattered across the codebase — only `ApiClient` builds Retrofit.
- ❌ Mock data toggled at the Retrofit layer — handle demo mode via an explicit branch in the Activity.
- ❌ Direct MQTT connection from the app — use the backend.
- ❌ Activities longer than ~1500 lines (the existing `MainActivity` is a known outlier; don't replicate its size).
- ❌ Unclosed WebSocket / SSE clients — every `Activity.onDestroy` that opened a stream must close it.
- ❌ Adding a getter/setter pair to a model "for symmetry" — public fields are fine for Gson POJOs.

---

## 15. Adding a New Feature (End-to-End Checklist)

For feature `Foo`:

1. **Models** — add `Foo`, `CreateFooRequest`, `UpdateFooRequest` (and any nested types) in `models/`.
2. **API** — add endpoints to `ApiService.java`, returning `Call<ApiResponse<...>>`.
3. **Mock data** — add canned data + responses to `MockDataProvider` for demo mode.
4. **Cache** — if the data is reused across screens, add a TTL'd cache field + getter/setter to `Globals`.
5. **Strings** — add labels to `res/values/strings.xml`.
6. **Layout** — `activity_foo.xml`, `item_foo.xml` (if list), `dialog_foo_*.xml` (if dialogs).
7. **Adapter** — `FooListAdapter` if it's a list.
8. **Activity** — `FooActivity extends AppCompatActivity` with API call + demo branch + cache check.
9. **Manifest** — register the Activity.
10. **Navigation** — wire entry points (bottom nav, click handlers in MainActivity, etc.).

---

## Quick Reference: Activity Skeleton

```java
public class FooActivity extends AppCompatActivity {
    private static final String TAG = "FooActivity";

    private AuthManager auth;
    private ApiService api;
    private FooListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foo);

        auth = new AuthManager(this);
        if (!auth.isLoggedIn()) { redirectToLogin(); return; }

        api = ApiClient.getClient().create(ApiService.class);

        RecyclerView rv = findViewById(R.id.recycler);
        adapter = new FooListAdapter(this::onFooClick);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadFoos();
    }

    private void loadFoos() {
        if (auth.isDemoMode()) {
            adapter.submit(MockDataProvider.getFoos());
            return;
        }
        if (Globals.hasValidFooCache()) {
            adapter.submit(Globals.getCachedFoos());
            return;
        }
        api.getFoos().enqueue(new Callback<ApiResponse<List<Foo>>>() {
            @Override public void onResponse(Call<ApiResponse<List<Foo>>> c,
                                             Response<ApiResponse<List<Foo>>> r) {
                if (r.isSuccessful() && r.body() != null && r.body().success) {
                    Globals.setCachedFoos(r.body().data);
                    adapter.submit(r.body().data);
                } else {
                    Toast.makeText(FooActivity.this, R.string.error_load_foos, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<Foo>>> c, Throwable t) {
                Log.e(TAG, "loadFoos failed", t);
                Toast.makeText(FooActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
```
