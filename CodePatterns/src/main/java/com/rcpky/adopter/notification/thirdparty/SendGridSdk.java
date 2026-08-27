package com.rcpky.adopter.notification.thirdparty;

public class SendGridSdk {
    public SendGridResponse sendEmail(
            String emailAddress,
            String subject,
            String htmlContent
    ) {
        // External SDK
        System.out.println("Processing sendgrid email");
        return new SendGridResponse("transactionId", true, "sendgrid email success");
    }
}
