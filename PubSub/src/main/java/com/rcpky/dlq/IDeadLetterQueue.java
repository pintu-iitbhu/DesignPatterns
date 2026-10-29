package com.rcpky.dlq;

import java.util.List;

/**
 * Contract for storing messages that could not be delivered after all retries.
 *
 * Extension: add replay(topicName, IBroker) to re-publish DLQ entries for
 *            reprocessing after the root cause is fixed.
 */
public interface IDeadLetterQueue {
    void add(DeadLetterEntry entry);
    List<DeadLetterEntry> getAll();
    int size();
}
