# Job Scheduler — LLD Design Document

---

## Functional Requirements

### Core Scheduling
1. Schedule a job to run **once** after a given delay.
2. Schedule a job to run **repeatedly at a fixed rate** (consistent wall-clock cadence).
3. Schedule a job to run **repeatedly with a fixed delay** (guaranteed gap between end of one run and start of next).
4. **Cancel** a scheduled job by its ID at any time.
5. **Graceful shutdown** — in-flight jobs finish; no new jobs accepted.

### Resilience (Job Processing Service)
6. **Leases** — each job execution has an optional time-bound ownership window (`leaseTimeoutMs`). If the task does not finish within the window, it is forcibly cancelled and treated as failed.
7. **Retries** — on failure (exception or lease expiry), automatically re-attempt the job up to `maxRetries` times using **exponential backoff** (`retryDelayMs × 2^attempt`).
8. **Dead-Letter Queue (DLQ)** — jobs that exhaust all retry attempts are routed to a DLQ for inspection and optional replay, rather than silently discarded.

---

## Non-Functional Requirements
1. **Thread-safe** — concurrent `schedule()`, `cancel()`, and `shutdown()` calls must be safe.
2. **Low-latency dispatch** — jobs must fire at (or very close to) their scheduled time.
3. **Fault-tolerant** — a failing or timed-out task must not crash the scheduler or affect other jobs.
4. **Extensible** — adding new job types, execution backends, or DLQ implementations must not require changes to core scheduling logic.
5. **Deadlock-free** — lease enforcement must not starve the worker thread pool.

---

## Class Diagram

```
com.rcpky
│
├── models/
│   ├── JobStatus          (PENDING → RUNNING → COMPLETED / FAILED / DEAD_LETTERED
│   │                       PENDING → CANCELLED; RUNNING → PENDING on retry)
│   ├── ReschedulingPolicy (@FunctionalInterface — Strategy for next run time)
│   ├── JobType            (ONCE, FIXED_RATE, FIXED_DELAY — carries ReschedulingPolicy)
│   └── Job                (id, task, type, period, maxRetries, retryDelayMs,
│                           leaseTimeoutMs, volatile nextRunTime,
│                           AtomicReference<JobStatus>, AtomicInteger retryCount)
│
├── store/
│   ├── IJobQueue          (enqueue, awaitNext, remove, contains, shutdown)
│   └── PriorityJobQueue   (PriorityQueue<Job> + ConcurrentHashMap registry + ReentrantLock)
│
├── executors/
│   ├── ITaskExecutor              (execute, submit→Future<?>, shutdown, awaitTermination)
│   ├── ThreadPoolTaskExecutor     (fixed OS thread pool — CPU-bound jobs, backpressure)
│   ├── CachedThreadPoolTaskExecutor (unbounded cached pool — default lease executor)
│   └── VirtualThreadExecutor      (one virtual thread per task — I/O-bound jobs)
│
├── dlq/
│   ├── IDeadLetterQueue   (add, getAll, size)
│   ├── InMemoryDeadLetterQueue (CopyOnWriteArrayList — read-heavy, write-rare)
│   └── DeadLetterEntry    (immutable: jobId, jobType, failureReason,
│                           totalAttempts, deadLetteredAt)
│
└── scheduler/
    ├── IJobScheduler      (scheduleOnce, scheduleAtFixedRate, scheduleWithFixedDelay,
    │                       scheduleWithOptions, cancel, shutdown)
    └── JobScheduler       (dispatcher thread + IJobQueue + taskExecutor
                            + leaseExecutor + IDeadLetterQueue)
```

---

## Job Lifecycle State Machine

