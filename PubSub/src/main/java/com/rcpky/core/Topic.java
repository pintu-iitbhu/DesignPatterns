package com.rcpky.core;

import com.rcpky.dlq.IDeadLetterQueue;
import com.rcpky.filter.MessageFilter;
import com.rcpky.model.Message;
import com.rcpky.retention.RetentionPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single topic.
 *
 * messageLog    — bounded list of retained messages (governed by RetentionPolicy).
 * evictedCount  — cumulative count of messages trimmed from the head of the log.
 *                 Subscription offsets are absolute (0 = first message ever);
 *                 listIndex = absoluteOffset - evictedCount.
 * subscriptions — insertion-ordered map of subscriber id → Subscription.
 *
 * Thread safety:
 *   All state mutations (messageLog, evictedCount, subscriptions) occur under 'this'.
 *   An immutable snapshot of both the log and evictedCount is taken inside the lock
 *   and passed to Subscription.deliver(), which runs outside the lock.
 *   This means a slow consumer can never stall publishers, and there is no risk of
 *   ConcurrentModificationException on the live messageLog.
 *
 * Extension: replace ArrayList with ArrayDeque for O(1) head-removal on eviction.
 *            Add a scheduled sweep thread for TTL eviction without a publish trigger.
 */
class Topic {

    private final String name;
    private final List<Message> messageLog;
    private final Map<String, Subscription> subscriptions;
    private final RetentionPolicy retention;
    private int evictedCount;

    Topic(String name, RetentionPolicy retention) {
        this.name          = name;
        this.messageLog    = new ArrayList<>();
        this.subscriptions = new LinkedHashMap<>();
        this.retention     = retention;
        this.evictedCount  = 0;
    }

    /**
     * Append a message, apply retention, then fan-out to all subscriptions.
     * An immutable log snapshot is taken under the lock; delivery is async.
     */
    void publish(Message message) {
        List<Subscription> subSnapshot;
        List<Message> logSnapshot;
        int currentEvicted;
        synchronized (this) {
            messageLog.add(message);
            applyRetention();
            subSnapshot    = new ArrayList<>(subscriptions.values());
            logSnapshot    = Collections.unmodifiableList(new ArrayList<>(messageLog));
            currentEvicted = this.evictedCount;
        }
        subSnapshot.forEach(sub -> sub.deliver(logSnapshot, currentEvicted));
    }

    /**
     * Register a new subscriber. Late subscribers replay from the oldest retained
     * message. Re-subscribing an already-subscribed id is a no-op (idempotent).
     */
    void subscribe(ISubscriber subscriber, MessageFilter filter,
                   int maxRetries, IDeadLetterQueue dlq) {
        Subscription sub;
        List<Message> logSnapshot;
        int currentEvicted;
        synchronized (this) {
            if (subscriptions.containsKey(subscriber.getId())) return;
            sub            = new Subscription(subscriber, filter, maxRetries, dlq);
            subscriptions.put(subscriber.getId(), sub);
            logSnapshot    = Collections.unmodifiableList(new ArrayList<>(messageLog));
            currentEvicted = this.evictedCount;
        }
        sub.deliver(logSnapshot, currentEvicted);
    }

    /** Remove a subscription and shut down its delivery executor. */
    synchronized void unsubscribe(String subscriberId) {
        Subscription sub = subscriptions.remove(subscriberId);
        if (sub != null) sub.shutdown();
    }

    synchronized void pause(String subscriberId) {
        getSubscriptionOrThrow(subscriberId).pause();
    }

    void resume(String subscriberId) {
        Subscription sub;
        List<Message> logSnapshot;
        int currentEvicted;
        synchronized (this) {
            sub            = getSubscriptionOrThrow(subscriberId);
            logSnapshot    = Collections.unmodifiableList(new ArrayList<>(messageLog));
            currentEvicted = this.evictedCount;
        }
        sub.resume(logSnapshot, currentEvicted);
    }

    void awaitDelivery() throws InterruptedException {
        List<Subscription> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(subscriptions.values());
        }
        for (Subscription sub : snapshot) {
            sub.awaitDelivery();
        }
    }

    synchronized void shutdown() {
        subscriptions.values().forEach(Subscription::shutdown);
        subscriptions.clear();
    }

    synchronized List<Message> getMessageLog() {
        return Collections.unmodifiableList(new ArrayList<>(messageLog));
    }

    String getName() { return name; }

    /**
     * Trim messages from the head of the log to enforce retention limits.
     * Must be called under 'this' lock (guaranteed by publish()).
     * Note: ArrayList.remove(0) is O(n); use ArrayDeque for O(1) in production.
     */
    private void applyRetention() {
        long cutoff = System.currentTimeMillis() - retention.getTtlMs();
        while (!messageLog.isEmpty() && messageLog.get(0).getTimestamp() < cutoff) {
            messageLog.remove(0);
            evictedCount++;
        }
        while (messageLog.size() > retention.getMaxMessages()) {
            messageLog.remove(0);
            evictedCount++;
        }
    }

    private Subscription getSubscriptionOrThrow(String subscriberId) {
        Subscription sub = subscriptions.get(subscriberId);
        if (sub == null) {
            throw new IllegalArgumentException(
                    "No subscription found for subscriber '" + subscriberId
                    + "' on topic '" + name + "'");
        }
        return sub;
    }
}
