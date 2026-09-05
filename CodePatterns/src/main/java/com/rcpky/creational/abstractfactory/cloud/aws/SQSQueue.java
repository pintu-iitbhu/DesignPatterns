package com.rcpky.creational.abstractfactory.cloud.aws;

import com.rcpky.creational.abstractfactory.cloud.MessageQueue;

public class SQSQueue implements MessageQueue {

    @Override
    public void publish(String message) {
        System.out.println("Publishing to AWS SQS: " + message);
    }
}