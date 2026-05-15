package com.talentbaby.app.models;

public class NutritionRecipeDisplay {
    private final String title;
    private final int imageResId;
    private final boolean locked;

    public NutritionRecipeDisplay(String title, int imageResId, boolean locked) {
        this.title = title;
        this.imageResId = imageResId;
        this.locked = locked;
    }

    public String getTitle() {
        return title;
    }

    public int getImageResId() {
        return imageResId;
    }

    public boolean isLocked() {
        return locked;
    }
}
