package com.rcpky.structural.bridgedesign.notificationdelivery.delivery_type;

import com.rcpky.structural.bridgedesign.notificationdelivery.NotificationDelivery;

public class EmailDelivery implements NotificationDelivery {

    @Override
    public void deliver(String recipient, String message) {
        System.out.println(
                "Sending EMAIL to " + recipient
                        + ": " + message
        );
    }
}
