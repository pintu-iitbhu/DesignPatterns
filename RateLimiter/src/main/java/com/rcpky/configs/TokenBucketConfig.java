package com.rcpky;

public record TokenBucketConfig(int capacity, int refillTokens, long refillPeriodMillis) implements RateLimitConfig {

    public TokenBucketConfig {
        if (capacity <= 0 || refillTokens <= 0 || refillPeriodMillis <= 0) {
            throw new IllegalArgumentException("Invalid token bucket configuration");
        }
    }

    @Override
    public int limit() {
        return capacity;
    }
}
