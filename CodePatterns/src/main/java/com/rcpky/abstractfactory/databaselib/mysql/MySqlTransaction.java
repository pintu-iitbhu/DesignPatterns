package com.rcpky.abstractfactory.databaselib.mysql;

import com.rcpky.abstractfactory.databaselib.interfaces.Transaction;

public class MySqlTransaction implements Transaction {
    @Override
    public void begin() {
        System.out.println("[MySqlTransaction] Transaction begin");
    }

    @Override
    public void commit() {
        System.out.println("[MySqlTransaction] Transaction commit");
    }

    @Override
    public void rollback() {
        System.out.println("[MySqlTransaction] Transaction rollback");
    }
}
