package com.rcpky.decorator.apiclient;

public class Retry implements  ApiClient {
    private final ApiClient apiClient;

    public Retry(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiResponse execute(ApiRequest request) {
        // Retry logic
        System.out.println("Retrying...");
        return apiClient.execute(request);
    }
}
