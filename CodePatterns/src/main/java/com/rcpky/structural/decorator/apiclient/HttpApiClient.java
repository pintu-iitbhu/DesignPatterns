package com.rcpky.structural.decorator.apiclient;

public class HttpApiClient implements  ApiClient {
    @Override
    public ApiResponse execute(ApiRequest request) {
        // Actual HTTP call
        System.out.println("Calling external API...");

        return new ApiResponse();
    }
}