```
                    ┌─────────────────────────────────────────┐
                    │              PENDING                     │
                    └──────┬──────────────────────────────────┘
                           │  dispatcher picks up (CAS)
                           ▼
                    ┌─────────────────────────────────────────┐
                    │              RUNNING                     │
                    └──────┬────────────┬─────────────────────┘
           success         │            │  failure / lease expiry
          ┌────────────────┘            └──────────────────────┐
          │                                                      │
   isRecurring?                                          retriesUsed < maxRetries?
    YES       NO                                          YES            NO
     │         │                                           │              │
     ▼         ▼                                           ▼              ▼
  PENDING   COMPLETED                                   PENDING      DLQ wired?
 (reschedule)  (remove)                              (re-enqueue     YES      NO
                                                    with backoff)    │        │
                                                                      ▼        ▼
                                             PENDING → CANCELLED  DEAD_   FAILED
                                              (cancel() called)  LETTERED
```

---

## Core Design Decisions

### 1. Producer-Consumer with a Single Dispatcher Thread
- **One dispatcher thread** calls `jobQueue.awaitNext()` in a loop — it only decides WHAT to dispatch and WHEN.
- **N worker threads** (or virtual threads) execute the actual task logic via `taskExecutor`.
- Slow or failing tasks never delay dispatch of other jobs.

### 2. PriorityQueue (min-heap) for Ordering
- Jobs are ordered by `nextRunTime`. The dispatcher always peeks the earliest-due job.
- `O(log n)` insert and poll; `O(1)` peek.
- `awaitNanos(delay)` sleeps exactly until the head job is due. A new earlier job wakes the dispatcher via `signalAll()`.

### 3. Single ReentrantLock Covers peek → wait → poll
- `PriorityQueue` is not thread-safe.
- `peek()` + `awaitNanos()` + `poll()` must be **atomic** — releasing the lock between them would allow another thread to insert an earlier job undetected.

### 4. ConcurrentHashMap as Job Registry
- O(1) lookup/remove by job ID for `cancel()`, retry re-enqueue, and DLQ routing.
- Does not need the main lock — `put/remove/containsKey` are independently thread-safe.

### 5. Lazy Deletion for Cancellation
- `cancel()` removes the job from the **registry only** (O(1)).
- The entry stays in the `PriorityQueue` until it naturally reaches the head.
- When dispatched, the status check (`CANCELLED`) drops it silently.
- Avoids `PriorityQueue.remove()` which is O(n) and requires holding the lock.

### 6. Strategy Pattern — ReschedulingPolicy
- `FIXED_RATE`: `nextRunTime = lastScheduled + period` — compensates for execution time, keeps wall-clock cadence.
- `FIXED_DELAY`: `nextRunTime = now + period` — guarantees a minimum rest gap after each execution.
- `JobScheduler.reschedule()` delegates to `job.getType().nextRunTime(...)` — zero knowledge of which policy applies.

### 7. Dependency Inversion — IJobQueue, ITaskExecutor, IDeadLetterQueue
- `JobScheduler` depends only on interfaces — never on concrete classes.
- Swap storage (e.g. Redis Sorted Set), execution (e.g. virtual threads), or DLQ (e.g. Kafka topic) without touching the scheduler.

### 8. CAS for Status Transitions (AtomicReference)
- `compareAndSet(PENDING, RUNNING)` in `runJob()` prevents double-execution.
- `compareAndSet(PENDING, CANCELLED)` in `cancel()` is race-free with dispatch.
- `compareAndSet(RUNNING, PENDING)` on retry re-enqueue resets the job for the next attempt.
- Chosen over `synchronized` because only single-field atomic transitions are needed.

### 9. volatile Fields
- `nextRunTime` — written by worker (reschedule/retry), read by dispatcher; `volatile` ensures visibility without a full lock.
- `running` — written by `shutdown()`, read by `dispatch()`; same reasoning.

### 10. Lease Enforcement via Two Separate ITaskExecutor Instances
- `taskExecutor` (fixed pool, N threads): runs `runJob()` — the orchestration wrapper.
- `leaseExecutor` (unbounded cached pool): runs `job.getTask()` when `leaseTimeoutMs > 0`.
- The worker thread (on `taskExecutor`) submits to `leaseExecutor` and blocks on `Future.get(leaseTimeoutMs)`.
- **Why two?** If the task ran on the same fixed pool, all N threads could block on `Future.get()` with no thread left to execute the tasks → deadlock. Separate pools eliminate this entirely.
- `leaseExecutor` defaults to `CachedThreadPoolTaskExecutor` but can be swapped for `VirtualThreadExecutor` for I/O-heavy leased jobs.

