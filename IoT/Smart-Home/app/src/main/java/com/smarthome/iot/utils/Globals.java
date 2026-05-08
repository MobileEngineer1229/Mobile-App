package com.smarthome.iot.utils;

import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.models.Room;
import com.smarthome.iot.models.SmartScene;
import com.smarthome.iot.models.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Global application state and configuration
 * Stores shared state across the application
 * 
 * Structure:
 * - API Health Status
 * - User Data (loaded on login):
 *   - User Profile Info
 *   - User Homes
 *   - User Rooms
 *   - User Devices (cached by homeId)
 *   - User Location (primary home location)
 */
public class Globals {
    // ========== API Health Status ==========
    private static boolean apiHealthStatus = false; // Default to false until checked
    private static long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_CACHE_DURATION = 60000; // 1 minute cache
    private static boolean healthCheckPerformed = false; // Track if health check has been performed
    private static boolean mqttBrokerAvailable = false; // MQTT broker status from health check
    
    // ========== User Data (loaded on login) ==========
    
    // User Profile Info
    private static User currentUser = null;
    private static long lastUserLoadTime = 0;
    
    // User Homes
    private static List<Home> userHomes = new ArrayList<>();
    private static Home primaryHome = null;
    private static long lastHomesLoadTime = 0;
    
    // User Rooms (cached by homeId)
    private static List<Room> userRooms = new ArrayList<>();
    private static Integer cachedRoomsHomeId = null;
    private static long lastRoomsLoadTime = 0;
    
    // User Devices (cached by homeId)
    private static List<Device> cachedDevices = new ArrayList<>();
    private static Integer cachedHomeId = null;
    private static long lastDeviceLoadTime = 0;
    
    // User Location (from primary home)
    private static String userLocationAddress = null;
    private static Double userLocationLatitude = null;
    private static Double userLocationLongitude = null;
    private static String userLocationCountry = null;
    
    // User Scenes (cached by homeId and type)
    private static List<SmartScene> cachedScenes = new ArrayList<>();
    private static Integer cachedScenesHomeId = null;
    private static String cachedScenesType = null; // "automation" or "tap_to_run"
    private static long lastScenesLoadTime = 0;

    // ========== Selected Room (from Main UI) ==========
    private static Integer selectedRoomId = null; // null or 0 means "All Rooms"

    // ========== Test Mode ==========
    private static boolean testMode = true; // Set to true during testing to skip Bluetooth provisioning

    // Cache durations
    private static final long DEVICE_CACHE_DURATION = 300000; // 5 minutes
    private static final long ROOMS_CACHE_DURATION = 300000; // 5 minutes
    private static final long HOMES_CACHE_DURATION = 600000; // 10 minutes
    private static final long SCENES_CACHE_DURATION = 300000; // 5 minutes

    // ========== WebSocket Configuration ==========
    // WebSocket URL for real-time device updates
    // Server: 172.86.88.76:3003
    private static final String WEBSOCKET_BASE_URL = "ws://172.86.88.76:3003/api/v1/devices/stream";

    // Device status polling interval (3 seconds)
    public static final long DEVICE_POLLING_INTERVAL = 3000; // 3 seconds

    // WebSocket connection state
    private static boolean isWebSocketConnected = false;
    private static long lastWebSocketConnectionTime = 0;

    // ========== Getter/Setter Methods ==========

    public static Integer getSelectedRoomId() {
        return selectedRoomId;
    }

    public static void setSelectedRoomId(Integer roomId) {
        selectedRoomId = roomId;
    }

    public static boolean isTestMode() {
        return testMode;
    }

    public static void setTestMode(boolean enabled) {
        testMode = enabled;
    }

    /**
     * Get the current API health status
     * @return true if API is healthy, false otherwise
     * Note: Returns true if health check hasn't been performed yet (optimistic approach)
     */
    public static boolean isApiHealthy() {
        // If health check hasn't been performed yet, allow calls (optimistic)
        if (!healthCheckPerformed) {
            return true;
        }
        return apiHealthStatus;
    }

