package com.rcpky.dlq;

import java.util.List;

/**
 * Contract for a dead-letter store.
 *
 * A DLQ is a sink, not a processing queue — it only receives entries;
 * it never feeds them back into the main scheduler automatically.
 * Replaying DLQ entries is an explicit, operator-driven action.
 *
 * Why not extend IJobQueue?
 *   IJobQueue is a timed-delivery queue with blocking semantics.
 *   A DLQ is an append-only audit log with read access for inspection.
 *   Merging the two would violate the Interface Segregation Principle.
 */
public interface IDeadLetterQueue {

    // Appends a dead-lettered job entry. Thread-safe; must not block.
    void add(DeadLetterEntry entry);

    // Returns a snapshot of all dead-lettered entries (oldest first).
    List<DeadLetterEntry> getAll();

    // Total number of entries ever added (monotonically increasing).
    int size();
}
