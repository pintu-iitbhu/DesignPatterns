package com.rcpky.creational.abstractfactory.databaselib.postgre;

import com.rcpky.creational.abstractfactory.databaselib.interfaces.QueryBuilder;

public class PostgreSqlQueryBuilder implements QueryBuilder {
    @Override
    public void build() {
        System.out.println("[PostgreSqlQueryBuilder] Building query");
    }

    @Override
    public void execute() {
        System.out.println("[PostgreSqlQueryBuilder] Executing query");
    }
}
