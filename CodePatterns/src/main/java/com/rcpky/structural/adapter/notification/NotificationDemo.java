package com.rcpky.structural.adapter.notification;

import com.rcpky.structural.adapter.notification.thirdparty.SendGridSdk;
import com.rcpky.structural.adapter.notification.thirdparty.TwilioSdk;
import com.rcpky.structural.adapter.notification.thirdparty.WhatsAppSdk;

import java.util.HashMap;

public class NotificationDemo {
    public  static void main(String[] args) {
        NotificationSystem notificationSystem = new NotificationSystem(
                new SendGridSdk(),
                new TwilioSdk(),
                new WhatsAppSdk()
        );

        Notification notification = new Notification(
                "recipient",
                "subject",
                "message",
                new HashMap<>(),
                "phoneNumber",
                NotificationChannel.EMAIL
        );

        NotificationResult notificationResult = notificationSystem.send(notification);
        System.out.println(notificationResult.getErrorMessage());
    }
}
