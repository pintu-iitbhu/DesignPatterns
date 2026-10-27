package com.rcpky.rule;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import static com.rcpky.rule.ConditionOperator.*;

/**
 * Registry of {@link ConditionMatcher} strategies keyed by {@link ConditionOperator}.
 *
 * <p>All built-in operators are registered by default. To add support for a new
 * operator, add its constant to {@link ConditionOperator} and call
 * {@link #register(ConditionOperator, ConditionMatcher)} — no changes to
 * {@link RuleEvaluator} are needed.
 *
 * <pre>{@code
 *   ConditionMatcherRegistry registry = new ConditionMatcherRegistry();
 *   registry.register(STARTS_WITH,
 *       (cv, condV) -> cv instanceof String s && s.startsWith(condV.toString()));
 * }</pre>
 */
public class ConditionMatcherRegistry {

    private final Map<ConditionOperator, ConditionMatcher> matchers =
            new EnumMap<>(ConditionOperator.class);

    public ConditionMatcherRegistry() {
        register(EQUALS,                Object::equals);
        register(NOT_EQUALS,            (cv, condV) -> !cv.equals(condV));
        register(IN,                    (cv, condV) -> condV instanceof Collection<?> col && col.contains(cv));
        register(NOT_IN,                (cv, condV) -> condV instanceof Collection<?> col && !col.contains(cv));
        register(CONTAINS,              (cv, condV) -> cv instanceof String s && s.contains(condV.toString()));
        register(GREATER_THAN,          (cv, condV) -> compare(cv, condV) > 0);
        register(LESS_THAN,             (cv, condV) -> compare(cv, condV) < 0);
        register(GREATER_THAN_OR_EQUAL, (cv, condV) -> compare(cv, condV) >= 0);
        register(LESS_THAN_OR_EQUAL,    (cv, condV) -> compare(cv, condV) <= 0);
    }

    public void register(ConditionOperator op, ConditionMatcher matcher) {
        matchers.put(op, matcher);
    }

    public boolean evaluate(ConditionOperator op, Object contextValue, Object conditionValue) {
        ConditionMatcher matcher = matchers.get(op);
        if (matcher == null) {
            throw new IllegalArgumentException("No matcher registered for operator: " + op);
        }
        return matcher.matches(contextValue, conditionValue);
    }

    @SuppressWarnings("unchecked")
    private static int compare(Object a, Object b) {
        if (a instanceof Comparable && b instanceof Comparable) {
            return ((Comparable<Object>) a).compareTo(b);
        }
        throw new IllegalArgumentException(
                "Cannot compare non-comparable types: "
                + a.getClass().getSimpleName() + " and " + b.getClass().getSimpleName());
    }
}
