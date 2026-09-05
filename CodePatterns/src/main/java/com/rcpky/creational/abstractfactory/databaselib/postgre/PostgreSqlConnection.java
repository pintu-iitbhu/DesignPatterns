package com.rcpky.creational.abstractfactory.databaselib.postgre;

import com.rcpky.creational.abstractfactory.databaselib.interfaces.Connection;

public class PostgreSqlConnection implements Connection {
    @Override
    public void connect() {
        System.out.println("[PostgreSqlConnection] Connecting to PostgreSql database");
    }

    @Override
    public void disconnect() {
        System.out.println("[PostgreSqlConnection] Disconnecting from PostgreSql database");
    }
}
