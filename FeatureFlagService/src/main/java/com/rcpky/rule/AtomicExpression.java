package com.rcpky.rule;

import java.util.function.Predicate;

/**
 * Leaf node of the rule expression tree.
 * Delegates evaluation of a single {@link Condition} to the supplied predicate.
 */
public record AtomicExpression(Condition condition) implements RuleExpression {

    @Override
    public boolean evaluate(Predicate<Condition> conditionEvaluator) {
        return conditionEvaluator.test(condition);
    }
}