    /**
     * Set the API health status
     * @param healthy true if API is healthy, false otherwise
     */
    public static void setApiHealthStatus(boolean healthy) {
        apiHealthStatus = healthy;
        healthCheckPerformed = true; // Mark that health check has been performed
        lastHealthCheckTime = System.currentTimeMillis();
        android.util.Log.d("Globals", "API health status set to: " + healthy + " (health check performed: true)");
    }

    public static boolean isMqttBrokerAvailable() {
        return mqttBrokerAvailable;
    }

    public static void setMqttBrokerAvailable(boolean available) {
        mqttBrokerAvailable = available;
        android.util.Log.d("Globals", "MQTT broker available: " + available);
    }

    /**
     * Check if health status is stale and needs refresh
     * @return true if health check should be performed
     */
    public static boolean shouldCheckHealth() {
        long timeSinceLastCheck = System.currentTimeMillis() - lastHealthCheckTime;
        return timeSinceLastCheck > HEALTH_CHECK_CACHE_DURATION || lastHealthCheckTime == 0;
    }

    /**
     * Get the timestamp of the last health check
     * @return timestamp in milliseconds
     */
    public static long getLastHealthCheckTime() {
        return lastHealthCheckTime;
    }

    /**
     * Reset health status (useful for testing or when server changes)
     */
    public static void resetHealthStatus() {
        apiHealthStatus = false;
        healthCheckPerformed = false;
        lastHealthCheckTime = 0;
    }
    
    /**
     * Check if health check has been performed
     * @return true if health check has been performed at least once
     */
    public static boolean isHealthCheckPerformed() {
        return healthCheckPerformed;
    }
    
    // ========== Device Cache Methods ==========
    
    /**
     * Get cached devices for a home
     * @param homeId The home ID to get devices for
     * @return List of cached devices, or null if cache is invalid or for different home
     */
    public static List<Device> getCachedDevices(Integer homeId) {
        // Check if cache is valid
        if (cachedHomeId == null || !cachedHomeId.equals(homeId)) {
            android.util.Log.d("Globals", "Device cache invalid: cachedHomeId=" + cachedHomeId + ", requested homeId=" + homeId);
            return null;
        }
        
        // Check if cache is stale
        long timeSinceLastLoad = System.currentTimeMillis() - lastDeviceLoadTime;
        if (timeSinceLastLoad > DEVICE_CACHE_DURATION) {
            android.util.Log.d("Globals", "Device cache stale: " + (timeSinceLastLoad / 1000) + " seconds old");
            return null;
        }
        
        android.util.Log.d("Globals", "Returning cached devices: " + cachedDevices.size() + " devices for homeId=" + homeId);
        return new ArrayList<>(cachedDevices); // Return a copy to prevent external modification
    }
    
    /**
     * Get all cached devices regardless of homeId or staleness.
     * Used by MqttMessageHandler to look up devices by MQTT metadata.
     */
    public static List<Device> getCachedDevices() {
        return new ArrayList<>(cachedDevices);
    }

    /**
     * Cache devices for a home
     * @param devices List of devices to cache
     * @param homeId The home ID these devices belong to
     */
    public static void setCachedDevices(List<Device> devices, Integer homeId) {
        if (devices == null) {
            cachedDevices = new ArrayList<>();
        } else {
            cachedDevices = new ArrayList<>(devices); // Store a copy
        }
        cachedHomeId = homeId;
        lastDeviceLoadTime = System.currentTimeMillis();
        android.util.Log.d("Globals", "Cached " + cachedDevices.size() + " devices for homeId=" + homeId);
    }
    
    /**
     * Check if device cache is valid for a given home
     * @param homeId The home ID to check
     * @return true if cache is valid and not stale
     */
    public static boolean isDeviceCacheValid(Integer homeId) {
        if (cachedHomeId == null || !cachedHomeId.equals(homeId)) {
            return false;
        }
        
        long timeSinceLastLoad = System.currentTimeMillis() - lastDeviceLoadTime;
        return timeSinceLastLoad <= DEVICE_CACHE_DURATION;
    }
    
