package com.rcpky.ledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLedgerRepository implements LedgerRepository {

    private final Map<String, List<LedgerEntry>> byTransactionId = new ConcurrentHashMap<>();

    @Override
    public void save(LedgerEntry entry) {
        byTransactionId.computeIfAbsent(entry.transactionId(), k -> new ArrayList<>()).add(entry);
    }

    @Override
    public void saveAll(List<LedgerEntry> entries) {
        entries.forEach(this::save);
    }

    @Override
    public List<LedgerEntry> findByTransactionId(String transactionId) {
        return List.copyOf(byTransactionId.getOrDefault(transactionId, List.of()));
    }

    @Override
    public List<LedgerEntry> findAll() {
        return byTransactionId.values().stream().flatMap(List::stream).toList();
    }
}
