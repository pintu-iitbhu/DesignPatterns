package com.rcpky.abstractfactory.databaselib.mysql;

import com.rcpky.abstractfactory.databaselib.interfaces.QueryBuilder;

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
