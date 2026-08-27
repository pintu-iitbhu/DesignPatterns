package com.rcpky.abstractfactory.databaselib.interfaces;

public interface Transaction {
    void begin();
    void commit();
    void rollback();
}
