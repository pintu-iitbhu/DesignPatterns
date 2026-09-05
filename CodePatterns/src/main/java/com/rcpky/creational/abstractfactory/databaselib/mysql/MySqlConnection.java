package com.rcpky.creational.abstractfactory.databaselib.mysql;

import com.rcpky.creational.abstractfactory.databaselib.interfaces.Connection;

public class MySqlConnection implements Connection {
    @Override
    public void connect() {
        System.out.println("[MySqlConnection] Connecting to MySQL database");
    }

    @Override
    public void disconnect() {
        System.out.println("[MySqlConnection] Disconnecting from MySQL database");
    }
}
