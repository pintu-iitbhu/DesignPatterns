package com.rcpky.core;

import com.rcpky.filter.MessageFilter;
import com.rcpky.retention.RetentionPolicy;

import java.util.Map;

/**
 * Central broker interface — decouples publishers and subscribers from the
 * concrete Broker implementation. Wire this type throughout production code
 * to enable easy testing, alternative implementations, and DI injection.
 */
public interface IBroker {

    // ─── Topic Management ────────────────────────────────────────────────────

    /** Create a topic with unlimited retention. Idempotent. */
    void createTopic(String topicName);

    /** Create a topic with a custom retention policy. Idempotent. */
    void createTopic(String topicName, RetentionPolicy policy);

    // ─── Publisher API ───────────────────────────────────────────────────────

    void publish(String topicName, String payload);
    void publish(String topicName, String payload, Map<String, String> metadata);

    // ─── Subscriber API ──────────────────────────────────────────────────────

    /** Subscribe with no filter; receives every message. maxRetries defaults to 0. */
    void subscribe(String topicName, ISubscriber subscriber);

    /** Subscribe with an optional filter predicate. maxRetries defaults to 0. */
    void subscribe(String topicName, ISubscriber subscriber, MessageFilter filter);

    /** Subscribe with a filter and explicit retry count on onMessage() failure. */
    void subscribe(String topicName, ISubscriber subscriber, MessageFilter filter, int maxRetries);

    void unsubscribe(String topicName, String subscriberId);

    // ─── Availability Control ────────────────────────────────────────────────

    /**
     * Mark a subscriber temporarily unavailable. Messages accumulate in the
     * retained log and are delivered in full order on resume().
     */
    void pause(String topicName, String subscriberId);

    /** Resume a paused subscriber — triggers immediate catch-up delivery. */
    void resume(String topicName, String subscriberId);

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    /** Block until every in-flight async delivery across all topics has completed. */
    void awaitAllDeliveries() throws InterruptedException;

    /** Shut down all per-subscriber delivery executors. Call before JVM exit. */
    void shutdown();
}
