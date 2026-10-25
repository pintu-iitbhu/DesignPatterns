package com.rcpky.engine;

import com.rcpky.exception.FlagNotFoundException;
import com.rcpky.model.*;
import com.rcpky.store.FlagStore;
import com.rcpky.utils.Validator;

import java.util.Optional;

public class FlagEvaluator {

    private final FlagStore store;
    private final RuleEvaluator ruleEvaluator;
    private final RolloutEvaluator rolloutEvaluator;
    private final Validator validator;

    public FlagEvaluator(FlagStore store) {
        this(store, new RuleEvaluator());
    }

    public FlagEvaluator(FlagStore store, RuleEvaluator ruleEvaluator) {
        this.store = store;
        this.ruleEvaluator = ruleEvaluator;
        this.rolloutEvaluator = new RolloutEvaluator();
        this.validator = new Validator();
    }

    public <T> EvaluationResult<T> evaluate(String flagName, EvaluationContext ctx, Class<T> type) {
        FlagDefinition flag = store.get(flagName)
                .orElseThrow(() -> new FlagNotFoundException(flagName));

        if (!flag.isActive()) {
            return new EvaluationResult<>(cast(flag.defaultValue(), type), EvaluationReason.DISABLED);
        }
        validator.validateType(flag, type);

        Optional<Object> ruleMatch = ruleEvaluator.evaluate(flag.rules(), ctx);
        if (ruleMatch.isPresent()) {
            return new EvaluationResult<>(cast(ruleMatch.get(), type), EvaluationReason.RULE_MATCH);
        }

        String bucketKey = resolveBucketKey(flag.rollout(), ctx);
        Optional<Object> rollout = rolloutEvaluator.evaluate(flagName, bucketKey, flag.rollout());
        if (rollout.isPresent()) {
            return new EvaluationResult<>(cast(rollout.get(), type), EvaluationReason.ROLLOUT);
        }

        return new EvaluationResult<>(cast(flag.defaultValue(), type), EvaluationReason.DEFAULT);
    }

    private String resolveBucketKey(Rollout rollout, EvaluationContext ctx) {
        if (rollout == null || rollout.bucketingKey() == null) {
            return ctx.userId();
        }
        Object resolved = ctx.attributes().get(rollout.bucketingKey());
        return resolved != null ? resolved.toString() : ctx.userId();
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value, Class<T> type) {
        return (T) value;
    }
}