### 11. Retry with Exponential Backoff
- `handleFailure()` reads `retryCount.getAndIncrement()` (0-based).
- If `retriesUsed < maxRetries`: re-enqueue with `backoff = retryDelayMs × 2^retriesUsed` (1×, 2×, 4×, …).
- Otherwise: route to DLQ (if wired) or mark `FAILED` (legacy path, no DLQ).
- `AtomicInteger retryCount` is on `Job` — thread-safe, no external lock needed.

### 12. Dead-Letter Queue (DLQ) — Optional Dependency
- `IDeadLetterQueue` is injected into `JobScheduler`. When `null`, failed jobs fall back to `FAILED` (backward-compatible).
- `InMemoryDeadLetterQueue` uses `CopyOnWriteArrayList` — DLQ reads (monitoring) are frequent and lock-free; DLQ writes (retries exhausted) are rare.
- `DeadLetterEntry` is immutable — a historical audit record, not a live scheduling object.

---

## Thread Interaction Diagram

```
schedule()              cancel()             shutdown()
    │                       │                     │
    ▼                       ▼                     ▼
jobQueue.enqueue()    registry.remove()      running=false
signalAll() ─────►    CAS PENDING→CANCELLED  jobQueue.shutdown()
                                             leaseExecutor.shutdown()
                                             taskExecutor.shutdown()
                                             signalAll() ──────────►
                                                                     │
        ┌────────────────────────────────────────────────────────────┘
        ▼
  dispatcher thread (single)
  jobQueue.awaitNext()  ← blocks until job is due or shutdown
        │
        ▼  (if not CANCELLED)
  taskExecutor.execute(runJob)
        │
        ├── leaseTimeoutMs == 0 ──► job.getTask().run()  (direct)
        │
        └── leaseTimeoutMs  > 0 ──► leaseExecutor.submit(job.getTask())
                                         └─ Future.get(leaseTimeoutMs)
                                               ├── ok          → success path
                                               ├── TimeoutEx   → handleFailure("Lease expired")
                                               └── ExecutionEx → handleFailure(cause)
        │
        ├── success + ONCE     → COMPLETED, registry.remove()
        ├── success + recurring → reschedule() → PENDING, jobQueue.enqueue()
        └── failure             → handleFailure()
                                      ├── retriesUsed < maxRetries
                                      │       → PENDING, re-enqueue (backoff)
                                      └── retries exhausted
                                              ├── DLQ wired → DEAD_LETTERED, dlq.add()
                                              └── no DLQ    → FAILED
```

---

## Interview Questions & Answers

### Core Design

**Q: Why use a PriorityQueue instead of a simple list or delay queue?**
> PriorityQueue (min-heap) guarantees `O(log n)` insert and `O(1)` peek. The dispatcher always knows the next due job without scanning all jobs. A list would require `O(n)` scan. `DelayQueue` from JDK does the same internally but couples storage to blocking — we separated them behind `IJobQueue`.

**Q: Why a single dispatcher thread instead of letting workers pick jobs directly?**
> Single dispatcher eliminates contention on the PriorityQueue. If multiple threads polled the queue, they'd all need the lock simultaneously. With one dispatcher, the lock is held by one thread at a time. Workers get work handed to them — they never touch the queue.

**Q: How does cancel() handle a job that is currently RUNNING?**
> It removes the job from the registry immediately (O(1)). The CAS `PENDING→CANCELLED` fails because the status is `RUNNING` — intentional. The running task completes, but `reschedule()` / `handleFailure()` check `jobQueue.contains()` and find it gone, so the job is not re-enqueued. It stops after the current execution.

**Q: What happens if a job's task throws an exception?**
> `runJob()` catches the exception and calls `handleFailure()`. If `maxRetries > 0`, the job is re-enqueued with exponential backoff. After all retries are exhausted it is dead-lettered (if DLQ is wired) or marked `FAILED`. The scheduler continues normally for all other jobs.

