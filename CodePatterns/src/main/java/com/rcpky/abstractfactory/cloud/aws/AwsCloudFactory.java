package com.rcpky.abstractfactory.cloud.aws;

import com.rcpky.abstractfactory.cloud.Database;
import com.rcpky.abstractfactory.cloud.MessageQueue;
import com.rcpky.abstractfactory.cloud.Storage;
import com.rcpky.abstractfactory.cloud.client.CloudFactory;

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
