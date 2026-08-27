package com.rcpky.decorator.apiclient;

public class Logging implements  ApiClient {

    private final ApiClient apiClient;
    public Logging(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public ApiResponse execute(ApiRequest request) {
        System.out.println("Logging .....");
        return apiClient.execute(request);
    }
}
