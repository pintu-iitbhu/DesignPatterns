package com.rcpky.engine;

import com.rcpky.domain.RiskLevel;

import java.util.List;
import java.util.Set;

public record EngineResult(
        String transactionId,
        int totalScore,
        RiskLevel riskLevel,
        Set<String> flags,
        Set<String> categories,
        List<RuleResult> ruleResults,
        boolean approved,
        String rejectionReason
) {}
