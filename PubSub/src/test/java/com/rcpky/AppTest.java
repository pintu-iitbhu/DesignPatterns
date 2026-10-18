package com.rcpky;

import com.rcpky.core.Broker;
import com.rcpky.core.ISubscriber;
import com.rcpky.filter.KeyValueFilter;
import com.rcpky.model.Message;
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

    private static Map<String, String> meta(String... kvPairs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kvPairs.length - 1; i += 2) m.put(kvPairs[i], kvPairs[i + 1]);
        return m;
    }

    @Before
    public void setUp() {
        Broker.reset();
        broker = Broker.getInstance();
    }

    @Test
    public void fanOut_allSubscribersReceiveAllMessages() {
        broker.createTopic("t1");
        CapturingSubscriber s1 = new CapturingSubscriber("s1");
        CapturingSubscriber s2 = new CapturingSubscriber("s2");
        broker.subscribe("t1", s1);
        broker.subscribe("t1", s2);

        broker.publish("t1", "msg1");
        broker.publish("t1", "msg2");
        broker.publish("t1", "msg3");

        assertEquals(3, s1.received.size());
        assertEquals(3, s2.received.size());
        assertEquals("msg1", s1.received.get(0).getPayload());
        assertEquals("msg3", s2.received.get(2).getPayload());
    }

    @Test
    public void lateSubscriber_receivesFullHistory() {
        broker.createTopic("t2");
        broker.publish("t2", "past-1");
        broker.publish("t2", "past-2");

        CapturingSubscriber late = new CapturingSubscriber("late");
        broker.subscribe("t2", late);

        broker.publish("t2", "new-3");

        assertEquals(3, late.received.size());
        assertEquals("past-1", late.received.get(0).getPayload());
        assertEquals("new-3",  late.received.get(2).getPayload());
    }

    @Test
    public void filteredSubscription_onlyMatchingMessagesDelivered() {
        broker.createTopic("t3");
        CapturingSubscriber filtered = new CapturingSubscriber("eu-only");
        broker.subscribe("t3", filtered, new KeyValueFilter(meta("region", "EU")));

        broker.publish("t3", "us-order",   meta("region", "US"));
        broker.publish("t3", "eu-order-1", meta("region", "EU"));
        broker.publish("t3", "eu-order-2", meta("region", "EU"));
        broker.publish("t3", "apac-order", meta("region", "APAC"));

        assertEquals(2, filtered.received.size());
        assertEquals("eu-order-1", filtered.received.get(0).getPayload());
        assertEquals("eu-order-2", filtered.received.get(1).getPayload());
    }

    @Test
    public void unsubscribe_stopsDelivery() {
        broker.createTopic("t4");
        CapturingSubscriber s = new CapturingSubscriber("eve");
        broker.subscribe("t4", s);

        broker.publish("t4", "before-unsub");
        broker.unsubscribe("t4", s.getId());
        broker.publish("t4", "after-unsub");

        assertEquals(1, s.received.size());
        assertEquals("before-unsub", s.received.get(0).getPayload());
    }

    @Test
    public void pauseResume_guaranteedDeliveryOfMissedMessages() {
        broker.createTopic("t5");
        CapturingSubscriber s = new CapturingSubscriber("frank");
        broker.subscribe("t5", s);

        broker.publish("t5", "before-pause");
        broker.pause("t5", s.getId());

        broker.publish("t5", "during-pause-1");
        broker.publish("t5", "during-pause-2");
        broker.publish("t5", "during-pause-3");

        assertEquals("No messages during pause", 1, s.received.size());

        broker.resume("t5", s.getId());

        assertEquals("All messages after resume", 4, s.received.size());
        assertEquals("during-pause-1", s.received.get(1).getPayload());
        assertEquals("during-pause-3", s.received.get(3).getPayload());
    }

    @Test
    public void resubscribe_isIdempotent() {
        broker.createTopic("t6");
        CapturingSubscriber s = new CapturingSubscriber("idem");
        broker.subscribe("t6", s);
        broker.subscribe("t6", s);

        broker.publish("t6", "only-once");

        assertEquals(1, s.received.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void publishToUnknownTopic_throws() {
        broker.publish("non-existent", "boom");
    }
}
