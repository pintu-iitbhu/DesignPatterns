package com.rcpky.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuditRepository implements AuditRepository {

    private final Map<String, List<AuditEvent>> byTransactionId = new ConcurrentHashMap<>();

    @Override
    public void save(AuditEvent event) {
        byTransactionId.computeIfAbsent(event.transactionId(), k -> new ArrayList<>()).add(event);
    }

    @Override
    public List<AuditEvent> findByTransactionId(String transactionId) {
        return List.copyOf(byTransactionId.getOrDefault(transactionId, List.of()));
    }

    @Override
    public List<AuditEvent> findAll() {
        return byTransactionId.values().stream().flatMap(List::stream).toList();
    }
}
