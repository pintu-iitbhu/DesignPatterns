package com.rcpky;

import com.rcpky.core.Broker;
import com.rcpky.core.ISubscriber;
import com.rcpky.dlq.DeadLetterEntry;
import com.rcpky.dlq.IDeadLetterQueue;
import com.rcpky.dlq.impl.InMemoryDeadLetterQueue;
import com.rcpky.filter.KeyValueFilter;
import com.rcpky.model.Message;
import com.rcpky.retention.RetentionPolicy;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AppTest {

    private Broker broker;

    private static class CapturingSubscriber implements ISubscriber {
        private final String id;
        final List<Message> received = new ArrayList<>();

        CapturingSubscriber(String id) { this.id = id; }

        @Override public String getId() { return id; }

        @Override
        public void onMessage(Message message) { received.add(message); }
    }

    private static class FailingSubscriber implements ISubscriber {
        private final String id;
        final List<Message> errorMessages = new ArrayList<>();

        FailingSubscriber(String id) { this.id = id; }

        @Override public String getId() { return id; }

        @Override
        public void onMessage(Message message) {
            throw new RuntimeException("fail: " + message.getPayload());
        }

        @Override
        public void onError(Message message, Exception e) {
            errorMessages.add(message);
        }
    }

    private static Map<String, String> meta(String... kvPairs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kvPairs.length - 1; i += 2) m.put(kvPairs[i], kvPairs[i + 1]);
        return m;
    }

    private void await() throws InterruptedException {
        broker.awaitAllDeliveries();
    }

    @Before
    public void setUp() {
        Broker.reset();
        broker = Broker.getInstance();
    }

    // ─── Original 7 tests (updated for async delivery) ───────────────────────

    @Test
    public void fanOut_allSubscribersReceiveAllMessages() throws InterruptedException {
        broker.createTopic("t1");
        CapturingSubscriber s1 = new CapturingSubscriber("s1");
        CapturingSubscriber s2 = new CapturingSubscriber("s2");
        broker.subscribe("t1", s1);
        broker.subscribe("t1", s2);

        broker.publish("t1", "msg1");
        broker.publish("t1", "msg2");
        broker.publish("t1", "msg3");
        await();

        assertEquals(3, s1.received.size());
        assertEquals(3, s2.received.size());
        assertEquals("msg1", s1.received.get(0).getPayload());
        assertEquals("msg3", s2.received.get(2).getPayload());
    }

    @Test
    public void lateSubscriber_receivesFullHistory() throws InterruptedException {
        broker.createTopic("t2");
        broker.publish("t2", "past-1");
        broker.publish("t2", "past-2");

        CapturingSubscriber late = new CapturingSubscriber("late");
        broker.subscribe("t2", late);

        broker.publish("t2", "new-3");
        await();

        assertEquals(3, late.received.size());
        assertEquals("past-1", late.received.get(0).getPayload());
        assertEquals("new-3",  late.received.get(2).getPayload());
    }

    @Test
    public void filteredSubscription_onlyMatchingMessagesDelivered() throws InterruptedException {
        broker.createTopic("t3");
        CapturingSubscriber filtered = new CapturingSubscriber("eu-only");
        broker.subscribe("t3", filtered, new KeyValueFilter(meta("region", "EU")));

        broker.publish("t3", "us-order",   meta("region", "US"));
        broker.publish("t3", "eu-order-1", meta("region", "EU"));
        broker.publish("t3", "eu-order-2", meta("region", "EU"));
        broker.publish("t3", "apac-order", meta("region", "APAC"));
        await();

        assertEquals(2, filtered.received.size());
        assertEquals("eu-order-1", filtered.received.get(0).getPayload());
        assertEquals("eu-order-2", filtered.received.get(1).getPayload());
    }

    @Test
    public void unsubscribe_stopsDelivery() throws InterruptedException {
        broker.createTopic("t4");
        CapturingSubscriber s = new CapturingSubscriber("eve");
        broker.subscribe("t4", s);

        broker.publish("t4", "before-unsub");
        await();

        broker.unsubscribe("t4", s.getId());
        broker.publish("t4", "after-unsub");
        await();

        assertEquals(1, s.received.size());
        assertEquals("before-unsub", s.received.get(0).getPayload());
    }

    @Test
    public void pauseResume_guaranteedDeliveryOfMissedMessages() throws InterruptedException {
        broker.createTopic("t5");
        CapturingSubscriber s = new CapturingSubscriber("frank");
        broker.subscribe("t5", s);

        broker.publish("t5", "before-pause");
        await();

        broker.pause("t5", s.getId());

        broker.publish("t5", "during-pause-1");
        broker.publish("t5", "during-pause-2");
        broker.publish("t5", "during-pause-3");
        await();

        assertEquals("No messages during pause", 1, s.received.size());

        broker.resume("t5", s.getId());
        await();

        assertEquals("All messages after resume", 4, s.received.size());
        assertEquals("during-pause-1", s.received.get(1).getPayload());
        assertEquals("during-pause-3", s.received.get(3).getPayload());
    }

    @Test
    public void resubscribe_isIdempotent() throws InterruptedException {
        broker.createTopic("t6");
        CapturingSubscriber s = new CapturingSubscriber("idem");
        broker.subscribe("t6", s);
        broker.subscribe("t6", s);

        broker.publish("t6", "only-once");
        await();

        assertEquals(1, s.received.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void publishToUnknownTopic_throws() {
        broker.publish("non-existent", "boom");
    }

    // ─── DLQ tests ───────────────────────────────────────────────────────────

    @Test
    public void dlq_failedDeliveryAfterRetriesDeadLetters() throws InterruptedException {
        IDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        Broker.reset(dlq);
        broker = Broker.getInstance();

        broker.createTopic("t7");
        FailingSubscriber bad = new FailingSubscriber("bad");
        broker.subscribe("t7", bad, null, 1);  // 1 retry → 2 total attempts

        broker.publish("t7", "msg-a");
        broker.publish("t7", "msg-b");
        await();

        assertEquals(2, dlq.size());
        assertEquals(2, bad.errorMessages.size());

        DeadLetterEntry entry = dlq.getAll().get(0);
        assertEquals("bad", entry.getSubscriberId());
        assertEquals(2, entry.getTotalAttempts());
    }

    @Test
    public void dlq_successfulDeliveryDoesNotDeadLetter() throws InterruptedException {
        IDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        Broker.reset(dlq);
        broker = Broker.getInstance();

        broker.createTopic("t8");
        CapturingSubscriber good = new CapturingSubscriber("good");
        broker.subscribe("t8", good, null, 2);

        broker.publish("t8", "ok-msg");
        await();

        assertEquals(1, good.received.size());
        assertEquals(0, dlq.size());
    }

    // ─── Retention tests ─────────────────────────────────────────────────────

    @Test
    public void retention_maxMessages_lateSubscriberSeesOnlyRetained() throws InterruptedException {
        broker.createTopic("t9", RetentionPolicy.ofMaxMessages(3));

        broker.publish("t9", "evicted-1");
        broker.publish("t9", "evicted-2");
        broker.publish("t9", "retained-3");
        broker.publish("t9", "retained-4");
        broker.publish("t9", "retained-5");

        CapturingSubscriber late = new CapturingSubscriber("late");
        broker.subscribe("t9", late);
        await();

        assertEquals(3, late.received.size());
        assertEquals("retained-3", late.received.get(0).getPayload());
        assertEquals("retained-5", late.received.get(2).getPayload());
    }

    @Test
    public void retention_activeSubscriberReceivesAllBeforeEviction() throws InterruptedException {
        broker.createTopic("t10", RetentionPolicy.ofMaxMessages(2));

        CapturingSubscriber s = new CapturingSubscriber("early");
        broker.subscribe("t10", s);

        broker.publish("t10", "msg-1");
        broker.publish("t10", "msg-2");
        broker.publish("t10", "msg-3");  // evicts msg-1
        await();

        assertEquals(3, s.received.size());
    }

    @Test
    public void retention_ttl_evictsOldMessages() throws InterruptedException {
        broker.createTopic("t11", RetentionPolicy.ofTtl(50));  // 50 ms TTL

        broker.publish("t11", "old-msg");
        Thread.sleep(100);  // let the message expire past the TTL

        broker.publish("t11", "new-msg");  // triggers TTL sweep on publish

        CapturingSubscriber late = new CapturingSubscriber("late-ttl");
        broker.subscribe("t11", late);
        await();

        assertEquals(1, late.received.size());
        assertEquals("new-msg", late.received.get(0).getPayload());
    }
}
