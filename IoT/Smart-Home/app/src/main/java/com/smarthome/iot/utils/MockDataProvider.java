package com.smarthome.iot.utils;

import com.smarthome.iot.models.ChatbotMessage;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.models.Notification;
import com.smarthome.iot.models.Room;
import com.smarthome.iot.models.SceneCondition;
import com.smarthome.iot.models.SceneTask;
import com.smarthome.iot.models.SmartScene;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Mock data provider for demo user testing
 * Provides mock data when backend API is not accessible
 */
public class MockDataProvider {
    
    private static final String DEMO_EMAIL = "offline@demo.local"; // Changed from demo@smartify.com to allow real backend usage
    
    // Store dynamically created scenes (for runtime scene creation)
    private static final List<SmartScene> dynamicScenes = new ArrayList<>();
    private static int nextSceneId = 100; // Start from 100 to avoid conflicts with static scenes
    
    /**
     * Check if current user is demo user
     * Disabled — all features now use real backend API
     */
    public static boolean isDemoUser(AuthManager authManager) {
        return false;
    }
    
    /**
     * Get mock homes for demo user
     */
    public static List<Home> getMockHomes() {
        List<Home> homes = new ArrayList<>();
        
        // Primary Home
        Home primaryHome = new Home();
        primaryHome.setId(1);
        primaryHome.setUserId(1);
        primaryHome.setName("My Home");
        primaryHome.setAddress("701 7th Ave, New York, 10036, USA");
        primaryHome.setLatitude(40.7128);
        primaryHome.setLongitude(-74.0060);
        primaryHome.setCountry("United States");
        primaryHome.setPrimary(true);
        homes.add(primaryHome);
        
        // Secondary Home
        Home vacationHome = new Home();
        vacationHome.setId(2);
        vacationHome.setUserId(1);
        vacationHome.setName("My Apartment");
        vacationHome.setAddress("1600 Amphitheatre Parkway, Mountain View, CA 94043, USA");
        vacationHome.setLatitude(37.4220);
        vacationHome.setLongitude(-122.0841);
        vacationHome.setCountry("United States");
        vacationHome.setPrimary(false);
        homes.add(vacationHome);

        // Third Home
        Home officeHome = new Home();
        officeHome.setId(3);
        officeHome.setUserId(1);
        officeHome.setName("My Office");
        officeHome.setAddress("1 Infinite Loop, Cupertino, CA 95014, USA");
        officeHome.setLatitude(37.3318);
        officeHome.setLongitude(-122.0312);
        officeHome.setCountry("United States");
        officeHome.setPrimary(false);
        homes.add(officeHome);

        // Fourth Home
        Home parentsHome = new Home();
        parentsHome.setId(4);
        parentsHome.setUserId(1);
        parentsHome.setName("My Parents' House");
        parentsHome.setAddress("123 Main Street, Los Angeles, CA 90001, USA");
        parentsHome.setLatitude(34.0522);
        parentsHome.setLongitude(-118.2437);
        parentsHome.setCountry("United States");
        parentsHome.setPrimary(false);
        homes.add(parentsHome);

        // Fifth Home
        Home gardenHome = new Home();
        gardenHome.setId(5);
        gardenHome.setUserId(1);
        gardenHome.setName("My Garden");
        gardenHome.setAddress("456 Park Avenue, San Francisco, CA 94102, USA");
        gardenHome.setLatitude(37.7749);
        gardenHome.setLongitude(-122.4194);
        gardenHome.setCountry("United States");
        gardenHome.setPrimary(false);
        homes.add(gardenHome);

        return homes;
    }
    
    /**
     * Get mock rooms for demo user
     */
    public static List<Room> getMockRooms(Integer homeId) {
        List<Room> rooms = new ArrayList<>();
        
        if (homeId == null || homeId == 1) {
            // Rooms for primary home (My Home)
            rooms.add(createRoom(1, "Living Room", 1));
            rooms.add(createRoom(2, "Bedroom", 1));
            rooms.add(createRoom(3, "Bathroom", 1));
            rooms.add(createRoom(4, "Kitchen", 1));
            rooms.add(createRoom(5, "Dining Room", 1));
            rooms.add(createRoom(6, "Backyard", 1));
        } else if (homeId == 2) {
            // Rooms for My Apartment
            rooms.add(createRoom(7, "Living Room", 2));
            rooms.add(createRoom(8, "Bedroom", 2));
            rooms.add(createRoom(9, "Kitchen", 2));
            rooms.add(createRoom(10, "Bathroom", 2));
        } else if (homeId == 3) {
            // Rooms for My Office
            rooms.add(createRoom(11, "Conference Room", 3));
            rooms.add(createRoom(12, "Reception", 3));
            rooms.add(createRoom(13, "Workspace", 3));
        } else if (homeId == 4) {
            // Rooms for My Parents' House
            rooms.add(createRoom(14, "Living Room", 4));
            rooms.add(createRoom(15, "Master Bedroom", 4));
            rooms.add(createRoom(16, "Guest Bedroom", 4));
            rooms.add(createRoom(17, "Kitchen", 4));
            rooms.add(createRoom(18, "Bathroom", 4));
        } else if (homeId == 5) {
            // Rooms for My Garden
            rooms.add(createRoom(19, "Greenhouse", 5));
            rooms.add(createRoom(20, "Storage Shed", 5));
        } else {
            // Default rooms for any other home
            rooms.add(createRoom(21, "Living Room", homeId));
            rooms.add(createRoom(22, "Bedroom", homeId));
        }
        
        return rooms;
    }
    
