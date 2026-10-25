package com.rcpky.model;

import java.util.List;

/**
 * A targeting rule: if {@code expression} evaluates to {@code true} for a given
 * {@link EvaluationContext}, the flag returns {@code targetValue}.
 *
 * <p>Use the static factories for the common flat AND / OR cases:
 * <pre>{@code
 *   Rule.and(List.of(condA, condB), "variant-a")
 *   Rule.or(List.of(condA, condB), "variant-b")
 * }</pre>
 *
 * <p>Build a {@link CompositeExpression} directly for nested logic:
 * <pre>{@code
 *   // (plan == "pro" AND country == "US") OR (plan == "enterprise")
 *   new Rule(
 *       new CompositeExpression(OR, List.of(
 *           new CompositeExpression(AND, List.of(atomicPlanPro, atomicCountryUS)),
 *           new AtomicExpression(conditionEnterprise)
 *       )),
 *       "premium-variant"
 *   )
 * }</pre>
 */
public record Rule(RuleExpression expression, Object targetValue) {

    public static Rule and(List<Condition> conditions, Object targetValue) {
        return new Rule(
                new CompositeExpression(LogicOperator.AND,
                        conditions.stream().<RuleExpression>map(AtomicExpression::new).toList()),
                targetValue);
    }

    public static Rule or(List<Condition> conditions, Object targetValue) {
        return new Rule(
                new CompositeExpression(LogicOperator.OR,
                        conditions.stream().<RuleExpression>map(AtomicExpression::new).toList()),
                targetValue);
    }
}
