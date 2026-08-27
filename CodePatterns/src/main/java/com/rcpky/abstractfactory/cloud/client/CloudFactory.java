package com.rcpky.abstractfactory.cloud.client;

import com.rcpky.abstractfactory.cloud.Database;
import com.rcpky.abstractfactory.cloud.MessageQueue;
import com.rcpky.abstractfactory.cloud.Storage;

public interface CloudFactory {
    Storage createStorage();
    MessageQueue createMessageQueue();
    Database createDatabase();
}
