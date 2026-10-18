package com.rcpky.core;

import com.rcpky.filter.MessageFilter;
import com.rcpky.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single topic.
 *
 * Internals:
 *   messageLog   — append-only ordered list of all published messages.
 *                  Acts as the durable store; each subscriber tracks its
 *                  own read offset into this list.
 *   subscriptions — ordered map (insertion order preserved) of subscriber
 *                  id → Subscription.
 *
 * Thread safety:
 *   All methods synchronize on 'this' for state mutation (messageLog + subscriptions).
 *   Crucially, subscriber.onMessage() is called OUTSIDE the synchronized block:
 *     1. Append message and snapshot subscriptions under the lock.
 *     2. Release lock.
 *     3. Deliver to each subscription (Subscription.deliver is itself synchronized).
 *   This means a slow consumer never blocks concurrent publishers.
 *
 * Extension: add per-topic retention policy (TTL / max-size) here.
 *            For async delivery replace the loop with an ExecutorService.
 */
public class Topic {

    private final String name;
    private final List<Message> messageLog;
    private final Map<String, Subscription> subscriptions;

    Topic(String name) {
        this.name = name;
        this.messageLog = new ArrayList<>();
        this.subscriptions = new LinkedHashMap<>();
    }

    /**
     * Append a message to the log and fan-out to all active subscriptions.
     * Inactive (paused) subscriptions skip delivery; their offset is frozen
     * so they catch up automatically on resume.
     *
     * Lock is released before delivery so a slow consumer never stalls publishers.
     */
    void publish(Message message) {
        List<Subscription> snapshot;
        synchronized (this) {
            messageLog.add(message);
            snapshot = new ArrayList<>(subscriptions.values());
        }
        snapshot.forEach(sub -> sub.deliver(messageLog));
    }

    /**
     * Register a new subscriber, optionally with a filter.
     * Late subscribers receive all messages from offset 0 (guaranteed delivery).
     * Re-subscribing an already-subscribed id is a no-op (idempotent).
     *
     * If a publish() races with subscribe(), Subscription.deliver() is synchronized
     * so the new subscriber cannot receive the same message twice.
     */
    void subscribe(ISubscriber subscriber, MessageFilter filter) {
        Subscription sub;
        synchronized (this) {
            if (subscriptions.containsKey(subscriber.getId())) return;
            sub = new Subscription(subscriber, filter);
            subscriptions.put(subscriber.getId(), sub);
        }
        sub.deliver(messageLog);
    }

    /** Remove a subscription. Pending (unread) messages are discarded for this subscriber. */
    synchronized void unsubscribe(String subscriberId) {
        subscriptions.remove(subscriberId);
    }

    /** Mark a subscriber as temporarily unavailable; messages accumulate in the log. */
    synchronized void pause(String subscriberId) {
        getSubscriptionOrThrow(subscriberId).pause();
    }

    /** Resume a paused subscriber and immediately deliver all missed messages. */
    void resume(String subscriberId) {
        Subscription sub;
        synchronized (this) {
            sub = getSubscriptionOrThrow(subscriberId);
        }
        sub.resume(messageLog);
    }

    synchronized List<Message> getMessageLog() {
        return Collections.unmodifiableList(new ArrayList<>(messageLog));
    }

    String getName() { return name; }

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
