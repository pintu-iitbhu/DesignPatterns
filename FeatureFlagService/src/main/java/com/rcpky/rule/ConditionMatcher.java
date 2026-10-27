package com.rcpky.rule;

/**
 * Strategy for evaluating a single condition operator.
 * Implement this to plug in a new matching strategy without modifying
 * {@link RuleEvaluator} or the switch logic.
 */
@FunctionalInterface
public interface ConditionMatcher {
    boolean matches(Object contextValue, Object conditionValue);
}