    /**
     * Get device count for a home (mock data)
     */
    public static int getMockDeviceCount(int homeId) {
        // Return different device counts for different homes
        switch (homeId) {
            case 1: return 37; // My Home - most devices
            case 2: return 12; // My Apartment
            case 3: return 8;  // My Office
            case 4: return 25; // My Parents' House
            case 5: return 5;  // My Garden
            default: return 10;
        }
    }
    
    private static Room createRoom(int id, String name, int homeId) {
        Room room = new Room();
        room.setId(id);
        room.setUserId(1);
        room.setName(name);
        room.setHomeId(homeId);
        return room;
    }
    
    /**
     * Get mock notifications for demo user
     */
    public static List<Notification> getMockNotifications(String category) {
        List<Notification> notifications = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        
        // Smart Home notifications
        if (category == null || category.equals("smart_home")) {
            // Today - Unread
            cal.set(Calendar.HOUR_OF_DAY, 10);
            cal.set(Calendar.MINUTE, 30);
            notifications.add(createNotification(1, "Account Security Alert", 
                "We've noticed some unusual activity on your account. Please review your security settings.",
                "security", "smart_home", "security", false, cal.getTime()));
            
            cal.set(Calendar.HOUR_OF_DAY, 14);
            cal.set(Calendar.MINUTE, 15);
            notifications.add(createNotification(2, "Device Connected", 
                "New device \"Living Room Sensor\" has been successfully connected to your smart home.",
                "info", "smart_home", "device", false, cal.getTime()));
            
            cal.set(Calendar.HOUR_OF_DAY, 16);
            cal.set(Calendar.MINUTE, 45);
            notifications.add(createNotification(3, "Automation Activated", 
                "Your \"Morning Routine\" automation has been activated successfully.",
                "feature", "smart_home", "star", false, cal.getTime()));
            
            // Yesterday - Unread
            cal.add(Calendar.DAY_OF_MONTH, -1);
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
            notifications.add(createNotification(4, "Device Maintenance", 
                "Time for scheduled maintenance on \"Kitchen Thermostat\". Please check device status.",
                "alert", "smart_home", "device", false, cal.getTime()));
        }
        
        // General notifications
        if (category == null || category.equals("general")) {
            // Today - Read
            cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 8);
            cal.set(Calendar.MINUTE, 0);
            notifications.add(createNotification(5, "Energy Usage Report", 
                "Your energy consumption this month is 15% lower than last month. Great job!",
                "info", "general", "energy", true, cal.getTime()));
            
            // Yesterday - Read
            cal.add(Calendar.DAY_OF_MONTH, -1);
            cal.set(Calendar.HOUR_OF_DAY, 12);
            cal.set(Calendar.MINUTE, 30);
            notifications.add(createNotification(6, "Bill Reminder", 
                "Your monthly electricity bill is due in 3 days. Amount: $125.50",
                "reminder", "general", "calendar", true, cal.getTime()));
        }
        
