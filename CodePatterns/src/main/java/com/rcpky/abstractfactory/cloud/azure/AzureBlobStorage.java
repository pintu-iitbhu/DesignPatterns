package com.rcpky.abstractfactory.cloud.azure;

import com.rcpky.abstractfactory.cloud.Storage;

public class AzureBlobStorage implements Storage {

    @Override
    public void upload(String fileName) {
        System.out.println(
                "Uploading to Azure Blob Storage: " + fileName
        );
    }
}
