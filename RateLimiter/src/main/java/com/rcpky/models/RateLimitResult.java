package com.rcpky.models;

public record RateLimitResult(
        boolean allowed,
        int remaining,
        long retryAfterMillis
) {}
