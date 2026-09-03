package com.rcpky.bridgedesign.notificationdelivery.delivery_type;

import com.rcpky.bridgedesign.notificationdelivery.NotificationDelivery;

public class PushDelivery implements NotificationDelivery {

    @Override
    public void deliver(String recipient, String message) {
        System.out.println(
                "Sending PUSH notification to "
                        + recipient + ": " + message
        );
    }
}
