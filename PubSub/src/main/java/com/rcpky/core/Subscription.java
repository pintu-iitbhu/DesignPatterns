package com.rcpky.core;

import com.rcpky.filter.MessageFilter;
import com.rcpky.model.Message;

import java.util.List;

/**
 * Tracks per-subscriber state within a single topic:
 *   - offset      : next message index to deliver from the topic's log
 *   - filter      : optional predicate; null means "accept all"
 *   - active      : when false the subscriber is considered unavailable;
 *                   messages accumulate in the log (offset frozen) and are
 *                   delivered in-order when the subscriber resumes.
 *
 * Thread safety:
 *   - active is volatile so pause() written by one thread is immediately
 *     visible to a deliver() call on another thread.
 *   - deliver() is synchronized: the check-get-increment of offset is atomic,
 *     preventing two concurrent publish fan-outs from delivering the same
 *     message twice to this subscriber.
 *     (onMessage() is called inside the lock; document that callbacks must
 *     not re-enter the broker synchronously to avoid potential stalls.)
 *
 * Extension points:
 *   - Add retryCount map + DLQ reference for at-least-once ACK semantics.
 *   - Add TTL to bound how long unavailable messages are retained.
 */
class Subscription {

    private final ISubscriber subscriber;
    private final MessageFilter filter;
    private int offset;
    private volatile boolean active;


    Subscription(ISubscriber subscriber, MessageFilter filter) {
        this.subscriber = subscriber;
        this.filter = filter;
        this.offset = 0;
        this.active = true;
    }

    /**
     * Deliver all unread messages from the topic log to this subscriber.
     * Filtered-out messages still advance the offset (silently consumed).
     * Does nothing if the subscriber is currently inactive.
     *
     * Synchronized: guarantees that even if two threads (e.g. two concurrent
     * publishers) both snapshot this subscription and call deliver() at the
     * same time, the offset advances atomically — no message is delivered twice
     * and no message is skipped.
     */
    synchronized void deliver(List<Message> messageLog) {
        if (!active) return;

        while (offset < messageLog.size()) {
            Message msg = messageLog.get(offset);
            offset++;
            if (filter == null || filter.matches(msg)) {
                subscriber.onMessage(msg);
            }
        }
    }

    /** Mark subscriber as unavailable; incoming messages accumulate in log. */
    void pause() {
        this.active = false;
    }

    /**
     * Mark subscriber as available again and immediately catch up
     * on all messages published while it was paused.
     * Setting active before calling deliver() ensures that a concurrent
     * publish() fan-out that races with resume() will not skip delivery.
     */
    void resume(List<Message> messageLog) {
        this.active = true;
        deliver(messageLog);
    }

    String getSubscriberId()  { return subscriber.getId(); }
    boolean isActive()        { return active; }
    int getOffset()           { return offset; }
}
