package com.rcpky;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TokenBucketAlgorithm implements RateLimitAlgorithm {
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryAcquire(String key, RateLimitConfig config, long nowMillis) {
        if (!(config instanceof TokenBucketConfig policy)) {
            throw new IllegalArgumentException("TokenBucketConfig required");
        }

        final RateLimitResult[] result = new RateLimitResult[1];

        buckets.compute(key, (ignored, oldBucket) -> {
            double tokens = oldBucket == null
                    ? policy.capacity()
                    : oldBucket.tokens();

            long lastRefill = oldBucket == null
                    ? nowMillis
                    : oldBucket.lastRefillMillis();

            long elapsed = Math.max(0L, nowMillis - lastRefill);

            double refillRate =
                    (double) policy.refillTokens()
                            / policy.refillPeriodMillis();

            tokens = Math.min(
                    policy.capacity(),
                    tokens + elapsed * refillRate
            );

            long updatedRefillTime = Math.max(lastRefill, nowMillis);

            if (tokens >= 1.0) {
                tokens -= 1.0;

                result[0] = new RateLimitResult(
                        true,
                        (int) Math.floor(tokens),
                        0
                );
            } else {
                long retryAfter = (long) Math.ceil(
                        (1.0 - tokens) / refillRate
                );

                result[0] = new RateLimitResult(
                        false,
                        0,
                        retryAfter
                );
            }

            return new Bucket(tokens, updatedRefillTime);
        });

        return result[0];
    }
}
