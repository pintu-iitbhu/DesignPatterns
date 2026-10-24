package com.rcpky.api;


import com.rcpky.engine.FlagEvaluator;
import com.rcpky.model.EvaluationContext;
import com.rcpky.model.EvaluationResult;
import com.rcpky.model.FlagDefinition;
import com.rcpky.store.FlagStore;
import com.rcpky.store.InMemoryFlagStore;
import com.rcpky.utils.Validator;

/**
 * In-process feature flag library.
 *
 * <p>Usage:
 * <pre>{@code
 *   FeatureFlagClient client = new FeatureFlagClient();
 *
 *   client.register(new FlagDefinition(
 *       "dark-mode",
 *       FlagType.BOOLEAN,
 *       false,
 *       List.of(
 *           new Rule(LogicOperator.AND,
 *               List.of(new Condition("plan", ConditionOperator.IN, List.of("pro", "enterprise"))),
 *               true)
 *       ),
 *       new Rollout(20, true)
 *   ));
 *
 *   boolean enabled = client.getBooleanFlag("dark-mode",
 *       EvaluationContext.of("user-42", Map.of("plan", "free")));
 * }</pre>
 */
public class FeatureFlagClient {

    private final FlagStore store;
    private final FlagEvaluator evaluator;
    private final Validator validator;

    public FeatureFlagClient(FlagStore store, FlagEvaluator evaluator, Validator validator) {
        this.store = store;
        this.evaluator = evaluator;
        this.validator = validator;
    }

    public FeatureFlagClient() {
        this(new InMemoryFlagStore());
    }

    public FeatureFlagClient(FlagStore store) {
        this.store = store;
        this.evaluator = new FlagEvaluator(store);
        this.validator = new Validator();
    }

    public void register(FlagDefinition flag) {

        validator.validateDefaultType(flag, flag.defaultValue());
        store.put(flag);
    }

    public void deregister(String flagName) {
        store.remove(flagName);
    }

    public boolean getBooleanFlag(String flagName, EvaluationContext ctx) {
        return evaluator.evaluate(flagName, ctx, Boolean.class).value();
    }

    public String getStringFlag(String flagName, EvaluationContext ctx) {
        return evaluator.evaluate(flagName, ctx, String.class).value();
    }

    public int getIntegerFlag(String flagName, EvaluationContext ctx) {
        return evaluator.evaluate(flagName, ctx, Integer.class).value();
    }

    public <T> EvaluationResult<T> evaluate(String flagName, EvaluationContext ctx, Class<T> type) {
        return evaluator.evaluate(flagName, ctx, type);
    }
}
