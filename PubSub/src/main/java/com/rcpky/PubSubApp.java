package com.rcpky;

import com.rcpky.core.Broker;
import com.rcpky.core.ISubscriber;
import com.rcpky.filter.KeyValueFilter;
import com.rcpky.model.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo driver — five self-contained scenarios that cover every requirement.
 *
 *  1. Basic fan-out        — multiple subscribers, all receive all messages in order
 *  2. Late subscriber      — subscribes after messages exist; gets full history (offset=0)
 *  3. Filtered consumption — key-value filter; only matching messages delivered
 *  4. Unsubscribe          — stops delivery after unsubscribe
 *  5. Consumer unavailable — paused consumer accumulates messages; delivered on resume
 */
public class PubSubApp {

    // ── Inline subscriber implementation ──────────────────────────────────────

    static class PrintSubscriber implements ISubscriber {
        private final String id;
        int messageCount = 0;

        PrintSubscriber(String id) { this.id = id; }

        @Override public String getId() { return id; }

        @Override
        public void onMessage(Message message) {
            messageCount++;
            System.out.printf("  [%-14s] (%d) payload='%s'  metadata=%s%n",
                    id, messageCount, message.getPayload(), message.getMetadata());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void header(String title) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("  " + title);
        System.out.println("═".repeat(60));
    }

    private static Map<String, String> meta(String... kvPairs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kvPairs.length - 1; i += 2) {
            m.put(kvPairs[i], kvPairs[i + 1]);
        }
        return m;
    }

    // ── Main ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        scenario1_basicFanOut();
        scenario2_lateSubscriber();
        scenario3_filteredConsumption();
        scenario4_unsubscribe();
        scenario5_unavailableConsumer();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: Basic Fan-Out
    //   Two subscribers on the same topic both receive every message in order.
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

        System.out.printf("%nAlice received %d messages | Bob received %d messages%n",
                alice.messageCount, bob.messageCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: Late Subscriber
    //   Charlie subscribes after 2 messages are already published.
    //   Guaranteed delivery: Charlie gets all 2 past messages + 1 new one.
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

        System.out.printf("%nCharlie received %d messages (expected 3)%n", charlie.messageCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Filtered Consumption
    //   Diana subscribes with a key-value filter: region=EU.
    //   Only EU messages are delivered; other messages silently advance offset.
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario3_filteredConsumption() {
        header("Scenario 3 — Filtered Consumption (region=EU only)");
        Broker.reset();
        Broker broker = Broker.getInstance();

        broker.createTopic("orders");

        PrintSubscriber diana = new PrintSubscriber("Diana(EU-only)");
        broker.subscribe("orders", diana,
                new KeyValueFilter(meta("region", "EU")));

        broker.publish("orders", "Order #1 from US customer", meta("region", "US"));
        broker.publish("orders", "Order #2 from EU customer", meta("region", "EU"));
        broker.publish("orders", "Order #3 from APAC customer", meta("region", "APAC"));
        broker.publish("orders", "Order #4 from EU customer", meta("region", "EU"));

        System.out.printf("%nDiana received %d messages (expected 2 — EU orders only)%n",
                diana.messageCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: Unsubscribe
    //   Eve receives the first message, unsubscribes, then a second message
    //   is published — Eve must NOT receive it.
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario4_unsubscribe() {
        header("Scenario 4 — Unsubscribe Mid-Stream");
        Broker.reset();
        Broker broker = Broker.getInstance();

        broker.createTopic("alerts");

        PrintSubscriber eve = new PrintSubscriber("Eve");
        broker.subscribe("alerts", eve);

        broker.publish("alerts", "Alert #1 — Eve should receive this");

        System.out.println("  [Eve unsubscribes]");
        broker.unsubscribe("alerts", eve.getId());

        broker.publish("alerts", "Alert #2 — Eve must NOT receive this");

        System.out.printf("%nEve received %d message(s) (expected 1)%n", eve.messageCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: Consumer Unavailable (pause / resume)
    //   Frank is paused while 3 messages are published.
    //   On resume, all 3 are delivered in order — guaranteed delivery.
    // ─────────────────────────────────────────────────────────────────────────
    static void scenario5_unavailableConsumer() {
        header("Scenario 5 — Consumer Unavailable (pause → catch-up on resume)");
        Broker.reset();
        Broker broker = Broker.getInstance();

        broker.createTopic("logs");

        PrintSubscriber frank = new PrintSubscriber("Frank");
        broker.subscribe("logs", frank);

        broker.publish("logs", "Log entry #1 — Frank is active");

        System.out.println("  [Frank goes offline — paused]");
        broker.pause("logs", frank.getId());

        broker.publish("logs", "Log entry #2 — Frank is paused");
        broker.publish("logs", "Log entry #3 — Frank is still paused");
        broker.publish("logs", "Log entry #4 — Frank is still paused");

        System.out.printf("  Frank received so far: %d message(s)%n", frank.messageCount);

        System.out.println("  [Frank comes back online — resumed]");
        broker.resume("logs", frank.getId());

        System.out.printf("%nFrank received %d message(s) total (expected 4 — guaranteed delivery)%n",
                frank.messageCount);
    }
}
