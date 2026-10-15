package com.rcpky;

import com.rcpky.algo.RateLimitAlgorithm;
import com.rcpky.configs.RateLimitConfig;
import com.rcpky.models.RateLimitResult;

public class RateLimiterService<C extends RateLimitConfig> {

    private final RateLimitAlgorithm<C> algorithm;
    private final C config;

    public RateLimiterService(RateLimitAlgorithm<C> algorithm, C config) {
        this.algorithm = algorithm;
        this.config = config;
    }

    public RateLimitResult allowRequest(String userId) {
        return algorithm.tryAcquire(
                userId,
                config,
                System.currentTimeMillis()
        );
    }

}
