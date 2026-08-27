package com.rcpky.decorator.apiclient;

public class ClientDemo {

    public  static void main(String[] args) {
        //Authentication
        //      ↓
        //Logging
        //      ↓
        //Retry
        //      ↓
        //Actual HTTP Client
        ApiClient apiClient = new HttpApiClient();
        apiClient = new Retry(apiClient);
        apiClient = new Logging(apiClient);
        apiClient = new Authentication(apiClient);

        apiClient.execute(new ApiRequest());
    }
}
