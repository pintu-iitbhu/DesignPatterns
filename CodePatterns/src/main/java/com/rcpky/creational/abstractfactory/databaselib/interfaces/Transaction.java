package com.rcpky.creational.abstractfactory.databaselib.interfaces;

public interface Transaction {
    void begin();
    void commit();
    void rollback();
}
