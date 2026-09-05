package com.rcpky.structural.bridgedesign.notificationdelivery.notification_type;

import com.rcpky.structural.bridgedesign.notificationdelivery.NotificationDelivery;
import com.rcpky.structural.bridgedesign.notificationdelivery.Notification;

public class AlertNotification extends Notification {


    public AlertNotification(
            NotificationDelivery delivery
    ) {
        super(delivery);
    }

    @Override
    public void notify(
            String recipient,
            String message
    ) {

        String formattedMessage =
                "[ALERT] " + message;

        delivery.deliver(
                recipient,
                formattedMessage
        );
    }
}
