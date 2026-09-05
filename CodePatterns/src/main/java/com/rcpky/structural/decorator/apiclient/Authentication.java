package com.rcpky.structural.decorator.apiclient;

public class Authentication implements ApiClient{
    private final ApiClient apiClient;

    public Authentication(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public ApiResponse execute(ApiRequest request) {
        // Authentication logic
        System.out.println("Authenticating...");
        return apiClient.execute(request);
    }
}
