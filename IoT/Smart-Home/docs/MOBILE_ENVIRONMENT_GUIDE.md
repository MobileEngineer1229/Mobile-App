# Mobile Environment Guide

Reusable build-and-environment conventions for native **Android (Java)** apps, extracted from the Smartify Smart-Home Android client. Pair with `MOBILE_APP_STRUCTURE_RULES.md` (architecture rules) — this file covers the toolchain, Gradle, build configuration, signing, networking, and dev workflow.

---

## 1. Toolchain

| Component        | Version             | Notes                                              |
|------------------|---------------------|----------------------------------------------------|
| JDK              | 8 (`VERSION_1_8`)   | Source + target compatibility set in `build.gradle`|
| Gradle Wrapper   | matches AGP minimum | Use `./gradlew` always — never the system `gradle` |
| Android Gradle Plugin | aligned with `compileSdk 33` | Pinned in root `build.gradle`            |
| Android SDK      | API 33 install      | Includes platform-tools, build-tools, emulator     |
| `compileSdk`     | 33                  |                                                    |
| `targetSdk`      | 33                  | Aligned with `compileSdk`                          |
| `minSdk`         | 24                  | Android 7.0 (Nougat) — covers ~98% of active devices|

**Rule:** Don't bump `compileSdk` and `targetSdk` separately. Bump them together, in their own commit, with a release notes entry.

**Rule:** Pin all dependency versions explicitly. No dynamic versions (`+`, `latest.release`).

---

## 2. Repository Layout

```
<app-root>/
├── app/
│   ├── build.gradle                  # App module build script
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/<org>/<app>/...
│       │   └── res/
│       ├── test/                     # JUnit 4 unit tests
│       └── androidTest/              # Espresso instrumented tests
├── build.gradle                      # Root build script
├── settings.gradle                   # Modules
├── gradle.properties                 # JVM args, AndroidX flags
├── gradle/wrapper/                   # Gradle wrapper JAR + properties
├── gradlew, gradlew.bat              # Wrapper launchers
├── local.properties                  # SDK path + secrets (gitignored)
├── docs/
└── design/                           # Figma exports, design references
```

**Gitignored** (must NOT be committed):
```
/local.properties
/.gradle/
/build/
/app/build/
/.idea/
*.iml
*.jks
*.keystore
google-services.json     # if using Firebase
```

---

## 3. `build.gradle` Conventions

### 3.1 App module (`app/build.gradle`)

