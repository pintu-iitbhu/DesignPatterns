package com.rcpky.dlq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe, in-memory DLQ implementation.
 *
 * Why CopyOnWriteArrayList?
 *   DLQ writes (add) are rare — only when a job exhausts all retries.
 *   DLQ reads (getAll, size) may happen frequently from monitoring code.
 *   CopyOnWriteArrayList optimises for read-heavy access: reads are lock-free
 *   (they operate on a stable snapshot). Writes are O(n) copy, which is
 *   acceptable given the low write frequency of a DLQ.
 *
 * Production extension point: swap this for a persistent store
 *   (Kafka topic, DB table, S3 object) without changing callers — they
 *   depend only on IDeadLetterQueue.
 */
public class InMemoryDeadLetterQueue implements IDeadLetterQueue {

    private final CopyOnWriteArrayList<DeadLetterEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void add(DeadLetterEntry entry) {
        entries.add(entry);
        System.err.println("[DLQ] " + entry);
    }

    @Override
    public List<DeadLetterEntry> getAll() {
        return new ArrayList<>(entries); // defensive copy of the snapshot
    }

    @Override
    public int size() {
        return entries.size();
    }
}
