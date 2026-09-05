package com.rcpky.creational.abstractfactory.cloud.aws;

import com.rcpky.creational.abstractfactory.cloud.Database;

public class RDSDatabase implements Database {

    @Override
    public void save(String data) {
        System.out.println("Saving to AWS RDS: " + data);
    }
}