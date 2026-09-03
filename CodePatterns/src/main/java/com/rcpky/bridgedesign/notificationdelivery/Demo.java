package com.rcpky.bridgedesign.notificationdelivery;

import com.rcpky.bridgedesign.notificationdelivery.delivery_type.EmailDelivery;
import com.rcpky.bridgedesign.notificationdelivery.delivery_type.PushDelivery;
import com.rcpky.bridgedesign.notificationdelivery.delivery_type.SmsDelivery;
import com.rcpky.bridgedesign.notificationdelivery.notification_type.AlertNotification;
import com.rcpky.bridgedesign.notificationdelivery.notification_type.MarketingNotification;
import com.rcpky.bridgedesign.notificationdelivery.notification_type.TransactionalNotification;

public class Demo {

    public  static void main(String[] args) {
        Notification transactionalEmail =
                new TransactionalNotification(
                        new EmailDelivery()
                );

        transactionalEmail.notify(
                "user@example.com",
                "Your payment was successful"
        );


        Notification alertSms =
                new AlertNotification(
                        new SmsDelivery()
                );

        alertSms.notify(
                "+919876543210",
                "Suspicious login detected"
        );


        Notification marketingPush =
                new MarketingNotification(
                        new PushDelivery()
                );

        marketingPush.notify(
                "device-token-123",
                "50% discount today!"
        );
    }
}
