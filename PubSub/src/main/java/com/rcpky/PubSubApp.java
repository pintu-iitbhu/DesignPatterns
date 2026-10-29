package com.rcpky;

import com.rcpky.core.Broker;
import com.rcpky.core.ISubscriber;
import com.rcpky.dlq.IDeadLetterQueue;
import com.rcpky.dlq.impl.InMemoryDeadLetterQueue;
import com.rcpky.filter.KeyValueFilter;
import com.rcpky.model.Message;
import com.rcpky.retention.RetentionPolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo driver — seven self-contained scenarios.
 *
 *  1. Basic fan-out        — multiple subscribers, all receive all messages in order
 *  2. Late subscriber      — subscribes after messages exist; gets full history
 *  3. Filtered consumption — key-value filter; only matching messages delivered
 *  4. Unsubscribe          — stops delivery after unsubscribe
 *  5. Consumer unavailable — paused consumer accumulates messages; delivered on resume
 *  6. DLQ on failure       — onMessage() fails, retries, then dead-letters to DLQ
 *  7. Retention policy     — topic with max-3 log; late subscriber sees only retained
 *
 * Delivery is now async (per-subscriber single-thread executor).
 * await(broker) drains all in-flight tasks before reading counts.
 */
public class PubSubApp {

    // ── Subscriber implementations ────────────────────────────────────────────

    static class PrintSubscriber implements ISubscriber {
        private final String id;
        int messageCount = 0;

        PrintSubscriber(String id) { this.id = id; }

        @Override public String getId() { return id; }

        @Override
        public void onMessage(Message message) {
            messageCount++;
            System.out.printf("  [%-18s] (%d) payload='%s'  metadata=%s%n",
                    id, messageCount, message.getPayload(), message.getMetadata());
        }
    }

    static class FailingSubscriber implements ISubscriber {
        private final String id;
        int errorCount = 0;

        FailingSubscriber(String id) { this.id = id; }

        @Override public String getId() { return id; }

        @Override
        public void onMessage(Message message) {
            throw new RuntimeException("Simulated failure for: " + message.getPayload());
        }