    /**
     * Clear device cache
     */
    public static void clearDeviceCache() {
        cachedDevices.clear();
        cachedHomeId = null;
        lastDeviceLoadTime = 0;
        android.util.Log.d("Globals", "Device cache cleared");
    }
    
    /**
     * Add a device to the cache (e.g., after creating a new device)
     * @param device The device to add
     */
    public static void addDeviceToCache(Device device) {
        if (device != null && cachedDevices != null) {
            // Check if device already exists (by ID)
            boolean exists = false;
            for (int i = 0; i < cachedDevices.size(); i++) {
                if (cachedDevices.get(i).getId().equals(device.getId())) {
                    // Update existing device
                    cachedDevices.set(i, device);
                    exists = true;
                    android.util.Log.d("Globals", "Updated device in cache: " + device.getName());
                    break;
                }
            }
            if (!exists) {
                cachedDevices.add(device);
                android.util.Log.d("Globals", "Added device to cache: " + device.getName());
            }
        }
    }
    
    /**
     * Remove a device from the cache
     * @param deviceId The device ID to remove
     */
    public static void removeDeviceFromCache(Integer deviceId) {
        if (deviceId != null && cachedDevices != null) {
            for (int i = 0; i < cachedDevices.size(); i++) {
                if (cachedDevices.get(i).getId().equals(deviceId)) {
                    cachedDevices.remove(i);
                    android.util.Log.d("Globals", "Removed device from cache: deviceId=" + deviceId);
                    break;
                }
            }
        }
    }
    
    // ========== User Profile Info Methods ==========
    
