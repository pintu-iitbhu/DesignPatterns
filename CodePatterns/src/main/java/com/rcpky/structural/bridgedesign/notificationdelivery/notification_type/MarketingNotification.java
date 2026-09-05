package com.rcpky.structural.bridgedesign.notificationdelivery.notification_type;

import com.rcpky.structural.bridgedesign.notificationdelivery.NotificationDelivery;
import com.rcpky.structural.bridgedesign.notificationdelivery.Notification;

public class MarketingNotification extends Notification {

    public MarketingNotification(
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
                "[MARKETING] " + message;

        delivery.deliver(
                recipient,
                formattedMessage
        );
    }
}
