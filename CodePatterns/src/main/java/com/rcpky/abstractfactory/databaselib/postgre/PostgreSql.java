package com.rcpky.abstractfactory.databaselib.postgre;

import com.rcpky.abstractfactory.databaselib.IDatabaseFactory;
import com.rcpky.abstractfactory.databaselib.interfaces.Connection;
import com.rcpky.abstractfactory.databaselib.interfaces.QueryBuilder;
import com.rcpky.abstractfactory.databaselib.interfaces.Transaction;

public class PostgreSql {
    private final Connection connection;
    private final QueryBuilder queryBuilder;
    private final Transaction transaction;


    public PostgreSql(IDatabaseFactory factory) {
        this.connection = factory.getConnection();
        this.queryBuilder = factory.getQueryBuilder();
        this.transaction = factory.getTransaction();
    }
}
