package com.rcpky.creational.abstractfactory.databaselib.postgre;

import com.rcpky.creational.abstractfactory.databaselib.IDatabaseFactory;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.Connection;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.QueryBuilder;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.Transaction;

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
