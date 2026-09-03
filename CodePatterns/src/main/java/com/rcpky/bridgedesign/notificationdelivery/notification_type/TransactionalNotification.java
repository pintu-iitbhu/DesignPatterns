package com.rcpky.bridgedesign.notificationdelivery.notification_type;

import com.rcpky.bridgedesign.notificationdelivery.NotificationDelivery;
import com.rcpky.bridgedesign.notificationdelivery.Notification;

public class TransactionalNotification extends Notification {

    public TransactionalNotification(
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
                "[TRANSACTIONAL] " + message;

        delivery.deliver(
                recipient,
                formattedMessage
        );
    }
}
