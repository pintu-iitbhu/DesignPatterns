package com.rcpky.abstractfactory.cloud.azure;

import com.rcpky.abstractfactory.cloud.Database;
import com.rcpky.abstractfactory.cloud.MessageQueue;
import com.rcpky.abstractfactory.cloud.Storage;
import com.rcpky.abstractfactory.cloud.client.CloudFactory;

public class AzureCloudFactory implements CloudFactory {
    @Override
    public Storage createStorage() {
        return new AzureBlobStorage();
    }

    @Override
    public MessageQueue createMessageQueue() {
        return new AzureServiceBus();
    }

    @Override
    public Database createDatabase() {
        return new AzureSqlDatabase();
    }
}
