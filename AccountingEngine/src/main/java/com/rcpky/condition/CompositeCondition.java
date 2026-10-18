package com.rcpky.condition;

import com.rcpky.domain.Transaction;
import com.rcpky.rule.RuleCondition;

import java.util.List;

public class CompositeCondition implements RuleCondition {

    public enum LogicOperator { AND, OR, NOT }

    private final LogicOperator operator;
    private final List<RuleCondition> conditions;

    public CompositeCondition(LogicOperator operator, RuleCondition... conditions) {
        this.operator = operator;
        this.conditions = List.of(conditions);
    }

    @Override
    public boolean evaluate(Transaction tx) {
        return switch (operator) {
            case AND -> conditions.stream().allMatch(c -> c.evaluate(tx));
            case OR  -> conditions.stream().anyMatch(c -> c.evaluate(tx));
            case NOT -> conditions.size() == 1 && !conditions.get(0).evaluate(tx);
        };
    }
}
