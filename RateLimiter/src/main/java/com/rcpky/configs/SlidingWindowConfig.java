package com.rcpky.configs;

public record SlidingWindowConfig(
        int limit,
        long windowMillis
) implements RateLimitConfig {

    public SlidingWindowConfig {
        if (limit <= 0 || windowMillis <= 0) {
            throw new IllegalArgumentException("Invalid sliding window configuration");
        }
    }

    @Override
    public int limit() {
        return limit;
    }
}
