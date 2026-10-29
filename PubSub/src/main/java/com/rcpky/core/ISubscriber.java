package com.rcpky.core;

import com.rcpky.model.Message;

/**
 * Subscriber (consumer) contract.
 * Implement this interface to receive messages from a topic.
 *
 * onMessage()  — called for each matching message (async, in the subscriber's
 *                dedicated delivery thread — order is guaranteed).
 * onError()    — called when onMessage() fails after all configured retries.
 *                Default is a no-op; override for alerting, metrics, etc.
 */
public interface ISubscriber {
    String getId();
    void onMessage(Message message);

    /** Called after all retries for a message are exhausted. Default: no-op. */
    default void onError(Message message, Exception e) {}
}
