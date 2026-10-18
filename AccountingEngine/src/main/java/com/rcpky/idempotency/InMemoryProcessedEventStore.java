package com.rcpky.idempotency;

import com.rcpky.engine.EngineResult;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Map<String, EngineResult> store = new ConcurrentHashMap<>();

    @Override
    public boolean isProcessed(String transactionId) {
        return store.containsKey(transactionId);
    }

    @Override
    public void markProcessed(String transactionId, EngineResult result) {
        store.put(transactionId, result);
    }

    @Override
    public Optional<EngineResult> getResult(String transactionId) {
        return Optional.ofNullable(store.get(transactionId));
    }
}
