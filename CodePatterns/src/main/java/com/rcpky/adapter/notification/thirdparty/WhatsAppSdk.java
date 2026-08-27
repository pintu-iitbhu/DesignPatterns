package com.rcpky.adapter.notification.thirdparty;

import java.util.Map;

public class WhatsAppSdk {

    public WhatsAppResponse sendMessage(
            String phoneNumber,
            String templateName,
            Map<String, String> parameters
    ) {
        // External SDK
        System.out.println("Processing whatsapp message");
        return new WhatsAppResponse("transactionId", true, "whatsapp message success");
    }
}
