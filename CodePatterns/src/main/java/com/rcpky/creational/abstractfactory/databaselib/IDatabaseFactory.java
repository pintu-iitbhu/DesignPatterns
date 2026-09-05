package com.rcpky.creational.abstractfactory.databaselib;

import com.rcpky.creational.abstractfactory.databaselib.interfaces.Connection;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.QueryBuilder;
import com.rcpky.creational.abstractfactory.databaselib.interfaces.Transaction;

public interface IDatabaseFactory {
    Connection getConnection();
    QueryBuilder getQueryBuilder();
    Transaction getTransaction();
}
