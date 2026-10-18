package com.rcpky.condition;

import com.rcpky.domain.Transaction;
import com.rcpky.domain.TransactionType;
import com.rcpky.rule.RuleCondition;

import java.util.Set;

public class TransactionTypeCondition implements RuleCondition {

    private final Set<TransactionType> allowedTypes;

    public TransactionTypeCondition(TransactionType... types) {
        this.allowedTypes = Set.of(types);
    }

    @Override
    public boolean evaluate(Transaction tx) {
        return allowedTypes.contains(tx.type());
    }
}
