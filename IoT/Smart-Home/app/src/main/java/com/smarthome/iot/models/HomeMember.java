package com.smarthome.iot.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model class representing a home member
 */
public class HomeMember {
    @SerializedName("id")
    private int id;

    @SerializedName("home_id")
    private int homeId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("role")
    private String role;

    @SerializedName("joined_at")
    private String joinedAt;

    @SerializedName("created_at")
    private String createdAt;

    // User details (may be nested or flattened depending on API response)
    @SerializedName("user")
    private User user;

    @SerializedName("email")
    private String email;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("profile_picture")
    private String profilePicture;

    // Constructors
    public HomeMember() {}

    public HomeMember(int id, int homeId, int userId, String role) {
        this.id = id;
        this.homeId = homeId;
        this.userId = userId;
        this.role = role;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHomeId() {
        return homeId;
    }

    public void setHomeId(int homeId) {
        this.homeId = homeId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(String joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEmail() {
        // Try to get email from nested user object first
        if (user != null && user.getEmail() != null) {
            return user.getEmail();
        }
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        // Try to get from nested user object first
        if (user != null && user.getFirstName() != null) {
            return user.getFirstName();
        }
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        // Try to get from nested user object first
        if (user != null && user.getLastName() != null) {
            return user.getLastName();
        }
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getProfilePicture() {
        // Try to get from nested user object first
        if (user != null && user.getProfilePicture() != null) {
            return user.getProfilePicture();
        }
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    /**
     * Get full name combining first and last name
     */
    public String getFullName() {
        String first = getFirstName();
        String last = getLastName();
        
        if (first != null && last != null) {
            return first + " " + last;
        } else if (first != null) {
            return first;
        } else if (last != null) {
            return last;
        }
        return "Unknown";
    }

    /**
     * Get display email (truncated if too long)
     */
    public String getDisplayEmail() {
        String emailStr = getEmail();
        if (emailStr != null && emailStr.length() > 25) {
            return emailStr.substring(0, 22) + "...";
        }
        return emailStr;
    }

    /**
     * Check if this member is the owner
     */
    public boolean isOwner() {
        return "owner".equalsIgnoreCase(role);
    }

    /**
     * Check if this member is an admin
     */
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    /**
     * Get role display text (capitalized)
     */
    public String getRoleDisplayText() {
        if (role == null) return "Member";
        return role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
    }
}
