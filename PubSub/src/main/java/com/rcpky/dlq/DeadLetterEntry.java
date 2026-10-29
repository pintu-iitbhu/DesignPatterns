package com.rcpky.dlq;

import com.rcpky.model.Message;

/**
 * Immutable snapshot of a failed message delivery.
 * Created when a subscriber's onMessage() fails after all configured retries.
 */
public final class DeadLetterEntry {

    private final String  subscriberId;
    private final Message message;
    private final String  failureReason;
    private final int     totalAttempts;
    private final long    deadLetteredAt;

    public DeadLetterEntry(String subscriberId, Message message,
                           String failureReason, int totalAttempts) {
        this.subscriberId   = subscriberId;
        this.message        = message;
        this.failureReason  = failureReason;
        this.totalAttempts  = totalAttempts;
        this.deadLetteredAt = System.currentTimeMillis();
    }

    public String  getSubscriberId()   { return subscriberId; }
    public Message getMessage()        { return message; }
    public String  getFailureReason()  { return failureReason; }
    public int     getTotalAttempts()  { return totalAttempts; }
    public long    getDeadLetteredAt() { return deadLetteredAt; }

    @Override
    public String toString() {
        return "DeadLetterEntry{subscriber='" + subscriberId
                + "', topic='" + message.getTopicName()
                + "', payload='" + message.getPayload()
                + "', reason='" + failureReason
                + "', attempts=" + totalAttempts + "}";
    }
}
