package com.rcpky.abstractfactory.cloud.azure;

import com.rcpky.abstractfactory.cloud.MessageQueue;

public class AzureServiceBus implements MessageQueue {

    @Override
    public void publish(String message) {
        System.out.println(
                "Publishing to Azure Service Bus: " + message
        );
    }
}