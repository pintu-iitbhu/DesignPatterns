package com.rcpky.proxy.documentservice;

public class SecurityContext {

    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public static void setUser(User user) {
        currentUser.set(user);
    }

    public static User getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
