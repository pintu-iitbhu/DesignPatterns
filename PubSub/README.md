# Persistent In-Memory Pub/Sub Queue System

## Problem Statement

Design and implement a **persistent (in-memory) Pub/Sub queue mechanism** with guaranteed delivery of every published message for all subscribed consumers on a given topic, in the same order.

### Core Requirements

- Expose topics (create/manage)
- Publisher to push messages against a topic
- Subscribe and unsubscribe from a topic
- A topic can have multiple subscribers (consumers)
- Subscriber to consume messages from a topic
- Maintain the **state of consumption** of each message per topic per consumer
- Maintain **order of message consumption** for each consumer
- **Filtered message consumption** in a topic by a consumer
- Handle cases where **consumers are not available** — decide what to do with those messages

---

## Requirement Clarifying Questions (LLD Interview)

> These are the questions to ask the interviewer before jumping into design.

---

### 1. Topic Management

- **Q1.1** — Who creates topics — publishers, an admin API, or both? Can anyone create a topic on first publish?
- **Q1.2** — Can topics be deleted? If yes, what happens to unacknowledged messages and active subscribers?
- **Q1.3** — Is there a limit on the number of topics?
- **Q1.4** — Are topic names case-sensitive? Any naming constraints (e.g., alphanumeric only)?

---

### 2. Publisher Semantics

- **Q2.1** — Can multiple publishers push to the same topic simultaneously? Do we need to worry about concurrent publishing ordering guarantees?
- **Q2.2** — What is the message payload type? Arbitrary string, JSON blob, or a typed object?
- **Q2.3** — Is there a maximum message size limit?
- **Q2.4** — Should the publish call be synchronous (blocking until at least one subscriber receives) or fire-and-forget (async)?
- **Q2.5** — Do we need publisher acknowledgement / idempotency (duplicate publish detection)?

---

### 3. Subscriber Semantics

- **Q3.1** — Is subscription push-based (broker calls the consumer) or pull-based (consumer polls the broker)? Or both?
- **Q3.2** — Can a subscriber subscribe to a topic that has already received messages? If yes, should they receive **all past messages** (replay from beginning), only **new messages** (from subscription time), or a **configurable offset**?
- **Q3.3** — Can the same subscriber subscribe to the same topic multiple times? Should that be a no-op or an error?
- **Q3.4** — When a subscriber unsubscribes, should their offset/state be retained so they can re-subscribe and resume? Or is it wiped?
- **Q3.5** — Is there a concept of **consumer groups** (multiple consumers sharing load across one logical subscriber group), or is every subscriber independent (each gets all messages)?

---

### 4. Message Delivery & Acknowledgement

- **Q4.1** — What delivery guarantee is expected?
  - **At-least-once**: message may be redelivered after a failure
  - **At-most-once**: message delivered once, may be lost
  - **Exactly-once**: hardest, requires idempotency on consumer side
- **Q4.2** — Does a consumer need to explicitly **acknowledge (ACK)** a message after processing? If not acknowledged, should the broker retry?
- **Q4.3** — If ACK is required, is there a **timeout window** after which an unacknowledged message is redelivered?
- **Q4.4** — Should failed/unacknowledged messages go to a **Dead Letter Queue (DLQ)**? After how many retries?

---

### 5. Message Ordering

- **Q5.1** — Is ordering guaranteed globally across the whole topic, or per-partition/per-key?
- **Q5.2** — If a consumer processes messages out-of-order (due to retries), is that acceptable, or must strict FIFO be enforced?
- **Q5.3** — Should the system support **message priority** within a topic (higher-priority messages delivered first)?

---

### 6. Filtered Message Consumption

- **Q6.1** — What kind of filtering is required? By message attributes/headers, message content (regex), or a custom predicate?
- **Q6.2** — Does filtering happen at the **broker side** (only matching messages are stored/forwarded to that consumer) or **consumer side** (consumer receives all and skips)?
- **Q6.3** — Can filter criteria be updated after subscription without re-subscribing?
- **Q6.4** — Should filtered-out messages be counted as "consumed" (offset advances) or silently skipped (offset still advances but no callback)?

---

### 7. Consumer Unavailability

- **Q7.1** — How do we know a consumer is "unavailable"? Is there a heartbeat/health-check mechanism, or do we treat it as unavailable after a callback timeout?
- **Q7.2** — For unavailable consumers, should messages be **held and delivered when the consumer comes back** (durable subscription), or **dropped after a TTL**?
- **Q7.3** — Is there a configurable **message TTL** (time-to-live) after which undelivered messages are discarded?
- **Q7.4** — Is there a **max queue depth** per consumer? What happens when it's exceeded — block publisher, drop oldest, or drop newest?
- **Q7.5** — Should we distinguish between a consumer that is temporarily slow vs. permanently gone?

---

### 8. Persistence & Storage

- **Q8.1** — "Persistent in-memory" — does this mean messages must survive consumer restarts but NOT broker restarts (pure in-memory)? Or is some disk-backed persistence needed?
- **Q8.2** — Should consumer offsets (read pointers) be persisted across broker restarts?
- **Q8.3** — Is there a **retention policy**? Should messages be deleted after all consumers have acknowledged them, or kept for a configurable window (e.g., last N messages or last T hours)?
- **Q8.4** — Any memory budget concern? Should the system evict old messages under memory pressure?

