package com.rcpky.bridgedesign.notificationdelivery;

public abstract class Notification {
    protected final NotificationDelivery delivery;

    protected Notification(NotificationDelivery delivery) {
        this.delivery = delivery;
    }

    public abstract void notify(
            String recipient,
            String message
    );
}
