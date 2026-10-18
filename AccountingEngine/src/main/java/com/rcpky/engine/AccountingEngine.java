package com.rcpky.engine;

import com.rcpky.domain.Transaction;
import com.rcpky.rule.Rule;

public interface AccountingEngine {
    EngineResult evaluate(Transaction transaction);
    void addRule(Rule rule);
    void removeRule(String ruleId);
    void updateRule(Rule rule);
}
