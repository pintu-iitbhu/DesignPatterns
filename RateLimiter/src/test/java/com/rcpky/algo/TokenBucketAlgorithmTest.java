package com.rcpky.algo;

import com.rcpky.configs.TokenBucketConfig;
import com.rcpky.models.Bucket;
import com.rcpky.models.RateLimitResult;
import com.rcpky.store.InMemoryStateStore;
import com.rcpky.store.StateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketAlgorithmTest {

    private TokenBucketAlgorithm algo;
    private TokenBucketConfig config;

    @BeforeEach
    void setUp() {
        StateStore<Bucket> store = new InMemoryStateStore<>(Duration.ofHours(1));
        algo = new TokenBucketAlgorithm(store);
        config = new TokenBucketConfig(5, 1, 1_000);
    }

    @Test
    void allowsUpToCapacity() {
        for (int i = 0; i < 5; i++) {
            assertTrue(algo.tryAcquire("user", config, 0L).allowed(),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void deniesWhenBucketExhausted() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user", config, 0L);

        RateLimitResult result = algo.tryAcquire("user", config, 0L);

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
        assertTrue(result.retryAfterMillis() > 0);
    }

    @Test
    void remainingDecrementsOnEachRequest() {
        assertEquals(4, algo.tryAcquire("user", config, 0L).remaining());
        assertEquals(3, algo.tryAcquire("user", config, 0L).remaining());
        assertEquals(2, algo.tryAcquire("user", config, 0L).remaining());
    }

    @Test
    void refillsOneTokenAfterOnePeriod() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user", config, 0L);
        assertFalse(algo.tryAcquire("user", config, 0L).allowed());

        RateLimitResult afterRefill = algo.tryAcquire("user", config, 1_000L);

        assertTrue(afterRefill.allowed());
    }

    @Test
    void refillsAllTokensAfterFullCapacityPeriod() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user", config, 0L);

        for (int i = 0; i < 5; i++) {
            assertTrue(algo.tryAcquire("user", config, 5_000L).allowed(),
                    "Request " + (i + 1) + " after full refill should be allowed");
        }
    }

    @Test
    void usersAreIsolated() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user-A", config, 0L);
        assertFalse(algo.tryAcquire("user-A", config, 0L).allowed());

        assertTrue(algo.tryAcquire("user-B", config, 0L).allowed());
    }
}
