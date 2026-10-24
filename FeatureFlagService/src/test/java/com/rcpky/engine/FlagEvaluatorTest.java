package com.rcpky.engine;

import com.rcpky.exception.FlagNotFoundException;
import com.rcpky.exception.TypeMismatchException;
import com.rcpky.model.*;
import com.rcpky.store.InMemoryFlagStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class FlagEvaluatorTest {

    private InMemoryFlagStore store;
    private FlagEvaluator evaluator;

    @BeforeEach
    void setUp() {
        store = new InMemoryFlagStore();
        evaluator = new FlagEvaluator(store);
    }

    @Test
    void evaluate_throwsFlagNotFoundException_forUnknownFlag() {
        EvaluationContext ctx = EvaluationContext.anonymous();
        assertThrows(FlagNotFoundException.class,
                () -> evaluator.evaluate("unknown-flag", ctx, Boolean.class));
    }

    @Test
    void evaluate_throwsTypeMismatchException_forWrongType() {
        store.put(new FlagDefinition("bool-flag", FlagType.BOOLEAN, false, List.of(), null, true));
        EvaluationContext ctx = EvaluationContext.anonymous();
        assertThrows(TypeMismatchException.class,
                () -> evaluator.evaluate("bool-flag", ctx, String.class));
    }

    @Test
    void evaluate_returnsDefault_whenNoRulesAndNoRollout() {
        store.put(new FlagDefinition("my-flag", FlagType.BOOLEAN, false, List.of(), null, true));
        EvaluationContext ctx = EvaluationContext.of("u1", Map.of());

        EvaluationResult<Boolean> result = evaluator.evaluate("my-flag", ctx, Boolean.class);

        assertFalse(result.value());
        assertEquals(EvaluationReason.DEFAULT, result.reason());
    }

    @Test
    void evaluate_returnsRuleMatch_whenConditionMatches() {
        Rule rule = new Rule(LogicOperator.AND,
                List.of(new Condition("plan", ConditionOperator.EQUALS, "enterprise")), true);
        store.put(new FlagDefinition("feature-x", FlagType.BOOLEAN, false, List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "enterprise"));
        EvaluationResult<Boolean> result = evaluator.evaluate("feature-x", ctx, Boolean.class);

        assertTrue(result.value());
        assertEquals(EvaluationReason.RULE_MATCH, result.reason());
    }

    @Test
    void evaluate_returnsDefault_whenRuleDoesNotMatch() {
        Rule rule = new Rule(LogicOperator.AND,
                List.of(new Condition("plan", ConditionOperator.EQUALS, "enterprise")), true);
        store.put(new FlagDefinition("feature-x", FlagType.BOOLEAN, false, List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "free"));
        EvaluationResult<Boolean> result = evaluator.evaluate("feature-x", ctx, Boolean.class);

        assertFalse(result.value());
        assertEquals(EvaluationReason.DEFAULT, result.reason());
    }

    @Test
    void evaluate_returnsRollout_whenNoRuleMatchesAndUserInRollout() {
        store.put(new FlagDefinition("rollout-flag", FlagType.BOOLEAN, false,
                List.of(), new Rollout(100, true, null), true));

        EvaluationContext ctx = EvaluationContext.of("any-user", Map.of());
        EvaluationResult<Boolean> result = evaluator.evaluate("rollout-flag", ctx, Boolean.class);

        assertTrue(result.value());
        assertEquals(EvaluationReason.ROLLOUT, result.reason());
    }

    @Test
    void evaluate_returnsDefault_whenNoRuleMatchesAndUserNotInRollout() {
        store.put(new FlagDefinition("rollout-flag", FlagType.BOOLEAN, false,
                List.of(), new Rollout(0, true, null), true));

        EvaluationContext ctx = EvaluationContext.of("any-user", Map.of());
        EvaluationResult<Boolean> result = evaluator.evaluate("rollout-flag", ctx, Boolean.class);

        assertFalse(result.value());
        assertEquals(EvaluationReason.DEFAULT, result.reason());
    }

    @Test
    void evaluate_ruleMatchTakesPrecedenceOverRollout() {
        Rule rule = new Rule(LogicOperator.AND,
                List.of(new Condition("plan", ConditionOperator.EQUALS, "pro")), "rule-variant");
        store.put(new FlagDefinition("priority-flag", FlagType.STRING, "default",
                List.of(rule), new Rollout(100, "rollout-variant", null), true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "pro"));
        EvaluationResult<String> result = evaluator.evaluate("priority-flag", ctx, String.class);

        assertEquals("rule-variant", result.value());
        assertEquals(EvaluationReason.RULE_MATCH, result.reason());
    }

    @Test
    void evaluate_stringFlag_returnsCorrectVariant() {
        Rule rule = new Rule(LogicOperator.AND,
                List.of(new Condition("country", ConditionOperator.IN, List.of("US", "CA"))), "en-US");
        store.put(new FlagDefinition("locale-flag", FlagType.STRING, "en-GB", List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals("en-US", evaluator.evaluate("locale-flag", ctx, String.class).value());
    }

    @Test
    void evaluate_disabledFlag_returnsDefaultWithDisabledReason_ignoringRulesAndRollout() {
        Rule rule = new Rule(LogicOperator.AND,
                List.of(new Condition("plan", ConditionOperator.EQUALS, "enterprise")), true);
        store.put(new FlagDefinition("disabled-flag", FlagType.BOOLEAN, false,
                List.of(rule), new Rollout(100, true, null), false));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "enterprise"));
        EvaluationResult<Boolean> result = evaluator.evaluate("disabled-flag", ctx, Boolean.class);

        assertFalse(result.value());
        assertEquals(EvaluationReason.DISABLED, result.reason());
    }

    @Test
    void evaluate_integerFlag_returnsCorrectValue() {
        Rule rule = new Rule(LogicOperator.AND,
                List.of(new Condition("plan", ConditionOperator.EQUALS, "enterprise")), 1000);
        store.put(new FlagDefinition("rate-limit", FlagType.INTEGER, 100, List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "enterprise"));
        assertEquals(1000, evaluator.evaluate("rate-limit", ctx, Integer.class).value());
    }
}
