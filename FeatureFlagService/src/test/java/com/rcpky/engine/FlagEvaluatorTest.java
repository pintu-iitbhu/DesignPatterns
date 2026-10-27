package com.rcpky.engine;

import com.rcpky.evaluation.EvaluationContext;
import com.rcpky.evaluation.EvaluationReason;
import com.rcpky.evaluation.EvaluationResult;
import com.rcpky.exception.FlagNotFoundException;
import com.rcpky.exception.TypeMismatchException;
import com.rcpky.flag.FlagDefinition;
import com.rcpky.flag.FlagEvaluator;
import com.rcpky.flag.FlagType;
import com.rcpky.flag.FlagValue;
import com.rcpky.rollout.Rollout;
import com.rcpky.rule.Condition;
import com.rcpky.rule.Rule;
import com.rcpky.store.InMemoryFlagStore;

import static com.rcpky.rule.ConditionOperator.*;
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
        store.put(new FlagDefinition("bool-flag", FlagType.BOOLEAN, FlagValue.of(false), List.of(), null, true));
        EvaluationContext ctx = EvaluationContext.anonymous();
        assertThrows(TypeMismatchException.class,
                () -> evaluator.evaluate("bool-flag", ctx, String.class));
    }

    @Test
    void evaluate_returnsDefault_whenNoRulesAndNoRollout() {
        store.put(new FlagDefinition("my-flag", FlagType.BOOLEAN, FlagValue.of(false), List.of(), null, true));
        EvaluationContext ctx = EvaluationContext.of("u1", Map.of());

        EvaluationResult<Boolean> result = evaluator.evaluate("my-flag", ctx, Boolean.class);

        assertFalse(result.value());
        assertEquals(EvaluationReason.DEFAULT, result.reason());
    }

    @Test
    void evaluate_returnsRuleMatch_whenConditionMatches() {
        Rule rule = Rule.and(List.of(new Condition("plan", EQUALS, "enterprise")), FlagValue.of(true));
        store.put(new FlagDefinition("feature-x", FlagType.BOOLEAN, FlagValue.of(false), List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "enterprise"));
        EvaluationResult<Boolean> result = evaluator.evaluate("feature-x", ctx, Boolean.class);

        assertTrue(result.value());
        assertEquals(EvaluationReason.RULE_MATCH, result.reason());
    }

    @Test
    void evaluate_returnsDefault_whenRuleDoesNotMatch() {
        Rule rule = Rule.and(List.of(new Condition("plan", EQUALS, "enterprise")), FlagValue.of(true));
        store.put(new FlagDefinition("feature-x", FlagType.BOOLEAN, FlagValue.of(false), List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "free"));
        EvaluationResult<Boolean> result = evaluator.evaluate("feature-x", ctx, Boolean.class);

        assertFalse(result.value());
        assertEquals(EvaluationReason.DEFAULT, result.reason());
    }

    @Test
    void evaluate_returnsRollout_whenNoRuleMatchesAndUserInRollout() {
        store.put(new FlagDefinition("rollout-flag", FlagType.BOOLEAN, FlagValue.of(false),
                List.of(), new Rollout(100, FlagValue.of(true), null), true));

        EvaluationContext ctx = EvaluationContext.of("any-user", Map.of());
        EvaluationResult<Boolean> result = evaluator.evaluate("rollout-flag", ctx, Boolean.class);

        assertTrue(result.value());
        assertEquals(EvaluationReason.ROLLOUT, result.reason());
    }

    @Test
    void evaluate_returnsDefault_whenNoRuleMatchesAndUserNotInRollout() {
        store.put(new FlagDefinition("rollout-flag", FlagType.BOOLEAN, FlagValue.of(false),
                List.of(), new Rollout(0, FlagValue.of(true), null), true));

        EvaluationContext ctx = EvaluationContext.of("any-user", Map.of());
        EvaluationResult<Boolean> result = evaluator.evaluate("rollout-flag", ctx, Boolean.class);

        assertFalse(result.value());
        assertEquals(EvaluationReason.DEFAULT, result.reason());
    }

    @Test
    void evaluate_ruleMatchTakesPrecedenceOverRollout() {
        Rule rule = Rule.and(List.of(new Condition("plan", EQUALS, "pro")), FlagValue.of("rule-variant"));
        store.put(new FlagDefinition("priority-flag", FlagType.STRING, FlagValue.of("default"),
                List.of(rule), new Rollout(100, FlagValue.of("rollout-variant"), null), true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "pro"));
        EvaluationResult<String> result = evaluator.evaluate("priority-flag", ctx, String.class);

        assertEquals("rule-variant", result.value());
        assertEquals(EvaluationReason.RULE_MATCH, result.reason());
    }

    @Test
    void evaluate_stringFlag_returnsCorrectVariant() {
        Rule rule = Rule.and(List.of(new Condition("country", IN, List.of("US", "CA"))), FlagValue.of("en-US"));
        store.put(new FlagDefinition("locale-flag", FlagType.STRING, FlagValue.of("en-GB"), List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals("en-US", evaluator.evaluate("locale-flag", ctx, String.class).value());
    }

    @Test
    void evaluate_disabledFlag_returnsDefaultWithDisabledReason_ignoringRulesAndRollout() {
        Rule rule = Rule.and(List.of(new Condition("plan", EQUALS, "enterprise")), FlagValue.of(true));
        store.put(new FlagDefinition("disabled-flag", FlagType.BOOLEAN, FlagValue.of(false),
                List.of(rule), new Rollout(100, FlagValue.of(true), null), false));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "enterprise"));
        EvaluationResult<Boolean> result = evaluator.evaluate("disabled-flag", ctx, Boolean.class);

        assertFalse(result.value());
        assertEquals(EvaluationReason.DISABLED, result.reason());
    }

    @Test
    void evaluate_integerFlag_returnsCorrectValue() {
        Rule rule = Rule.and(List.of(new Condition("plan", EQUALS, "enterprise")), FlagValue.of(1000));
        store.put(new FlagDefinition("rate-limit", FlagType.INTEGER, FlagValue.of(100), List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "enterprise"));
        assertEquals(1000, evaluator.evaluate("rate-limit", ctx, Integer.class).value());
    }
}
