package com.rcpky.condition;

import com.rcpky.domain.Transaction;
import com.rcpky.domain.TransactionStatus;
import com.rcpky.rule.RuleCondition;

import java.util.Set;

public class TransactionStatusCondition implements RuleCondition {

    private final Set<TransactionStatus> statuses;

    public TransactionStatusCondition(TransactionStatus... statuses) {
        this.statuses = Set.of(statuses);
    }

    @Override
    public boolean evaluate(Transaction tx) {
        return statuses.contains(tx.status());
    }
}
