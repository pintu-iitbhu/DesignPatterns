package com.rcpky.abstractfactory.databaselib.postgre;

import com.rcpky.abstractfactory.databaselib.IDatabaseFactory;
import com.rcpky.abstractfactory.databaselib.interfaces.Connection;
import com.rcpky.abstractfactory.databaselib.interfaces.QueryBuilder;
import com.rcpky.abstractfactory.databaselib.interfaces.Transaction;

public class PostgreSqlFactory implements IDatabaseFactory {
    @Override
    public Connection getConnection() {
        return new PostgreSqlConnection();
    }

    @Override
    public QueryBuilder getQueryBuilder() {
        return new PostgreSqlQueryBuilder();
    }

    @Override
    public Transaction getTransaction() {
        return new PostgreSqlTransaction();
    }
}
