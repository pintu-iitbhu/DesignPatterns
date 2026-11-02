package com.rcpky;

import com.rcpky.api.FeatureFlagClient;
import com.rcpky.evaluation.EvaluationContext;
import com.rcpky.evaluation.EvaluationResult;
import com.rcpky.flag.FlagDefinition;
import com.rcpky.flag.FlagType;
import com.rcpky.flag.FlagValue;
import com.rcpky.rollout.Rollout;
import com.rcpky.rule.Condition;
import com.rcpky.rule.ConditionOperator;
import com.rcpky.rule.Rule;

import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           FEATURE FLAG SERVICE — DEMO                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        FeatureFlagClient client = new FeatureFlagClient();

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 1: Boolean flag "dark-mode" — 30% rollout
        //   Some users get ROLLOUT, others get DEFAULT (consistent bucketing)
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 1: Boolean flag \"dark-mode\" — 30% rollout ━━━");
        client.register(new FlagDefinition(
                "dark-mode",
                FlagType.BOOLEAN,
                FlagValue.of(false),
                List.of(),
                new Rollout(30, FlagValue.of(true), null),
                true
        ));

        String[] users = {"user-001", "user-002", "user-003", "user-004", "user-005",
                          "user-006", "user-007", "user-008", "user-009", "user-010"};
        for (String userId : users) {
            EvaluationResult<Boolean> r = client.evaluate("dark-mode",
                    EvaluationContext.of(userId, Map.of()), Boolean.class);
            System.out.printf("  %-10s → dark-mode=%s  [%s]%n", userId, r.value(), r.reason());
        }
        System.out.println();

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 2: String flag "theme-color"
        //   Rule: plan=premium → "gold"  (RULE_MATCH)
        //   Fallback default            → "blue" (DEFAULT)
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 2: String flag \"theme-color\" — rule: plan=premium → \"gold\" ━━━");
        client.register(new FlagDefinition(
                "theme-color",
                FlagType.STRING,
                FlagValue.of("blue"),
                List.of(
                        Rule.and(
                                List.of(new Condition("plan", ConditionOperator.EQUALS, "premium")),
                                FlagValue.of("gold")
                        )
                ),
                null,
                true
        ));

        for (Map.Entry<String, String> entry : Map.of(
                "alice", "premium",
                "bob",   "free",
                "carol", "enterprise").entrySet()) {
            EvaluationContext ctx = EvaluationContext.of(entry.getKey(), Map.of("plan", entry.getValue()));
            EvaluationResult<String> r = client.evaluate("theme-color", ctx, String.class);
            System.out.printf("  %-6s (plan=%-10s) → theme-color=\"%s\"  [%s]%n",
                    entry.getKey(), entry.getValue(), r.value(), r.reason());
        }
        System.out.println();

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 3: Disabled flag "beta-feature"
        //   Always returns DEFAULT regardless of rules or rollout
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 3: Disabled flag \"beta-feature\" — always DEFAULT ━━━");
        client.register(new FlagDefinition(
                "beta-feature",
                FlagType.BOOLEAN,
                FlagValue.of(false),
                List.of(Rule.and(
                        List.of(new Condition("plan", ConditionOperator.EQUALS, "enterprise")),
                        FlagValue.of(true)
                )),
                new Rollout(100, FlagValue.of(true), null),
                false   // ← disabled
        ));

        EvaluationResult<Boolean> beta = client.evaluate("beta-feature",
                EvaluationContext.of("enterprise-user", Map.of("plan", "enterprise")), Boolean.class);
        System.out.printf("  enterprise-user (plan=enterprise) → beta-feature=%s  [%s]%n%n",
                beta.value(), beta.reason());

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 4: AND composite rule
        //   country=US AND role=admin → "admin-config-v2"  (RULE_MATCH)
        //   otherwise                 → "default-config"   (DEFAULT)
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 4: AND rule — country=US AND role=admin → \"admin-config-v2\" ━━━");
        client.register(new FlagDefinition(
                "admin-config",
                FlagType.STRING,
                FlagValue.of("default-config"),
                List.of(
                        Rule.and(
                                List.of(
                                        new Condition("country", ConditionOperator.EQUALS, "US"),
                                        new Condition("role",    ConditionOperator.EQUALS, "admin")
                                ),
                                FlagValue.of("admin-config-v2")
                        )
                ),
                null,
                true
        ));

        record UserAttr(String id, String country, String role) {}
        List<UserAttr> adminUsers = List.of(
                new UserAttr("u1", "US",  "admin"),
                new UserAttr("u2", "US",  "viewer"),
                new UserAttr("u3", "IN",  "admin"),
                new UserAttr("u4", "IN",  "viewer")
        );
        for (UserAttr u : adminUsers) {
            EvaluationContext ctx = EvaluationContext.of(u.id(),
                    Map.of("country", u.country(), "role", u.role()));
            EvaluationResult<String> r = client.evaluate("admin-config", ctx, String.class);
            System.out.printf("  %-3s (country=%-3s role=%-7s) → \"%s\"  [%s]%n",
                    u.id(), u.country(), u.role(), r.value(), r.reason());
        }
        System.out.println();

        // ═══════════════════════════════════════════════════════════════════════
        // SCENARIO 5: OR rule — plan=premium OR plan=enterprise → enabled
        //   Either plan qualifies; free plan falls through to DEFAULT (false)
        // ═══════════════════════════════════════════════════════════════════════
        System.out.println("━━━ SCENARIO 5: OR rule — plan=premium OR plan=enterprise → enabled ━━━");
        client.register(new FlagDefinition(
                "advanced-analytics",
                FlagType.BOOLEAN,
                FlagValue.of(false),
                List.of(
                        Rule.or(
                                List.of(
                                        new Condition("plan", ConditionOperator.EQUALS, "premium"),
                                        new Condition("plan", ConditionOperator.EQUALS, "enterprise")
                                ),
                                FlagValue.of(true)
                        )
                ),
                null,
                true
        ));

        for (String plan : List.of("premium", "enterprise", "free", "trial")) {
            EvaluationContext ctx = EvaluationContext.of("u-" + plan, Map.of("plan", plan));
            EvaluationResult<Boolean> r = client.evaluate("advanced-analytics", ctx, Boolean.class);
            System.out.printf("  plan=%-12s → advanced-analytics=%s  [%s]%n",
                    plan, r.value(), r.reason());
        }

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                   END OF DEMONSTRATION                       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
}
