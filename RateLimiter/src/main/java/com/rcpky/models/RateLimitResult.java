package com.rcpky;

public record RateLimitResult(
        boolean allowed,
        int remaining,
        long retryAfterMillis
) {}
