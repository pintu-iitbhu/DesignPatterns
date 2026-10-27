# Job Scheduler — LLD Design Document

---

## Functional Requirements
1. Schedule a job to run **once** after a given delay.
2. Schedule a job to run **repeatedly at a fixed rate** (consistent cadence).
3. Schedule a job to run **repeatedly with a fixed delay** (gap between end of one run and start of next).
4. **Cancel** a scheduled job by its ID.
5. **Graceful shutdown** — in-flight jobs finish; no new jobs accepted.

## Non-Functional Requirements
1. Thread-safe — concurrent `schedule()`, `cancel()`, and `shutdown()` calls must be safe.
2. Low-latency dispatch — jobs must fire at (or very close to) their scheduled time.
3. Fault-tolerant — a failing task must not crash the scheduler or affect other jobs.
4. Extensible — adding new job types or execution backends must not require changes to core scheduling logic.

---

## Class Diagram

```
com.rcpky
│
├── models/
│   ├── JobStatus          (PENDING → RUNNING → COMPLETED/FAILED; PENDING → CANCELLED)
│   ├── ReschedulingPolicy (@FunctionalInterface — Strategy for next run time)
│   ├── JobType            (ONCE, FIXED_RATE, FIXED_DELAY — carries ReschedulingPolicy)
│   └── Job                (id, task, type, period, volatile nextRunTime, AtomicRef<status>)
│
├── store/
│   ├── IJobQueue          (enqueue, awaitNext, remove, contains, shutdown)
│   └── PriorityJobQueue   (PriorityQueue<Job> + ConcurrentHashMap registry + ReentrantLock)
│
├── executors/
│   ├── ITaskExecutor         (execute, shutdown, awaitTermination)
│   ├── ThreadPoolTaskExecutor (fixed OS thread pool — best for CPU-bound jobs)
│   └── VirtualThreadExecutor  (one virtual thread per task — best for I/O-bound jobs)
│
└── scheduler/
    ├── IJobScheduler      (scheduleOnce, scheduleAtFixedRate, scheduleWithFixedDelay, cancel, shutdown)
    └── JobScheduler       (dispatcher thread + IJobQueue + ITaskExecutor)
```

---

## Core Design Decisions

### 1. Producer-Consumer with a Single Dispatcher Thread
- **One dispatcher thread** calls `jobQueue.awaitNext()` in a loop — it only decides WHAT to dispatch and WHEN.
- **N worker threads** (or virtual threads) execute the actual task logic.
- This separation means a slow task never delays dispatch of other jobs.

### 2. PriorityQueue (min-heap) for Ordering
- Jobs are ordered by `nextRunTime`. The dispatcher always peeks the earliest-due job.
- `O(log n)` insert and poll.
- The dispatcher uses `awaitNanos(delay)` to sleep exactly until the head job is due. If a new earlier job is enqueued, `signalAll()` wakes the dispatcher to re-evaluate.

### 3. Single ReentrantLock Covers peek → wait → poll
- `PriorityQueue` is not thread-safe.
- `peek()` + `awaitNanos()` + `poll()` must be **atomic** — releasing the lock between them would allow another thread to insert an earlier job undetected.

### 4. ConcurrentHashMap as Job Registry
- O(1) lookup/remove by job ID for `cancel()` and reschedule checks.
- Does not need the main lock — `put/remove/containsKey` are independently thread-safe.

### 5. Lazy Deletion for Cancellation
- `cancel()` removes the job from the **registry only** (O(1)).
- The entry stays in the `PriorityQueue` until it naturally reaches the head.
- When dispatched, the status check (`CANCELLED`) drops it silently.
- Avoids `PriorityQueue.remove()` which is O(n) and requires holding the lock.

### 6. Strategy Pattern — ReschedulingPolicy
- `FIXED_RATE`: `nextRunTime = lastScheduled + period` — compensates for execution time, keeps wall-clock cadence.
- `FIXED_DELAY`: `nextRunTime = now + period` — guarantees a minimum gap after each execution.
- `JobScheduler.reschedule()` delegates to `job.getType().nextRunTime(...)` — zero knowledge of which policy.

### 7. Dependency Inversion — IJobQueue + ITaskExecutor
- `JobScheduler` has no `new PriorityQueue`, no `new ThreadPoolExecutor`.
- Both are injected — allows swapping storage (e.g. Redis queue) or execution (e.g. virtual threads) without touching the scheduler.

