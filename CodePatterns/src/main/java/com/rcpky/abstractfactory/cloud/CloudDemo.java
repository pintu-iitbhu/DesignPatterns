package com.rcpky.abstractfactory.cloud;

import com.rcpky.abstractfactory.cloud.aws.AwsCloudFactory;
import com.rcpky.abstractfactory.cloud.client.CloudApplication;
import com.rcpky.abstractfactory.cloud.client.CloudFactory;

public class CloudDemo {

    public static void main(String[] args) {
        CloudFactory factory = new AwsCloudFactory();
        CloudApplication app = new CloudApplication(factory);
        app.process();
    }
}