---

### 9. Concurrency & Thread Safety

- **Q9.1** — Should publishing and consuming happen concurrently from different threads?
- **Q9.2** — Should each consumer run on its own thread, or is a shared thread pool acceptable?
- **Q9.3** — What are the expected throughput and latency SLAs? (Helps size locks vs. lock-free structures)

---

### 10. API / Interface Design

- **Q10.1** — Should the system expose a programmatic Java API, a REST API, or both?
- **Q10.2** — Should consumers implement a callback interface (`onMessage`) for push delivery, or is a `poll()` method sufficient?
- **Q10.3** — Is there a need for batch publish or batch consume APIs?
- **Q10.4** — Do we need observability hooks — e.g., metrics per topic (message count, lag per consumer, throughput)?

---

## Assumptions (after clarification)

> Answers/decisions taken for this implementation:

| # | Assumption |
|---|-----------|
| 1 | Topics are created explicitly; any publisher or the broker can create a topic |
| 2 | Push-based delivery via `ISubscriber.onMessage()` callback; pull via `consumeNext()` also supported |
| 3 | New subscribers receive **all past messages** from offset 0 (durable replay) by default; configurable |
| 4 | **At-least-once** delivery with explicit ACK; unacknowledged messages are retried up to 3 times, then sent to DLQ |
| 5 | Every independent subscriber receives **every message** (fan-out); no consumer groups |
| 6 | Ordering is **per-topic, global FIFO** |
| 7 | Filtering is **consumer-side predicate** passed at subscribe time; filtered messages advance the offset silently |
| 8 | Unavailable consumers accumulate messages in their per-consumer queue (bounded); TTL = configurable (default: infinite) |
| 9 | Purely **in-memory**; no disk persistence; offsets are lost on broker restart |
| 10 | Concurrent publishers and consumers; per-topic `ReentrantReadWriteLock`; per-consumer offset with `AtomicLong` |
| 11 | Message retention: messages are retained until **all subscribers have acknowledged** them (or TTL expires) |

---

## High-Level Design

```
┌────────────┐   publish(topic, msg)    ┌──────────────────────────────────────┐
│  Publisher │ ───────────────────────► │              Broker                  │
└────────────┘                          │  ┌─────────────────────────────────┐ │
                                        │  │  TopicRegistry (ConcurrentMap)  │ │
                                        │  │  topic → Topic                  │ │
                                        │  └───────────────┬─────────────────┘ │
                                        │                  │                   │
                                        │  ┌───────────────▼─────────────────┐ │
                                        │  │  Topic                          │ │
                                        │  │  - messageLog: List<Message>    │ │
                                        │  │  - subscriptions: List<Sub>     │ │
                                        │  └───────────────┬─────────────────┘ │
                                        │                  │ fan-out           │
                                        │  ┌───────────────▼─────────────────┐ │
                                        │  │  SubscriptionManager            │ │
                                        │  │  per subscriber:                │ │
                                        │  │  - offset (AtomicLong)          │ │
                                        │  │  - filter (Predicate<Message>)  │ │
                                        │  │  - status (ACTIVE/PAUSED)       │ │
                                        │  │  - pendingQueue (bounded)       │ │
                                        │  │  - retryCount per message       │ │
                                        │  └───────────────┬─────────────────┘ │
                                        └──────────────────┼────────────────────┘
                                                           │ onMessage(msg) / poll()
                                              ┌────────────▼──────────┐
                                              │     ISubscriber        │
                                              │  (Consumer A, B, C…)  │
                                              └───────────────────────┘
```

---

## Key Classes (planned)

| Class / Interface | Responsibility |
|---|---|
| `Message` | Immutable value object: id, topic, payload, timestamp, headers |
| `Topic` | Holds the ordered message log and list of subscriptions |
| `Subscription` | Per-consumer state: offset, filter, status, retry map |
| `ISubscriber` | Consumer callback interface: `onMessage(Message)`, `ack(messageId)` |
| `IPublisher` | Publisher interface: `publish(String topic, Message msg)` |
| `Broker` | Singleton orchestrator: topic registry, fan-out dispatcher |
| `DeadLetterQueue` | Stores messages that exceeded retry limit |
| `MessageDispatcher` | Background thread pool driving push delivery per subscription |
| `PubSubApp` | Demo / driver class |

---

## Demo Scenarios (planned)

1. Single publisher, multiple subscribers — all receive all messages in order
2. Late subscriber — receives all messages from offset 0
3. Subscriber with filter — only receives matching messages; offset advances past non-matching
4. Consumer temporarily unavailable — messages queued; delivered on reconnect
5. Unacknowledged message retry — retried 3× then sent to DLQ
6. Concurrent publishers on the same topic — ordering preserved
7. Unsubscribe mid-stream — no further delivery; re-subscribe resumes from last ACK'd offset
8. Topic with TTL — messages older than TTL evicted; late consumer sees only live messages
9. Consumer group simulation — two consumers competing on the same subscription slot

---

## Running the Project

```bash
# Build
mvn clean compile

# Run demo
mvn exec:java -Dexec.mainClass="com.rcpky.PubSubApp"

# Tests
mvn test
```
