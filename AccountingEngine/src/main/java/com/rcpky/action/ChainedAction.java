package com.rcpky.action;

import com.rcpky.domain.Transaction;
import com.rcpky.engine.EvaluationContext;
import com.rcpky.rule.ActionResult;
import com.rcpky.rule.RuleAction;

import java.util.List;

public class ChainedAction implements RuleAction {

    private final List<RuleAction> actions;

    public ChainedAction(RuleAction... actions) {
        this.actions = List.of(actions);
    }

    @Override
    public ActionResult execute(Transaction transaction, EvaluationContext context) {
        int totalDelta = 0;
        StringBuilder messages = new StringBuilder();
        for (RuleAction action : actions) {
            ActionResult result = action.execute(transaction, context);
            totalDelta += result.scoreDelta();
            if (!messages.isEmpty()) messages.append("; ");
            messages.append(result.message());
        }
        return new ActionResult("CHAINED", totalDelta, messages.toString());
    }
}
