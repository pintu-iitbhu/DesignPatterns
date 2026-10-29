package com.rcpky.core;

import com.rcpky.dlq.IDeadLetterQueue;
import com.rcpky.filter.MessageFilter;
import com.rcpky.model.Message;
import com.rcpky.retention.RetentionPolicy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central broker — the single entry-point for publishers and subscribers.
 * Implements IBroker; all production code should depend on the interface.
 *
 * Singleton: one broker per JVM process. Use reset() between test scenarios.
 * DLQ: pass an IDeadLetterQueue to getInstance(dlq) / reset(dlq) to capture
 *      messages that fail delivery after all retries.
 *
 * Thread safety:
 *   - getInstance() / reset() are synchronized on Broker.class.
 *   - topics uses ConcurrentHashMap; per-topic state is guarded by each
 *     Topic's intrinsic lock.
 *   - Per-subscriber delivery runs in a dedicated single-thread executor
 *     (message order guaranteed, publisher thread never blocked).
 *
 * Extension: replace singleton with DI; wire IBroker throughout.
 */
public class Broker implements IBroker {

    private static Broker instance;

    private final Map<String, Topic> topics;
    private final IDeadLetterQueue   dlq;

    private Broker(IDeadLetterQueue dlq) {
        this.topics = new ConcurrentHashMap<>();
        this.dlq    = dlq;
    }

    /** Thread-safe singleton access (no DLQ). */
    public static synchronized Broker getInstance() {
        if (instance == null) {
            instance = new Broker(null);
        }
        return instance;
    }

    /** Thread-safe singleton access — wires a DLQ on first call only. */
    public static synchronized Broker getInstance(IDeadLetterQueue dlq) {
        if (instance == null) {
            instance = new Broker(dlq);
        }
        return instance;
    }

    /** Shut down the current instance and create a fresh one (no DLQ). */
    public static synchronized void reset() {
        if (instance != null) instance.shutdown();
        instance = new Broker(null);
    }

    /** Shut down the current instance and create a fresh one with a DLQ. */
    public static synchronized void reset(IDeadLetterQueue dlq) {
        if (instance != null) instance.shutdown();
        instance = new Broker(dlq);
    }

    // ─── Topic Management ────────────────────────────────────────────────────

    @Override
    public void createTopic(String topicName) {
        createTopic(topicName, RetentionPolicy.UNLIMITED);
    }

    @Override
    public void createTopic(String topicName, RetentionPolicy policy) {
        topics.putIfAbsent(topicName, new Topic(topicName, policy));
    }

    // ─── Publisher API ───────────────────────────────────────────────────────

    @Override
    public void publish(String topicName, String payload) {
        publish(topicName, payload, null);
    }

    @Override
    public void publish(String topicName, String payload, Map<String, String> metadata) {
        Message msg = new Message(topicName, payload, metadata);
        getTopicOrThrow(topicName).publish(msg);
    }

    // ─── Subscriber API ──────────────────────────────────────────────────────

    @Override
    public void subscribe(String topicName, ISubscriber subscriber) {
        subscribe(topicName, subscriber, null, 0);
    }

    @Override
    public void subscribe(String topicName, ISubscriber subscriber, MessageFilter filter) {
        subscribe(topicName, subscriber, filter, 0);
    }

    @Override
    public void subscribe(String topicName, ISubscriber subscriber,
                          MessageFilter filter, int maxRetries) {
        getTopicOrThrow(topicName).subscribe(subscriber, filter, maxRetries, dlq);
    }

    @Override
    public void unsubscribe(String topicName, String subscriberId) {
        getTopicOrThrow(topicName).unsubscribe(subscriberId);
    }

    // ─── Availability Control ────────────────────────────────────────────────

    @Override
    public void pause(String topicName, String subscriberId) {
        getTopicOrThrow(topicName).pause(subscriberId);
    }

    @Override
    public void resume(String topicName, String subscriberId) {
        getTopicOrThrow(topicName).resume(subscriberId);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    public void awaitAllDeliveries() throws InterruptedException {
        for (Topic topic : topics.values()) {
            topic.awaitDelivery();
        }
    }

    @Override
    public void shutdown() {
        topics.values().forEach(Topic::shutdown);
        topics.clear();
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private Topic getTopicOrThrow(String topicName) {
        Topic topic = topics.get(topicName);
        if (topic == null) {
            throw new IllegalArgumentException("Topic not found: '" + topicName + "'. "
                    + "Call createTopic() first.");
        }
        return topic;
    }
}
