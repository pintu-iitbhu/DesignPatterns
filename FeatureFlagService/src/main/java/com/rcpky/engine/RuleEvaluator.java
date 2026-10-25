package com.rcpky.engine;

import com.rcpky.model.Condition;
import com.rcpky.model.EvaluationContext;
import com.rcpky.model.Rule;

import java.util.List;
import java.util.Optional;

public class RuleEvaluator {

    private final ConditionMatcherRegistry registry;

    public RuleEvaluator() {
        this.registry = new ConditionMatcherRegistry();
    }

    public RuleEvaluator(ConditionMatcherRegistry registry) {
        this.registry = registry;
    }

    public Optional<Object> evaluate(List<Rule> rules, EvaluationContext ctx) {
        for (Rule rule : rules) {
            if (rule.expression().evaluate(c -> evaluateCondition(c, ctx))) {
                return Optional.of(rule.targetValue());
            }
        }
        return Optional.empty();
    }

    private boolean evaluateCondition(Condition condition, EvaluationContext ctx) {
        Object contextValue = ctx.attributes().get(condition.attribute());
        if (contextValue == null) return false;
        return registry.evaluate(condition.operator(), contextValue, condition.value());
    }
}
