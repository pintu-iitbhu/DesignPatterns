package com.rcpky.action;

import com.rcpky.domain.Transaction;
import com.rcpky.engine.EvaluationContext;
import com.rcpky.rule.ActionResult;
import com.rcpky.rule.RuleAction;

public class RejectAction implements RuleAction {

    private final int scoreDelta;
    private final String reason;

    public RejectAction(int scoreDelta, String reason) {
        this.scoreDelta = scoreDelta;
        this.reason = reason;
    }

    @Override
    public ActionResult execute(Transaction transaction, EvaluationContext context) {
        context.addScore(scoreDelta);
        context.reject(reason);
        return new ActionResult("REJECT", scoreDelta, "Rejected: " + reason);
    }
}
