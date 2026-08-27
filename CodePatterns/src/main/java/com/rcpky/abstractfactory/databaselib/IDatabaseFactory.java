package com.rcpky.abstractfactory.databaselib;

import com.rcpky.abstractfactory.databaselib.interfaces.Connection;
import com.rcpky.abstractfactory.databaselib.interfaces.QueryBuilder;
import com.rcpky.abstractfactory.databaselib.interfaces.Transaction;

public interface IDatabaseFactory {
    Connection getConnection();
    QueryBuilder getQueryBuilder();
    Transaction getTransaction();
}
