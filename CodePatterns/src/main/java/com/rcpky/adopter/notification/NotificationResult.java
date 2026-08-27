package com.rcpky.adopter.notification;

import com.rcpky.adopter.notification.thirdparty.SendGridResponse;
import com.rcpky.adopter.notification.thirdparty.TwilioResponse;
import com.rcpky.adopter.notification.thirdparty.WhatsAppResponse;

public class NotificationResult {

    public boolean isSuccess() {
        return success;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private final boolean success;
    private final String providerMessageId;
    private final String errorMessage;

    public NotificationResult(boolean success, String providerMessageId, String errorMessage) {
        this.success = success;
        this.providerMessageId = providerMessageId;
        this.errorMessage = errorMessage;
    }

    public static NotificationResult fromSendGridResponse(SendGridResponse response) {
        return new NotificationResult(response.isSuccess(), response.getTransactionId(), response.getMessage());
    }

    public static NotificationResult fromTwilioResponse(TwilioResponse response) {
        return new NotificationResult(response.isSuccess(), response.getTransactionId(), response.getMessage());
    }

    public static NotificationResult fromWhatsAppResponse(WhatsAppResponse response) {
        return new NotificationResult(response.isSuccess(), response.getTransactionId(), response.getMessage());
    }


}