        return notifications;
    }
    
    private static Notification createNotification(int id, String title, String message, 
            String type, String category, String icon, boolean isRead, Date createdAt) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(1);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setCategory(category);
        notification.setIcon(icon);
        notification.setRead(isRead);
        notification.setCreatedAt(createdAt != null ? createdAt : new Date());
        return notification;
    }
    
    /**
     * Get mock chatbot history for demo user
     */
    public static List<ChatbotMessage> getMockChatHistory() {
        List<ChatbotMessage> messages = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -2);
        
        // User message
        ChatbotMessage msg1 = new ChatbotMessage();
        msg1.setId(1);
        msg1.setUserId(1);
        msg1.setRole("user");
        msg1.setMessage("Hello! How can I set up automation?");
        msg1.setCreatedAt(cal.getTime());
        messages.add(msg1);
        
        // Assistant response
        cal.add(Calendar.MINUTE, 1);
        ChatbotMessage msg2 = new ChatbotMessage();
        msg2.setId(2);
        msg2.setUserId(1);
        msg2.setRole("assistant");
        msg2.setMessage("Hello! I'd be happy to help you set up automation. Automation allows you to create rules that trigger actions based on conditions. For example, you can set your lights to turn on automatically when you arrive home. Would you like me to guide you through creating your first automation?");
        msg2.setCreatedAt(cal.getTime());
        messages.add(msg2);
        
        // User message
        cal.add(Calendar.MINUTE, 5);
        ChatbotMessage msg3 = new ChatbotMessage();
        msg3.setId(3);
        msg3.setUserId(1);
        msg3.setRole("user");
        msg3.setMessage("How do I control my devices?");
        msg3.setCreatedAt(cal.getTime());
        messages.add(msg3);
        
        // Assistant response
        cal.add(Calendar.MINUTE, 1);
        ChatbotMessage msg4 = new ChatbotMessage();
        msg4.setId(4);
        msg4.setUserId(1);
        msg4.setRole("assistant");
        msg4.setMessage("You can control your devices in several ways:\n\n1. **Manual Control**: Open the device from your home screen and use the controls\n2. **Voice Commands**: Use voice assistants like Google Assistant or Alexa\n3. **Automation**: Set up rules to control devices automatically\n4. **Schedules**: Create time-based schedules for your devices\n\nWhich method would you like to learn more about?");
        msg4.setCreatedAt(cal.getTime());
        messages.add(msg4);
        
        return messages;
    }
    
    /**
     * Get mock chatbot response for a user message
     */
    public static String getMockBotResponse(String userMessage) {
        String message = userMessage.toLowerCase();
        
        if (message.contains("hello") || message.contains("hi") || message.contains("hey")) {
            return "Hello! I'm Bobo, your smart home assistant. How can I help you today?";
        } else if (message.contains("automation") || message.contains("automate")) {
            return "Automation is a great feature! You can create rules that automatically control your devices based on conditions like time, location, or device status. Would you like me to help you create your first automation?";
        } else if (message.contains("device") || message.contains("control")) {
            return "You can control your devices through the main screen, using voice commands, or by setting up automations. Which devices would you like to control?";
        } else if (message.contains("energy") || message.contains("consumption") || message.contains("bill")) {
            return "I can help you monitor your energy consumption! You can view detailed reports in the Analytics section. Your current monthly usage is 15% lower than last month - great job!";
        } else if (message.contains("help") || message.contains("support")) {
            return "I'm here to help! I can assist you with:\n• Setting up devices\n• Creating automations\n• Managing your home\n• Energy monitoring\n• Troubleshooting\n\nWhat would you like help with?";
        } else if (message.contains("setup") || message.contains("set up")) {
            return "To set up your smart home:\n1. Add devices from the main screen\n2. Assign devices to rooms\n3. Create automations for convenience\n4. Monitor energy usage\n\nWould you like step-by-step guidance for any of these?";
        } else {
            return "I understand you're asking about \"" + userMessage + "\". Let me help you with that. Could you provide a bit more detail about what you'd like to know?";
        }
    }
    
    /**
     * Get mock smart scenes for demo user
     */
    /**
     * Add a dynamically created scene (for runtime scene creation)
     */
    public static void addDynamicScene(SmartScene scene) {
        if (scene != null) {
            // Set ID if not set
            if (scene.getId() == null) {
                scene.setId(nextSceneId++);
            }
            dynamicScenes.add(scene);
        }
    }
    
    public static List<SmartScene> getMockScenes(String type) {
        List<SmartScene> scenes = new ArrayList<>();
        
        if ("automation".equals(type)) {
            // Automation scenes - matching database demo data with colors and icons
            List<SceneCondition> conditions1 = new ArrayList<>();
            conditions1.add(createTimeCondition());
            List<SceneTask> tasks1 = new ArrayList<>();
            tasks1.add(createLightTask());
            scenes.add(createAutomationSceneWithStyle(1, "Turn ON All the Lights", true, 
                "#405FF2", "ic_sun", conditions1, tasks1));
            
            List<SceneCondition> conditions2 = new ArrayList<>();
            conditions2.add(createLocationCondition());
            scenes.add(createAutomationSceneWithStyle(2, "Go to Office", true,
                "#26A69A", "ic_briefcase", conditions2, createOfficeTasks()));
            
            List<SceneCondition> conditions3 = new ArrayList<>();
            conditions3.add(createWorkModeCondition());
            scenes.add(createAutomationSceneWithStyle(3, "Energy Saver Mode", false,
                "#FFA726", "ic_energy", conditions3, createEnergySaverTasks()));
            
            List<SceneCondition> conditions4 = new ArrayList<>();
            conditions4.add(createTapToRunCondition());
            List<SceneTask> tasks4 = new ArrayList<>();
            tasks4.add(createWorkModeTask());
            scenes.add(createAutomationSceneWithStyle(4, "Work Mode Activate", true,
                "#42A5F5", "ic_briefcase", conditions4, tasks4));
            
            List<SceneCondition> conditions5 = new ArrayList<>();
            conditions5.add(createTimeCondition());
            scenes.add(createAutomationSceneWithStyle(5, "Night Time Bliss", true,
                "#6C5CE7", "ic_sun", conditions5, createNightTasks()));
            
            List<SceneCondition> conditions6 = new ArrayList<>();
            conditions6.add(createTemperatureCondition());
            conditions6.add(createHumidityCondition());
            List<SceneTask> tasks6 = new ArrayList<>();
            tasks6.add(createACTask());
            scenes.add(createAutomationSceneWithStyle(6, "Turn on the AC", true,
                "#E74C3C", "ic_air_conditioner", conditions6, tasks6));
            
            // Additional automation scenes from database
            List<SceneCondition> conditions7 = new ArrayList<>();
            conditions7.add(createLocationCondition());
            List<SceneTask> tasks7 = new ArrayList<>();
            tasks7.add(createLightTask());
            scenes.add(createAutomationSceneWithStyle(7, "Welcome Home Automation", true,
                "#FF6B35", "ic_sun", conditions7, tasks7));
            
            List<SceneCondition> conditions8 = new ArrayList<>();
            conditions8.add(createTimeCondition());
            scenes.add(createAutomationSceneWithStyle(8, "Bedtime Bliss Automation", true,
                "#9B59B6", "ic_clock", conditions8, createNightTasks()));
            
            List<SceneCondition> conditions9 = new ArrayList<>();
            conditions9.add(createLocationCondition());
            List<SceneTask> tasks9 = new ArrayList<>();
            tasks9.add(createLightTask());
            scenes.add(createAutomationSceneWithStyle(9, "Leave Home Automation", true,
                "#E74C3C", "ic_location", conditions9, tasks9));
            
            List<SceneCondition> conditions10 = new ArrayList<>();
            conditions10.add(createTimeCondition());
            List<SceneTask> tasks10 = new ArrayList<>();
            tasks10.add(createLightTask());
            scenes.add(createAutomationSceneWithStyle(10, "Morning Routine", true,
                "#F39C12", "ic_sun", conditions10, tasks10));
            
            List<SceneCondition> conditions11 = new ArrayList<>();
            conditions11.add(createTimeCondition());
            List<SceneTask> tasks11 = new ArrayList<>();
            tasks11.add(createLightTask());
            scenes.add(createAutomationSceneWithStyle(11, "Evening Routine", true,
                "#E67E22", "ic_sun", conditions11, tasks11));
            
            List<SceneCondition> conditions12 = new ArrayList<>();
            conditions12.add(createTimeCondition());
            List<SceneTask> tasks12 = new ArrayList<>();
            tasks12.add(createLightTask());
            scenes.add(createAutomationSceneWithStyle(12, "Weekend Mode", true,
                "#3498DB", "ic_sun", conditions12, tasks12));
            
            List<SceneCondition> conditions13 = new ArrayList<>();
            conditions13.add(createWorkModeCondition());
            scenes.add(createAutomationSceneWithStyle(13, "Vacation Mode", false,
                "#16A085", "ic_sun", conditions13, createEnergySaverTasks()));
            
            List<SceneCondition> conditions14 = new ArrayList<>();
            conditions14.add(createTapToRunCondition());
            List<SceneTask> tasks14 = new ArrayList<>();
            tasks14.add(createLightTask());
            scenes.add(createAutomationSceneWithStyle(14, "Party Mode", false,
                "#E91E63", "ic_sun", conditions14, tasks14));
            
            List<SceneCondition> conditions15 = new ArrayList<>();
            conditions15.add(createTimeCondition());
            List<SceneTask> tasks15 = new ArrayList<>();
            tasks15.add(createLightTask());
            scenes.add(createAutomationSceneWithStyle(15, "Movie Night", true,
                "#673AB7", "ic_sun", conditions15, tasks15));
        } else if ("tap_to_run".equals(type)) {
            // Tap-to-Run scenes - matching the UI design and database demo data
            // 1. Quick Light On (Orange, sun icon) - 1 task
            scenes.add(createTapToRunScene(16, "Quick Light On", true, "#FFC107", "ic_sun", createLightTask()));
            
            // 2. All Devices Off (Grey, power icon) - 1 task
            SceneTask allOffTask = new SceneTask();
            allOffTask.setType("control_device");
            allOffTask.setDeviceName("All Devices");
            allOffTask.setFunction("OFF");
            scenes.add(createTapToRunScene(17, "All Devices Off", true, "#607D8B", "ic_plug", allOffTask));
            
            // 3. Bedtime Prep (Blue, moon icon) - 2 tasks
            List<SceneTask> bedtimeTasks = new ArrayList<>();
            SceneTask bedtimeTask1 = new SceneTask();
            bedtimeTask1.setType("control_device");
            bedtimeTask1.setDeviceName("Bedroom Lights");
            bedtimeTask1.setFunction("DIM");
            bedtimeTasks.add(bedtimeTask1);
            SceneTask bedtimeTask2 = new SceneTask();
            bedtimeTask2.setType("control_device");
            bedtimeTask2.setDeviceName("Thermostat");
            bedtimeTask2.setFunction("SET_20");
            bedtimeTasks.add(bedtimeTask2);
            scenes.add(createTapToRunScene(18, "Bedtime Prep", true, "#6C5CE7", "ic_sun", bedtimeTasks));
            
            // 4. Evening Chill (Orange, sun icon) - 4 tasks
            List<SceneTask> eveningTasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                SceneTask task = new SceneTask();
                task.setType("control_device");
                task.setDeviceName("Living Room Lights");
                task.setFunction(i % 2 == 0 ? "ON" : "DIM");
                eveningTasks.add(task);
            }
            scenes.add(createTapToRunScene(19, "Evening Chill", true, "#FFA726", "ic_sun", eveningTasks));
            
            // 5. Boost Productivity (Teal, briefcase icon) - 1 task
            SceneTask productivityTask = new SceneTask();
            productivityTask.setType("control_device");
            productivityTask.setDeviceName("Office Lights");
            productivityTask.setFunction("ON");
            scenes.add(createTapToRunScene(20, "Boost Productivity", true, "#26A69A", "ic_briefcase", productivityTask));
            
            // 6. Get Energized (Orange, energy icon) - 3 tasks
            List<SceneTask> energizedTasks = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                SceneTask task = new SceneTask();
                task.setType("control_device");
                task.setDeviceName("All Lights");
                task.setFunction("ON");
                energizedTasks.add(task);
            }
            scenes.add(createTapToRunScene(21, "Get Energized", true, "#FF9800", "ic_energy", energizedTasks));
            
            // 7. Home Office (Blue, briefcase icon) - 2 tasks
            List<SceneTask> officeTasks = createOfficeTasks();
            scenes.add(createTapToRunScene(22, "Home Office", true, "#42A5F5", "ic_briefcase", officeTasks));
            
            // 8. Reading Corner (Brown, book icon) - 4 tasks
            List<SceneTask> readingTasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                SceneTask task = new SceneTask();
                task.setType("control_device");
                task.setDeviceName("Reading Light");
                task.setFunction("ON");
                readingTasks.add(task);
            }
            scenes.add(createTapToRunScene(23, "Reading Corner", true, "#795548", "ic_sun", readingTasks));
            
            // 9. Outdoor Party (Teal, party icon) - 3 tasks
            List<SceneTask> partyTasks = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                SceneTask task = new SceneTask();
                task.setType("control_device");
                task.setDeviceName("Outdoor Lights");
                task.setFunction("ON");
                partyTasks.add(task);
            }
            scenes.add(createTapToRunScene(24, "Outdoor Party", true, "#009688", "ic_sun", partyTasks));
            
            // 10. Relaxation Mode (Purple, relax icon) - 2 tasks
            List<SceneTask> relaxationTasks = new ArrayList<>();
            SceneTask relaxTask1 = new SceneTask();
            relaxTask1.setType("control_device");
            relaxTask1.setDeviceName("Living Room Lights");
            relaxTask1.setFunction("DIM");
            relaxationTasks.add(relaxTask1);
            SceneTask relaxTask2 = new SceneTask();
            relaxTask2.setType("control_device");
            relaxTask2.setDeviceName("Smart Speaker");
            relaxTask2.setFunction("PLAY_RELAX");
            relaxationTasks.add(relaxTask2);
            scenes.add(createTapToRunScene(25, "Relaxation Mode", true, "#8E24AA", "ic_sun", relaxationTasks));
            
            // 11. Focus Mode (Cyan, focus icon) - 2 tasks
            List<SceneTask> focusTasks = new ArrayList<>();
            SceneTask focusTask1 = new SceneTask();
            focusTask1.setType("control_device");
            focusTask1.setDeviceName("Office Lights");
            focusTask1.setFunction("ON");
            focusTasks.add(focusTask1);
            SceneTask focusTask2 = new SceneTask();
            focusTask2.setType("control_device");
            focusTask2.setDeviceName("Smart Speaker");
            focusTask2.setFunction("MUTE");
            focusTasks.add(focusTask2);
            scenes.add(createTapToRunScene(26, "Focus Mode", true, "#00ACC1", "ic_sun", focusTasks));
            
            // 12. Security On (Red, security icon) - 1 task
            SceneTask securityTask = new SceneTask();
            securityTask.setType("change_arm_mode");
            securityTask.setArmMode("arm_stay");
            scenes.add(createTapToRunScene(27, "Security On", true, "#D32F2F", "ic_shield", securityTask));
            
            // 13. Comfort Zone (Indigo, comfort icon) - 2 tasks
            List<SceneTask> comfortTasks = new ArrayList<>();
            SceneTask comfortTask1 = new SceneTask();
            comfortTask1.setType("control_device");
            comfortTask1.setDeviceName("Smart Thermostat");
            comfortTask1.setFunction("SET_22");
            comfortTasks.add(comfortTask1);
            SceneTask comfortTask2 = new SceneTask();
            comfortTask2.setType("control_device");
            comfortTask2.setDeviceName("Living Room Lights");
            comfortTask2.setFunction("ON");
            comfortTasks.add(comfortTask2);
            scenes.add(createTapToRunScene(28, "Comfort Zone", true, "#5C6BC0", "ic_sun", comfortTasks));
        }
        
        // Add dynamically created scenes that match the requested type
        for (SmartScene dynamicScene : dynamicScenes) {
            if (dynamicScene.getType() != null && dynamicScene.getType().equals(type)) {
                scenes.add(dynamicScene);
            }
        }
        
        // Ensure we always return at least some scenes for testing
        if (scenes.isEmpty()) {
            android.util.Log.w("MockDataProvider", "No scenes found for type: " + type + ", creating default scenes");
            if ("automation".equals(type)) {
                // Create at least one default automation scene
                List<SceneCondition> defaultConditions = new ArrayList<>();
                defaultConditions.add(createTimeCondition());
                List<SceneTask> defaultTasks = new ArrayList<>();
                defaultTasks.add(createLightTask());
                scenes.add(createAutomationSceneWithStyle(1, "Default Automation", true, "#405FF2", "ic_sun", defaultConditions, defaultTasks));
            } else {
                // Create at least one default tap-to-run scene
                scenes.add(createTapToRunScene(1, "Default Tap-to-Run", true, "#405FF2", "ic_sun", createLightTask()));
            }
        }
        
        android.util.Log.d("MockDataProvider", "Returning " + scenes.size() + " scenes for type: " + type);
        if (scenes.size() > 0) {
            android.util.Log.d("MockDataProvider", "First scene: " + scenes.get(0).getName() + ", Type: " + scenes.get(0).getType());
        }
        return scenes;
    }
    
    private static SmartScene createAutomationScene(int id, String name, boolean enabled, 
            SceneCondition... conditions) {
        SmartScene scene = new SmartScene();
        scene.setId(id);
        scene.setName(name);
        scene.setType("automation");
        scene.setEnabled(enabled);
        scene.setConditionLogic("any");
        scene.setHomeId(1);
        scene.setColor("#405FF2");
        scene.setIcon("ic_sun");
        
        List<SceneCondition> conditionList = new ArrayList<>();
        for (SceneCondition condition : conditions) {
            conditionList.add(condition);
        }
        scene.setConditions(conditionList);
        
        // Add default task if none provided
        if (scene.getTasks() == null || scene.getTasks().isEmpty()) {
            List<SceneTask> tasks = new ArrayList<>();
            tasks.add(createLightTask());
            scene.setTasks(tasks);
        }
        
        return scene;
    }
    
    private static SmartScene createAutomationSceneWithStyle(int id, String name, boolean enabled,
            String color, String icon, List<SceneCondition> conditions, List<SceneTask> tasks) {
        SmartScene scene = new SmartScene();
        scene.setId(id);
        scene.setName(name);
        scene.setType("automation");
        scene.setEnabled(enabled);
        scene.setConditionLogic("any");
        scene.setHomeId(1);
        scene.setColor(color);
        scene.setIcon(icon);
        scene.setConditions(conditions != null ? conditions : new ArrayList<>());
        scene.setTasks(tasks != null ? tasks : new ArrayList<>());
        return scene;
    }
    
    private static SmartScene createAutomationScene(int id, String name, boolean enabled, 
            List<SceneCondition> conditions, List<SceneTask> tasks) {
        SmartScene scene = new SmartScene();
        scene.setId(id);
        scene.setName(name);
        scene.setType("automation");
        scene.setEnabled(enabled);
        scene.setConditionLogic("any");
        scene.setConditions(conditions != null ? conditions : new ArrayList<>());
        scene.setTasks(tasks != null ? tasks : new ArrayList<>());
        scene.setHomeId(1); // Set default home ID
        // Set default color and icon if not set
        if (scene.getColor() == null) {
            scene.setColor("#405FF2"); // Default primary color
        }
        if (scene.getIcon() == null) {
            scene.setIcon("ic_sun"); // Default icon
        }
        return scene;
    }
    
    private static SmartScene createAutomationScene(int id, String name, boolean enabled, 
            List<SceneCondition> conditions, SceneTask task) {
        List<SceneTask> tasks = new ArrayList<>();
        if (task != null) {
            tasks.add(task);
        }
        return createAutomationScene(id, name, enabled, conditions, tasks);
    }
    
    private static SmartScene createTapToRunScene(int id, String name, boolean enabled, SceneTask... tasks) {
        return createTapToRunScene(id, name, enabled, null, null, tasks);
    }
    
    private static SmartScene createTapToRunScene(int id, String name, boolean enabled, String color, String icon, SceneTask... tasks) {
        SmartScene scene = new SmartScene();
        scene.setId(id);
        scene.setName(name);
        scene.setType("tap_to_run");
        scene.setEnabled(enabled);
        scene.setHomeId(1);
        scene.setColor(color != null ? color : "#405FF2");
        scene.setIcon(icon != null ? icon : "ic_sun");
        
        List<SceneTask> taskList = new ArrayList<>();
        for (SceneTask task : tasks) {
            if (task != null) {
                taskList.add(task);
            }
        }
        // Ensure at least one task
        if (taskList.isEmpty()) {
            taskList.add(createLightTask());
        }
        scene.setTasks(taskList);
        
        return scene;
    }
    
    private static SmartScene createTapToRunScene(int id, String name, boolean enabled, String color, String icon, List<SceneTask> tasks) {
        SmartScene scene = new SmartScene();
        scene.setId(id);
        scene.setName(name);
        scene.setType("tap_to_run");
        scene.setEnabled(enabled);
        scene.setHomeId(1);
        scene.setColor(color != null ? color : "#405FF2");
        scene.setIcon(icon != null ? icon : "ic_sun");
        if (tasks != null && !tasks.isEmpty()) {
            scene.setTasks(tasks);
        } else {
            // Ensure at least one task
            List<SceneTask> defaultTasks = new ArrayList<>();
            defaultTasks.add(createLightTask());
            scene.setTasks(defaultTasks);
        }
        
        return scene;
    }
    
    private static SceneCondition createTimeCondition() {
        SceneCondition condition = new SceneCondition();
        condition.setType("time");
        condition.setOperator("schedule");
        return condition;
    }
    
    private static SceneCondition createLocationCondition() {
        SceneCondition condition = new SceneCondition();
        condition.setType("location");
        condition.setOperator("arrive");
        condition.setLocation("Office");
        return condition;
    }
    
    private static SceneCondition createTemperatureCondition() {
        SceneCondition condition = new SceneCondition();
        condition.setType("temperature");
        condition.setOperator(">");
        condition.setValue(20.0);
        condition.setUnit("C");
        condition.setLocation("New York City");
        return condition;
    }
    
    private static SceneCondition createHumidityCondition() {
        SceneCondition condition = new SceneCondition();
        condition.setType("humidity");
        condition.setOperator("dry");
        condition.setLocation("New York City");
        return condition;
    }
    
    private static SceneCondition createWorkModeCondition() {
        SceneCondition condition = new SceneCondition();
        condition.setType("arm_mode");
        condition.setOperator("work");
        return condition;
    }
    
    private static SceneCondition createTapToRunCondition() {
        SceneCondition condition = new SceneCondition();
        condition.setType("tap_to_run");
        condition.setOperator("manual");
        return condition;
    }
    
    private static SceneTask createLightTask() {
        SceneTask task = new SceneTask();
        task.setType("control_device");
        task.setDeviceName("All Lights");
        task.setFunction("ON");
        return task;
    }
    
    private static SceneTask createACTask() {
        SceneTask task = new SceneTask();
        task.setType("control_device");
        task.setDeviceName("Air Conditioner");
        task.setRoomName("Living Room");
        task.setFunction("ON");
        return task;
    }
    
    private static List<SceneTask> createOfficeTasks() {
        List<SceneTask> tasks = new ArrayList<>();
        SceneTask task1 = new SceneTask();
        task1.setType("control_device");
        task1.setDeviceName("Thermostat");
        task1.setFunction("ON");
        tasks.add(task1);
        
        SceneTask task2 = new SceneTask();
        task2.setType("delay");
        task2.setDelaySeconds(30);
        tasks.add(task2);
        
        return tasks;
    }
    
    private static List<SceneTask> createEnergySaverTasks() {
        List<SceneTask> tasks = new ArrayList<>();
        SceneTask task1 = new SceneTask();
        task1.setType("change_arm_mode");
        task1.setArmMode("energy_saver");
        tasks.add(task1);
        
        SceneTask task2 = new SceneTask();
        task2.setType("send_notification");
        task2.setNotificationMessage("Energy Saver Mode Activated");
        tasks.add(task2);
        
        return tasks;
    }
    
    private static SceneTask createWorkModeTask() {
        SceneTask task = new SceneTask();
        task.setType("change_arm_mode");
        task.setArmMode("work");
        return task;
    }
    
    private static List<SceneTask> createNightTasks() {
        List<SceneTask> tasks = new ArrayList<>();
        SceneTask task1 = new SceneTask();
        task1.setType("control_device");
        task1.setDeviceName("Bedroom Lights");
        task1.setFunction("DIM");
        tasks.add(task1);
        
        SceneTask task2 = new SceneTask();
        task2.setType("control_device");
        task2.setDeviceName("Thermostat");
        task2.setFunction("SET_20");
        tasks.add(task2);
        
        return tasks;
    }
    
    private static List<SceneTask> createAllOffTasks() {
        List<SceneTask> tasks = new ArrayList<>();
        SceneTask task = new SceneTask();
        task.setType("control_device");
        task.setDeviceName("All Devices");
        task.setFunction("OFF");
        tasks.add(task);
        return tasks;
    }
    
    /**
     * Get mock devices for scene task selection
     */
    public static List<Device> getMockDevicesForScene(String category, Integer roomId) {
        List<Device> devices = new ArrayList<>();
        
        if ("lightning".equals(category) || "all".equals(category)) {
            // Lightning devices (12 total)
            devices.add(createDevice(1, "Smart Lamp", "smart_lamp", 1)); // Living Room
            devices.add(createDevice(2, "Smart Lamp", "smart_lamp", 2)); // Bedroom
            devices.add(createDevice(3, "Lamp", "light", 3)); // Bathroom
            devices.add(createDevice(4, "Smart Lamp", "smart_lamp", 4)); // Toilet
            devices.add(createDevice(5, "Lamp", "light", 2)); // Bedroom
            devices.add(createDevice(6, "Smart Lamp", "smart_lamp", 5)); // Kitchen
            devices.add(createDevice(7, "Smart Lamp", "smart_lamp", 6)); // Dining Room
            devices.add(createDevice(8, "Lamp", "light", 7)); // Backyard
            devices.add(createDevice(9, "Smart Lamp", "smart_lamp", 1)); // Living Room
            devices.add(createDevice(10, "Lamp", "light", 2)); // Bedroom
            devices.add(createDevice(11, "Smart Lamp", "smart_lamp", 3)); // Bathroom
            devices.add(createDevice(12, "Lamp", "light", 1)); // Living Room
        }
        if ("cameras".equals(category) || "all".equals(category)) {
            // Camera devices (8 total)
            devices.add(createDevice(13, "Smart V1 CCTV", "camera", 1)); // Living Room
            devices.add(createDevice(14, "Smart V2 CCTV", "camera", 2)); // Bedroom
            devices.add(createDevice(15, "Smart V3 CCTV", "camera", 5)); // Kitchen
            devices.add(createDevice(16, "Smart V1 CCTV", "camera", 3)); // Bathroom
            devices.add(createDevice(17, "Smart Webcam", "camera", 4)); // Toilet
            devices.add(createDevice(18, "Smart V1 CCTV", "camera", 6)); // Dining Room
            devices.add(createDevice(19, "Smart V2 CCTV", "camera", 7)); // Backyard
            devices.add(createDevice(20, "Smart V3 CCTV", "camera", 7)); // Backyard
        }
        if ("electrical".equals(category) || "all".equals(category)) {
            // Electrical devices (6 total)
            devices.add(createDevice(21, "Air Conditioner", "air_conditioner", 1)); // Living Room
            devices.add(createDevice(22, "Router", "router", 1)); // Living Room
            devices.add(createDevice(23, "Stereo Speaker", "speaker", 1)); // Living Room
            devices.add(createDevice(24, "Smart Thermostat", "thermostat", 1)); // Living Room
            devices.add(createDevice(25, "Smart Plug", "plug", 2)); // Bedroom
            devices.add(createDevice(26, "Smart Switch", "switch", 5)); // Kitchen
        }
        
        // Filter by room if specified
        if (roomId != null) {
            devices.removeIf(device -> !roomId.equals(device.getRoomId()));
        }
        
        return devices;
    }
    
    private static Device createDevice(int id, String name, String type, int roomId) {
        Device device = new Device(id, name, type);
        device.setRoomId(roomId);
        device.setOnline(true);
        return device;
    }
}
