package com.rcpky.adapter.notification.thirdparty;

public class TwilioSdk {

    public TwilioResponse sendSms(
            String phoneNumber,
            String message
    ) {
        // External SDK
        System.out.println("Processing twilio sms");
        return new TwilioResponse("transactionId", true, "twilio sms success");
    }
}
