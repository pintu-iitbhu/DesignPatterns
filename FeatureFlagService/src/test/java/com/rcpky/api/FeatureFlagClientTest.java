package com.rcpky.api;

import com.rcpky.exception.FlagNotFoundException;
import com.rcpky.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.rcpky.model.ConditionOperator.*;
import static com.rcpky.model.LogicOperator.*;


import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;


class FeatureFlagClientTest {

    private FeatureFlagClient client;

    @BeforeEach
    void setUp() {
        client = new FeatureFlagClient();
    }

    // --- Boolean flags ---

    @Test
    void getBooleanFlag_returnsDefault_whenNoRuleMatches() {
        client.register(new FlagDefinition("dark-mode", FlagType.BOOLEAN, false, List.of(), null, true));
        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "free"));
        assertFalse(client.getBooleanFlag("dark-mode", ctx));
    }

    @Test
    void getBooleanFlag_returnsTrue_forMatchingTargetingRule() {
        Rule rule = Rule.and(List.of(
                new Condition("plan", IN, List.of("pro", "enterprise"))
        ), true);
        client.register(new FlagDefinition("dark-mode", FlagType.BOOLEAN, false, List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "enterprise"));
        assertTrue(client.getBooleanFlag("dark-mode", ctx));
    }

    // --- String flags ---

    @Test
    void getStringFlag_returnsDefault_whenNoRuleMatches() {
        client.register(new FlagDefinition("checkout-flow", FlagType.STRING, "v1", List.of(), null, true));
        EvaluationContext ctx = EvaluationContext.of("u1", Map.of());
        assertEquals("v1", client.getStringFlag("checkout-flow", ctx));
    }

    @Test
    void getStringFlag_returnsVariant_forMatchingRule() {
        Rule rule = Rule.and(List.of(
                new Condition("country", EQUALS, "US")
        ), "v2");
        client.register(new FlagDefinition("checkout-flow", FlagType.STRING, "v1", List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("country", "US"));
        assertEquals("v2", client.getStringFlag("checkout-flow", ctx));
    }

    // --- Integer flags ---

    @Test
    void getIntegerFlag_returnsDefault_whenNoRuleMatches() {
        client.register(new FlagDefinition("max-retries", FlagType.INTEGER, 3, List.of(), null, true));
        EvaluationContext ctx = EvaluationContext.of("u1", Map.of());
        assertEquals(3, client.getIntegerFlag("max-retries", ctx));
    }

    @Test
    void getIntegerFlag_returnsHigherLimit_forEnterpriseUser() {
        Rule rule = Rule.and(List.of(
                new Condition("plan", EQUALS, "enterprise")
        ), 100);
        client.register(new FlagDefinition("max-retries", FlagType.INTEGER, 3, List.of(rule), null, true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "enterprise"));
        assertEquals(100, client.getIntegerFlag("max-retries", ctx));
    }

    // --- Rollout ---

    @Test
    void rollout_isStickyForSameUser() {
        client.register(new FlagDefinition("gradual-release", FlagType.BOOLEAN, false,
                List.of(), new Rollout(50, true, null), true));

        EvaluationContext ctx = EvaluationContext.of("sticky-user-99", Map.of());
        boolean firstResult = client.getBooleanFlag("gradual-release", ctx);

        for (int i = 0; i < 20; i++) {
            assertEquals(firstResult, client.getBooleanFlag("gradual-release", ctx), "Rollout result must be sticky for the same user");
        }
    }

    @Test
    void rollout_anonymousContext_returnsDefault() {
        client.register(new FlagDefinition("gradual-release", FlagType.BOOLEAN, false,
                List.of(), new Rollout(100, true, null), true));

        assertFalse(client.getBooleanFlag("gradual-release", EvaluationContext.anonymous()),
                "Anonymous users (no userId) should fall through to default");
    }

    // --- AND + OR complex rule ---

    @Test
    void complexRule_AND_then_OR_conditions() {
        Rule andRule = Rule.and(List.of(
                new Condition("plan", EQUALS, "pro"),
                new Condition("country", IN, List.of("US", "CA"))
        ), "pro-na-variant");

        Rule orRule = Rule.or(List.of(
                new Condition("user_id", EQUALS, "beta-tester-1"),
                new Condition("user_id", EQUALS, "beta-tester-2")
        ), "beta-variant");

        client.register(new FlagDefinition("feature-y", FlagType.STRING, "default",
                List.of(andRule, orRule), null, true));

        EvaluationContext proUserUS = EvaluationContext.of("u1",
                Map.of("plan", "pro", "country", "US", "user_id", "u1"));
        assertEquals("pro-na-variant", client.getStringFlag("feature-y", proUserUS));

        EvaluationContext betaTester = EvaluationContext.of("beta-tester-2",
                Map.of("plan", "free", "country", "DE", "user_id", "beta-tester-2"));
        assertEquals("beta-variant", client.getStringFlag("feature-y", betaTester));

        EvaluationContext noMatch = EvaluationContext.of("u3",
                Map.of("plan", "free", "country", "DE", "user_id", "u3"));
        assertEquals("default", client.getStringFlag("feature-y", noMatch));
    }

    // --- Disabled flag (kill-switch) ---

    @Test
    void disabledFlag_returnsDefaultAndDisabledReason_ignoringRulesAndRollout() {
        Rule rule = Rule.and(List.of(new Condition("plan", EQUALS, "enterprise")), true);
        client.register(new FlagDefinition("kill-switch", FlagType.BOOLEAN, false,
                List.of(rule), new Rollout(100, true, null), false));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("plan", "enterprise"));
        EvaluationResult<Boolean> result = client.evaluate("kill-switch", ctx, Boolean.class);

        assertFalse(result.value(), "Disabled flag must return default regardless of rules");
        assertEquals(EvaluationReason.DISABLED, result.reason());
    }

    @Test
    void rollout_bucketsByTenantId_whenBucketingKeyIsConfigured() {
        client.register(new FlagDefinition("tenant-rollout", FlagType.BOOLEAN, false,
                List.of(), new Rollout(100, true, "tenant_id"), true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of("tenant_id", "acme-corp"));
        assertTrue(client.getBooleanFlag("tenant-rollout", ctx),
                "100% rollout bucketed by tenant_id should include all tenants");

        EvaluationContext ctxSameTenant = EvaluationContext.of("u2", Map.of("tenant_id", "acme-corp"));
        assertEquals(
                client.getBooleanFlag("tenant-rollout", ctx),
                client.getBooleanFlag("tenant-rollout", ctxSameTenant),
                "Same tenant must always get the same result regardless of userId");
    }

    // --- Error cases ---

    @Test
    void throws_whenFlagNotRegistered() {
        EvaluationContext ctx = EvaluationContext.anonymous();
        assertThrows(FlagNotFoundException.class,
                () -> client.getBooleanFlag("nonexistent", ctx));
    }

    @Test
    void evaluate_exposesReasonAlongWithValue() {
        client.register(new FlagDefinition("explained-flag", FlagType.BOOLEAN, false,
                List.of(), new Rollout(100, true, null), true));

        EvaluationContext ctx = EvaluationContext.of("u1", Map.of());
        EvaluationResult<Boolean> result = client.evaluate("explained-flag", ctx, Boolean.class);

        assertTrue(result.value());
        assertEquals(EvaluationReason.ROLLOUT, result.reason());
    }

    @Test
    void deregister_removesFlag() {
        client.register(new FlagDefinition("temp-flag", FlagType.BOOLEAN, false, List.of(), null, true));
        client.deregister("temp-flag");
        assertThrows(FlagNotFoundException.class,
                () -> client.getBooleanFlag("temp-flag", EvaluationContext.anonymous()));
    }
}
