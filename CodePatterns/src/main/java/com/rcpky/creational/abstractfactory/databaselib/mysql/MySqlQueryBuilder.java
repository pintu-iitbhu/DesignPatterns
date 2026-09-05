package com.rcpky.creational.abstractfactory.databaselib.mysql;

import com.rcpky.creational.abstractfactory.databaselib.interfaces.QueryBuilder;

public class MySqlQueryBuilder implements QueryBuilder {
    @Override
    public void build() {
        System.out.println("[MySqlQueryBuilder] Building query");
    }

    @Override
    public void execute() {
        System.out.println("[MySqlQueryBuilder] Executing query");
    }
}
