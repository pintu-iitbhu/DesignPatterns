package com.rcpky.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record Transaction(
        String id,
        String accountId,
        String merchantId,
        BigDecimal amount,
        String currency,
        TransactionType type,
        Instant timestamp,
        Map<String, Object> metadata
) {}
