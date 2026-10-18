package com.rcpky.action;

import com.rcpky.domain.Transaction;
import com.rcpky.engine.EvaluationContext;
import com.rcpky.rule.ActionResult;
import com.rcpky.rule.RuleAction;

public class CategoryAction implements RuleAction {

    private final String category;

    public CategoryAction(String category) {
        this.category = category;
    }

    @Override
    public ActionResult execute(Transaction transaction, EvaluationContext context) {
        context.addCategory(category);
        return new ActionResult("CATEGORY", 0, "Categorized as: " + category);
    }
}
