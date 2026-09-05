package com.rcpky.structural.proxy.documentservice;

public class User {

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    private final String userId;
    private final String role;

    public User(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    // constructor + getters
}
