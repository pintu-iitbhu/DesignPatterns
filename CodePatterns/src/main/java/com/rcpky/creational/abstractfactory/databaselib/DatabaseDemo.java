package com.rcpky.creational.abstractfactory.databaselib;

import com.rcpky.creational.abstractfactory.databaselib.mysql.MySqlFactory;

public class DatabaseDemo {

    public  static void main(String[] args) {

        MySqlFactory factory = new MySqlFactory();
        DbApp app = new DbApp(factory);
        app.run();


    }
}
