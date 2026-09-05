package com.rcpky.structural.adapter.notification;

import com.rcpky.structural.adapter.notification.thirdparty.SendGridSdk;
import com.rcpky.structural.adapter.notification.thirdparty.TwilioSdk;
import com.rcpky.structural.adapter.notification.thirdparty.WhatsAppSdk;

public class NotificationSystem implements NotificationSender{

    private SendGridSdk sendGridSdk;
    private TwilioSdk twilioSdk;
    private WhatsAppSdk whatsAppSdk;

    public NotificationSystem(SendGridSdk sendGridSdk,TwilioSdk twilioSdk,WhatsAppSdk whatsAppSdk ) {
        this.sendGridSdk = sendGridSdk;
        this.twilioSdk = twilioSdk;
        this.whatsAppSdk = whatsAppSdk;
    }

    @Override
    public NotificationResult send(Notification notification) {
        switch (notification.getChannel()) {
            case EMAIL:
                return NotificationResult.fromSendGridResponse(this.sendGridSdk.sendEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getMessage()
                ));
            case SMS:
                return NotificationResult.fromTwilioResponse(this.twilioSdk.sendSms(
                        notification.getPhoneNumber(),
                        notification.getMessage()
                ));
            case WHATSAPP:
                return NotificationResult.fromWhatsAppResponse(this.whatsAppSdk.sendMessage(
                        notification.getPhoneNumber(),
                        notification.getMessage(),
                        notification.getParameters()

                ));
            default:
                throw new IllegalArgumentException("Invalid notification channel");

        }
    }
}
