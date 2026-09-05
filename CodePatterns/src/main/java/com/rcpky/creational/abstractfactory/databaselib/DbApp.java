package com.rcpky.creational.abstractfactory.databaselib;

import com.rcpky.creational.abstractfactory.databaselib.interfaces.Connection;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.QueryBuilder;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.Transaction;

public class DbApp {

    private final IDatabaseFactory factory;

    public DbApp(IDatabaseFactory factory) {
        this.factory = factory;
    }

    public void run() {
        Connection connection = factory.getConnection();
        QueryBuilder queryBuilder = factory.getQueryBuilder();
        Transaction transaction = factory.getTransaction();

        connection.connect();
        queryBuilder.build();
        transaction.begin();
        transaction.commit();
        connection.disconnect();
    }
}
