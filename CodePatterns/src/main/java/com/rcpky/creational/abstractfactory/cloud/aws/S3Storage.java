package com.rcpky.creational.abstractfactory.cloud.aws;

import com.rcpky.creational.abstractfactory.cloud.Storage;

public class S3Storage implements Storage {

    @Override
    public void upload(String fileName) {
        System.out.println("Uploading to AWS S3: " + fileName);
    }
}
