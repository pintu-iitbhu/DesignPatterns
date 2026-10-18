package com.rcpky.action;

import com.rcpky.domain.Transaction;
import com.rcpky.engine.EvaluationContext;
import com.rcpky.rule.ActionResult;
import com.rcpky.rule.RuleAction;

public class ScoreAction implements RuleAction {

    private final int scoreDelta;
    private final String message;

    public ScoreAction(int scoreDelta, String message) {
        this.scoreDelta = scoreDelta;
        this.message = message;
    }

    @Override
    public ActionResult execute(Transaction transaction, EvaluationContext context) {
        context.addScore(scoreDelta);
        return new ActionResult("SCORE", scoreDelta, message);
    }
}
