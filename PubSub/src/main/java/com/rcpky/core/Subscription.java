package com.rcpky.core;

import com.rcpky.dlq.DeadLetterEntry;
import com.rcpky.dlq.IDeadLetterQueue;
import com.rcpky.filter.MessageFilter;
import com.rcpky.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks per-subscriber state within a single topic.
 *
 * offset           : absolute message offset (from topic lifetime start).
 *                    listIndex = absoluteOffset - evictedCount.
 *                    Clamped to evictedCount when a subscriber falls behind
 *                    the retention window.
 * filter           : optional predicate; null means "accept all".
 * active           : paused subscribers freeze their offset; missed messages
 *                    are delivered in-order on resume.
 * maxRetries       : how many times to retry onMessage() before dead-lettering.
 * dlq              : optional; receives DeadLetterEntry after retry exhaustion.
 * deliveryExecutor : single-thread executor — decouples delivery from the
 *                    publisher thread and preserves per-subscriber ordering.
 * pending          : count of in-flight executor tasks; used by awaitDelivery().
 */
class Subscription {

    private final ISubscriber    subscriber;
    private final MessageFilter  filter;
    private int                  offset;
    private volatile boolean     active;
    private final int            maxRetries;
    private final IDeadLetterQueue dlq;
    private final ExecutorService  deliveryExecutor;
    private final AtomicInteger    pending = new AtomicInteger(0);

    Subscription(ISubscriber subscriber, MessageFilter filter,
                 int maxRetries, IDeadLetterQueue dlq) {
        this.subscriber      = subscriber;
        this.filter          = filter;
        this.offset          = 0;
        this.active          = true;
        this.maxRetries      = maxRetries;
        this.dlq             = dlq;
        this.deliveryExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pubsub-" + subscriber.getId());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Dispatch all unread messages from the immutable log snapshot to this
     * subscriber's single-thread executor. The offset is advanced under this
     * lock so no message is ever dispatched twice across concurrent publishes.
     *
     * logSnapshot  : immutable copy of the topic log taken atomically with evictedCount.
     * evictedCount : messages trimmed from the front of the log (absolute base offset).
     */
    synchronized void deliver(List<Message> logSnapshot, int evictedCount) {
        if (!active) return;
        if (offset < evictedCount) {
            offset = evictedCount;
        }

        int startIndex = offset - evictedCount;
        if (startIndex >= logSnapshot.size()) return;

        List<Message> toDispatch = new ArrayList<>();
        for (int i = startIndex; i < logSnapshot.size(); i++) {
            Message msg = logSnapshot.get(i);
            offset++;
            if (filter == null || filter.matches(msg)) {
                toDispatch.add(msg);
            }
        }

        for (Message msg : toDispatch) {
            pending.incrementAndGet();
            deliveryExecutor.submit(() -> {
                try {
                    deliverWithRetry(msg);
                } finally {
                    pending.decrementAndGet();
                }
            });
        }
    }

    /**
     * Attempt onMessage() up to (1 + maxRetries) times with linear back-off.
     * On total exhaustion: call onError() then route to the DLQ if wired.
     */
    private void deliverWithRetry(Message message) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= maxRetries) {
            try {
                subscriber.onMessage(message);
                return;
            } catch (Exception e) {
                lastException = e;
                attempts++;
                if (attempts <= maxRetries) {
                    try { Thread.sleep(50L * attempts); }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        subscriber.onError(message, lastException);
        if (dlq != null) {
            dlq.add(new DeadLetterEntry(
                    subscriber.getId(), message,
                    lastException != null ? lastException.getMessage() : "unknown",
                    attempts));
        }
    }

    void pause() { this.active = false; }

    void resume(List<Message> logSnapshot, int evictedCount) {
        this.active = true;
        deliver(logSnapshot, evictedCount);
    }

    /**
     * Spin-wait until all in-flight deliveries on this executor have finished.
     * The volatile write in pending.decrementAndGet() establishes happens-before
     * with the subsequent volatile read in the calling thread, so the caller
     * safely observes all onMessage() side-effects after this returns.
     */
    void awaitDelivery() throws InterruptedException {
        while (pending.get() > 0) {
            Thread.sleep(5);
        }
    }

    void shutdown() { deliveryExecutor.shutdown(); }

    String  getSubscriberId() { return subscriber.getId(); }
    boolean isActive()        { return active; }
    int     getOffset()       { return offset; }
}
