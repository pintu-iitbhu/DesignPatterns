package com.rcpky.rule;

import com.rcpky.domain.Transaction;
import com.rcpky.engine.EvaluationContext;

public interface RuleAction {
    ActionResult execute(Transaction transaction, EvaluationContext context);
}