### 8. CAS for Status Transitions (AtomicReference)
- `compareAndSet(PENDING, RUNNING)` in `runJob()` prevents double-execution.
- `compareAndSet(PENDING, CANCELLED)` in `cancel()` is race-free with dispatch.
- Chosen over `synchronized` because only single-field atomic transitions are needed.

### 9. volatile for nextRunTime and running Flag
- `nextRunTime` is written by a worker thread (reschedule) and read by the dispatcher — `volatile` ensures cross-thread visibility without a full lock.
- `running` is written by `shutdown()` and read by `dispatch()` — same reasoning.

---

## Thread Interaction Diagram

```
schedule()          cancel()           shutdown()
    │                   │                   │
    ▼                   ▼                   ▼
jobQueue.enqueue()  registry.remove()   running=false
signalAll() ──────►                     jobQueue.shutdown()
                                        signalAll() ──────►
                                                           │
        ┌──────────────────────────────────────────────────┘
        ▼
  dispatcher thread (single)
  jobQueue.awaitNext()  ← blocks until job is due or shutdown
        │
        ▼  (if not CANCELLED)
  taskExecutor.execute(runJob)
        │
        ▼
  worker thread pool
  runJob() → task.run() → reschedule() or complete()
```

---

## Interview Questions & Answers

### Core Design

**Q: Why use a PriorityQueue instead of a simple list or delay queue?**
> PriorityQueue (min-heap) guarantees `O(log n)` insert and `O(1)` peek. The dispatcher always knows the next due job without scanning all jobs. A list would require `O(n)` scan. `DelayQueue` from JDK does the same thing internally but couples storage to blocking — we separated them behind `IJobQueue`.

**Q: Why do you have a single dispatcher thread instead of letting workers pick jobs directly?**
> Single dispatcher eliminates contention on the PriorityQueue. If multiple threads polled the queue, they'd all need to hold the lock simultaneously. With one dispatcher, the lock is only ever held by one thread at a time. Workers get work handed to them — they never touch the queue.

**Q: How does your cancel() handle a job that is currently RUNNING?**
> It removes the job from the registry immediately (O(1)). The CAS `PENDING→CANCELLED` will fail because the status is `RUNNING` — that's intentional. The running task completes normally, but when `runJob()` calls `reschedule()`, `jobQueue.contains()` returns false (job removed from registry) so it is not re-enqueued. The job stops after its current execution.

**Q: What happens if a job's task throws an exception?**
> `runJob()` wraps the task in a try-catch. The status transitions to `FAILED`. The exception is logged. For recurring jobs, we do NOT reschedule after failure — this prevents an infinitely crashing job from flooding the queue. The scheduler continues running normally for all other jobs.

**Q: How do you ensure a job isn't executed twice (double-execution)?**
> The CAS `compareAndSet(PENDING, RUNNING)` at the start of `runJob()`. If two threads somehow both received the same job, only one CAS succeeds. The other thread bails out immediately. In practice, the single dispatcher makes this impossible, but the CAS is defensive for future changes.

**Q: Why is nextRunTime volatile instead of AtomicLong?**
> Only one thread writes it (worker thread in `reschedule()`), and one thread reads it (dispatcher). No compound read-modify-write is needed. `volatile` provides the visibility guarantee at lower overhead than `AtomicLong` which also provides CAS operations we don't need here.

**Q: How does FIXED_RATE differ from FIXED_DELAY?**
> `FIXED_RATE`: next = `lastScheduled + period`. If a job takes 200ms and the period is 1000ms, the next run fires 800ms later. Cadence is wall-clock consistent.
> `FIXED_DELAY`: next = `now + period`. Same numbers: next run fires 1000ms after the job finishes. Guarantees a rest gap regardless of execution time. Use FIXED_RATE for heartbeats/metrics; FIXED_DELAY for polling where piling up is dangerous.

**Q: What is lazy deletion and why do you use it here?**
> When `cancel()` is called, we remove the job from the registry but leave it in the PriorityQueue. `PriorityQueue.remove(element)` is O(n) and requires holding the lock, blocking the dispatcher. Lazy deletion is O(1): when the cancelled job reaches the head of the heap and gets polled, the dispatcher checks `getStatus() == CANCELLED` and discards it in O(1).

**Q: Why use ReentrantLock + Condition instead of synchronized + wait/notify?**
> `Condition.awaitNanos(delay)` — we need timed waiting with nanosecond precision. `Object.wait(timeout)` only takes milliseconds. Also, `ReentrantLock` gives explicit lock/unlock control and multiple condition variables if needed (e.g. separate conditions for empty queue vs shutdown).

