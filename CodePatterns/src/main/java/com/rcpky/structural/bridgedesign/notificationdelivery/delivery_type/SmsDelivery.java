package com.rcpky.structural.bridgedesign.notificationdelivery.delivery_type;

import com.rcpky.structural.bridgedesign.notificationdelivery.NotificationDelivery;

public class SmsDelivery implements NotificationDelivery {

    @Override
    public void deliver(String recipient, String message) {
        System.out.println(
                "Sending SMS to " + recipient
                        + ": " + message
        );
    }
}
