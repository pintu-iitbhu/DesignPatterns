package com.rcpky.algo;

import com.rcpky.configs.RateLimitConfig;
import com.rcpky.models.RateLimitResult;

public interface RateLimitAlgorithm<C extends RateLimitConfig> {
    RateLimitResult tryAcquire(String key, C config, long nowMillis);
}
