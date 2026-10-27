package com.rcpky.rule;

import java.util.List;
import java.util.function.Predicate;

/**
 * Internal node of the rule expression tree.
 * Combines child expressions with AND (all must match) or OR (any must match).
 * Children may themselves be {@link CompositeExpression}s, enabling arbitrary nesting.
 *
 * <p>Example — (plan == "pro" AND country == "US") OR (plan == "enterprise"):
 * <pre>{@code
 *   new CompositeExpression(OR, List.of(
 *       new CompositeExpression(AND, List.of(atomicPlanPro, atomicCountryUS)),
 *       new AtomicExpression(conditionEnterprise)
 *   ))
 * }</pre>
 */
public record CompositeExpression(LogicOperator operator, List<RuleExpression> children)
        implements RuleExpression {

    public CompositeExpression {
        children = children == null ? List.of() : List.copyOf(children);
    }

    @Override
    public boolean evaluate(Predicate<Condition> conditionEvaluator) {
        if (children.isEmpty()) return false;
        return switch (operator) {
            case AND -> children.stream().allMatch(c -> c.evaluate(conditionEvaluator));
            case OR  -> children.stream().anyMatch(c -> c.evaluate(conditionEvaluator));
        };
    }
}
