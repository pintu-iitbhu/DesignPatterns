package com.rcpky;

public interface RateLimitAlgorithm {
    RateLimitResult tryAcquire(String key, RateLimitConfig config, long nowMillis);
}