---

### Design & Extensibility

**Q: How would you add a new job type, say EXPONENTIAL_BACKOFF?**
> 1. Add a new lambda to `ReschedulingPolicy`: `(lastScheduled, period) -> lastScheduled + someBackoffFormula`.
> 2. Add `EXPONENTIAL_BACKOFF(ReschedulingPolicy.EXPONENTIAL_BACKOFF)` to `JobType`.
> `JobScheduler` needs zero changes. That's the Strategy Pattern payoff.

**Q: How would you make this distributed (multi-node)?**
> Replace `PriorityJobQueue` with a distributed `IJobQueue` implementation backed by Redis Sorted Sets (score = nextRunTime) or Kafka. The `JobScheduler` doesn't change — it still calls `awaitNext()`. Each node competes for jobs using distributed locking (Redlock) or Kafka consumer groups.

**Q: How would you persist jobs across restarts?**
> Implement a `PersistentJobQueue` that wraps a DB (PostgreSQL, MySQL). On startup, reload PENDING jobs from DB into the in-memory heap. On `enqueue()`, write to DB first, then to heap. On `remove()`, update DB status. The `IJobQueue` interface makes this swap transparent to `JobScheduler`.

---

## Possible Extensions to Suggest in Interview

### 1. Builder Pattern for Job
As job config grows (priority, retry count, timeout, tags, name), the constructor becomes unwieldy.
```java
Job job = new Job.Builder(() -> doWork())
    .delay(500).period(1000).type(JobType.FIXED_RATE)
    .priority(Priority.HIGH).retries(3).timeout(5000)
    .build();
```

### 2. Observer / Listener for Job Lifecycle Events
Hook metrics, alerting, and audit logs without touching `JobScheduler`.
```java
scheduler.addListener(new JobLifecycleListener() {
    void onStarted(Job job)             { metrics.increment("job.started"); }
    void onCompleted(Job job)           { metrics.record("job.latency", ...); }
    void onFailed(Job job, Exception e) { alerting.fire(job, e); }
    void onCancelled(Job job)           { auditLog.write(job); }
});
```

### 3. Job Priority
Add a `priority` field to `Job`. `PriorityJobQueue` orders by `(nextRunTime, priority)` so high-priority jobs scheduled at the same time run first.

### 4. Retry Policy
On failure, instead of immediately marking FAILED, consult a `RetryPolicy`:
```java
RetryPolicy.fixed(3, 1000)          // retry 3 times, 1s apart
RetryPolicy.exponential(3, 1000)    // 1s, 2s, 4s backoff
RetryPolicy.none()                  // current behaviour
```

### 5. Job Timeout Enforcement
Wrap the task in a `Future` with a timeout. If the task exceeds the limit, cancel the `Future` and mark the job `TIMED_OUT`.

### 6. CRON Expression Support
Add `JobType.CRON` with a policy that parses a cron string and computes the next fire time using a library like `cron-utils` or a hand-rolled parser.

### 7. Virtual Thread Executor (already implemented)
Swap `ThreadPoolTaskExecutor` for `VirtualThreadExecutor` for I/O-bound workloads. Zero code changes to `JobScheduler`.

### 8. Metrics & Monitoring
- Queue depth gauge
- Job execution latency histogram
- Failure rate counter
- Thread pool utilisation

### 9. Persistent / Distributed Queue
Replace `PriorityJobQueue` with a Redis Sorted Set backed implementation. Score = `nextRunTime`. Multiple scheduler nodes compete with `SETNX` (distributed lock) for job ownership.

### 10. Dead Letter Queue (DLQ)
After exhausting retries, route the job to a DLQ for manual inspection or replay instead of silently dropping it.

---

## Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| **Command** | `Runnable` in `Job` | Scheduler knows nothing about what the task does |
| **Strategy** | `ReschedulingPolicy` on `JobType` | Swap scheduling algorithm without changing scheduler |
| **Dependency Inversion** | `IJobQueue`, `ITaskExecutor` | Swap storage/execution backend without changing `JobScheduler` |
| **Producer-Consumer** | Dispatcher + worker pool | Decouple job detection from job execution |
| **Monitor Object** | `ReentrantLock + Condition` in `PriorityJobQueue` | Safe shared-state access with precise timed waiting |
| **Lazy Deletion** | `cancel()` in registry vs heap | O(1) cancel instead of O(n) heap removal |
