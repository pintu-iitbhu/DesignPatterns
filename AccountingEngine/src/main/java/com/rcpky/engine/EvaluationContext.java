package com.rcpky.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EvaluationContext {

    private int totalScore;
    private boolean rejected;
    private String rejectionReason;
    private final Set<String> flags = new HashSet<>();
    private final Set<String> categories = new HashSet<>();
    private final List<RuleResult> ruleResults = new ArrayList<>();

    public void addScore(int delta) {
        this.totalScore += delta;
    }

    public void addFlag(String flag) {
        this.flags.add(flag);
    }

    public void addCategory(String category) {
        this.categories.add(category);
    }

    public void addRuleResult(RuleResult result) {
        this.ruleResults.add(result);
    }

    public void reject(String reason) {
        this.rejected = true;
        this.rejectionReason = reason;
    }

    public int totalScore() { return totalScore; }
    public boolean isRejected() { return rejected; }
    public String rejectionReason() { return rejectionReason; }
    public Set<String> flags() { return Set.copyOf(flags); }
    public Set<String> categories() { return Set.copyOf(categories); }
    public List<RuleResult> ruleResults() { return List.copyOf(ruleResults); }
}
