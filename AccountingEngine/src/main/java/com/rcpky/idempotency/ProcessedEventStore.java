package com.rcpky.idempotency;

import com.rcpky.engine.EngineResult;

import java.util.Optional;

public interface ProcessedEventStore {
    boolean isProcessed(String transactionId);
    void markProcessed(String transactionId, EngineResult result);
    Optional<EngineResult> getResult(String transactionId);
}
