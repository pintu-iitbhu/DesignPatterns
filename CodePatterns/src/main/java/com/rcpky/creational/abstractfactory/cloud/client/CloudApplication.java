package com.rcpky.creational.abstractfactory.cloud.client;

import com.rcpky.creational.abstractfactory.cloud.Database;
import com.rcpky.creational.abstractfactory.cloud.MessageQueue;
import com.rcpky.creational.abstractfactory.cloud.Storage;

public class CloudApplication {

    private final Database database;
    private final Storage storage;
    private  final MessageQueue messageQueue;

    public CloudApplication(CloudFactory factory) {
        this.database = factory.createDatabase();
        this.storage = factory.createStorage();
        this.messageQueue = factory.createMessageQueue();
    }

    public void process() {
        storage.upload("report.pdf");

        database.save("Order Created");

        messageQueue.publish("ORDER_CREATED");
    }
}
