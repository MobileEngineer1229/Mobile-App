package com.smarthome.iot.network;

import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.ChatHistoryResponse;
import com.smarthome.iot.models.ChatbotMessage;
import com.smarthome.iot.models.ChatbotResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.DeviceConsumptionResponse;
import com.smarthome.iot.models.DeviceConsumptionSummary;
import com.smarthome.iot.models.ForgotPasswordRequest;
import com.smarthome.iot.models.ForgotPasswordResponse;
import com.smarthome.iot.models.HealthResponse;
import com.smarthome.iot.models.Home;
import com.smarthome.iot.models.HomeMember;
import com.smarthome.iot.models.LinkedAccount;
import com.smarthome.iot.models.LoginRequest;
import com.smarthome.iot.models.LoginResponse;
import com.smarthome.iot.models.MonthlySummary;
import com.smarthome.iot.models.MonthlySummaryResponse;
import com.smarthome.iot.models.Notification;
import com.smarthome.iot.models.NotificationPreference;
import com.smarthome.iot.models.NotificationStats;
import com.smarthome.iot.models.ResetPasswordRequest;
import com.smarthome.iot.models.ResetPasswordResponse;
import com.smarthome.iot.models.Room;
import com.smarthome.iot.models.SceneLog;
import com.smarthome.iot.models.SignupRequest;
import com.smarthome.iot.models.SignupResponse;
import com.smarthome.iot.models.SmartScene;
import com.smarthome.iot.models.StatisticsDataPoint;
import com.smarthome.iot.models.StatisticsResponse;
import com.smarthome.iot.models.User;
import com.smarthome.iot.models.VerifyOTPRequest;
import com.smarthome.iot.models.VerifyOTPResponse;
import com.smarthome.iot.models.VersionCheckResponse;
import com.smarthome.iot.models.VoiceAssistant;
import com.smarthome.iot.utils.MockDataProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;

/**
 * Local API implementation for the mockup_IOT demo branch.
 * Every endpoint returns deterministic mock data so the app can be shown without backend services.
 */
public class MockApiService implements ApiService {
    private static final String MOCK_TOKEN = "mockup-iot-token";
    private int nextDeviceId = 500;
    private int nextHomeId = 100;
    private int nextRoomId = 200;

    private static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    private static <T> Call<ApiResponse<T>> api(T data) {
        return new MockCall<>(ok(data));
    }

    private User demoUser(String email) {
        User user = new User();
        user.setId(1);
        user.setEmail(email != null && !email.isEmpty() ? email : "demo@smartify.com");
        user.setFirstName("Demo");
        user.setLastName("Owner");
        user.setPhone("+1 555 0100");
        user.setCreatedAt("2026-05-27T00:00:00Z");
        user.setUpdatedAt("2026-05-27T00:00:00Z");
        return user;
    }

    @Override
    public Call<HealthResponse> checkHealth() {
        HealthResponse response = new HealthResponse();
        response.setStatus("ok");
        response.setEnvironment("mockup");
        response.setMqtt("connected");
        response.setTimestamp(new Date().toString());
        return new MockCall<>(response);
    }

