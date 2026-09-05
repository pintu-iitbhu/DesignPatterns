package com.rcpky.creational.abstractfactory.cloud.client;

import com.rcpky.creational.abstractfactory.cloud.Database;
import com.rcpky.creational.abstractfactory.cloud.MessageQueue;
import com.rcpky.creational.abstractfactory.cloud.Storage;

public interface CloudFactory {
    Storage createStorage();
    MessageQueue createMessageQueue();
    Database createDatabase();
}
