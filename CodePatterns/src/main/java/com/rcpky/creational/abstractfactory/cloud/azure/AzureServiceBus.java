package com.rcpky.creational.abstractfactory.cloud.azure;

import com.rcpky.creational.abstractfactory.cloud.MessageQueue;

public class AzureServiceBus implements MessageQueue {

    @Override
    public void publish(String message) {
        System.out.println(
                "Publishing to Azure Service Bus: " + message
        );
    }
}