    @Override
    public Call<LoginResponse> login(LoginRequest request) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(true);
        LoginResponse.LoginData data = new LoginResponse.LoginData();
        data.setToken(MOCK_TOKEN);
        data.setUser(demoUser(request != null ? request.getEmail() : null));
        response.setData(data);
        return new MockCall<>(response);
    }

    @Override
    public Call<SignupResponse> signup(SignupRequest request) {
        SignupResponse response = new SignupResponse();
        response.setSuccess(true);
        SignupResponse.SignupData data = new SignupResponse.SignupData();
        data.setToken(MOCK_TOKEN);
        User user = demoUser(request != null ? request.getEmail() : null);
        if (request != null) {
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setPhone(request.getPhone());
        }
        data.setUser(user);
        response.setData(data);
        return new MockCall<>(response);
    }

    @Override
    public Call<ApiResponse<ForgotPasswordResponse>> forgotPassword(ForgotPasswordRequest request) {
        ForgotPasswordResponse data = new ForgotPasswordResponse();
        data.setMessage("Mock OTP sent. Use 123456.");
        data.setOtpSent(true);
        data.setExpiresInMinutes(10);
        return api(data);
    }

    @Override
    public Call<ApiResponse<VerifyOTPResponse>> verifyOTP(VerifyOTPRequest request) {
        VerifyOTPResponse data = new VerifyOTPResponse();
        data.setValid(true);
        data.setMessage("OTP verified in mockup mode.");
        return api(data);
    }

    @Override
    public Call<ApiResponse<ResetPasswordResponse>> resetPassword(ResetPasswordRequest request) {
        ResetPasswordResponse data = new ResetPasswordResponse();
        data.setSuccess(true);
        data.setMessage("Password reset completed in mockup mode.");
        return api(data);
    }

    @Override
    public Call<ApiResponse<List<Room>>> getRooms(Integer homeId) {
        return api(MockDataProvider.getMockRooms(homeId));
    }

    @Override
    public Call<ApiResponse<Room>> createRoom(Map<String, Object> roomData) {
        Room room = new Room();
        room.setId(nextRoomId++);
        room.setUserId(1);
        room.setName(value(roomData, "name", "Demo Room"));
        room.setHomeId(intValue(roomData, "homeId", 1));
        return api(room);
    }

    @Override
    public Call<ApiResponse<List<Device>>> getDevices(Integer roomId, Integer homeId, String status, String type, Integer page, Integer limit) {
        List<Device> devices = MockDataProvider.getMockDevicesForScene("all", roomId);
        if (type != null && !type.isEmpty()) {
            devices.removeIf(device -> !type.equalsIgnoreCase(device.getType()));
        }
        for (Device device : devices) {
            device.setStatus(status != null ? status : "online");
            device.setOnline(true);
        }
        return api(devices);
    }

    @Override
    public Call<ApiResponse<Device>> getDeviceById(int deviceId) {
        Device device = findDevice(deviceId);
        return api(device != null ? device : createDeviceObject(deviceId, "Mock Device", "smart_lamp", 1));
    }

    @Override
    public Call<ApiResponse<Device>> createDevice(Map<String, Object> deviceData) {
        Device device = createDeviceObject(nextDeviceId++, value(deviceData, "name", "Mock Device"), value(deviceData, "type", "smart_lamp"), intValue(deviceData, "roomId", 1));
        return api(device);
    }

    @Override
    public Call<ApiResponse<Device>> updateDevice(int deviceId, Map<String, Object> deviceData) {
        Device device = createDeviceObject(deviceId, value(deviceData, "name", "Mock Device"), value(deviceData, "type", "smart_lamp"), intValue(deviceData, "roomId", 1));
        return api(device);
    }

    @Override
    public Call<ApiResponse<Object>> discoverDevices() {
        return api(new HashMap<String, Object>());
    }

    @Override
    public Call<ApiResponse<Object>> verifyMqttDevice(Map<String, Object> body) {
        return api(new HashMap<String, Object>());
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> registerMqttDevice(Map<String, Object> body) {
        Map<String, Object> data = successMap();
        data.put("clientId", "mock-device-client");
        data.put("username", "mock-device");
        data.put("password", "mock-password");
        return api(data);
    }

    @Override
    public Call<ApiResponse<Object>> provisionMqttDevice(Map<String, Object> body) {
        return api(successMap());
    }

    @Override
    public Call<ApiResponse<Object>> sendDeviceCommand(Map<String, Object> body) {
        return api(successMap());
    }

    @Override
    public Call<ApiResponse<Object>> getDeviceStates(int deviceId) {
        return api(deviceState());
    }

    @Override
    public Call<ApiResponse<Object>> getCommandHistory(int deviceId) {
        return api(new ArrayList<Map<String, Object>>());
    }

    @Override
    public Call<ApiResponse<Object>> getDeviceTypes(String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("category", category != null ? category : "all");
        data.put("types", new String[]{"smart_lamp", "camera", "speaker", "air_conditioner", "thermostat", "plug"});
        return api(data);
    }

    @Override
    public Call<ApiResponse<MonthlySummaryResponse>> getMonthlySummary() {
        MonthlySummaryResponse response = new MonthlySummaryResponse();
        response.setThisMonth(month("May 2026", 284.6, 74.90));
        response.setPreviousMonth(month("April 2026", 326.4, 88.35));
        return api(response);
    }

    @Override
    public Call<ApiResponse<StatisticsResponse>> getStatistics(String dateRange, String startDate, String endDate) {
        StatisticsResponse response = new StatisticsResponse();
        response.setDateRange(dateRange != null ? dateRange : "month");
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        List<StatisticsDataPoint> points = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            StatisticsDataPoint point = new StatisticsDataPoint();
            point.setPeriod("Day " + i);
            point.setConsumptionKwh(28.0 + i * 2.4);
            point.setCostUsd(6.5 + i * 0.7);
            points.add(point);
        }
        response.setData(points);
        return api(response);
    }

    @Override
    public Call<ApiResponse<DeviceConsumptionResponse>> getDeviceConsumption(String dateRange, String startDate, String endDate, Integer deviceId, String deviceType, Integer roomId, String groupBy) {
        DeviceConsumptionResponse response = new DeviceConsumptionResponse();
        List<DeviceConsumptionSummary> devices = new ArrayList<>();
        devices.add(consumption(1, "Living Room Smart Lamp", "smart_lamp", 1, "Living Room", 42.4, 11.12, 6));
        devices.add(consumption(13, "Smart V1 CCTV", "camera", 1, "Living Room", 61.2, 16.08, 3));
        devices.add(consumption(21, "Air Conditioner", "air_conditioner", 1, "Living Room", 138.6, 36.41, 1));
        devices.add(consumption(23, "Stereo Speaker", "speaker", 1, "Living Room", 19.1, 5.02, 1));
        response.setDevices(devices);
        response.setTotalConsumptionKwh(261.3);
        response.setTotalCostUsd(68.63);
        return api(response);
    }

    @Override
    public Call<ApiResponse<Object>> getDeviceTypeDetails(String deviceType, String dateRange, String startDate, String endDate) {
        Map<String, Object> data = successMap();
        data.put("deviceType", deviceType);
        data.put("dateRange", dateRange);
        return api(data);
    }

    @Override
    public Call<ApiResponse<User>> getProfile() {
        return api(demoUser("demo@smartify.com"));
    }

    @Override
    public Call<ApiResponse<User>> updateProfile(Map<String, Object> profileData) {
        User user = demoUser(value(profileData, "email", "demo@smartify.com"));
        user.setFirstName(value(profileData, "firstName", user.getFirstName()));
        user.setLastName(value(profileData, "lastName", user.getLastName()));
        user.setPhone(value(profileData, "phone", user.getPhone()));
        return api(user);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> getSettings() {
        return api(settings());
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> updateSettings(Map<String, Object> settingsData) {
        return api(settingsData != null ? settingsData : settings());
    }

    @Override
    public Call<ApiResponse<Void>> logout() {
        return api(null);
    }

    @Override
    public Call<ApiResponse<List<VoiceAssistant>>> getVoiceAssistants() {
        List<VoiceAssistant> assistants = new ArrayList<>();
        assistants.add(new VoiceAssistant(1, "Google Assistant", true));
        assistants.add(new VoiceAssistant(2, "Amazon Alexa", false));
        assistants.add(new VoiceAssistant(3, "Apple Siri", true));
        return api(assistants);
    }

    @Override
    public Call<ApiResponse<VoiceAssistant>> linkVoiceAssistant(int assistantId) {
        return api(new VoiceAssistant(assistantId, assistantName(assistantId), true));
    }

    @Override
    public Call<ApiResponse<Void>> unlinkVoiceAssistant(int assistantId) {
        return api(null);
    }

    @Override
    public Call<ApiResponse<List<NotificationPreference>>> getNotificationPreferences() {
        List<NotificationPreference> preferences = new ArrayList<>();
        preferences.add(new NotificationPreference("security", true));
        preferences.add(new NotificationPreference("device", true));
        preferences.add(new NotificationPreference("energy", true));
        return api(preferences);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> updateNotificationPreferences(Map<String, Object> preferences) {
        return api(preferences != null ? preferences : successMap());
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> updateNotificationPreference(String type, Map<String, Object> preference) {
        Map<String, Object> data = preference != null ? preference : successMap();
        data.put("type", type);
        return api(data);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> getSecuritySettings() {
        Map<String, Object> data = new HashMap<>();
        data.put("twoFactorAuth", true);
        data.put("biometricLogin", true);
        data.put("loginAlerts", true);
        return api(data);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> updateSecuritySettings(Map<String, Object> settings) {
        return api(settings != null ? settings : successMap());
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> updateSecuritySetting(String type, Map<String, Object> setting) {
        Map<String, Object> data = setting != null ? setting : successMap();
        data.put("type", type);
        return api(data);
    }

    @Override
    public Call<ApiResponse<Void>> changePassword(Map<String, String> passwordData) {
        return api(null);
    }

    @Override
    public Call<ApiResponse<Void>> deactivateAccount() {
        return api(null);
    }

    @Override
    public Call<ApiResponse<Void>> deleteAccount() {
        return api(null);
    }

    @Override
    public Call<ApiResponse<List<LinkedAccount>>> getLinkedAccounts() {
        List<LinkedAccount> accounts = new ArrayList<>();
        accounts.add(new LinkedAccount(1, "Google", true));
        accounts.add(new LinkedAccount(2, "Apple", false));
        accounts.add(new LinkedAccount(3, "Facebook", false));
        return api(accounts);
    }

    @Override
    public Call<ApiResponse<LinkedAccount>> linkAccount(String provider) {
        return api(new LinkedAccount(10, provider, true));
    }

    @Override
    public Call<ApiResponse<Void>> unlinkAccount(int accountId) {
        return api(null);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> getAdditionalSettings() {
        Map<String, Object> data = new HashMap<>();
        data.put("developerMode", false);
        data.put("offlineMode", true);
        data.put("mockupMode", true);
        return api(data);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> updateAdditionalSettings(Map<String, Object> settings) {
        return api(settings != null ? settings : successMap());
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> getDataAnalytics() {
        Map<String, Object> data = new HashMap<>();
        data.put("energyTracking", true);
        data.put("monthlyUsageKwh", 284.6);
        data.put("estimatedBillUsd", 74.90);
        return api(data);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> downloadUserData() {
        Map<String, Object> data = successMap();
        data.put("downloadUrl", "mock://smartify/user-data.json");
        return api(data);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> getAppAppearance() {
        Map<String, Object> data = new HashMap<>();
        data.put("theme", "dark");
        data.put("language", "English");
        return api(data);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> updateAppAppearance(Map<String, Object> appearance) {
        return api(appearance != null ? appearance : successMap());
    }

    @Override
    public Call<ApiResponse<ChatbotResponse>> sendChatbotMessage(Map<String, String> messageData) {
        String text = messageData != null ? messageData.get("message") : "";
        ChatbotResponse response = new ChatbotResponse();
        response.setConversationId("mock-conversation");
        response.setUserMessage(chatMessage(900, "user", text));
        response.setAssistantMessage(chatMessage(901, "assistant", MockDataProvider.getMockBotResponse(text)));
        return api(response);
    }

    @Override
    public Call<ApiResponse<ChatHistoryResponse>> getChatHistory(Integer page, Integer limit) {
        ChatHistoryResponse response = new ChatHistoryResponse();
        List<ChatbotMessage> messages = MockDataProvider.getMockChatHistory();
        response.setMessages(messages);
        response.setTotal(messages.size());
        response.setPage(page != null ? page : 1);
        response.setLimit(limit != null ? limit : 20);
        response.setTotalPages(1);
        return api(response);
    }

    @Override
    public Call<ApiResponse<Void>> clearChatHistory() {
        return api(null);
    }

    @Override
    public Call<ApiResponse<List<Notification>>> getNotifications(Integer page, Integer limit, String type, String category, Boolean isRead) {
        List<Notification> notifications = MockDataProvider.getMockNotifications(category);
        if (isRead != null) {
            notifications.removeIf(notification -> notification.isRead() != isRead);
        }
        return api(notifications);
    }

    @Override
    public Call<ApiResponse<NotificationStats>> getNotificationStats() {
        NotificationStats stats = new NotificationStats();
        stats.setTotal(6);
        stats.setUnread(4);
        Map<String, Integer> byType = new HashMap<>();
        byType.put("security", 1);
        byType.put("device", 2);
        byType.put("energy", 1);
        stats.setByType(byType);
        return api(stats);
    }

    @Override
    public Call<ApiResponse<Void>> markAllNotificationsAsRead() {
        return api(null);
    }

    @Override
    public Call<ApiResponse<Notification>> getNotificationById(int notificationId) {
        for (Notification notification : MockDataProvider.getMockNotifications(null)) {
            if (notification.getId() == notificationId) {
                return api(notification);
            }
        }
        return api(MockDataProvider.getMockNotifications(null).get(0));
    }

    @Override
    public Call<ApiResponse<Notification>> markNotificationAsRead(int notificationId) {
        Notification notification = MockDataProvider.getMockNotifications(null).get(0);
        notification.setRead(true);
        return api(notification);
    }

    @Override
    public Call<ApiResponse<Void>> deleteNotification(int notificationId) {
        return api(null);
    }

    @Override
    public Call<ApiResponse<List<Home>>> getHomes() {
        return api(MockDataProvider.getMockHomes());
    }

    @Override
    public Call<ApiResponse<Home>> getPrimaryHome() {
        return api(MockDataProvider.getMockHomes().get(0));
    }

    @Override
    public Call<ApiResponse<Home>> getHomeById(int homeId) {
        for (Home home : MockDataProvider.getMockHomes()) {
            if (home.getId() == homeId) {
                return api(home);
            }
        }
        return api(MockDataProvider.getMockHomes().get(0));
    }

    @Override
    public Call<ApiResponse<Home>> createHome(Map<String, Object> homeData) {
        Home home = new Home();
        home.setId(nextHomeId++);
        home.setUserId(1);
        home.setName(value(homeData, "name", "Demo Home"));
        home.setAddress(value(homeData, "address", "701 7th Ave, New York, 10036, USA"));
        home.setCountry(value(homeData, "country", "United States"));
        home.setLatitude(doubleValue(homeData, "latitude", 40.7128));
        home.setLongitude(doubleValue(homeData, "longitude", -74.0060));
        home.setPrimary(true);
        return api(home);
    }

    @Override
    public Call<ApiResponse<Home>> updateHome(int homeId, Map<String, Object> homeData) {
        Home home = MockDataProvider.getMockHomes().get(0);
        home.setId(homeId);
        home.setName(value(homeData, "name", home.getName()));
        return api(home);
    }

    @Override
    public Call<ApiResponse<Void>> deleteHome(int homeId) {
        return api(null);
    }

    @Override
    public Call<ApiResponse<Home>> joinHome(Map<String, Object> joinData) {
        return api(MockDataProvider.getMockHomes().get(1));
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> sendHomeInvitation(int homeId, Map<String, Object> invitationData) {
        Map<String, Object> data = invitationData != null ? invitationData : successMap();
        data.put("homeId", homeId);
        data.put("status", "sent");
        return api(data);
    }

    @Override
    public Call<ApiResponse<List<HomeMember>>> getHomeMembers(int homeId) {
        List<HomeMember> members = new ArrayList<>();
        HomeMember owner = new HomeMember(1, homeId, 1, "owner");
        owner.setUser(demoUser("demo@smartify.com"));
        members.add(owner);
        HomeMember admin = new HomeMember(2, homeId, 2, "admin");
        admin.setUser(demoUser("alex@smartify.com"));
        admin.getUser().setFirstName("Alex");
        admin.getUser().setLastName("Kim");
        members.add(admin);
        return api(members);
    }

    @Override
    public Call<ApiResponse<HomeMember>> getMemberById(int homeId, int memberId) {
        HomeMember member = new HomeMember(memberId, homeId, memberId, memberId == 1 ? "owner" : "member");
        member.setUser(demoUser(memberId == 1 ? "demo@smartify.com" : "member@smartify.com"));
        return api(member);
    }

    @Override
    public Call<ApiResponse<HomeMember>> addHomeMember(int homeId, Map<String, Object> memberData) {
        HomeMember member = new HomeMember(20, homeId, 20, value(memberData, "role", "member"));
        member.setUser(demoUser(value(memberData, "email", "guest@smartify.com")));
        return api(member);
    }

    @Override
    public Call<ApiResponse<HomeMember>> updateHomeMember(int homeId, int memberId, Map<String, Object> memberData) {
        HomeMember member = new HomeMember(memberId, homeId, memberId, value(memberData, "role", "member"));
        member.setUser(demoUser("member@smartify.com"));
        return api(member);
    }

    @Override
    public Call<ApiResponse<Void>> removeHomeMember(int homeId, int memberId) {
        return api(null);
    }

    @Override
    public Call<ApiResponse<Room>> getRoomById(int roomId) {
        for (Room room : MockDataProvider.getMockRooms(null)) {
            if (room.getId() == roomId) {
                return api(room);
            }
        }
        return api(new Room(roomId, "Demo Room"));
    }

    @Override
    public Call<ApiResponse<Room>> updateRoom(int roomId, Map<String, Object> roomData) {
        Room room = new Room(roomId, value(roomData, "name", "Updated Room"));
        room.setHomeId(intValue(roomData, "homeId", 1));
        return api(room);
    }

    @Override
    public Call<ApiResponse<Void>> deleteRoom(int roomId) {
        return api(null);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> controlDevicePower(int deviceId, Map<String, Boolean> powerData) {
        Map<String, Object> data = deviceState();
        data.put("deviceId", deviceId);
        if (powerData != null && powerData.containsKey("isOn")) {
            data.put("power", powerData.get("isOn"));
        }
        return api(data);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> controlLamp(int deviceId, Map<String, Object> lampSettings) {
        return api(withDeviceId(lampSettings, deviceId));
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> controlCamera(int deviceId, Map<String, Object> cameraCommand) {
        return api(withDeviceId(cameraCommand, deviceId));
    }

    @Override
    public Call<ApiResponse<Map<String, String>>> getCameraStream(int deviceId) {
        Map<String, String> data = new HashMap<>();
        data.put("streamUrl", "mock://camera/" + deviceId + "/live");
        return api(data);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> controlSpeaker(int deviceId, Map<String, Object> speakerSettings) {
        return api(withDeviceId(speakerSettings, deviceId));
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> controlAC(int deviceId, Map<String, Object> acSettings) {
        return api(withDeviceId(acSettings, deviceId));
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> getDeviceState(int deviceId) {
        return api(withDeviceId(deviceState(), deviceId));
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> executeDeviceCommand(int deviceId, Map<String, Object> commandData) {
        return api(withDeviceId(commandData, deviceId));
    }

    @Override
    public Call<ApiResponse<List<Device>>> getDevicesByCategory(String category, Integer roomId) {
        return api(MockDataProvider.getMockDevicesForScene(category != null ? category : "all", roomId));
    }

    @Override
    public Call<ApiResponse<VersionCheckResponse>> checkAppVersion(Map<String, Object> versionData) {
        VersionCheckResponse response = new VersionCheckResponse();
        VersionCheckResponse.VersionInfo info = new VersionCheckResponse.VersionInfo();
        info.setVersionName("1.0.2");
        info.setVersionCode(2);
        response.setCurrentVersion(info);
        response.setMinimumRequiredVersion("1.0.0");
        response.setUpdateAvailable(false);
        response.setUpdateRequired(false);
        response.setMessage("Mockup build is up to date.");
        return api(response);
    }

    @Override
    public Call<ApiResponse<List<SmartScene>>> getScenes(String type, Integer homeId) {
        return api(MockDataProvider.getMockScenes(type));
    }

    @Override
    public Call<ApiResponse<SmartScene>> getSceneById(int sceneId) {
        for (SmartScene scene : MockDataProvider.getMockScenes("automation")) {
            if (scene.getId() != null && scene.getId() == sceneId) {
                return api(scene);
            }
        }
        for (SmartScene scene : MockDataProvider.getMockScenes("tap_to_run")) {
            if (scene.getId() != null && scene.getId() == sceneId) {
                return api(scene);
            }
        }
        return api(MockDataProvider.getMockScenes("automation").get(0));
    }

    @Override
    public Call<ApiResponse<SmartScene>> createScene(Map<String, Object> sceneData) {
        SmartScene scene = new SmartScene();
        scene.setName(value(sceneData, "name", "Mock Scene"));
        scene.setType(value(sceneData, "type", "automation"));
        scene.setHomeId(intValue(sceneData, "homeId", 1));
        scene.setEnabled(true);
        scene.setColor(value(sceneData, "color", "#405FF2"));
        scene.setIcon(value(sceneData, "icon", "ic_sun"));
        MockDataProvider.addDynamicScene(scene);
        return api(scene);
    }

    @Override
    public Call<ApiResponse<SmartScene>> updateScene(int sceneId, Map<String, Object> sceneData) {
        SmartScene scene = MockDataProvider.getMockScenes(value(sceneData, "type", "automation")).get(0);
        scene.setId(sceneId);
        if (sceneData != null && sceneData.containsKey("enabled")) {
            scene.setEnabled(Boolean.TRUE.equals(sceneData.get("enabled")));
        }
        return api(scene);
    }

    @Override
    public Call<ApiResponse<Void>> deleteScene(int sceneId) {
        return api(null);
    }

    @Override
    public Call<ApiResponse<Map<String, Object>>> executeScene(int sceneId) {
        Map<String, Object> data = successMap();
        data.put("sceneId", sceneId);
        data.put("executedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        return api(data);
    }

    @Override
    public Call<ApiResponse<Void>> updateSceneOrder(Map<String, Object> orderData) {
        return api(null);
    }

    @Override
    public Call<ApiResponse<List<SceneLog>>> getSceneLogs(int sceneId, Integer page, Integer limit) {
        List<SceneLog> logs = new ArrayList<>();
        logs.add(new SceneLog(1, sceneId, "Morning Routine", "succeeded", new Date()));
        logs.add(new SceneLog(2, sceneId, "Evening Routine", "succeeded", new Date(System.currentTimeMillis() - 3600000)));
        return api(logs);
    }

    private Device findDevice(int deviceId) {
        for (Device device : MockDataProvider.getMockDevicesForScene("all", null)) {
            if (device.getId() != null && device.getId() == deviceId) {
                return device;
            }
        }
        return null;
    }

    private Device createDeviceObject(int id, String name, String type, int roomId) {
        Device device = new Device(id, name, type);
        device.setRoomId(roomId);
        device.setStatus("online");
        device.setOnline(true);
        device.setOn(true);
        device.setMetadata(deviceState());
        return device;
    }

    private MonthlySummary month(String month, double kwh, double cost) {
        MonthlySummary summary = new MonthlySummary();
        summary.setMonth(month);
        summary.setConsumptionKwh(kwh);
        summary.setCostUsd(cost);
        return summary;
    }

    private DeviceConsumptionSummary consumption(int id, String name, String type, int roomId, String roomName, double kwh, double cost, int count) {
        DeviceConsumptionSummary summary = new DeviceConsumptionSummary();
        summary.setDeviceId(id);
        summary.setDeviceName(name);
        summary.setDeviceType(type);
        summary.setRoomId(roomId);
        summary.setRoomName(roomName);
        summary.setTotalConsumptionKwh(kwh);
        summary.setTotalCostUsd(cost);
        summary.setDeviceCount(count);
        return summary;
    }

    private ChatbotMessage chatMessage(int id, String role, String message) {
        ChatbotMessage chat = new ChatbotMessage();
        chat.setId(id);
        chat.setUserId(1);
        chat.setRole(role);
        chat.setMessage(message);
        chat.setCreatedAt(new Date());
        return chat;
    }

    private Map<String, Object> settings() {
        Map<String, Object> data = new HashMap<>();
        data.put("notifications", true);
        data.put("darkMode", true);
        data.put("language", "English");
        return data;
    }

    private Map<String, Object> successMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("mockupMode", true);
        return data;
    }

    private Map<String, Object> deviceState() {
        Map<String, Object> data = new HashMap<>();
        data.put("power", true);
        data.put("brightness", 72);
        data.put("temperature", 22);
        data.put("volume", 38);
        data.put("mode", "auto");
        data.put("online", true);
        return data;
    }

    private Map<String, Object> withDeviceId(Map<String, Object> data, int deviceId) {
        Map<String, Object> result = data != null ? new HashMap<>(data) : successMap();
        result.put("deviceId", deviceId);
        result.put("mockupMode", true);
        return result;
    }

    private String assistantName(int id) {
        switch (id) {
            case 2:
                return "Amazon Alexa";
            case 3:
                return "Apple Siri";
            default:
                return "Google Assistant";
        }
    }

    private String value(Map<String, ?> map, String key, String fallback) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return fallback;
        }
        return String.valueOf(map.get(key));
    }

    private int intValue(Map<String, ?> map, String key, int fallback) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return fallback;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double doubleValue(Map<String, ?> map, String key, double fallback) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) {
            return fallback;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
