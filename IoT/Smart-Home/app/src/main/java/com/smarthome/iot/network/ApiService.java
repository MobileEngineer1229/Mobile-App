package com.smarthome.iot.network;

import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.LoginRequest;
import com.smarthome.iot.models.LoginResponse;
import com.smarthome.iot.models.Room;
import com.smarthome.iot.models.MonthlySummaryResponse;
import com.smarthome.iot.models.StatisticsResponse;
import com.smarthome.iot.models.DeviceConsumptionResponse;
import com.smarthome.iot.models.ForgotPasswordRequest;
import com.smarthome.iot.models.ForgotPasswordResponse;
import com.smarthome.iot.models.VerifyOTPRequest;
import com.smarthome.iot.models.VerifyOTPResponse;
import com.smarthome.iot.models.ResetPasswordRequest;
import com.smarthome.iot.models.ResetPasswordResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import com.smarthome.iot.models.HealthResponse;
import java.util.List;
import java.util.Map;

public interface ApiService {
    // Health Check (no auth required)
    @GET("health")
    retrofit2.Call<HealthResponse> checkHealth();
    // Authentication
    @POST("users/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("users/signup")
    Call<com.smarthome.iot.models.SignupResponse> signup(@Body com.smarthome.iot.models.SignupRequest request);

    // Password Reset
    @POST("users/forgot-password")
    Call<ApiResponse<ForgotPasswordResponse>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("users/verify-otp")
    Call<ApiResponse<VerifyOTPResponse>> verifyOTP(@Body VerifyOTPRequest request);

    @POST("users/reset-password")
    Call<ApiResponse<ResetPasswordResponse>> resetPassword(@Body ResetPasswordRequest request);

    // Rooms
    @GET("rooms")
    Call<ApiResponse<List<Room>>> getRooms(@Query("homeId") Integer homeId);

    @POST("rooms")
    Call<ApiResponse<Room>> createRoom(@Body Map<String, Object> roomData);

    // Devices
    @GET("devices")
    Call<ApiResponse<List<Device>>> getDevices(
            @Query("roomId") Integer roomId,
            @Query("homeId") Integer homeId,
            @Query("status") String status,
            @Query("type") String type,
            @Query("page") Integer page,
            @Query("limit") Integer limit
    );

    @GET("devices/{id}")
    Call<ApiResponse<Device>> getDeviceById(@Path("id") int deviceId);

    @POST("devices")
    Call<ApiResponse<Device>> createDevice(@Body Map<String, Object> deviceData);

    @PUT("devices/{id}")
    Call<ApiResponse<Device>> updateDevice(@Path("id") int deviceId, @Body Map<String, Object> deviceData);

    // Device Discovery
    @GET("devices/discover")
    Call<ApiResponse<Object>> discoverDevices();

    // MQTT Device Verification
    @POST("mqtt/verify-device")
    Call<ApiResponse<Object>> verifyMqttDevice(@Body Map<String, Object> body);

    // MQTT Device Registration - registers device and creates EMQX credentials
    @POST("mqtt/register-device")
    Call<ApiResponse<Map<String, Object>>> registerMqttDevice(@Body Map<String, Object> body);

    // MQTT Send Provisioning Data to IoT Device via FastBee
    @POST("mqtt/provision-device")
    Call<ApiResponse<Object>> provisionMqttDevice(@Body Map<String, Object> body);

    // MQTT Device Commands
    @POST("mqtt/command")
    Call<ApiResponse<Object>> sendDeviceCommand(@Body Map<String, Object> body);

    // MQTT Device States
    @GET("mqtt/device-states/{deviceId}")
    Call<ApiResponse<Object>> getDeviceStates(@Path("deviceId") int deviceId);

    // MQTT Command History
    @GET("mqtt/command-history/{deviceId}")
    Call<ApiResponse<Object>> getCommandHistory(@Path("deviceId") int deviceId);

    // Device Types
    @GET("devices/types")
    Call<ApiResponse<Object>> getDeviceTypes(@Query("category") String category);

    // Reports
    @GET("reports/monthly-summary")
    Call<ApiResponse<MonthlySummaryResponse>> getMonthlySummary();

    @GET("reports/statistics")
    Call<ApiResponse<StatisticsResponse>> getStatistics(
            @Query("dateRange") String dateRange,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @GET("reports/devices")
    Call<ApiResponse<DeviceConsumptionResponse>> getDeviceConsumption(
            @Query("dateRange") String dateRange,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("deviceId") Integer deviceId,
            @Query("deviceType") String deviceType,
            @Query("roomId") Integer roomId,
            @Query("groupBy") String groupBy
    );

    @GET("reports/devices/{type}/details")
    Call<ApiResponse<Object>> getDeviceTypeDetails(
            @Path("type") String deviceType,
            @Query("dateRange") String dateRange,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    // User Profile
    @GET("users/profile")
    Call<ApiResponse<com.smarthome.iot.models.User>> getProfile();

    @PUT("users/profile")
    Call<ApiResponse<com.smarthome.iot.models.User>> updateProfile(@Body Map<String, Object> profileData);

    // User Settings
    @GET("users/settings")
    Call<ApiResponse<Map<String, Object>>> getSettings();

    @PUT("users/settings")
    Call<ApiResponse<Map<String, Object>>> updateSettings(@Body Map<String, Object> settingsData);

    // Logout
    @POST("users/logout")
    Call<ApiResponse<Void>> logout();

    // Voice Assistants
    @GET("voice-assistants")
    Call<ApiResponse<List<com.smarthome.iot.models.VoiceAssistant>>> getVoiceAssistants();

    @POST("voice-assistants/{id}/link")
    Call<ApiResponse<com.smarthome.iot.models.VoiceAssistant>> linkVoiceAssistant(@Path("id") int assistantId);

    @POST("voice-assistants/{id}/unlink")
    Call<ApiResponse<Void>> unlinkVoiceAssistant(@Path("id") int assistantId);

    // Notification Preferences
    @GET("users/notifications/preferences")
    Call<ApiResponse<List<com.smarthome.iot.models.NotificationPreference>>> getNotificationPreferences();

    @PUT("users/notifications/preferences")
    Call<ApiResponse<Map<String, Object>>> updateNotificationPreferences(@Body Map<String, Object> preferences);

    @PUT("users/notifications/preferences/{type}")
    Call<ApiResponse<Map<String, Object>>> updateNotificationPreference(@Path("type") String type, @Body Map<String, Object> preference);

    // Security Settings
    @GET("users/security/settings")
    Call<ApiResponse<Map<String, Object>>> getSecuritySettings();

    @PUT("users/security/settings")
    Call<ApiResponse<Map<String, Object>>> updateSecuritySettings(@Body Map<String, Object> settings);

    @PUT("users/security/settings/{type}")
    Call<ApiResponse<Map<String, Object>>> updateSecuritySetting(@Path("type") String type, @Body Map<String, Object> setting);

    // Account Management
    @POST("users/change-password")
    Call<ApiResponse<Void>> changePassword(@Body Map<String, String> passwordData);

    @POST("users/deactivate")
    Call<ApiResponse<Void>> deactivateAccount();

    @POST("users/delete")
    Call<ApiResponse<Void>> deleteAccount();

    // Linked Accounts
    @GET("users/linked-accounts")
    Call<ApiResponse<List<com.smarthome.iot.models.LinkedAccount>>> getLinkedAccounts();

    @POST("users/linked-accounts/{provider}/link")
    Call<ApiResponse<com.smarthome.iot.models.LinkedAccount>> linkAccount(@Path("provider") String provider);

    @POST("users/linked-accounts/{id}/unlink")
    Call<ApiResponse<Void>> unlinkAccount(@Path("id") int accountId);

    // Additional Settings
    @GET("users/additional-settings")
    Call<ApiResponse<Map<String, Object>>> getAdditionalSettings();

    @PUT("users/additional-settings")
    Call<ApiResponse<Map<String, Object>>> updateAdditionalSettings(@Body Map<String, Object> settings);

    // Data & Analytics
    @GET("users/data-analytics")
    Call<ApiResponse<Map<String, Object>>> getDataAnalytics();

    @POST("users/data-analytics/download")
    Call<ApiResponse<Map<String, Object>>> downloadUserData();

    // App Appearance
    @GET("users/app-appearance")
    Call<ApiResponse<Map<String, Object>>> getAppAppearance();

    @PUT("users/app-appearance")
    Call<ApiResponse<Map<String, Object>>> updateAppAppearance(@Body Map<String, Object> appearance);

    // Chatbot
    @POST("chatbot/message")
    Call<ApiResponse<com.smarthome.iot.models.ChatbotResponse>> sendChatbotMessage(@Body Map<String, String> messageData);

    @GET("chatbot/history")
    Call<ApiResponse<com.smarthome.iot.models.ChatHistoryResponse>> getChatHistory(
            @Query("page") Integer page,
            @Query("limit") Integer limit
    );

    @DELETE("chatbot/history")
    Call<ApiResponse<Void>> clearChatHistory();

    // Notifications
    @GET("notifications")
    Call<ApiResponse<List<com.smarthome.iot.models.Notification>>> getNotifications(
            @Query("page") Integer page,
            @Query("limit") Integer limit,
            @Query("type") String type,
            @Query("category") String category,
            @Query("isRead") Boolean isRead
    );

    @GET("notifications/stats")
    Call<ApiResponse<com.smarthome.iot.models.NotificationStats>> getNotificationStats();

    @PUT("notifications/read-all")
    Call<ApiResponse<Void>> markAllNotificationsAsRead();

    @GET("notifications/{id}")
    Call<ApiResponse<com.smarthome.iot.models.Notification>> getNotificationById(@Path("id") int notificationId);

    @PUT("notifications/{id}/read")
    Call<ApiResponse<com.smarthome.iot.models.Notification>> markNotificationAsRead(@Path("id") int notificationId);

    @DELETE("notifications/{id}")
    Call<ApiResponse<Void>> deleteNotification(@Path("id") int notificationId);

    // Homes (Location Management)
    @GET("homes")
    Call<ApiResponse<List<com.smarthome.iot.models.Home>>> getHomes();

    @GET("homes/primary")
    Call<ApiResponse<com.smarthome.iot.models.Home>> getPrimaryHome();

    @GET("homes/{id}")
    Call<ApiResponse<com.smarthome.iot.models.Home>> getHomeById(@Path("id") int homeId);

    @POST("homes")
    Call<ApiResponse<com.smarthome.iot.models.Home>> createHome(@Body Map<String, Object> homeData);

    @PUT("homes/{id}")
    Call<ApiResponse<com.smarthome.iot.models.Home>> updateHome(@Path("id") int homeId, @Body Map<String, Object> homeData);

    @DELETE("homes/{id}")
    Call<ApiResponse<Void>> deleteHome(@Path("id") int homeId);

    @POST("homes/join")
    Call<ApiResponse<com.smarthome.iot.models.Home>> joinHome(@Body Map<String, Object> joinData);

    @POST("homes/{id}/invitations")
    Call<ApiResponse<Map<String, Object>>> sendHomeInvitation(@Path("id") int homeId, @Body Map<String, Object> invitationData);

    // Home Members
    @GET("homes/{id}/members")
    Call<ApiResponse<List<com.smarthome.iot.models.HomeMember>>> getHomeMembers(@Path("id") int homeId);

    @GET("homes/{id}/members/{memberId}")
    Call<ApiResponse<com.smarthome.iot.models.HomeMember>> getMemberById(@Path("id") int homeId, @Path("memberId") int memberId);

    @POST("homes/{id}/members")
    Call<ApiResponse<com.smarthome.iot.models.HomeMember>> addHomeMember(@Path("id") int homeId, @Body Map<String, Object> memberData);

    @PUT("homes/{id}/members/{memberId}")
    Call<ApiResponse<com.smarthome.iot.models.HomeMember>> updateHomeMember(@Path("id") int homeId, @Path("memberId") int memberId, @Body Map<String, Object> memberData);

    @DELETE("homes/{id}/members/{memberId}")
    Call<ApiResponse<Void>> removeHomeMember(@Path("id") int homeId, @Path("memberId") int memberId);

    // Rooms - Additional endpoints
    @GET("rooms/{id}")
    Call<ApiResponse<Room>> getRoomById(@Path("id") int roomId);

    @PUT("rooms/{id}")
    Call<ApiResponse<Room>> updateRoom(@Path("id") int roomId, @Body Map<String, Object> roomData);

    @DELETE("rooms/{id}")
    Call<ApiResponse<Void>> deleteRoom(@Path("id") int roomId);

    // Device Control
    @POST("devices/{id}/control/power")
    Call<ApiResponse<Map<String, Object>>> controlDevicePower(@Path("id") int deviceId, @Body Map<String, Boolean> powerData);

    @POST("devices/{id}/control/lamp")
    Call<ApiResponse<Map<String, Object>>> controlLamp(@Path("id") int deviceId, @Body Map<String, Object> lampSettings);

    @POST("devices/{id}/control/camera")
    Call<ApiResponse<Map<String, Object>>> controlCamera(@Path("id") int deviceId, @Body Map<String, Object> cameraCommand);

    @GET("devices/{id}/control/camera/stream")
    Call<ApiResponse<Map<String, String>>> getCameraStream(@Path("id") int deviceId);

    @POST("devices/{id}/control/speaker")
    Call<ApiResponse<Map<String, Object>>> controlSpeaker(@Path("id") int deviceId, @Body Map<String, Object> speakerSettings);

    @POST("devices/{id}/control/ac")
    Call<ApiResponse<Map<String, Object>>> controlAC(@Path("id") int deviceId, @Body Map<String, Object> acSettings);

    @GET("devices/{id}/control/state")
    Call<ApiResponse<Map<String, Object>>> getDeviceState(@Path("id") int deviceId);

    // Execute device command (unified command interface)
    @POST("devices/{id}/command")
    Call<ApiResponse<Map<String, Object>>> executeDeviceCommand(
            @Path("id") int deviceId,
            @Body Map<String, Object> commandData
    );

    // Get devices by category
    @GET("devices/category/{category}")
    Call<ApiResponse<List<com.smarthome.iot.models.Device>>> getDevicesByCategory(
            @Path("category") String category,
            @Query("roomId") Integer roomId
    );

    // App Version Check
    @POST("app/version/check")
    Call<ApiResponse<com.smarthome.iot.models.VersionCheckResponse>> checkAppVersion(@Body Map<String, Object> versionData);

    // Smart Scenes
    @GET("scenes")
    Call<ApiResponse<List<com.smarthome.iot.models.SmartScene>>> getScenes(
            @Query("type") String type,
            @Query("homeId") Integer homeId
    );

    @GET("scenes/{id}")
    Call<ApiResponse<com.smarthome.iot.models.SmartScene>> getSceneById(@Path("id") int sceneId);

    @POST("scenes")
    Call<ApiResponse<com.smarthome.iot.models.SmartScene>> createScene(@Body Map<String, Object> sceneData);

    @PUT("scenes/{id}")
    Call<ApiResponse<com.smarthome.iot.models.SmartScene>> updateScene(@Path("id") int sceneId, @Body Map<String, Object> sceneData);

    @DELETE("scenes/{id}")
    Call<ApiResponse<Void>> deleteScene(@Path("id") int sceneId);

    @POST("scenes/{id}/execute")
    Call<ApiResponse<Map<String, Object>>> executeScene(@Path("id") int sceneId);

    @POST("scenes/reorder")
    Call<ApiResponse<Void>> updateSceneOrder(@Body Map<String, Object> orderData);

    @GET("scenes/{id}/logs")
    Call<ApiResponse<List<com.smarthome.iot.models.SceneLog>>> getSceneLogs(
            @Path("id") int sceneId,
            @Query("page") Integer page,
            @Query("limit") Integer limit
    );
}

