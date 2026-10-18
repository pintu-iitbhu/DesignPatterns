package com.rcpky.action;

import com.rcpky.domain.Transaction;
import com.rcpky.engine.EvaluationContext;
import com.rcpky.ledger.LedgerEntry;
import com.rcpky.rule.ActionResult;
import com.rcpky.rule.RuleAction;

import java.util.List;
import java.util.function.Function;

public class LedgerAction implements RuleAction {

    private final Function<Transaction, List<LedgerEntry>> entryGenerator;

    public LedgerAction(Function<Transaction, List<LedgerEntry>> entryGenerator) {
        this.entryGenerator = entryGenerator;
    }

    @Override
    public ActionResult execute(Transaction transaction, EvaluationContext context) {
        List<LedgerEntry> entries = entryGenerator.apply(transaction);
        entries.forEach(context::addLedgerEntry);
        return new ActionResult("LEDGER", 0, entries.size() + " ledger entries generated");
    }
}
