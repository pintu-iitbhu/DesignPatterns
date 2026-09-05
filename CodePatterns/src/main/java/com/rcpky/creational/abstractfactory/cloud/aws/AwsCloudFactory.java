package com.rcpky.creational.abstractfactory.cloud.aws;

import com.rcpky.creational.abstractfactory.cloud.Database;
import com.rcpky.creational.abstractfactory.cloud.MessageQueue;
import com.rcpky.creational.abstractfactory.cloud.Storage;
import com.rcpky.creational.abstractfactory.cloud.client.CloudFactory;

public class AwsCloudFactory implements CloudFactory {

    @Override
    public Storage createStorage() {
        return new S3Storage();
    }

    @Override
    public MessageQueue createMessageQueue() {
        return new SQSQueue();
    }

    @Override
    public Database createDatabase() {
        return new RDSDatabase();
    }
}
