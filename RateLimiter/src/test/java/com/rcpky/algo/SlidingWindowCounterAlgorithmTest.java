package com.rcpky.algo;

import com.rcpky.configs.SlidingWindowConfig;
import com.rcpky.models.RateLimitResult;
import com.rcpky.models.WindowState;
import com.rcpky.store.InMemoryStateStore;
import com.rcpky.store.StateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowCounterAlgorithmTest {

    private SlidingWindowCounterAlgorithm algo;
    private SlidingWindowConfig config;

    @BeforeEach
    void setUp() {
        StateStore<WindowState> store = new InMemoryStateStore<>(Duration.ofHours(1));
        algo = new SlidingWindowCounterAlgorithm(store);
        config = new SlidingWindowConfig(5, 60_000);
    }

    @Test
    void allowsUpToLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(algo.tryAcquire("user", config, 0L).allowed(),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void deniesWhenLimitExceeded() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user", config, 0L);

        RateLimitResult result = algo.tryAcquire("user", config, 0L);

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
        assertTrue(result.retryAfterMillis() > 0);
    }

    @Test
    void allowsAfterTwoFullWindowsElapse() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user", config, 0L);

        RateLimitResult result = algo.tryAcquire("user", config, 120_000L);

        assertTrue(result.allowed());
    }

    @Test
    void previousWindowWeightReducesCarryOver() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user", config, 0L);

        // At 50% into the new window: previous weight = 0.5 → carryOver = 5 * 0.5 = 2.5
        // estimatedCount = 0 + 2.5 = 2.5 → 2 more allowed before hitting limit of 5
        RateLimitResult halfwayResult = algo.tryAcquire("user", config, 90_000L);
        assertTrue(halfwayResult.allowed());
    }

    @Test
    void usersAreIsolated() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user-A", config, 0L);
        assertFalse(algo.tryAcquire("user-A", config, 0L).allowed());

        assertTrue(algo.tryAcquire("user-B", config, 0L).allowed());
    }
}
