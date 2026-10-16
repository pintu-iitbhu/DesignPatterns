package com.rcpky.engine;

import com.rcpky.model.Condition;
import com.rcpky.model.ConditionOperator;
import com.rcpky.model.EvaluationContext;
import com.rcpky.model.LogicOperator;
import com.rcpky.model.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        Rule rule = new Rule(LogicOperator.AND, List.of(), "matched");
        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals(Optional.empty(), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_AND_returnsTargetValue_whenAllConditionsMatch() {
        Rule rule = new Rule(LogicOperator.AND, List.of(
                new Condition("country", ConditionOperator.EQUALS, "US"),
                new Condition("plan", ConditionOperator.EQUALS, "pro")
        ), "us-pro-value");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US", "plan", "pro"));
        assertEquals(Optional.of("us-pro-value"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_AND_returnsEmpty_whenAnyConditionFails() {
        Rule rule = new Rule(LogicOperator.AND, List.of(
                new Condition("country", ConditionOperator.EQUALS, "US"),
                new Condition("plan", ConditionOperator.EQUALS, "pro")
        ), "us-pro-value");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US", "plan", "free"));
        assertEquals(Optional.empty(), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_OR_returnsTargetValue_whenAnyConditionMatches() {
        Rule rule = new Rule(LogicOperator.OR, List.of(
                new Condition("country", ConditionOperator.EQUALS, "US"),
                new Condition("country", ConditionOperator.EQUALS, "CA")
        ), "north-america");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "CA"));
        assertEquals(Optional.of("north-america"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_OR_returnsEmpty_whenNoConditionMatches() {
        Rule rule = new Rule(LogicOperator.OR, List.of(
                new Condition("country", ConditionOperator.EQUALS, "US"),
                new Condition("country", ConditionOperator.EQUALS, "CA")
        ), "north-america");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "DE"));
        assertEquals(Optional.empty(), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_returnsFirstMatchingRule() {
        Rule rule1 = new Rule(LogicOperator.AND,
                List.of(new Condition("plan", ConditionOperator.EQUALS, "pro")), "rule1-value");
        Rule rule2 = new Rule(LogicOperator.AND,
                List.of(new Condition("plan", ConditionOperator.EQUALS, "pro")), "rule2-value");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "pro"));
        assertEquals(Optional.of("rule1-value"), evaluator.evaluate(List.of(rule1, rule2), ctx));
    }

    @Test
    void evaluate_IN_operator_matches_whenAttributeInList() {
        Rule rule = new Rule(LogicOperator.AND, List.of(
                new Condition("country", ConditionOperator.IN, List.of("US", "CA", "UK"))
        ), "allowed");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "CA"));
        assertEquals(Optional.of("allowed"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_NOT_IN_operator_matches_whenAttributeNotInList() {
        Rule rule = new Rule(LogicOperator.AND, List.of(
                new Condition("country", ConditionOperator.NOT_IN, List.of("CN", "RU"))
        ), "allowed");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals(Optional.of("allowed"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_CONTAINS_operator_matches_substring() {
        Rule rule = new Rule(LogicOperator.AND, List.of(
                new Condition("email", ConditionOperator.CONTAINS, "@company.com")
        ), "internal-user");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("email", "alice@company.com"));
        assertEquals(Optional.of("internal-user"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_GREATER_THAN_operator_matches_numericComparison() {
        Rule rule = new Rule(LogicOperator.AND, List.of(
                new Condition("age", ConditionOperator.GREATER_THAN, 18)
        ), "adult");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("age", 25));
        assertEquals(Optional.of("adult"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_LESS_THAN_OR_EQUAL_operator() {
        Rule rule = new Rule(LogicOperator.AND, List.of(
                new Condition("score", ConditionOperator.LESS_THAN_OR_EQUAL, 100)
        ), "valid");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("score", 100));
        assertEquals(Optional.of("valid"), evaluator.evaluate(List.of(rule), ctx));
    }

    @Test
    void evaluate_returnsEmpty_whenAttributeMissingFromContext() {
        Rule rule = new Rule(LogicOperator.AND, List.of(
                new Condition("plan", ConditionOperator.EQUALS, "pro")
        ), "pro-value");

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals(Optional.empty(), evaluator.evaluate(List.of(rule), ctx));
    }
}
