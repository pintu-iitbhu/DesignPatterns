package com.rcpky.engine;

import com.rcpky.domain.RiskLevel;

public class ScoreCalculator {

    public RiskLevel toRiskLevel(int score) {
        if (score < 30) return RiskLevel.LOW;
        if (score < 60) return RiskLevel.MEDIUM;
        if (score < 85) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }
}
