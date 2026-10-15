package com.rcpky.algo;

import com.rcpky.configs.SlidingWindowConfig;
import com.rcpky.models.RateLimitResult;
import com.rcpky.models.WindowLog;
import com.rcpky.store.InMemoryStateStore;
import com.rcpky.store.StateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowLogAlgorithmTest {

    private SlidingWindowLogAlgorithm algo;
    private SlidingWindowConfig config;

    @BeforeEach
    void setUp() {
        StateStore<WindowLog> store = new InMemoryStateStore<>(Duration.ofHours(1));
        algo = new SlidingWindowLogAlgorithm(store);
        config = new SlidingWindowConfig(5, 60_000);
    }

    @Test
    void allowsUpToLimit() {
        for (int i = 0; i < 5; i++) {
            assertTrue(algo.tryAcquire("user", config, (long) i * 100).allowed(),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void deniesWhenLimitExceeded() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user", config, (long) i);

        RateLimitResult result = algo.tryAcquire("user", config, 100L);

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
        assertTrue(result.retryAfterMillis() > 0);
    }

    @Test
    void allowsExactlyWhenOldestTimestampExpires() {
        SlidingWindowConfig tightConfig = new SlidingWindowConfig(2, 1_000);

        algo.tryAcquire("user", tightConfig, 0L);
        algo.tryAcquire("user", tightConfig, 100L);

        assertFalse(algo.tryAcquire("user", tightConfig, 500L).allowed());

        // t=1001: first request (t=0) is now outside the 1000ms window
        RateLimitResult result = algo.tryAcquire("user", tightConfig, 1_001L);
        assertTrue(result.allowed());
    }

    @Test
    void retryAfterIsExactTimeUntilSlotOpens() {
        SlidingWindowConfig tightConfig = new SlidingWindowConfig(1, 1_000);

        algo.tryAcquire("user", tightConfig, 0L);           // fills at t=0
        RateLimitResult denied = algo.tryAcquire("user", tightConfig, 500L); // denied at t=500

        assertFalse(denied.allowed());
        // oldest is t=0, expires at t=1000 → retryAfter = 1000 - 500 = 500ms
        assertEquals(500L, denied.retryAfterMillis());
    }

    @Test
    void exactEvictionUnlikeCounterApproximation() {
        SlidingWindowConfig tightConfig = new SlidingWindowConfig(5, 60_000);

        // all 5 requests at end of "previous" window equivalent: t=59_000
        for (int i = 0; i < 5; i++) algo.tryAcquire("user", tightConfig, 59_000L);

        // at t=90_000 (30s later) — all 5 are still inside the 60s window
        // exact log: 5 still alive → denied
        RateLimitResult result = algo.tryAcquire("user", tightConfig, 90_000L);
        assertFalse(result.allowed(), "Exact log must deny — all 5 requests are within the last 60s");
    }

    @Test
    void usersAreIsolated() {
        for (int i = 0; i < 5; i++) algo.tryAcquire("user-A", config, (long) i);
        assertFalse(algo.tryAcquire("user-A", config, 100L).allowed());

        assertTrue(algo.tryAcquire("user-B", config, 100L).allowed());
    }
}
