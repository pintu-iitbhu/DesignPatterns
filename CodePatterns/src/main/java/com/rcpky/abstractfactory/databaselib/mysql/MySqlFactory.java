package com.rcpky.abstractfactory.databaselib.mysql;

import com.rcpky.abstractfactory.databaselib.IDatabaseFactory;
import com.rcpky.abstractfactory.databaselib.interfaces.Connection;
import com.rcpky.abstractfactory.databaselib.interfaces.QueryBuilder;
import com.rcpky.abstractfactory.databaselib.interfaces.Transaction;

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