```groovy
plugins { id 'com.android.application' }

android {
    namespace 'com.smarthome.iot'
    compileSdk 33

    defaultConfig {
        applicationId "com.smarthome.iot"
        minSdk 24
        targetSdk 33
        versionCode 2
        versionName "1.0.2"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            // Local dev. Verbose logging on.
        }
        release {
            minifyEnabled false   // Flip to true once ProGuard rules are verified
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),
                          'proguard-rules.pro'
        }
    }

    // Custom APK naming: Smartify_1.0.2.apk
    applicationVariants.all { variant ->
        variant.outputs.all { output ->
            output.outputFileName = "Smartify_${variant.versionName}.apk"
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

**Rules:**
- `applicationId` ≠ `namespace` is allowed but discouraged. Keep them identical unless there's a published reason.
- Bump `versionCode` (integer) on **every** Play Store upload. Bump `versionName` (string) on user-visible releases.
- Custom APK naming `<AppName>_<versionName>.apk` keeps QA artifacts identifiable.

### 3.2 Pinned dependency baseline

```groovy
dependencies {
    // AndroidX core
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.1'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'androidx.work:work-runtime:2.8.1'
    implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.1.0'

    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    implementation 'com.squareup.okhttp3:okhttp-sse:4.11.0'

    // JSON
    implementation 'com.google.code.gson:gson:2.10.1'

    // Media
    implementation 'com.github.bumptech.glide:glide:4.15.1'

    // QR
    implementation 'com.google.zxing:core:3.5.2'
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

    // Local DB (kept available even if unused)
    implementation 'androidx.room:room-runtime:2.5.0'
    annotationProcessor 'androidx.room:room-compiler:2.5.0'

    // Tests
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

**Rule:** Don't introduce a new networking/serialization library casually. Retrofit + OkHttp + Gson is the standard. Adding kotlinx-serialization, Moshi, etc. requires a deliberate decision because the entire `models/` directory mirrors Gson conventions.

### 3.3 `gradle.properties`

```properties
android.useAndroidX=true
android.enableJetifier=false
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
```

---

## 4. Build Types and Flavors

### 4.1 Built-in build types

- `debug` — local dev, no minify, verbose logging, allows cleartext HTTP, dev base URL.
- `release` — Play Store output, ProGuard on, minimal logging, prod base URL, signing config required.

### 4.2 When to add a flavor

Add a `productFlavor` only when you need:
- A separate `applicationId` (`.dev`, `.staging`) so QA and prod can coexist on a device, OR
- A staging environment between `debug` and `release`.

Recommended staging template:
```groovy
flavorDimensions += "env"
productFlavors {
    dev      { dimension "env"; applicationIdSuffix ".dev"     ; versionNameSuffix "-dev" }
    staging  { dimension "env"; applicationIdSuffix ".staging" ; versionNameSuffix "-stg" }
    prod     { dimension "env" }
}
```

**Rule:** Don't add flavors for *cosmetic* differences (colors, icons). Use resource overlays.

---

## 5. Base URL & Environment Configuration

### 5.1 The current state

Base URL is hardcoded in `ApiClient.java`:
```java
private static final String BASE_URL = "http://172.86.88.76:3003/api/v1/";
```

This works for a single environment but breaks the moment you ship to staging or prod separately. Migrate to `BuildConfig` fields.

### 5.2 Recommended pattern

In `app/build.gradle`:

```groovy
android {
    defaultConfig {
        buildConfigField "String", "API_BASE_URL",   '"http://172.86.88.76:3003/api/v1/"'
        buildConfigField "String", "HEALTH_BASE_URL",'"http://172.86.88.76:3003/"'
        buildConfigField "String", "WEBSOCKET_URL",  '"ws://172.86.88.76:3003/api/v1/devices/stream"'
    }

    buildTypes {
        debug {
            // Override for local dev (Android emulator → host machine)
            buildConfigField "String", "API_BASE_URL",    '"http://10.0.2.2:3003/api/v1/"'
            buildConfigField "String", "HEALTH_BASE_URL", '"http://10.0.2.2:3003/"'
            buildConfigField "String", "WEBSOCKET_URL",   '"ws://10.0.2.2:3003/api/v1/devices/stream"'
        }
        release {
            // Production
            buildConfigField "String", "API_BASE_URL",    '"https://api.smartify.example.com/api/v1/"'
            buildConfigField "String", "HEALTH_BASE_URL", '"https://api.smartify.example.com/"'
            buildConfigField "String", "WEBSOCKET_URL",   '"wss://api.smartify.example.com/api/v1/devices/stream"'
        }
    }
}
```

In `ApiClient.java`:
```java
private static final String BASE_URL        = BuildConfig.API_BASE_URL;
private static final String HEALTH_BASE_URL = BuildConfig.HEALTH_BASE_URL;
```

In `Globals.java`:
```java
private static final String WEBSOCKET_BASE_URL = BuildConfig.WEBSOCKET_URL;
```

### 5.3 Reaching the backend from each environment

| Run target                        | Base URL to use                              |
|-----------------------------------|----------------------------------------------|
| Android emulator (host = dev box) | `http://10.0.2.2:<port>/api/v1/`             |
| Physical device on same Wi-Fi     | `http://<host-LAN-IP>:<port>/api/v1/`        |
| Genymotion / generic emulator     | `http://10.0.3.2:<port>/api/v1/`             |
| Remote dev server                 | `http://<server-public-IP>:<port>/api/v1/`   |
| Production                        | `https://<api-domain>/api/v1/`               |

Default backend port for this stack: **3003**.

### 5.4 Discovering the host LAN IP

- Windows: `ipconfig` → "IPv4 Address" of the active adapter.
- macOS/Linux: `ifconfig | grep inet` or `ip addr show`.
- Both phone and dev box must be on the same network. Disable VPNs. Some routers isolate clients (AP isolation) — disable it for dev.

---

## 6. Network Security Config

For dev builds talking to plain HTTP, declare an explicit allow-list in `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>      <!-- emulator host -->
        <domain includeSubdomains="true">10.0.3.2</domain>      <!-- Genymotion host -->
        <domain includeSubdomains="true">172.86.88.76</domain>  <!-- LAN dev server -->
    </domain-config>
</network-security-config>
```

In `AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

**Rules:**
- Production base URL must be **HTTPS only**. No exception.
- Cleartext is allowed *only* for the dev hosts listed above. Never for arbitrary domains.

---

## 7. Manifest & Permissions

Required for an IoT app of this shape:

```xml
<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Foreground service for WebSocket / device status -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />   <!-- API 33+ -->

<!-- BLE provisioning (FastBee) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"     android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<!-- Pre-API 31 fallback -->
<uses-permission android:name="android.permission.BLUETOOTH"          android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"    android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />

<!-- Camera for QR -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

**Rules:**
- API 31+ Bluetooth permissions are runtime — request them with `ActivityCompat.requestPermissions` before BLE scan/connect.
- `POST_NOTIFICATIONS` is runtime on API 33+. Request before any foreground service that posts a notification.
- `ACCESS_FINE_LOCATION` is required for BLE scan only on API ≤ 30. On API 31+, use `BLUETOOTH_SCAN` with `usesPermissionFlags="neverForLocation"`.

---

## 8. Signing

### 8.1 Debug signing

Default debug keystore (`~/.android/debug.keystore`) — managed by Android Studio. No setup needed.

### 8.2 Release signing

Store keystore **outside** the repo. Reference it from `local.properties`:

```properties
# local.properties (gitignored)
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
RELEASE_STORE_FILE=C:\\keys\\smartify-release.jks
RELEASE_STORE_PASSWORD=••••••
RELEASE_KEY_ALIAS=smartify
RELEASE_KEY_PASSWORD=••••••
```

Wire into `app/build.gradle`:
```groovy
def localProps = new Properties()
def localPropsFile = rootProject.file('local.properties')
if (localPropsFile.exists()) {
    localPropsFile.withInputStream { localProps.load(it) }
}

android {
    signingConfigs {
        release {
            if (localProps['RELEASE_STORE_FILE']) {
                storeFile     file(localProps['RELEASE_STORE_FILE'])
                storePassword localProps['RELEASE_STORE_PASSWORD']
                keyAlias      localProps['RELEASE_KEY_ALIAS']
                keyPassword   localProps['RELEASE_KEY_PASSWORD']
            }
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            // ...
        }
    }
}
```

**Rules:**
- Keystore file: NEVER committed.
- Keystore passwords: NEVER committed. CI reads them from secret env vars.
- Lose the keystore = lose the ability to update the published app. Back it up to two separate offline locations.

---

## 9. ProGuard / R8

`proguard-rules.pro` keep rules required for this stack:

```proguard
# Gson model classes (POJOs serialized via reflection)
-keep class com.smarthome.iot.models.** { *; }

# Retrofit + OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

# ZXing
-keep class com.google.zxing.** { *; }
```

**Rule:** Flip `minifyEnabled true` only after running through every screen with the release build to verify reflection-heavy classes still serialize correctly. Common breakage: Gson models silently producing all-null fields.

---

## 10. Demo / Mock Mode

- Triggered by login with `demo@smartify.com` / `demo123456` (rules in `MOBILE_APP_STRUCTURE_RULES.md` §5.3).
- Implement as an `AuthManager.isDemoMode()` check that every Activity branches on, with `MockDataProvider` returning canned responses.
- Demo mode does **not** require any backend — useful for App Store screenshots, offline reviewers, and conference demos.

---

## 11. Local Dev Workflow

### 11.1 First-time setup

```bash
# 1. Clone
git clone <repo>
cd <repo>

# 2. Configure local.properties (sdk.dir + signing if release)
# (copy from local.properties.example if provided)

# 3. Build a debug APK
./gradlew assembleDebug
```

### 11.2 Running on emulator

1. Start the backend on port 3003.
2. In Android Studio, launch a Pixel 8 Pro emulator (matches Figma 430×888).
3. Run the `app` configuration. With BuildConfig setup (§5.2), the debug build automatically targets `10.0.2.2:3003`.
4. Verify health: app start logs `HealthCheckService` calling `/health`. Green status in `Globals.apiHealthStatus`.

### 11.3 Running on a physical device

1. Enable USB debugging on the phone.
2. Connect both phone and dev box to the same Wi-Fi (no VPN, no AP isolation).
3. Find the host LAN IP (§5.4).
4. Either: (a) override `API_BASE_URL` in a personal `debug` build flavor, or (b) point the existing `BASE_URL` field at the LAN IP for that session.
5. Make sure the LAN IP is in `network_security_config.xml`.
6. `./gradlew installDebug` (or click Run in Android Studio).

### 11.4 Common build commands

```bash
./gradlew assembleDebug              # Debug APK
./gradlew assembleRelease            # Release APK (needs signing config)
./gradlew installDebug               # Build + install on connected device
./gradlew test                       # Unit tests (JUnit 4)
./gradlew connectedAndroidTest       # Espresso tests (needs device/emulator)
./gradlew clean                      # Clear build outputs
./gradlew lint                       # Android lint
./gradlew dependencies               # Dependency tree
```

APK output: `app/build/outputs/apk/<buildType>/<AppName>_<versionName>.apk`.

---

## 12. CI / Release Checklist

Before tagging a release:

- [ ] `versionCode` bumped (integer) and `versionName` bumped (string).
- [ ] Release notes written in `docs/RELEASE_NOTES.md` (or commit message at minimum).
- [ ] `./gradlew test` passes.
- [ ] `./gradlew connectedAndroidTest` passes against emulator + at least one physical device.
- [ ] Manual smoke: login, demo login, create home, add device, control device, logout.
- [ ] Release build runs end-to-end on a physical device (don't trust debug builds for release validation).
- [ ] Strings reviewed: every new string is defined in `values/strings.xml`.
- [ ] No cleartext URLs leaked into release `BuildConfig` fields.
- [ ] ProGuard run (if `minifyEnabled true`) — verify Gson serialization on every screen.
- [ ] Keystore file & passwords accessible to whoever signs.

---

## 13. `local.properties` Template

Distribute as `local.properties.example`:

```properties
# Android SDK location (REQUIRED)
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk

# Release signing (only needed to build a release APK)
# Path to keystore (KEEP OUTSIDE THE REPO)
RELEASE_STORE_FILE=C:\\keys\\smartify-release.jks
RELEASE_STORE_PASSWORD=
RELEASE_KEY_ALIAS=smartify
RELEASE_KEY_PASSWORD=

# Optional: override base URL for personal dev (if you use BuildConfig wiring)
# DEV_API_BASE_URL=http://192.168.1.42:3003/api/v1/
```

The real `local.properties` must be in `.gitignore`.

---

## 14. Anti-Patterns to Reject

- ❌ Committing `local.properties`, keystores, or signing passwords.
- ❌ Hardcoded production base URL in a debug build (or vice-versa).
- ❌ Cleartext HTTP allowed for arbitrary domains in `network_security_config.xml`.
- ❌ Dynamic dependency versions (`+`, `latest.release`).
- ❌ Mismatched `compileSdk` / `targetSdk` across releases.
- ❌ `versionCode` reused or skipped — Play Store rejects duplicates and audits gaps.
- ❌ `minifyEnabled true` in release without ProGuard rules for every reflection-using library.
- ❌ Bluetooth permissions requested without runtime checks on API 31+.
- ❌ Debug logging interceptor (`Level.BODY`) shipped in release builds.
- ❌ Adding a flavor for cosmetic-only changes — use resources.

---

## Quick Reference

| What                     | Where / How                                                            |
|--------------------------|------------------------------------------------------------------------|
| SDK path                 | `local.properties` → `sdk.dir`                                         |
| Base URL (per build)     | `BuildConfig.API_BASE_URL` set in `app/build.gradle` per build type     |
| Cleartext dev hosts      | `res/xml/network_security_config.xml`                                  |
| Release signing secrets  | `local.properties` → `RELEASE_*` (gitignored)                          |
| App version              | `app/build.gradle` → `versionCode`, `versionName`                      |
| APK output naming        | `Smartify_<versionName>.apk` via `applicationVariants.all { ... }`     |
| Min/target/compile SDK   | `app/build.gradle` → `defaultConfig` + `compileSdk`                    |
| Demo mode credentials    | `demo@smartify.com` / `demo123456` → `MockDataProvider`                |
| Backend default port     | `3003` (REST) / `3003` (WebSocket on same host)                        |
| Emulator → host          | `10.0.2.2`                                                              |
| Genymotion → host        | `10.0.3.2`                                                              |
