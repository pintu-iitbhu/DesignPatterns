package com.rcpky.engine;

import com.rcpky.action.*;
import com.rcpky.builder.RuleBuilder;
import com.rcpky.condition.*;
import com.rcpky.domain.RiskLevel;
import com.rcpky.domain.Transaction;
import com.rcpky.domain.TransactionType;
import com.rcpky.repository.InMemoryRuleRepository;
import com.rcpky.repository.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountingEngineTest {

    private AccountingEngine engine;
    private RuleRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRuleRepository();
        engine = new DefaultAccountingEngine(repository);
    }

    private Transaction tx(String id, BigDecimal amount, TransactionType type, String merchantId) {
        return new Transaction(id, "ACC001", merchantId, amount, "USD", type, Instant.now(), Map.of());
    }

    @Test
    void highAmountDebitShouldScoreHighRisk() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("High Amount Debit").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, new BigDecimal("10000"))
                        .and(new TransactionTypeCondition(TransactionType.DEBIT)))
                .then(new ScoreAction(70, "High amount debit"))
                .build());

        EngineResult result = engine.evaluate(tx("T001", new BigDecimal("15000"), TransactionType.DEBIT, "M001"));

        assertEquals(70, result.totalScore());
        assertEquals(RiskLevel.HIGH, result.riskLevel());
        assertTrue(result.approved());
    }

    @Test
    void criticalScoreShouldMapToCriticalRiskLevel() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Critical Score").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, BigDecimal.ZERO))
                .then(new ScoreAction(90, "Critical score"))
                .build());

        EngineResult result = engine.evaluate(tx("T002", new BigDecimal("1"), TransactionType.DEBIT, "M001"));

        assertEquals(RiskLevel.CRITICAL, result.riskLevel());
    }

    @Test
    void rulePriorityShouldDetermineEvaluationOrder() {
        engine.addRule(RuleBuilder.builder()
                .id("R002").name("Low Priority").priority(10)
                .when(new AmountCondition(AmountCondition.Operator.GT, new BigDecimal("100")))
                .then(new ScoreAction(10, "Low priority hit"))
                .build());
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("High Priority").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, new BigDecimal("100")))
                .then(new ScoreAction(20, "High priority hit"))
                .build());

        EngineResult result = engine.evaluate(tx("T003", new BigDecimal("500"), TransactionType.CREDIT, "M001"));

        assertEquals(30, result.totalScore());
        assertEquals("High Priority", result.ruleResults().get(0).ruleName());
    }

    @Test
    void rejectActionShouldShortCircuitRemainingRules() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Reject High Value").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, new BigDecimal("50000")))
                .then(new RejectAction(100, "Amount exceeds limit"))
                .build());
        engine.addRule(RuleBuilder.builder()
                .id("R002").name("Score Rule").priority(2)
                .when(new AmountCondition(AmountCondition.Operator.GT, new BigDecimal("100")))
                .then(new ScoreAction(10, "Normal score"))
                .build());

        EngineResult result = engine.evaluate(tx("T004", new BigDecimal("100000"), TransactionType.DEBIT, "M001"));

        assertFalse(result.approved());
        assertEquals("Amount exceeds limit", result.rejectionReason());
        assertEquals(1, result.ruleResults().stream().filter(RuleResult::conditionMet).count());
    }

    @Test
    void disabledRuleShouldNotBeEvaluated() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Disabled Rule").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, BigDecimal.ZERO))
                .then(new ScoreAction(50, "Should not fire"))
                .disabled()
                .build());

        EngineResult result = engine.evaluate(tx("T005", new BigDecimal("1000"), TransactionType.DEBIT, "M001"));

        assertEquals(0, result.totalScore());
        assertEquals(RiskLevel.LOW, result.riskLevel());
        assertTrue(result.ruleResults().isEmpty());
    }

    @Test
    void compositeAndConditionShouldRequireAllConditions() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Suspicious Debit").priority(1)
                .when(new CompositeCondition(
                        CompositeCondition.LogicOperator.AND,
                        new AmountCondition(AmountCondition.Operator.GT, new BigDecimal("5000")),
                        new TransactionTypeCondition(TransactionType.DEBIT),
                        new MerchantCondition("SUSPICIOUS_MERCHANT")
                ))
                .then(new FlagAction("SUSPICIOUS_TRANSACTION", 40))
                .build());

        EngineResult match = engine.evaluate(tx("T006", new BigDecimal("6000"), TransactionType.DEBIT, "SUSPICIOUS_MERCHANT"));
        EngineResult noMatch = engine.evaluate(tx("T007", new BigDecimal("6000"), TransactionType.CREDIT, "SUSPICIOUS_MERCHANT"));

        assertTrue(match.flags().contains("SUSPICIOUS_TRANSACTION"));
        assertEquals(40, match.totalScore());
        assertFalse(noMatch.flags().contains("SUSPICIOUS_TRANSACTION"));
    }

    @Test
    void compositeOrConditionShouldMatchAny() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("International Merchant").priority(1)
                .when(new CompositeCondition(
                        CompositeCondition.LogicOperator.OR,
                        new MerchantCondition("INTL_1"),
                        new MerchantCondition("INTL_2")
                ))
                .then(new CategoryAction("INTERNATIONAL"))
                .build());

        EngineResult result = engine.evaluate(tx("T008", new BigDecimal("200"), TransactionType.DEBIT, "INTL_2"));

        assertTrue(result.categories().contains("INTERNATIONAL"));
    }

    @Test
    void multipleRulesShouldAccumulateScore() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("High Amount").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, new BigDecimal("5000")))
                .then(new ScoreAction(30, "High amount"))
                .build());
        engine.addRule(RuleBuilder.builder()
                .id("R002").name("Foreign Merchant").priority(2)
                .when(new MerchantCondition("FOREIGN_MERCHANT"))
                .then(new ScoreAction(20, "Foreign merchant"))
                .build());

        EngineResult result = engine.evaluate(tx("T009", new BigDecimal("7000"), TransactionType.DEBIT, "FOREIGN_MERCHANT"));

        assertEquals(50, result.totalScore());
        assertEquals(RiskLevel.MEDIUM, result.riskLevel());
    }

    @Test
    void negatedConditionShouldInvertMatch() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Non-Credit").priority(1)
                .when(new TransactionTypeCondition(TransactionType.CREDIT).negate())
                .then(new ScoreAction(10, "Non-credit transaction"))
                .build());

        EngineResult debit = engine.evaluate(tx("T010", new BigDecimal("100"), TransactionType.DEBIT, "M001"));
        EngineResult credit = engine.evaluate(tx("T011", new BigDecimal("100"), TransactionType.CREDIT, "M001"));

        assertEquals(10, debit.totalScore());
        assertEquals(0, credit.totalScore());
    }

    @Test
    void betweenConditionShouldMatchRange() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Medium Amount").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.BETWEEN, new BigDecimal("1000"), new BigDecimal("5000")))
                .then(new ScoreAction(15, "Medium amount range"))
                .build());

        EngineResult inRange = engine.evaluate(tx("T012", new BigDecimal("3000"), TransactionType.DEBIT, "M001"));
        EngineResult outOfRange = engine.evaluate(tx("T013", new BigDecimal("6000"), TransactionType.DEBIT, "M001"));

        assertEquals(15, inRange.totalScore());
        assertEquals(0, outOfRange.totalScore());
    }

    @Test
    void chainedActionShouldExecuteAllActions() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Flag and Score").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, new BigDecimal("20000")))
                .then(new ChainedAction(
                        new FlagAction("HIGH_VALUE", 25),
                        new CategoryAction("LARGE_TRANSACTION")
                ))
                .build());

        EngineResult result = engine.evaluate(tx("T014", new BigDecimal("25000"), TransactionType.TRANSFER, "M001"));

        assertTrue(result.flags().contains("HIGH_VALUE"));
        assertTrue(result.categories().contains("LARGE_TRANSACTION"));
        assertEquals(25, result.totalScore());
        assertEquals("CHAINED", result.ruleResults().get(0).actionType());
    }

    @Test
    void conditionNotMetShouldRecordSkippedRuleResult() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Refund Rule").priority(1)
                .when(new TransactionTypeCondition(TransactionType.REFUND))
                .then(new ScoreAction(5, "Refund detected"))
                .build());

        EngineResult result = engine.evaluate(tx("T015", new BigDecimal("100"), TransactionType.DEBIT, "M001"));

        assertEquals(1, result.ruleResults().size());
        assertFalse(result.ruleResults().get(0).conditionMet());
        assertEquals("NONE", result.ruleResults().get(0).actionType());
    }

    @Test
    void addAndRemoveRuleDynamically() {
        engine.addRule(RuleBuilder.builder()
                .id("R001").name("Temp Rule").priority(1)
                .when(new AmountCondition(AmountCondition.Operator.GT, BigDecimal.ZERO))
                .then(new ScoreAction(20, "Temp"))
                .build());

        EngineResult before = engine.evaluate(tx("T016", new BigDecimal("100"), TransactionType.DEBIT, "M001"));
        assertEquals(20, before.totalScore());

        engine.removeRule("R001");

        EngineResult after = engine.evaluate(tx("T017", new BigDecimal("100"), TransactionType.DEBIT, "M001"));
        assertEquals(0, after.totalScore());
    }
}
