package com.rcpky.flag;

import com.rcpky.evaluation.EvaluationContext;
import com.rcpky.evaluation.EvaluationReason;
import com.rcpky.evaluation.EvaluationResult;
import com.rcpky.exception.FlagNotFoundException;
import com.rcpky.rollout.Rollout;
import com.rcpky.rollout.RolloutEvaluator;
import com.rcpky.rollout.RolloutStrategy;
import com.rcpky.rule.RuleEvaluator;
import com.rcpky.store.FlagStore;
import com.rcpky.utils.Validator;

import java.util.Optional;

public class FlagEvaluator {

    private final FlagStore store;
    private final RuleEvaluator ruleEvaluator;
    private final RolloutStrategy rolloutStrategy;
    private final Validator validator;

    public FlagEvaluator(FlagStore store) {
        this(store, new RuleEvaluator());
    }

    public FlagEvaluator(FlagStore store, RuleEvaluator ruleEvaluator) {
        this(store, ruleEvaluator, new RolloutEvaluator());
    }

    public FlagEvaluator(FlagStore store, RuleEvaluator ruleEvaluator, RolloutStrategy rolloutStrategy) {
        this.store = store;
        this.ruleEvaluator = ruleEvaluator;
        this.rolloutStrategy = rolloutStrategy;
        this.validator = new Validator();
    }

    public <T> EvaluationResult<T> evaluate(String flagName, EvaluationContext ctx, Class<T> type) {
        FlagDefinition flag = store.get(flagName)
                .orElseThrow(() -> new FlagNotFoundException(flagName));

        if (!flag.isActive()) {
            return new EvaluationResult<>(flag.defaultValue().as(type), EvaluationReason.DISABLED);
        }
        validator.validateType(flag, type);

        Optional<FlagValue> ruleMatch = ruleEvaluator.evaluate(flag.rules(), ctx);
        if (ruleMatch.isPresent()) {
            return new EvaluationResult<>(ruleMatch.get().as(type), EvaluationReason.RULE_MATCH);
        }

        String bucketKey = resolveBucketKey(flag.rollout(), ctx);
        Optional<FlagValue> rollout = rolloutStrategy.evaluate(flagName, bucketKey, flag.rollout());
        if (rollout.isPresent()) {
            return new EvaluationResult<>(rollout.get().as(type), EvaluationReason.ROLLOUT);
        }

        return new EvaluationResult<>(flag.defaultValue().as(type), EvaluationReason.DEFAULT);
    }

    private String resolveBucketKey(Rollout rollout, EvaluationContext ctx) {
        if (rollout == null || rollout.bucketingKey() == null) {
            return ctx.userId();
        }
        Object resolved = ctx.attributes().get(rollout.bucketingKey());
        return resolved != null ? resolved.toString() : ctx.userId();
    }

}
