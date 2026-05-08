package com.smarthome.iot.models;

import java.util.Date;

public class SceneLog {
    private Integer id;
    private Integer sceneId;
    private String sceneName;
    private String status; // "succeeded" or "failed"
    private String errorMessage;
    private Date executionTimestamp;
    private Object metadata;

    public SceneLog() {
    }

    public SceneLog(Integer id, Integer sceneId, String sceneName, String status, Date executionTimestamp) {
        this.id = id;
        this.sceneId = sceneId;
        this.sceneName = sceneName;
        this.status = status;
        this.executionTimestamp = executionTimestamp;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getSceneId() { return sceneId; }
    public void setSceneId(Integer sceneId) { this.sceneId = sceneId; }

    public String getSceneName() { return sceneName; }
    public void setSceneName(String sceneName) { this.sceneName = sceneName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Date getExecutionTimestamp() { return executionTimestamp; }
    public void setExecutionTimestamp(Date executionTimestamp) { this.executionTimestamp = executionTimestamp; }

    public Object getMetadata() { return metadata; }
    public void setMetadata(Object metadata) { this.metadata = metadata; }
}
