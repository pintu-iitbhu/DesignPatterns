package com.rcpky.engine;

import com.rcpky.exception.FlagNotFoundException;
import com.rcpky.exception.TypeMismatchException;
import com.rcpky.model.*;
import com.rcpky.store.FlagStore;

import java.util.Optional;

public class FlagEvaluator {

    private final FlagStore store;
    private final RuleEvaluator ruleEvaluator;
    private final RolloutEvaluator rolloutEvaluator;

    public FlagEvaluator(FlagStore store) {
        this.store = store;
        this.ruleEvaluator = new RuleEvaluator();
        this.rolloutEvaluator = new RolloutEvaluator();
    }

    public <T> EvaluationResult<T> evaluate(String flagName, EvaluationContext ctx, Class<T> type) {
        FlagDefinition flag = store.get(flagName)
                .orElseThrow(() -> new FlagNotFoundException(flagName));

        validateType(flag, type);

        Optional<Object> ruleMatch = ruleEvaluator.evaluate(flag.rules(), ctx);
        if (ruleMatch.isPresent()) {
            return new EvaluationResult<>(cast(ruleMatch.get(), type), EvaluationReason.RULE_MATCH);
        }

        Optional<Object> rollout = rolloutEvaluator.evaluate(flagName, ctx.userId(), flag.rollout());
        if (rollout.isPresent()) {
            return new EvaluationResult<>(cast(rollout.get(), type), EvaluationReason.ROLLOUT);
        }

        return new EvaluationResult<>(cast(flag.defaultValue(), type), EvaluationReason.DEFAULT);
    }

    private void validateType(FlagDefinition flag, Class<?> requested) {
        boolean valid = switch (flag.type()) {
            case BOOLEAN -> requested == Boolean.class;
            case STRING -> requested == String.class;
            case INTEGER -> requested == Integer.class;
        };
        if (!valid) {
            throw new TypeMismatchException(flag.name(), flag.type(), requested);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value, Class<T> type) {
        return (T) value;
    }
}
