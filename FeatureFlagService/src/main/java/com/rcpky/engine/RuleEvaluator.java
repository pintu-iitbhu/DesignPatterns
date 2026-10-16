package com.rcpky.engine;

import com.rcpky.model.Condition;
import com.rcpky.model.EvaluationContext;
import com.rcpky.model.LogicOperator;
import com.rcpky.model.Rule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class RuleEvaluator {

    public Optional<Object> evaluate(List<Rule> rules, EvaluationContext ctx) {
        for (Rule rule : rules) {
            if (matches(rule, ctx)) {
                return Optional.of(rule.targetValue());
            }
        }
        return Optional.empty();
    }

    private boolean matches(Rule rule, EvaluationContext ctx) {
        List<Condition> conditions = rule.conditions();
        if (conditions.isEmpty()) return false;

        if (rule.logicOperator() == LogicOperator.AND) {
            return conditions.stream().allMatch(c -> evaluateCondition(c, ctx));
        } else {
            return conditions.stream().anyMatch(c -> evaluateCondition(c, ctx));
        }
    }

    private boolean evaluateCondition(Condition condition, EvaluationContext ctx) {
        Object contextValue = ctx.attributes().get(condition.attribute());
        if (contextValue == null) return false;

        Object conditionValue = condition.value();

        return switch (condition.operator()) {
            case EQUALS -> contextValue.equals(conditionValue);
            case NOT_EQUALS -> !contextValue.equals(conditionValue);
            case IN -> conditionValue instanceof Collection<?> col && col.contains(contextValue);
            case NOT_IN -> conditionValue instanceof Collection<?> col && !col.contains(contextValue);
            case CONTAINS -> contextValue instanceof String s && s.contains(conditionValue.toString());
            case GREATER_THAN -> compare(contextValue, conditionValue) > 0;
            case LESS_THAN -> compare(contextValue, conditionValue) < 0;
            case GREATER_THAN_OR_EQUAL -> compare(contextValue, conditionValue) >= 0;
            case LESS_THAN_OR_EQUAL -> compare(contextValue, conditionValue) <= 0;
        };
    }

    @SuppressWarnings("unchecked")
    private int compare(Object a, Object b) {
        if (a instanceof Comparable && b instanceof Comparable) {
            return ((Comparable<Object>) a).compareTo(b);
        }
        throw new IllegalArgumentException(
                "Cannot compare non-comparable types: " + a.getClass().getSimpleName()
                + " and " + b.getClass().getSimpleName());
    }
}