**Q: How do you ensure a job isn't executed twice (double-execution)?**
> CAS `compareAndSet(PENDING, RUNNING)` at the start of `runJob()`. Only one thread wins the CAS. The other bails out immediately. The single dispatcher makes this impossible in practice, but the CAS is a defensive guard for future changes.

**Q: Why is nextRunTime volatile instead of AtomicLong?**
> Only one thread writes it (worker thread in `reschedule()` or `handleFailure()`), and one reads it (dispatcher). No compound read-modify-write is needed. `volatile` provides visibility at lower overhead than `AtomicLong`.

**Q: How does FIXED_RATE differ from FIXED_DELAY?**
> `FIXED_RATE`: next = `lastScheduled + period`. Job takes 200ms, period 1000ms → next fires in 800ms. Wall-clock cadence is consistent.
> `FIXED_DELAY`: next = `now + period`. Same numbers → next fires 1000ms after the job finishes. Guarantees a rest gap regardless of execution time. Use FIXED_RATE for heartbeats/metrics; FIXED_DELAY for polling where pile-up is dangerous.

**Q: What is lazy deletion and why use it here?**
> `cancel()` removes the job from the registry but leaves it in the PriorityQueue. `PriorityQueue.remove(element)` is O(n) and requires the lock, blocking the dispatcher. Lazy deletion is O(1): when the job reaches the heap head and is polled, the dispatcher checks `CANCELLED` and discards it.

**Q: Why ReentrantLock + Condition instead of synchronized + wait/notify?**
> `Condition.awaitNanos(delay)` — nanosecond-precision timed waiting. `Object.wait(timeout)` only takes milliseconds. `ReentrantLock` also gives explicit lock control and supports multiple conditions if needed.

**Q: Why do you need two separate ITaskExecutor instances (taskExecutor + leaseExecutor)?**
> `runJob()` runs on a `taskExecutor` worker thread. For lease enforcement it submits the actual task to `leaseExecutor` and **blocks** on `Future.get(timeout)`. If both used the same fixed pool, all N threads could be blocked waiting for tasks that are also waiting for a free thread → deadlock. The `leaseExecutor` (unbounded cached pool) breaks this dependency: it always has a thread available.

**Q: How does lease enforcement work?**
> When `leaseTimeoutMs > 0`, `runJob()` submits `job.getTask()` to `leaseExecutor` and calls `Future.get(leaseTimeoutMs, MILLISECONDS)`. On `TimeoutException` it calls `future.cancel(true)` (sends interrupt to the lease thread) and routes the job to `handleFailure("Lease expired")`. This is deterministic — no interrupt-flag race conditions.

**Q: How does retry work with exponential backoff?**
> `handleFailure()` reads `retryCount.getAndIncrement()` (returns the value before increment). If `retriesUsed < maxRetries`, it computes `backoff = retryDelayMs × 2^retriesUsed` (1×, 2×, 4×, …), sets `job.nextRunTime = now + backoff`, and re-enqueues. The job's status goes back to `PENDING`, ready for the next dispatcher pick-up.

**Q: What is the DLQ and when does a job land there?**
> A Dead-Letter Queue is a sink for permanently failed jobs. A job lands there when it exhausts all retries (`retriesUsed >= maxRetries`) and a `IDeadLetterQueue` is wired into the scheduler. The job's status becomes `DEAD_LETTERED` and a `DeadLetterEntry` (immutable snapshot with failure reason and attempt count) is appended. If no DLQ is wired, the job simply becomes `FAILED` — backward-compatible with the original design.

**Q: Why is InMemoryDeadLetterQueue backed by CopyOnWriteArrayList?**
> DLQ writes are rare (only when retries are exhausted). DLQ reads are frequent (monitoring dashboards, admin tooling). `CopyOnWriteArrayList` is optimised for read-heavy access: reads are lock-free on a stable snapshot. The O(n) copy on write is acceptable given the low DLQ write frequency.

---

### Design & Extensibility

**Q: How would you add a new job type, say CRON?**
> 1. Add a cron-computing lambda to `ReschedulingPolicy`.
> 2. Add `CRON(ReschedulingPolicy.CRON)` to `JobType`.
> `JobScheduler.reschedule()` needs zero changes. Open/Closed Principle.

