package com.rcpky.structural.adapter.notification;

import java.util.Map;

public class Notification {

    private final String recipient;
    private final String subject;
    private final String message;
    private final Map<String, String> parameters;
    private final String phoneNumber;
    private final NotificationChannel channel;

    public  Notification(String recipient, String subject, String message, Map<String, String> parameters, String phoneNumber, NotificationChannel channel) {
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.parameters = parameters;
        this.phoneNumber = phoneNumber;
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public NotificationChannel getChannel() {
        return channel;
    }
    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
