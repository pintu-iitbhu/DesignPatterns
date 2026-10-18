package com.rcpky.core;

import com.rcpky.model.Message;

/**
 * Subscriber (consumer) contract.
 * Implement this interface to receive messages from a topic.
 *
 * Extension: add onError(Exception) for dead-letter handling,
 * or ack(String messageId) for explicit acknowledgement semantics.
 */
public interface ISubscriber {
    String getId();
    void onMessage(Message message);
}
