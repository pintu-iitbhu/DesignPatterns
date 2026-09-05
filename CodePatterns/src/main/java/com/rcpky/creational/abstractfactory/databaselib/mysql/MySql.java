package com.rcpky.creational.abstractfactory.databaselib.mysql;

import com.rcpky.creational.abstractfactory.databaselib.IDatabaseFactory;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.Connection;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.QueryBuilder;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.Transaction;

public class MySql {
    private final Connection connection;
    private final QueryBuilder queryBuilder;
    private final Transaction transaction;

    public MySql(IDatabaseFactory iDatabaseFactory) {
        this.connection = iDatabaseFactory.getConnection();
        this.queryBuilder = iDatabaseFactory.getQueryBuilder();
        this.transaction = iDatabaseFactory.getTransaction();
    }



}
