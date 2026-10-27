package com.rcpky.engine;

import com.rcpky.flag.FlagValue;
import com.rcpky.rollout.Rollout;
import com.rcpky.rollout.RolloutEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RolloutEvaluatorTest {

    private RolloutEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RolloutEvaluator();
    }

    @Test
    void evaluate_returnsEmpty_whenRolloutIsNull() {
        assertEquals(Optional.empty(), evaluator.evaluate("flag", "user-1", null));
    }

    @Test
    void evaluate_returnsEmpty_whenUserIdIsNull() {
        Rollout rollout = new Rollout(100, FlagValue.of(true), null);
        assertEquals(Optional.empty(), evaluator.evaluate("flag", null, rollout));
    }

    @Test
    void evaluate_returnsEmpty_whenUserIdIsBlank() {
        Rollout rollout = new Rollout(100, FlagValue.of(true), null);
        assertEquals(Optional.empty(), evaluator.evaluate("flag", "   ", rollout));
    }

    @Test
    void evaluate_returnsValue_forAllUsers_whenPercentageIs100() {
        Rollout rollout = new Rollout(100, FlagValue.of(true), null);
        for (int i = 0; i < 50; i++) {
            Optional<FlagValue> result = evaluator.evaluate("flag", "user-" + i, rollout);
            assertTrue(result.isPresent(), "user-" + i + " should be in 100% rollout");
        }
    }

    @Test
    void evaluate_returnsEmpty_forAllUsers_whenPercentageIs0() {
        Rollout rollout = new Rollout(0, FlagValue.of(true), null);
        for (int i = 0; i < 50; i++) {
            Optional<FlagValue> result = evaluator.evaluate("flag", "user-" + i, rollout);
            assertTrue(result.isEmpty(), "user-" + i + " should not be in 0% rollout");
        }
    }

    @RepeatedTest(10)
    void evaluate_isSticky_sameUserAlwaysGetsSameResult() {
        String userId = UUID.randomUUID().toString();
        Rollout rollout = new Rollout(50, FlagValue.of("variant-A"), null);

        Optional<FlagValue> first = evaluator.evaluate("my-flag", userId, rollout);
        for (int i = 0; i < 20; i++) {
            assertEquals(first, evaluator.evaluate("my-flag", userId, rollout),
                    "Bucketing must be deterministic for user=" + userId);
        }
    }

    @Test
    void evaluate_isIndependentPerFlag_sameUserDifferentFlags() {
        String userId = "sticky-user";
        Rollout rollout = new Rollout(50, FlagValue.of(true), null);

        Optional<FlagValue> resultA = evaluator.evaluate("flag-alpha", userId, rollout);
        Optional<FlagValue> resultB = evaluator.evaluate("flag-beta", userId, rollout);

        // Each flag buckets independently; results may differ
        // Just verify determinism per flag
        assertEquals(resultA, evaluator.evaluate("flag-alpha", userId, rollout));
        assertEquals(resultB, evaluator.evaluate("flag-beta", userId, rollout));
    }

    @Test
    void evaluate_rolloutDistributionIsApproximatelyCorrect() {
        Rollout rollout = new Rollout(50, FlagValue.of(true), null);
        int inRollout = 0;
        int total = 1000;

        for (int i = 0; i < total; i++) {
            if (evaluator.evaluate("dist-flag", "user-" + i, rollout).isPresent()) {
                inRollout++;
            }
        }

        double actualPercent = (inRollout * 100.0) / total;
        assertTrue(actualPercent >= 40 && actualPercent <= 60,
                "Expected ~50% rollout, got " + actualPercent + "%");
    }
}
