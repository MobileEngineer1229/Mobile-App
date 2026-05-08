package com.smarthome.iot.utils;

import android.content.Context;
import com.smarthome.iot.R;
import com.smarthome.iot.models.Device;

/**
 * Utility class for device-related operations
 * Centralizes device classification, icon selection, and category determination
 */
public class DeviceHelper {
    
    /**
     * Device control fragment types
     */
    public enum ControlFragmentType {
        LAMP,
        CAMERA,
        SPEAKER,
        AIR_CONDITIONER,
        BASIC // For devices without specific control fragments
    }
    
    /**
     * Get device category based on device name and type
     * Categories: "lightning", "cameras", "electrical"
     * 
     * @param device Device object with name and type
     * @return Category string: "lightning", "cameras", or "electrical"
     */
    public static String getDeviceCategory(Device device) {
        if (device == null) {
            return "electrical"; // Default to electrical for null devices
        }
        
        return getDeviceCategory(device.getName(), device.getType());
    }
    
    /**
     * Get device category based on device name and type strings
     * Categories: "lightning", "cameras", "electrical"
     * 
     * @param deviceName Device name
     * @param deviceType Device type from database (electronics, lamp, camera)
     * @return Category string: "lightning", "cameras", or "electrical"
     */
    public static String getDeviceCategory(String deviceName, String deviceType) {
        String typeLower = (deviceType != null) ? deviceType.toLowerCase().trim() : "";
        String nameLower = (deviceName != null) ? deviceName.toLowerCase().trim() : "";
        
        // First, check device type directly from database
        if ("lamp".equals(typeLower)) {
            return "lightning";
        } else if ("camera".equals(typeLower)) {
            return "cameras";
        } else if ("electronics".equals(typeLower)) {
            return "electrical";
        }
        
        // Fallback: Check device name for common patterns
        // Check for lightning (lamps, lights, bulbs)
        if (nameLower.contains("lamp") || nameLower.contains("light") || 
            nameLower.contains("bulb") || nameLower.contains("lighting") ||
            typeLower.contains("lamp") || typeLower.contains("light")) {
            return "lightning";
        }
        
        // Check for cameras
        if (nameLower.contains("camera") || nameLower.contains("cctv") || 
            nameLower.contains("webcam") || nameLower.contains("security") ||
            typeLower.contains("camera")) {
            return "cameras";
        }
        
        // Everything else is electrical/electronics
        return "electrical";
    }
    
    /**
     * Check if a device belongs to a specific category
     * 
     * @param device Device object
     * @param category Category to check: "lightning", "cameras", or "electrical"
     * @return true if device belongs to the category
     */
    public static boolean isDeviceInCategory(Device device, String category) {
        if (device == null || category == null) {
            return false;
        }
        
        String deviceCategory = getDeviceCategory(device);
        return deviceCategory.equalsIgnoreCase(category);
    }
    
    /**
     * Get device icon resource ID based on device name and type
     * Uses device name first (more specific), then falls back to type
     * 
     * @param context Android context for accessing resources
     * @param deviceName Device name
     * @param deviceType Device type from database
     * @return Drawable resource ID
     */
    public static int getDeviceIcon(Context context, String deviceName, String deviceType) {
        String typeLower = (deviceType != null) ? deviceType.toLowerCase().trim() : "";
        String nameLower = (deviceName != null) ? deviceName.toLowerCase().trim() : "";
        
        // LIGHTNING category: Check name first
        if (nameLower.contains("lamp") || nameLower.contains("light") || 
            nameLower.contains("bulb") || nameLower.contains("lighting") ||
            typeLower.equals("lamp")) {
            return R.drawable.ic_sun;
        } 
        // CAMERAS category: Check name first
        else if (nameLower.contains("camera") || nameLower.contains("cctv") || 
                 nameLower.contains("webcam") || nameLower.contains("security") ||
                 typeLower.equals("camera")) {
            return R.drawable.ic_camera;
        } 
        // ELECTRICAL category: Check name for specific devices
        else if (nameLower.contains("speaker") || nameLower.contains("stereo")) {
            return R.drawable.ic_speaker;
        } 
        else if ((nameLower.contains("air") && nameLower.contains("conditioner")) || 
                 nameLower.contains("conditioner") || nameLower.contains("ac")) {
            return R.drawable.ic_air_conditioner;
        }
        else if (nameLower.contains("thermostat") || nameLower.contains("temperature")) {
            return R.drawable.ic_device; // TODO: Add thermostat icon
        }
        else if (nameLower.contains("lock") || nameLower.contains("door lock")) {
            return R.drawable.ic_device; // TODO: Add lock icon
        }
        else if (nameLower.contains("tv") || nameLower.contains("television")) {
            return R.drawable.ic_device; // TODO: Add TV icon
        }
        else if (nameLower.contains("washing") || nameLower.contains("refrigerator") || 
                 nameLower.contains("dishwasher") || nameLower.contains("oven") ||
                 nameLower.contains("appliance")) {
            return R.drawable.ic_device; // TODO: Add appliance icons
        }
        else if (nameLower.contains("sensor") || nameLower.contains("detector") || 
                 nameLower.contains("motion") || nameLower.contains("smoke") ||
                 nameLower.contains("window") || nameLower.contains("humidity")) {
            return R.drawable.ic_device; // TODO: Add sensor icons
        }
        else if (typeLower.equals("electronics")) {
            return R.drawable.ic_device;
        }
        
        return R.drawable.ic_device;
    }
    
