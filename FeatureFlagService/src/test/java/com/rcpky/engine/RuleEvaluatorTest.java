package com.rcpky.engine;

import com.rcpky.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.rcpky.model.ConditionOperator.*;
import static com.rcpky.model.LogicOperator.*;
import static org.junit.jupiter.api.Assertions.*;

class RuleEvaluatorTest {

    private RuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RuleEvaluator();
    }

    @Test
    void evaluate_returnsEmpty_whenNoRules() {
        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals(Optional.empty(), evaluator.evaluate(List.of(), ctx));
    }

    @Test
    void evaluate_returnsEmpty_whenRuleHasNoConditions() {
        Rule rule = Rule.and(List.of(), "matched");
        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals(Optional.empty(), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_AND_returnsTargetValue_whenAllConditionsMatch() {
        Rule rule = Rule.and(List.of(
                new Condition("country", EQUALS, "US"),
                new Condition("plan", EQUALS, "pro")
        ), "us-pro-value");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US", "plan", "pro"));
        assertEquals(Optional.of("us-pro-value"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_AND_returnsEmpty_whenAnyConditionFails() {
        Rule rule = Rule.and(List.of(
                new Condition("country", EQUALS, "US"),
                new Condition("plan", EQUALS, "pro")
        ), "us-pro-value");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US", "plan", "free"));
        assertEquals(Optional.empty(), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_OR_returnsTargetValue_whenAnyConditionMatches() {
        Rule rule = Rule.or(List.of(
                new Condition("country", EQUALS, "US"),
                new Condition("country", EQUALS, "CA")
        ), "north-america");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "CA"));
        assertEquals(Optional.of("north-america"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_OR_returnsEmpty_whenNoConditionMatches() {
        Rule rule = Rule.or(List.of(
                new Condition("country", EQUALS, "US"),
                new Condition("country", EQUALS, "CA")
        ), "north-america");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "DE"));
        assertEquals(Optional.empty(), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_returnsFirstMatchingRule() {
        Rule rule1 = Rule.and(List.of(new Condition("plan", EQUALS, "pro")), "rule1-value");
        Rule rule2 = Rule.and(List.of(new Condition("plan", EQUALS, "pro")), "rule2-value");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "pro"));
        assertEquals(Optional.of("rule1-value"), evaluator.evaluate(List.of(rule1, rule2), ctx));
    }

    @Test
    void evaluate_IN_operator_matches_whenAttributeInList() {
        Rule rule = Rule.and(List.of(
                new Condition("country", IN, List.of("US", "CA", "UK"))
        ), "allowed");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "CA"));
        assertEquals(Optional.of("allowed"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_NOT_IN_operator_matches_whenAttributeNotInList() {
        Rule rule = Rule.and(List.of(
                new Condition("country", NOT_IN, List.of("CN", "RU"))
        ), "allowed");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals(Optional.of("allowed"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_CONTAINS_operator_matches_substring() {
        Rule rule = Rule.and(List.of(
                new Condition("email", CONTAINS, "@company.com")
        ), "internal-user");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("email", "alice@company.com"));
        assertEquals(Optional.of("internal-user"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_GREATER_THAN_operator_matches_numericComparison() {
        Rule rule = Rule.and(List.of(
                new Condition("age", GREATER_THAN, 18)
        ), "adult");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("age", 25));
        assertEquals(Optional.of("adult"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_LESS_THAN_OR_EQUAL_operator() {
        Rule rule = Rule.and(List.of(
                new Condition("score", LESS_THAN_OR_EQUAL, 100)
        ), "valid");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("score", 100));
        assertEquals(Optional.of("valid"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_returnsEmpty_whenAttributeMissingFromContext() {
        Rule rule = Rule.and(List.of(
                new Condition("plan", EQUALS, "pro")
        ), "pro-value");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals(Optional.empty(), evaluator.evaluate(List.of(rule), ctx));
    }

    // --- Composite expression (nested AND/OR) ---

    @Test
    void evaluate_nestedComposite_ANDwithinOR_matchesCorrectly() {
        // (plan == "pro" AND country == "US") OR (plan == "enterprise" AND country == "IN")
        CompositeExpression usProBranch = new CompositeExpression(AND, List.of(
                new AtomicExpression(new Condition("plan", EQUALS, "pro")),
                new AtomicExpression(new Condition("country", EQUALS, "US"))
        ));
        CompositeExpression inEnterpriseBranch = new CompositeExpression(AND, List.of(
                new AtomicExpression(new Condition("plan", EQUALS, "enterprise")),
                new AtomicExpression(new Condition("country", EQUALS, "IN"))
        ));
        Rule rule = new Rule(new CompositeExpression(OR, List.of(usProBranch, inEnterpriseBranch)), "premium");

        assertEquals(Optional.of("premium"),
                evaluator.evaluate(List.of(rule), EvaluationContext.of("u1", Map.of("plan", "pro", "country", "US"))));
        assertEquals(Optional.of("premium"),
                evaluator.evaluate(List.of(rule), EvaluationContext.of("u2", Map.of("plan", "enterprise", "country", "IN"))));
        assertEquals(Optional.empty(),
                evaluator.evaluate(List.of(rule), EvaluationContext.of("u3", Map.of("plan", "pro", "country", "IN"))));
    }

    // --- Custom matcher via registry ---

    @Test
    void customMatcher_registeredInRegistry_isUsedDuringEvaluation() {
        ConditionMatcherRegistry registry = new ConditionMatcherRegistry();
        registry.register(EQUALS, (cv, condV) -> cv.toString().equalsIgnoreCase(condV.toString()));

        RuleEvaluator customEvaluator = new RuleEvaluator(registry);
        Rule rule = Rule.and(List.of(new Condition("country", EQUALS, "us")), "matched");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals(Optional.of("matched"), customEvaluator.evaluate(List.of(rule), ctx),
                "Overridden EQUALS matcher should be case-insensitive");
    }
}
