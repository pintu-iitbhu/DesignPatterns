package com.rcpky.abstractfactory.databaselib.postgre;

import com.rcpky.abstractfactory.databaselib.interfaces.QueryBuilder;

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
