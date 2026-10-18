package com.rcpky.action;

import com.rcpky.domain.Transaction;
import com.rcpky.engine.EvaluationContext;
import com.rcpky.rule.ActionResult;
import com.rcpky.rule.RuleAction;

public class FlagAction implements RuleAction {

    private final String flagName;
    private final int scoreDelta;

    public FlagAction(String flagName, int scoreDelta) {
        this.flagName = flagName;
        this.scoreDelta = scoreDelta;
    }

    @Override
    public ActionResult execute(Transaction transaction, EvaluationContext context) {
        context.addFlag(flagName);
        context.addScore(scoreDelta);
        return new ActionResult("FLAG", scoreDelta, "Flagged as: " + flagName);
    }
}
