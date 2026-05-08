# Smart Home Mobile App

Native Android application for managing IoT smart home devices.

## 📱 Overview

This Android app provides a complete interface for managing smart home devices, rooms, homes, notifications, and interacting with a smart assistant chatbot.

## 🚀 Quick Start

### Prerequisites
- Android Studio (latest version)
- JDK 8+
- Android SDK 24+ (Android 7.0)
- Gradle 7.0+

### Setup

1. **Open Project**:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to `Smart-Home/` directory

2. **Sync Gradle**:
   - Android Studio will automatically sync Gradle files
   - Wait for dependencies to download

3. **Configure API Base URL**:
   - Open `app/src/main/java/com/smarthome/iot/network/ApiClient.java`
   - Update `BASE_URL`:
     - **Emulator**: `http://10.0.2.2:3003`
     - **Physical Device**: `http://<your-local-ip>:3003`

4. **Run App**:
   - Connect Android device or start emulator
   - Click "Run" button or press `Shift+F10`

## 🎯 Features

### Authentication
- **Sign Up**: Create new user account
- **Sign In**: Login with email and password
- **JWT Token Management**: Automatic token storage and refresh

### Device Management
- View all devices in a grid/list
- Filter devices by room
- Add new devices
- Assign devices to rooms
- Control device status (on/off)

### Home & Room Management
- Multiple home support
- Location selector modal
- Room filtering
- Create and manage rooms

### Notifications
- Two categories: General and Smart Home
- Unread/read status
- Mark as read functionality
- Notification statistics

### Smart Assistant (Chatbot)
- Interactive chat interface
- Chat history
- Contextual responses
- Clear conversation

### Settings
- **Profile Settings**: Edit user profile information
- **Notification Preferences**: Configure notification types
- **Security Settings**: Biometric, 2FA, password management
- **Account Management**: Deactivate/delete account

## 🧪 Demo Mode

The app includes a demo mode for testing without backend API:

### Demo User Credentials
- **Email**: `demo@smartify.com`
- **Password**: `demo123456`

### Mock Data
When logged in with the demo user, the app uses `MockDataProvider` for:
- **Homes**: 2 homes (My Home, Vacation Home)
- **Rooms**: 5 rooms for primary home, 1 for vacation home
- **Notifications**: 6 notifications (General and Smart Home categories)
- **Chat History**: Sample conversation history

See [MOCK_DATA_SETUP.md](MOCK_DATA_SETUP.md) for complete details on mock data structure and usage.

## 📁 Project Structure

```
Smart-Home/
├── app/
│   ├── src/main/
│   │   ├── java/com/smarthome/iot/
│   │   │   ├── models/           # Data models
│   │   │   ├── network/         # API integration (Retrofit)
│   │   │   ├── ui/              # Activities and UI components
│   │   │   │   ├── adapters/    # RecyclerView adapters
│   │   │   │   └── dialogs/     # Custom dialogs
│   │   │   ├── utils/           # Utilities (AuthManager, MockDataProvider)
│   │   │   └── MainActivity.java
│   │   └── res/
│   │       ├── layout/          # XML layouts
│   │       ├── values/           # Strings, colors, styles
│   │       └── drawable/        # Icons and drawables
│   └── build.gradle
└── build.gradle
```

## 🔧 Configuration

### API Configuration
- **File**: `app/src/main/java/com/smarthome/iot/network/ApiClient.java`
- **Base URL**: Set according to your environment
- **Timeout**: 30 seconds (default)

### Build Configuration
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 33 (Android 13)
- **Compile SDK**: 33
- **Java Version**: 8

## 📦 Dependencies

### Core
- **Retrofit 2.9.0**: HTTP client for API calls
- **OkHttp 4.11.0**: HTTP client library
- **Gson**: JSON serialization/deserialization

### UI
- **Material Design Components**: Modern UI components
- **CardView**: Card-based layouts
- **RecyclerView**: Efficient list rendering