    /**
     * Get device icon resource ID from Device object
     * 
     * @param context Android context
     * @param device Device object
     * @return Drawable resource ID
     */
    public static int getDeviceIcon(Context context, Device device) {
        if (device == null) {
            return R.drawable.ic_device;
        }
        return getDeviceIcon(context, device.getName(), device.getType());
    }
    
    /**
     * Determine which control fragment type to use for a device
     * 
     * @param deviceName Device name
     * @param deviceType Device type from database
     * @return ControlFragmentType enum
     */
    public static ControlFragmentType getControlFragmentType(String deviceName, String deviceType) {
        String typeLower = (deviceType != null) ? deviceType.toLowerCase().trim() : "";
        String nameLower = (deviceName != null) ? deviceName.toLowerCase().trim() : "";
        
        // Check for AIR CONDITIONER first (before lamp, since "air" might be in other device names)
        // Check both name and type for air conditioner patterns
        // Priority: Check for full "air conditioner" phrase first, then individual words
        if ((nameLower.contains("air") && nameLower.contains("conditioner")) ||
            nameLower.contains("air conditioner") || nameLower.contains("air-conditioner")) {
            return ControlFragmentType.AIR_CONDITIONER;
        } else if (nameLower.contains("conditioner") || 
                   nameLower.contains(" a/c") || nameLower.contains("a/c") ||
                   (nameLower.contains("ac ") && !nameLower.contains("camera")) || 
                   nameLower.startsWith("ac ") || nameLower.endsWith(" ac") ||
                   nameLower.equals("ac") || nameLower.equals("a/c") ||
                   typeLower.contains("air_conditioner") || typeLower.contains("air conditioner") ||
                   typeLower.equals("air_conditioner") || typeLower.equals("ac")) {
            return ControlFragmentType.AIR_CONDITIONER;
        }
        
        // LIGHTNING category: lamp, light (check after AC to avoid false matches)
        if (nameLower.contains("lamp") || nameLower.contains("light") || 
            nameLower.contains("bulb") || nameLower.contains("lighting") ||
            typeLower.equals("lamp") || typeLower.equals("smart_lamp")) {
            return ControlFragmentType.LAMP;
        } 
        // ELECTRICAL category: Check name for specific devices (check before camera to avoid conflicts)
        else if (nameLower.contains("speaker") || nameLower.contains("stereo") ||
                 typeLower.equals("speaker") || typeLower.equals("stereo")) {
            return ControlFragmentType.SPEAKER;
        }
        // CAMERAS category: camera, cctv
        else if (nameLower.contains("camera") || nameLower.contains("cctv") || 
                 nameLower.contains("webcam") || nameLower.contains("security") ||
                 typeLower.equals("camera")) {
            return ControlFragmentType.CAMERA;
        }
        // Other electrical devices without specific fragments
        // (thermostat, lock, tv, appliance, sensors, etc.)
        else {
            return ControlFragmentType.BASIC;
        }
    }
    
    /**
     * Get control fragment type from Device object
     * 
     * @param device Device object
     * @return ControlFragmentType enum
     */
    public static ControlFragmentType getControlFragmentType(Device device) {
        if (device == null) {
            return ControlFragmentType.BASIC;
        }
        return getControlFragmentType(device.getName(), device.getType());
    }
    
    /**
     * Get device type for backend API (maps to database type enum)
     * Backend expects: "lamp", "camera", or "electronics"
     * 
     * @param deviceName Device name
     * @return Backend device type: "lamp", "camera", or "electronics"
     */
    public static String getDeviceTypeFromName(String deviceName) {
        if (deviceName == null) {
            return "electronics"; // Default type
        }
        
        String normalizedName = deviceName.toLowerCase();
        
        // Check for lamp/light devices
        if (normalizedName.contains("lamp") || normalizedName.contains("light") || 
            normalizedName.contains("bulb") || normalizedName.contains("lighting")) {
            return "lamp";
        }
        
        // Check for camera devices
        if (normalizedName.contains("camera") || normalizedName.contains("cctv") || 
            normalizedName.contains("webcam") || normalizedName.contains("security")) {
            return "camera";
        }
        
        // Everything else is electronics
        return "electronics";
    }
    
    /**
     * Get device category from device name only (for metadata/display)
     * 
     * @param deviceName Device name
     * @return Category string: "lightning", "cameras", "electrical", or "other"
     */
    public static String getDeviceCategoryFromName(String deviceName) {
        if (deviceName == null) {
            return "other";
        }
        
        String normalizedName = deviceName.toLowerCase();
        
        if (normalizedName.contains("lamp") || normalizedName.contains("light")) {
            return "lightning";
        } else if (normalizedName.contains("camera") || normalizedName.contains("cctv")) {
            return "cameras";
        } else if (normalizedName.contains("speaker")) {
            return "electrical";
        } else if (normalizedName.contains("air") || normalizedName.contains("conditioner") || 
                   normalizedName.contains("ac")) {
            return "electrical";
        } else {
            return "electrical"; // Default to electrical for all other devices
        }
    }
}
