package com.rcpky.exception;

public class RuleNotFoundException extends RuntimeException {
    public RuleNotFoundException(String ruleId) {
        super("Rule not found: " + ruleId);
    }
}
