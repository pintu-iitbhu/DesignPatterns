package com.rcpky.retention;

/**
 * Immutable per-topic retention configuration.
 *
 * maxMessages — maximum number of messages to retain in the topic log.
 *               Oldest messages are evicted once the limit is exceeded.
 *               Default: Integer.MAX_VALUE (unlimited).
 *
 * ttlMs       — maximum age (milliseconds) a message is retained.
 *               Messages older than ttlMs are evicted on the next publish.
 *               Default: Long.MAX_VALUE (unlimited).
 *
 * Factory methods: ofMaxMessages(n), ofTtl(ms), of(n, ms), UNLIMITED.
 *
 * Extension: add a background sweep thread for TTL eviction without requiring
 *            a publish trigger.
 */
public final class RetentionPolicy {

    public static final RetentionPolicy UNLIMITED = new RetentionPolicy(Integer.MAX_VALUE, Long.MAX_VALUE);

    private final int  maxMessages;
    private final long ttlMs;

    private RetentionPolicy(int maxMessages, long ttlMs) {
        this.maxMessages = maxMessages;
        this.ttlMs       = ttlMs;
    }

    public static RetentionPolicy ofMaxMessages(int max) {
        return new RetentionPolicy(max, Long.MAX_VALUE);
    }

    public static RetentionPolicy ofTtl(long ttlMs) {
        return new RetentionPolicy(Integer.MAX_VALUE, ttlMs);
    }

    public static RetentionPolicy of(int maxMessages, long ttlMs) {
        return new RetentionPolicy(maxMessages, ttlMs);
    }

    public int  getMaxMessages() { return maxMessages; }
    public long getTtlMs()       { return ttlMs; }
}
