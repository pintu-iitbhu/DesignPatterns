package com.rcpky.algo;

import com.rcpky.configs.TokenBucketConfig;
import com.rcpky.models.Bucket;
import com.rcpky.models.RateLimitResult;
import com.rcpky.store.StateStore;

import java.util.concurrent.atomic.AtomicReference;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.ConcurrentMap;

public class TokenBucketAlgorithm implements RateLimitAlgorithm<TokenBucketConfig> {

    private final StateStore<Bucket> store;
    // private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>(); // Q2: unbounded heap growth

    public TokenBucketAlgorithm(StateStore<Bucket> store) {
        this.store = store;
    }

    @Override
    public RateLimitResult tryAcquire(String key, TokenBucketConfig policy, long nowMillis) {
        // final RateLimitResult[] result = new RateLimitResult[1]; // Q3: array trick — workaround for lambda effectively-final constraint
        AtomicReference<RateLimitResult> result = new AtomicReference<>();

        store.compute(key, (ignored, oldBucket) -> {
            double tokens = oldBucket == null ? policy.capacity() : oldBucket.tokens();

            long lastRefill = oldBucket == null ? nowMillis : oldBucket.lastRefillMillis();

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

                // result[0] = new RateLimitResult(true, (int) Math.floor(tokens), 0);
                result.set(new RateLimitResult(
                        true,
                        (int) Math.floor(tokens),
                        0
                ));
            } else {
                long retryAfter = (long) Math.ceil(
                        (1.0 - tokens) / refillRate
                );

                // result[0] = new RateLimitResult(false, 0, retryAfter);
                result.set(new RateLimitResult(
                        false,
                        0,
                        retryAfter
                ));
            }

            return new Bucket(tokens, updatedRefillTime);
        });

        // return result[0];
        return result.get();
    }
}
