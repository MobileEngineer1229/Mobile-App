package com.talentbaby.app.models;

import com.google.gson.annotations.SerializedName;

public class Recipe {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("min_age_months")
    private int minAgeMonths;

    @SerializedName("max_age_months")
    private int maxAgeMonths;

    @SerializedName("prep_time_minutes")
    private int prepTimeMinutes;

    @SerializedName("cook_time_minutes")
    private int cookTimeMinutes;

    @SerializedName("servings")
    private int servings;

    @SerializedName("ingredients")
    private String ingredients;

    @SerializedName("instructions")
    private String instructions;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("calories")
    private int calories;

    @SerializedName("nutrition_info")
    private String nutritionInfo;

    private transient int localImageResId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getMinAgeMonths() { return minAgeMonths; }
    public void setMinAgeMonths(int minAgeMonths) { this.minAgeMonths = minAgeMonths; }
    public int getMaxAgeMonths() { return maxAgeMonths; }
    public void setMaxAgeMonths(int maxAgeMonths) { this.maxAgeMonths = maxAgeMonths; }
    public int getPrepTimeMinutes() { return prepTimeMinutes; }
    public void setPrepTimeMinutes(int prepTimeMinutes) { this.prepTimeMinutes = prepTimeMinutes; }
    public int getCookTimeMinutes() { return cookTimeMinutes; }
    public void setCookTimeMinutes(int cookTimeMinutes) { this.cookTimeMinutes = cookTimeMinutes; }
    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }
    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }
    public String getNutritionInfo() { return nutritionInfo; }
    public void setNutritionInfo(String nutritionInfo) { this.nutritionInfo = nutritionInfo; }
    public int getLocalImageResId() { return localImageResId; }
    public void setLocalImageResId(int localImageResId) { this.localImageResId = localImageResId; }
}