        @Override
        public void onError(Message message, Exception e) {
            errorCount++;
            System.out.printf("  [%-18s] ERROR (%d) payload='%s' reason='%s'%n",
                    id, errorCount, message.getPayload(), e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void header(String title) {
        System.out.println("\n" + "═".repeat(62));
        System.out.println("  " + title);
        System.out.println("═".repeat(62));
    }

    private static Map<String, String> meta(String... kvPairs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kvPairs.length - 1; i += 2) m.put(kvPairs[i], kvPairs[i + 1]);
        return m;
    }

    /** Drain all in-flight async deliveries before reading message counts. */
    private static void await(Broker broker) {
        try { broker.awaitAllDeliveries(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── Main ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        scenario1_basicFanOut();
        scenario2_lateSubscriber();
        scenario3_filteredConsumption();
        scenario4_unsubscribe();
        scenario5_unavailableConsumer();
        scenario6_dlqOnFailure();
        scenario7_retentionPolicy();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Basic Fan-Out
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario1_basicFanOut() {
        header("Scenario 1 — Basic Fan-Out (2 subscribers)");
        Broker.reset();
        Broker broker = Broker.getInstance();
        broker.createTopic("news");

        PrintSubscriber alice = new PrintSubscriber("Alice");
        PrintSubscriber bob   = new PrintSubscriber("Bob");
        broker.subscribe("news", alice);
        broker.subscribe("news", bob);

        broker.publish("news", "Breaking: Market hits all-time high");
        broker.publish("news", "Tech: New AI model released");
        broker.publish("news", "Sports: Local team wins championship");

        await(broker);
        System.out.printf("%nAlice received %d messages | Bob received %d messages%n",
                alice.messageCount, bob.messageCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Late Subscriber
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario2_lateSubscriber() {
        header("Scenario 2 — Late Subscriber (receives full history)");
        Broker.reset();
        Broker broker = Broker.getInstance();
        broker.createTopic("events");

        broker.publish("events", "Event A - published before Charlie subscribed");
        broker.publish("events", "Event B - published before Charlie subscribed");

        System.out.println("  [Charlie subscribes now — expecting replay of A and B]");
        PrintSubscriber charlie = new PrintSubscriber("Charlie");
        broker.subscribe("events", charlie);

        broker.publish("events", "Event C - published after Charlie subscribed");

        await(broker);
        System.out.printf("%nCharlie received %d messages (expected 3)%n", charlie.messageCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Filtered Consumption
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario3_filteredConsumption() {
        header("Scenario 3 — Filtered Consumption (region=EU only)");
        Broker.reset();
        Broker broker = Broker.getInstance();
        broker.createTopic("orders");

        PrintSubscriber diana = new PrintSubscriber("Diana(EU-only)");
        broker.subscribe("orders", diana, new KeyValueFilter(meta("region", "EU")));

        broker.publish("orders", "Order #1 from US customer",   meta("region", "US"));
        broker.publish("orders", "Order #2 from EU customer",   meta("region", "EU"));
        broker.publish("orders", "Order #3 from APAC customer", meta("region", "APAC"));
        broker.publish("orders", "Order #4 from EU customer",   meta("region", "EU"));

        await(broker);
        System.out.printf("%nDiana received %d messages (expected 2 — EU orders only)%n",
                diana.messageCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Unsubscribe
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario4_unsubscribe() {
        header("Scenario 4 — Unsubscribe Mid-Stream");
        Broker.reset();
        Broker broker = Broker.getInstance();
        broker.createTopic("alerts");

        PrintSubscriber eve = new PrintSubscriber("Eve");
        broker.subscribe("alerts", eve);

        broker.publish("alerts", "Alert #1 — Eve should receive this");
        await(broker);

        System.out.println("  [Eve unsubscribes]");
        broker.unsubscribe("alerts", eve.getId());

        broker.publish("alerts", "Alert #2 — Eve must NOT receive this");
        await(broker);

        System.out.printf("%nEve received %d message(s) (expected 1)%n", eve.messageCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Consumer Unavailable (pause / resume)
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario5_unavailableConsumer() {
        header("Scenario 5 — Consumer Unavailable (pause → catch-up on resume)");
        Broker.reset();
        Broker broker = Broker.getInstance();
        broker.createTopic("logs");

        PrintSubscriber frank = new PrintSubscriber("Frank");
        broker.subscribe("logs", frank);

        broker.publish("logs", "Log entry #1 — Frank is active");
        await(broker);

        System.out.println("  [Frank goes offline — paused]");
        broker.pause("logs", frank.getId());

        broker.publish("logs", "Log entry #2 — Frank is paused");
        broker.publish("logs", "Log entry #3 — Frank is still paused");
        broker.publish("logs", "Log entry #4 — Frank is still paused");
        await(broker);

        System.out.printf("  Frank received so far: %d message(s)%n", frank.messageCount);

        System.out.println("  [Frank comes back online — resumed]");
        broker.resume("logs", frank.getId());
        await(broker);

        System.out.printf("%nFrank received %d message(s) total (expected 4 — guaranteed delivery)%n",
                frank.messageCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: DLQ on Delivery Failure
    //   A subscriber that always throws. maxRetries=2 → 3 total attempts per
    //   message. After exhaustion the message lands in the dead-letter queue.
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario6_dlqOnFailure() {
        header("Scenario 6 — DLQ on Delivery Failure (2 retries → dead-letter)");
        IDeadLetterQueue dlq = new InMemoryDeadLetterQueue();
        Broker.reset(dlq);
        Broker broker = Broker.getInstance();
        broker.createTopic("payments");

        FailingSubscriber bad = new FailingSubscriber("BadConsumer");
        broker.subscribe("payments", bad, null, 2);

        broker.publish("payments", "Payment #1 — $200");
        broker.publish("payments", "Payment #2 — $500");

        await(broker);

        System.out.printf("%nBadConsumer onError() called %d time(s)%n", bad.errorCount);
        System.out.printf("DLQ size: %d (expected 2)%n", dlq.size());
        dlq.getAll().forEach(e -> System.out.println("  " + e));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 7: Retention Policy (max 3 messages)
    //   5 messages published to a topic that retains only the last 3.
    //   A late subscriber joins after all 5 publishes and receives only the
    //   3 retained messages — the first 2 have been evicted.
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario7_retentionPolicy() {
        header("Scenario 7 — Retention Policy (max 3 messages in log)");
        Broker.reset();
        Broker broker = Broker.getInstance();
        broker.createTopic("stream", RetentionPolicy.ofMaxMessages(3));

        broker.publish("stream", "Message #1 — will be evicted");
        broker.publish("stream", "Message #2 — will be evicted");
        broker.publish("stream", "Message #3 — retained");
        broker.publish("stream", "Message #4 — retained");
        broker.publish("stream", "Message #5 — retained");

        System.out.println("  [Late subscriber joins; log retains only last 3]");
        PrintSubscriber late = new PrintSubscriber("LateSubscriber");
        broker.subscribe("stream", late);

        await(broker);

        System.out.printf("%nLateSubscriber received %d messages (expected 3 — #1 and #2 evicted)%n",
                late.messageCount);
    }
}
