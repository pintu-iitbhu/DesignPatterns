package com.rcpky.ledger;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntry(
        String entryId,
        String transactionId,
        String accountId,
        LedgerEntryType type,
        BigDecimal amount,
        String currency,
        String description,
        Instant timestamp
) {}
