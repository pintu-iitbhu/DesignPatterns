package com.rcpky.abstractfactory.databaselib;

import com.rcpky.abstractfactory.databaselib.mysql.MySqlFactory;

public class AbstractDemo {

    public  static void main(String[] args) {

        MySqlFactory factory = new MySqlFactory();
        DbApp app = new DbApp(factory);
        app.run();


    }
}