    /**
     * Get current user profile
     * @return User object or null if not set
     */
    public static User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Set current user profile (called on login)
     * @param user The user object
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
        lastUserLoadTime = System.currentTimeMillis();
        android.util.Log.d("Globals", "Current user set: " + (user != null ? user.getEmail() : "null"));
    }
    
    /**
     * Update current user profile
     * @param user Updated user object
     */
    public static void updateCurrentUser(User user) {
        if (user != null && currentUser != null && user.getId() == currentUser.getId()) {
            currentUser = user;
            lastUserLoadTime = System.currentTimeMillis();
            android.util.Log.d("Globals", "Current user updated: " + user.getEmail());
        }
    }
    
    // ========== User Homes Methods ==========
    
    /**
     * Get all user homes
     * @return List of homes, or empty list if not set
     */
    public static List<Home> getUserHomes() {
        return new ArrayList<>(userHomes); // Return a copy
    }
    
    /**
     * Set user homes (called on login or when homes are loaded)
     * @param homes List of homes
     */
    public static void setUserHomes(List<Home> homes) {
        if (homes == null) {
            userHomes = new ArrayList<>();
        } else {
            userHomes = new ArrayList<>(homes); // Store a copy
        }
        lastHomesLoadTime = System.currentTimeMillis();
        
        // Update primary home
        primaryHome = null;
        for (Home home : userHomes) {
            if (home.isPrimary()) {
                primaryHome = home;
                break;
            }
        }
        // If no primary found, use first home
        if (primaryHome == null && !userHomes.isEmpty()) {
            primaryHome = userHomes.get(0);
        }
        
        // Update user location from primary home
        updateUserLocationFromHome(primaryHome);
        
        android.util.Log.d("Globals", "User homes set: " + userHomes.size() + " homes, primary: " + (primaryHome != null ? primaryHome.getName() : "null"));
    }
    
    /**
     * Get primary home
     * @return Primary home or null if not set
     */
    public static Home getPrimaryHome() {
        return primaryHome;
    }
    
    /**
     * Set primary home
     * @param home The home to set as primary
     */
    public static void setPrimaryHome(Home home) {
        primaryHome = home;
        if (home != null) {
            // Add or update in userHomes list
            addOrUpdateHome(home);
            // Update user location from primary home
            updateUserLocationFromHome(home);
            android.util.Log.d("Globals", "Primary home set: " + home.getName());
        }
    }
    
    /**
     * Get home by ID
     * @param homeId The home ID
     * @return Home object or null if not found
     */
    public static Home getHomeById(Integer homeId) {
        if (homeId == null) return null;
        for (Home home : userHomes) {
            if (home.getId() == homeId) {
                return home;
            }
        }
        return null;
    }
    
    /**
     * Add or update a home in the list
     * @param home The home to add or update
     */
    public static void addOrUpdateHome(Home home) {
        if (home == null) return;
        
        boolean found = false;
        for (int i = 0; i < userHomes.size(); i++) {
            if (userHomes.get(i).getId() == home.getId()) {
                userHomes.set(i, home);
                found = true;
                android.util.Log.d("Globals", "Updated home in cache: " + home.getName());
                break;
            }
        }
        
        if (!found) {
            userHomes.add(home);
            android.util.Log.d("Globals", "Added home to cache: " + home.getName());
        }
        
        // Update primary home if needed
        if (home.isPrimary()) {
            primaryHome = home;
            updateUserLocationFromHome(home);
        }
    }
    
    /**
     * Remove a home from the list
     * @param homeId The home ID to remove
     */
    public static void removeHome(Integer homeId) {
        if (homeId == null) return;
        for (int i = 0; i < userHomes.size(); i++) {
            if (userHomes.get(i).getId() == homeId) {
                userHomes.remove(i);
                android.util.Log.d("Globals", "Removed home from cache: homeId=" + homeId);
                
                // Update primary home if removed home was primary
                if (primaryHome != null && primaryHome.getId() == homeId) {
                    primaryHome = userHomes.isEmpty() ? null : userHomes.get(0);
                    updateUserLocationFromHome(primaryHome);
                }
                break;
            }
        }
    }
    
    /**
     * Check if homes cache is valid
     * @return true if cache is valid and not stale
     */
    public static boolean isHomesCacheValid() {
        if (userHomes.isEmpty()) return false;
        long timeSinceLastLoad = System.currentTimeMillis() - lastHomesLoadTime;
        return timeSinceLastLoad <= HOMES_CACHE_DURATION;
    }
    
    // ========== User Rooms Methods ==========
    
    /**
     * Get user rooms for a specific home
     * @param homeId The home ID
     * @return List of rooms, or null if cache is invalid
     */
    public static List<Room> getUserRooms(Integer homeId) {
        // Check if cache is valid
        if (cachedRoomsHomeId == null || !cachedRoomsHomeId.equals(homeId)) {
            return null;
        }
        
        // Check if cache is stale
        long timeSinceLastLoad = System.currentTimeMillis() - lastRoomsLoadTime;
        if (timeSinceLastLoad > ROOMS_CACHE_DURATION) {
            return null;
        }
        
        return new ArrayList<>(userRooms); // Return a copy
    }
    
    /**
     * Set user rooms for a home (called when rooms are loaded)
     * @param rooms List of rooms
     * @param homeId The home ID these rooms belong to
     */
    public static void setUserRooms(List<Room> rooms, Integer homeId) {
        if (rooms == null) {
            userRooms = new ArrayList<>();
        } else {
            userRooms = new ArrayList<>(rooms); // Store a copy
        }
        cachedRoomsHomeId = homeId;
        lastRoomsLoadTime = System.currentTimeMillis();
        android.util.Log.d("Globals", "User rooms set: " + userRooms.size() + " rooms for homeId=" + homeId);
    }
    
    /**
     * Get room by ID
     * @param roomId The room ID
     * @return Room object or null if not found
     */
    public static Room getRoomById(Integer roomId) {
        if (roomId == null) return null;
        for (Room room : userRooms) {
            if (room.getId() == roomId) {
                return room;
            }
        }
        return null;
    }
    
    /**
     * Add or update a room in the list
     * @param room The room to add or update
     */
    public static void addOrUpdateRoom(Room room) {
        if (room == null) return;
        
        boolean found = false;
        for (int i = 0; i < userRooms.size(); i++) {
            if (userRooms.get(i).getId() == room.getId()) {
                userRooms.set(i, room);
                found = true;
                android.util.Log.d("Globals", "Updated room in cache: " + room.getName());
                break;
            }
        }
        
        if (!found) {
            userRooms.add(room);
            android.util.Log.d("Globals", "Added room to cache: " + room.getName());
        }
    }
    
    /**
     * Remove a room from the list
     * @param roomId The room ID to remove
     */
    public static void removeRoom(Integer roomId) {
        if (roomId == null) return;
        for (int i = 0; i < userRooms.size(); i++) {
            if (userRooms.get(i).getId() == roomId) {
                userRooms.remove(i);
                android.util.Log.d("Globals", "Removed room from cache: roomId=" + roomId);
                break;
            }
        }
    }
    
    /**
     * Check if rooms cache is valid for a given home
     * @param homeId The home ID to check
     * @return true if cache is valid and not stale
     */
    public static boolean isRoomsCacheValid(Integer homeId) {
        if (cachedRoomsHomeId == null || !cachedRoomsHomeId.equals(homeId)) {
            return false;
        }
        long timeSinceLastLoad = System.currentTimeMillis() - lastRoomsLoadTime;
        return timeSinceLastLoad <= ROOMS_CACHE_DURATION;
    }
    
    // ========== Scene Cache Methods ==========

    /**
     * Get cached scenes for a home and type
     * @param homeId The home ID
     * @param type Scene type ("automation" or "tap_to_run")
     * @return List of cached scenes, or null if cache is invalid
     */
    public static List<SmartScene> getCachedScenes(Integer homeId, String type) {
        if (cachedScenesHomeId == null || !cachedScenesHomeId.equals(homeId)) {
            return null;
        }
        if (cachedScenesType == null || !cachedScenesType.equals(type)) {
            return null;
        }
        long timeSinceLastLoad = System.currentTimeMillis() - lastScenesLoadTime;
        if (timeSinceLastLoad > SCENES_CACHE_DURATION) {
            return null;
        }
        android.util.Log.d("Globals", "Returning cached scenes: " + cachedScenes.size() + " scenes for homeId=" + homeId + ", type=" + type);
        return new ArrayList<>(cachedScenes);
    }

    /**
     * Cache scenes for a home and type
     * @param scenes List of scenes to cache
     * @param homeId The home ID
     * @param type Scene type ("automation" or "tap_to_run")
     */
    public static void setCachedScenes(List<SmartScene> scenes, Integer homeId, String type) {
        if (scenes == null) {
            cachedScenes = new ArrayList<>();
        } else {
            cachedScenes = new ArrayList<>(scenes);
        }
        cachedScenesHomeId = homeId;
        cachedScenesType = type;
        lastScenesLoadTime = System.currentTimeMillis();
        android.util.Log.d("Globals", "Cached " + cachedScenes.size() + " scenes for homeId=" + homeId + ", type=" + type);
    }

    /**
     * Check if scenes cache is valid for a given home and type
     * @param homeId The home ID
     * @param type Scene type
     * @return true if cache is valid and not stale
     */
    public static boolean isScenesCacheValid(Integer homeId, String type) {
        if (cachedScenesHomeId == null || !cachedScenesHomeId.equals(homeId)) {
            return false;
        }
        if (cachedScenesType == null || !cachedScenesType.equals(type)) {
            return false;
        }
        long timeSinceLastLoad = System.currentTimeMillis() - lastScenesLoadTime;
        return timeSinceLastLoad <= SCENES_CACHE_DURATION;
    }

    /**
     * Clear scenes cache
     */
    public static void clearScenesCache() {
        cachedScenes.clear();
        cachedScenesHomeId = null;
        cachedScenesType = null;
        lastScenesLoadTime = 0;
        android.util.Log.d("Globals", "Scenes cache cleared");
    }

    // ========== User Location Methods ==========
    
    /**
     * Update user location from home
     * @param home The home to extract location from
     */
    private static void updateUserLocationFromHome(Home home) {
        if (home != null) {
            userLocationAddress = home.getAddress();
            userLocationLatitude = home.getLatitude();
            userLocationLongitude = home.getLongitude();
            userLocationCountry = home.getCountry();
            android.util.Log.d("Globals", "User location updated from home: " + home.getName());
        } else {
            userLocationAddress = null;
            userLocationLatitude = null;
            userLocationLongitude = null;
            userLocationCountry = null;
        }
    }
    
    /**
     * Get user location address
     * @return Address string or null
     */
    public static String getUserLocationAddress() {
        return userLocationAddress;
    }
    
    /**
     * Get user location latitude
     * @return Latitude or null
     */
    public static Double getUserLocationLatitude() {
        return userLocationLatitude;
    }
    
    /**
     * Get user location longitude
     * @return Longitude or null
     */
    public static Double getUserLocationLongitude() {
        return userLocationLongitude;
    }
    
    /**
     * Get user location country
     * @return Country string or null
     */
    public static String getUserLocationCountry() {
        return userLocationCountry;
    }
    
    // ========== WebSocket Methods ==========

    /**
     * Get WebSocket URL
     * @param token JWT token for authentication
     * @return WebSocket URL with token query parameter
     */
    public static String getWebSocketUrl(String token) {
        if (token != null && !token.isEmpty()) {
            return WEBSOCKET_BASE_URL + "?token=" + token;
        }
        return WEBSOCKET_BASE_URL;
    }

    /**
     * Get WebSocket base URL without token
     * @return Base WebSocket URL
     */
    public static String getWebSocketBaseUrl() {
        return WEBSOCKET_BASE_URL;
    }

    /**
     * Get device polling interval
     * @return Polling interval in milliseconds (3 seconds)
     */
    public static long getDevicePollingInterval() {
        return DEVICE_POLLING_INTERVAL;
    }

    /**
     * Check if WebSocket is connected
     * @return true if connected, false otherwise
     */
    public static boolean isWebSocketConnected() {
        return isWebSocketConnected;
    }

    /**
     * Set WebSocket connection status
     * @param connected true if connected, false otherwise
     */
    public static void setWebSocketConnected(boolean connected) {
        isWebSocketConnected = connected;
        if (connected) {
            lastWebSocketConnectionTime = System.currentTimeMillis();
        }
        android.util.Log.d("Globals", "WebSocket connection status: " + connected);
    }

    /**
     * Get last WebSocket connection time
     * @return timestamp in milliseconds
     */
    public static long getLastWebSocketConnectionTime() {
        return lastWebSocketConnectionTime;
    }

    // ========== Clear All User Data (on logout) ==========

    /**
     * Clear all user data (called on logout)
     */
    public static void clearAllUserData() {
        android.util.Log.d("Globals", "Clearing all user data");

        // Clear user profile
        currentUser = null;
        lastUserLoadTime = 0;

        // Clear homes
        userHomes.clear();
        primaryHome = null;
        lastHomesLoadTime = 0;

        // Clear rooms
        userRooms.clear();
        cachedRoomsHomeId = null;
        lastRoomsLoadTime = 0;

        // Clear devices
        clearDeviceCache();

        // Clear scenes
        clearScenesCache();

        // Clear location
        userLocationAddress = null;
        userLocationLatitude = null;
        userLocationLongitude = null;
        userLocationCountry = null;

        // Clear WebSocket connection state
        isWebSocketConnected = false;
        lastWebSocketConnectionTime = 0;

        android.util.Log.d("Globals", "All user data cleared");
    }
}
