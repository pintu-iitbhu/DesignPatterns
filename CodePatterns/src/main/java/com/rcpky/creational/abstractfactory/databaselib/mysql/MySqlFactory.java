package com.rcpky.creational.abstractfactory.databaselib.mysql;

import com.rcpky.creational.abstractfactory.databaselib.IDatabaseFactory;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.Connection;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.QueryBuilder;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.Transaction;

public class MySqlFactory implements IDatabaseFactory {
    @Override
    public Connection getConnection() {
        return new MySqlConnection();
    }

    @Override
    public QueryBuilder getQueryBuilder() {
        return new MySqlQueryBuilder();
    }

    @Override
    public Transaction getTransaction() {
        return new MySqlTransaction();
    }
}
