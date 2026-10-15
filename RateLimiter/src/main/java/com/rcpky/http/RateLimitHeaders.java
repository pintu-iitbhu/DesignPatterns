package com.rcpky.http;

import com.rcpky.configs.RateLimitConfig;
import com.rcpky.models.RateLimitResult;

import java.util.HashMap;
import java.util.Map;

public class RateLimitHeaders {

    private RateLimitHeaders() {}

    public static Map<String, String> from(RateLimitResult result, RateLimitConfig config) {
        Map<String, String> headers = new HashMap<>();

        headers.put("X-RateLimit-Limit", String.valueOf(config.limit()));
        headers.put("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (!result.allowed()) {
            long retrySeconds = Math.max(1, (result.retryAfterMillis() + 999) / 1000);
            headers.put("Retry-After", String.valueOf(retrySeconds));
        }

        return Map.copyOf(headers);
    }
}
