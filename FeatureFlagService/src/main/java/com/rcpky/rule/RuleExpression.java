package com.rcpky.rule;

import java.util.function.Predicate;

/**
 * Composite pattern: a rule expression is either an atomic condition (leaf)
 * or a composite of child expressions joined by AND/OR (node).
 *
 * <p>The {@link Predicate} parameter decouples the model from the engine:
 * the engine supplies how each {@link Condition} is matched; the tree only
 * handles the logical composition.
 */
public sealed interface RuleExpression permits AtomicExpression, CompositeExpression {
    boolean evaluate(Predicate<Condition> conditionEvaluator);
}