### Storage
- **SharedPreferences**: Token and user data storage
- **Room Database** (planned): Local data caching

## 🔐 Authentication Flow

1. User signs up or logs in
2. Backend returns JWT token
3. Token stored in `SharedPreferences` via `AuthManager`
4. Token included in all API requests via `Authorization` header
5. Token validated on backend for protected endpoints

## 🌐 API Integration

### Base URL
- **Development (Emulator)**: `http://10.0.2.2:3003`
- **Development (Physical Device)**: `http://<local-ip>:3003`
- **Production**: Configure via BuildConfig

### API Service
All API calls are defined in `ApiService.java`:
- Authentication endpoints
- Device management
- Room management
- Home management
- Notifications
- Chatbot

### Error Handling
- Network errors: Show toast messages
- Authentication errors: Redirect to login
- Validation errors: Display inline errors

## 🎨 UI Design

### Theme
- **Dark Mode**: Primary theme
- **Colors**:
  - Primary: `#5B42F3`
  - Background: `#1F222A`
  - Cards: `#262A35`
  - Text: White with alpha variations

### Components
- Material Design buttons
- Card-based layouts
- RecyclerView for lists
- Custom dialogs and modals

### Icons (Iconify Integration)
- **Icon Library**: [Iconify](https://iconify.design) - Matches Figma design
- **Format**: Android VectorDrawable (converted from Iconify SVG)
- **Helper Class**: `IconifyHelper` utility for icon management
- **Documentation**: See [ICONIFY_INTEGRATION.md](ICONIFY_INTEGRATION.md) for detailed guide

**Quick Usage**:
```java
// Load icon with tint (using color resource)
Drawable icon = IconifyHelper.getIconWithTint(context, R.drawable.ic_home_mdi, R.color.white);
imageView.setImageDrawable(icon);

// Or using color int directly
Drawable icon2 = IconifyHelper.getIconWithTintColor(context, R.drawable.ic_home_mdi, 0xFFFFFFFF);
imageView.setImageDrawable(icon2);
```

**Converting Icons from Figma**:
1. Find icon name in Figma (e.g., `mdi:home`)
2. Download SVG from [iconify.design](https://iconify.design)
3. Import to Android Studio: **New → Vector Asset → Local file (SVG)**
4. Use in layouts with `app:tint` for color changes

## 🧩 Key Components

### AuthManager
- Manages user authentication state
- Stores/retrieves JWT token
- Handles user session

### MockDataProvider
- Provides mock data for demo user
- Used when backend is unavailable
- Includes homes, rooms, notifications, chat history

### ApiClient
- Retrofit instance configuration
- Base URL management
- Request interceptors for authentication

## 🐛 Troubleshooting

### Build Errors
- **Gradle Sync Failed**: Check internet connection, invalidate caches
- **SDK Not Found**: Install required SDK versions in Android Studio
- **Dependency Conflicts**: Check `build.gradle` for version conflicts

### Runtime Errors
- **API Connection Failed**: Check backend is running and URL is correct
- **Token Expired**: Re-login to get new token
- **Null Pointer Exceptions**: Check for null checks in code

### Emulator Issues
- **Cannot Connect to Backend**: Use `10.0.2.2` instead of `localhost`
- **Slow Performance**: Increase emulator RAM/CPU allocation

## 📝 Development Guidelines

### Code Style
- Follow Java naming conventions
- Use meaningful variable names
- Extract strings to `strings.xml`
- Add comments for complex logic

### Best Practices
- Handle network errors gracefully
- Show loading indicators for async operations
- Validate user input
- Test on multiple screen sizes
- Follow Material Design guidelines

## 🚀 Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

APK location: `app/build/outputs/apk/`

## 📄 License

ISC

## 📞 Support

For API documentation, see backend README or Swagger UI at `http://localhost:3003/api-docs`.
