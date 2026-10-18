package com.rcpky.condition;

import com.rcpky.domain.Transaction;
import com.rcpky.rule.RuleCondition;

import java.math.BigDecimal;

public class AmountCondition implements RuleCondition {

    public enum Operator { GT, LT, GTE, LTE, EQ, BETWEEN }

    private final Operator operator;
    private final BigDecimal threshold;
    private final BigDecimal upperBound;

    public AmountCondition(Operator operator, BigDecimal threshold) {
        this(operator, threshold, null);
    }

    public AmountCondition(Operator operator, BigDecimal threshold, BigDecimal upperBound) {
        this.operator = operator;
        this.threshold = threshold;
        this.upperBound = upperBound;
    }

    @Override
    public boolean evaluate(Transaction tx) {
        BigDecimal amount = tx.amount();
        return switch (operator) {
            case GT     -> amount.compareTo(threshold) > 0;
            case LT     -> amount.compareTo(threshold) < 0;
            case GTE    -> amount.compareTo(threshold) >= 0;
            case LTE    -> amount.compareTo(threshold) <= 0;
            case EQ     -> amount.compareTo(threshold) == 0;
            case BETWEEN -> upperBound != null
                         && amount.compareTo(threshold) >= 0
                         && amount.compareTo(upperBound) <= 0;
        };
    }
}
