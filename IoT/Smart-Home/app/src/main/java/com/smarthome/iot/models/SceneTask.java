package com.smarthome.iot.models;

import java.io.Serializable;

public class SceneTask implements Serializable {
    private Integer id;
    private String type; // "control_device", "select_scene", "change_arm_mode", "send_notification", "delay"
    private Integer deviceId;
    private String deviceName;
    private String roomName;
    private String function; // "ON", "OFF", or device-specific function
    private Integer sceneId; // For "select_scene" type
    private String sceneName;
    private String armMode; // For "change_arm_mode" type
    private String notificationMessage; // For "send_notification" type
    private Integer delaySeconds; // For "delay" type

    public SceneTask() {
    }

    public SceneTask(String type) {
        this.type = type;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getFunction() { return function; }
    public void setFunction(String function) { this.function = function; }

    public Integer getSceneId() { return sceneId; }
    public void setSceneId(Integer sceneId) { this.sceneId = sceneId; }

    public String getSceneName() { return sceneName; }
    public void setSceneName(String sceneName) { this.sceneName = sceneName; }

    public String getArmMode() { return armMode; }
    public void setArmMode(String armMode) { this.armMode = armMode; }

    public String getNotificationMessage() { return notificationMessage; }
    public void setNotificationMessage(String notificationMessage) { this.notificationMessage = notificationMessage; }

    public Integer getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(Integer delaySeconds) { this.delaySeconds = delaySeconds; }

    public String getDisplayText() {
        switch (type) {
            case "control_device":
                return deviceName + "\n" + roomName + " - Function: " + function;
            case "select_scene":
                return sceneName != null ? sceneName : "Select Smart Scene";
            case "change_arm_mode":
                return "Arm Mode: " + armMode;
            case "send_notification":
                return "Send Notification";
            case "delay":
                if (delaySeconds != null) {
                    int hours = delaySeconds / 3600;
                    int minutes = (delaySeconds % 3600) / 60;
                    int seconds = delaySeconds % 60;
                    if (hours > 0) {
                        return "Delay the Action\n" + hours + "h " + minutes + "m " + seconds + "s";
                    } else if (minutes > 0) {
                        return "Delay the Action\n" + minutes + " min " + seconds + " secs";
                    } else {
                        return "Delay the Action\n" + seconds + " secs";
                    }
                }
                return "Delay the Action";
            default:
                return type;
        }
    }
}
