package com.rcpky.creational.abstractfactory.databaselib.postgre;

import com.rcpky.creational.abstractfactory.databaselib.interfaces.Transaction;

public class PostgreSqlTransaction implements Transaction {
    @Override
    public void begin() {
        System.out.println("[PostgreSqlTransaction] Transaction begin");
    }

    @Override
    public void commit() {
        System.out.println("[PostgreSqlTransaction] Transaction commit");
    }

    @Override
    public void rollback() {
        System.out.println("[PostgreSqlTransaction] Transaction rollback");
    }
}
