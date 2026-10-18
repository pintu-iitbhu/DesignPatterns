package com.rcpky.core;

import com.rcpky.filter.MessageFilter;
import com.rcpky.model.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central broker — the single entry-point for publishers and subscribers.
 *
 * Responsibilities:
 *   - Topic registry (create / lookup)
 *   - Route publish() calls to the correct Topic
 *   - Delegate subscribe / unsubscribe / pause / resume to Topic
 *
 * Singleton: one broker per JVM process (reset() provided for testing).
 *
 * Thread safety:
 *   - getInstance() / reset() are synchronized on Broker.class.
 *   - topics uses ConcurrentHashMap so createTopic / publish / subscribe
 *     can run from any thread without external locking.
 *   - All mutation inside a Topic is guarded by the Topic's own monitor.
 *
 * Extension: replace the singleton with dependency-injection; add a
 *            MessageDispatcher (ExecutorService) for async push delivery.
 */
public class Broker {

    private static Broker instance;

    private final Map<String, Topic> topics;

    private Broker() {
        this.topics = new ConcurrentHashMap<>();
    }

    /** Thread-safe singleton access. */
    public static synchronized Broker getInstance() {
        if (instance == null) {
            instance = new Broker();
        }
        return instance;
    }

    /** Wipe all state — useful between test scenarios. */
    public static synchronized void reset() {
        instance = new Broker();
    }

    // ─── Topic Management ────────────────────────────────────────────────────

    /** Create a topic explicitly. Idempotent — safe to call multiple times. */
    public void createTopic(String topicName) {
        topics.putIfAbsent(topicName, new Topic(topicName));
    }

    // ─── Publisher API ───────────────────────────────────────────────────────

    public void publish(String topicName, String payload) {
        publish(topicName, payload, null);
    }

    public void publish(String topicName, String payload, Map<String, String> metadata) {
        Topic topic = getTopicOrThrow(topicName);
        Message msg = new Message(topicName, payload, metadata);
        topic.publish(msg);
    }

    // ─── Subscriber API ──────────────────────────────────────────────────────

    /** Subscribe without a filter — receives every message. */
    public void subscribe(String topicName, ISubscriber subscriber) {
        subscribe(topicName, subscriber, null);
    }

    /** Subscribe with an optional filter predicate. */
    public void subscribe(String topicName, ISubscriber subscriber, MessageFilter filter) {
        Topic topic = getTopicOrThrow(topicName);
        topic.subscribe(subscriber, filter);
    }

    public void unsubscribe(String topicName, String subscriberId) {
        getTopicOrThrow(topicName).unsubscribe(subscriberId);
    }

    // ─── Availability Control ────────────────────────────────────────────────

    /**
     * Mark a subscriber as temporarily unavailable on a topic.
     * Messages published during the pause period accumulate in the topic log
     * and are delivered in full, in order, when the subscriber resumes.
     */
    public void pause(String topicName, String subscriberId) {
        getTopicOrThrow(topicName).pause(subscriberId);
    }

    /**
     * Resume a paused subscriber — triggers immediate catch-up delivery
     * of all messages missed during the pause.
     */
    public void resume(String topicName, String subscriberId) {
        getTopicOrThrow(topicName).resume(subscriberId);
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
