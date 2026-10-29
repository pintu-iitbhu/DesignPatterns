package com.rcpky.dlq.impl;

import com.rcpky.dlq.DeadLetterEntry;
import com.rcpky.dlq.IDeadLetterQueue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory dead-letter queue.
 * Uses CopyOnWriteArrayList: writes (add) are rare, reads (getAll) are frequent.
 */
public class InMemoryDeadLetterQueue implements IDeadLetterQueue {

    private final CopyOnWriteArrayList<DeadLetterEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void add(DeadLetterEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<DeadLetterEntry> getAll() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public int size() {
        return entries.size();
    }
}
