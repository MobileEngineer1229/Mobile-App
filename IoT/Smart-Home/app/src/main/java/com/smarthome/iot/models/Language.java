package com.smarthome.iot.models;

public class Language {
    private String code;
    private String name;
    private int flagResId;

    public Language() {
    }

    public Language(String code, String name, int flagResId) {
        this.code = code;
        this.name = name;
        this.flagResId = flagResId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFlagResId() {
        return flagResId;
    }

    public void setFlagResId(int flagResId) {
        this.flagResId = flagResId;
    }
}

