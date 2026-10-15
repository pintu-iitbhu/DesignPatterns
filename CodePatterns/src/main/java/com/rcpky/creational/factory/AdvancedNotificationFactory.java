package com.rcpky.creational.factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

class AdvancedNotificationFactory {
    // Thread-safe map storing the creation logic (Suppliers) instead of hardcoded switch cases
    private static final Map<String, Supplier<Notification>> registry = new ConcurrentHashMap<>();

    // Register new products dynamically without modifying the factory class (Open/Closed Principle)
    public static void registerProduct(String type, Supplier<Notification> supplier) {
        registry.put(type.toUpperCase(), supplier);
    }

    // Static initializer to populate defaults
    static {
        registerProduct("EMAIL", EmailNotification::new);
        registerProduct("SMS", SMSNotification::new);
    }

    // Thread-safe, highly performant creation method
    public static Notification createNotification(String type) {
        Supplier<Notification> supplier = registry.get(type.toUpperCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown notification type: " + type);
        }
        return supplier.get(); // Instantiates instantly
    }
}