**Q: How would you make this distributed (multi-node)?**
> Replace `PriorityJobQueue` with a distributed `IJobQueue` backed by Redis Sorted Sets (score = nextRunTime) or Kafka. `JobScheduler` doesn't change. Each node competes for jobs using distributed locking (Redlock) or Kafka consumer groups. Leases become distributed ownership tokens with TTL.

**Q: How would you persist jobs across restarts?**
> Implement `PersistentJobQueue` wrapping a DB. On startup, reload `PENDING` jobs into the in-memory heap. On `enqueue()` write to DB first, then heap. On `remove()`, update DB status. The `IJobQueue` interface makes this swap transparent.

---

## Possible Extensions

### 1. Builder Pattern for Job
As job config grows (priority, tags, name, custom backoff), the 7-arg constructor becomes unwieldy.
```java
Job job = new Job.Builder(() -> doWork())
    .delay(500).period(1000).type(JobType.FIXED_RATE)
    .maxRetries(3).retryDelay(1000).leaseTimeout(5000)
    .build();
```

### 2. Observer / Listener for Job Lifecycle Events
Hook metrics, alerting, and audit logs without touching `JobScheduler`.
```java
scheduler.addListener(new JobLifecycleListener() {
    void onStarted(Job job)             { metrics.increment("job.started"); }
    void onCompleted(Job job)           { metrics.record("job.latency", ...); }
    void onFailed(Job job, Exception e) { alerting.fire(job, e); }
    void onDeadLettered(Job job)        { auditLog.write(job); }
});
```

### 3. Job Priority
Add a `priority` field to `Job`. `PriorityJobQueue` orders by `(nextRunTime, priority)` — high-priority jobs scheduled at the same time run first.

### 4. CRON Expression Support
Add `JobType.CRON` with a `ReschedulingPolicy` that parses a cron expression and computes the next fire time (e.g. via `cron-utils` or a hand-rolled parser).

### 5. DLQ Replay
Add `IDeadLetterQueue.replay(jobId, IJobScheduler)` — re-submits a dead-lettered job with a fresh retry count, enabling operator-driven recovery without code changes.

### 6. Metrics & Monitoring
- Queue depth gauge
- Job execution latency histogram (p50/p99)
- Retry rate counter
- DLQ depth gauge
- Lease expiry rate
- Thread pool utilisation

### 7. Persistent / Distributed Queue
Replace `PriorityJobQueue` with a Redis Sorted Set backed implementation. Score = `nextRunTime`. Multiple nodes compete with `SETNX` (distributed lock) for job ownership. Leases map to Redis key TTLs.

### 8. Virtual Thread Executor for Leased Jobs *(partially implemented)*
Swap `leaseExecutor` to `VirtualThreadExecutor` — virtual threads park cheaply when blocking on I/O, so 10,000 concurrent I/O-bound leased jobs use only a handful of OS threads.

---

## Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| **Command** | `Runnable` in `Job` | Scheduler knows nothing about what the task does |
| **Strategy** | `ReschedulingPolicy` on `JobType` | Swap scheduling algorithm without changing scheduler |
| **Dependency Inversion** | `IJobQueue`, `ITaskExecutor`, `IDeadLetterQueue` | Swap storage/execution/DLQ backend without touching `JobScheduler` |
| **Producer-Consumer** | Dispatcher + worker pool | Decouple job detection (dispatcher) from job execution (workers) |
| **Monitor Object** | `ReentrantLock + Condition` in `PriorityJobQueue` | Safe shared-state access with nanosecond-precision timed waiting |
| **Lazy Deletion** | `cancel()` — registry only, not heap | O(1) cancel instead of O(n) heap removal |
| **CAS / Lock-Free** | `AtomicReference<JobStatus>`, `AtomicInteger retryCount` | Race-free status transitions and retry counting without `synchronized` |
| **Null Object** | `dlq == null` path in `handleFailure()` | Backward-compatible: no DLQ wired → `FAILED` (legacy behaviour) |
