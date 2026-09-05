package com.rcpky.structural.decorator.apiclient;

public interface ApiClient {
    ApiResponse execute(ApiRequest request);
}
