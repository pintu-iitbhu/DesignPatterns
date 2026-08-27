package com.rcpky.abstractfactory.cloud.aws;

import com.rcpky.abstractfactory.cloud.MessageQueue;

public class SQSQueue implements MessageQueue {

    @Override
    public void publish(String message) {
        System.out.println("Publishing to AWS SQS: " + message);
    }
}