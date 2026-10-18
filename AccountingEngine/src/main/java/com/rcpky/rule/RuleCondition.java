package com.rcpky.rule;

import com.rcpky.domain.Transaction;

@FunctionalInterface
public interface RuleCondition {

    boolean evaluate(Transaction transaction);

    default RuleCondition and(RuleCondition other) {
        return tx -> this.evaluate(tx) && other.evaluate(tx);
    }

    default RuleCondition or(RuleCondition other) {
        return tx -> this.evaluate(tx) || other.evaluate(tx);
    }

    default RuleCondition negate() {
        return tx -> !this.evaluate(tx);
    }
}
