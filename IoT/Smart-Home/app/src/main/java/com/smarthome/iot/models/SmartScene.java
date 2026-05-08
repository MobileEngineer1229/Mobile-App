package com.smarthome.iot.models;

import java.util.List;

public class SmartScene {
    private Integer id;
    private String name;
    private String type; // "automation" or "tap_to_run"
    private List<SceneCondition> conditions;
    private List<SceneTask> tasks;
    private String conditionLogic; // "any" or "all"
    private boolean isEnabled;
    private Integer homeId;
    private String icon; // Icon resource name
    private String color; // Color hex code

    public SmartScene() {
    }

    public SmartScene(Integer id, String name, String type, List<SceneCondition> conditions, List<SceneTask> tasks, boolean isEnabled) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.conditions = conditions;
        this.tasks = tasks;
        this.conditionLogic = "any";
        this.isEnabled = isEnabled;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<SceneCondition> getConditions() { return conditions; }
    public void setConditions(List<SceneCondition> conditions) { this.conditions = conditions; }

    public List<SceneTask> getTasks() { return tasks; }
    public void setTasks(List<SceneTask> tasks) { this.tasks = tasks; }

    public String getConditionLogic() { return conditionLogic; }
    public void setConditionLogic(String conditionLogic) { this.conditionLogic = conditionLogic; }

    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }

    public Integer getHomeId() { return homeId; }
    public void setHomeId(Integer homeId) { this.homeId = homeId; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
