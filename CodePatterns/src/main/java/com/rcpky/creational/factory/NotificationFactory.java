package com.rcpky.creational.factory;

// 3. The Factory
class NotificationFactory {
    public Notification createNotification(String type) {
        if (type == null || type.isEmpty()) return null;

        switch (type.toUpperCase()) {
            case "EMAIL": return new EmailNotification();
            case "SMS":   return new SMSNotification();
            default: throw new IllegalArgumentException("Unknown type " + type);
        }
    }
}
