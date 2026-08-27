package com.rcpky.abstractfactory.cloud.azure;

import com.rcpky.abstractfactory.cloud.Database;

public class AzureSqlDatabase implements Database {

    @Override
    public void save(String data) {
        System.out.println(
                "Saving to Azure SQL: " + data
        );
    }
}
