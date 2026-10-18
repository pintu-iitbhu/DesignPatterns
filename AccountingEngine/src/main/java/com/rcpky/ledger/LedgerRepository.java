package com.rcpky.ledger;

import java.util.List;

public interface LedgerRepository {
    void save(LedgerEntry entry);
    void saveAll(List<LedgerEntry> entries);
    List<LedgerEntry> findByTransactionId(String transactionId);
    List<LedgerEntry> findAll();
}
