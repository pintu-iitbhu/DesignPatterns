package com.rcpky.action;

import com.rcpky.domain.Transaction;
import com.rcpky.engine.EvaluationContext;
import com.rcpky.ledger.LedgerEntry;
import com.rcpky.ledger.LedgerEntryType;
import com.rcpky.ledger.LedgerRepository;
import com.rcpky.rule.ActionResult;
import com.rcpky.rule.RuleAction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReversalAction implements RuleAction {

    private final LedgerRepository ledgerRepository;

    public ReversalAction(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public ActionResult execute(Transaction tx, EvaluationContext ctx) {
        String originalId = (String) tx.metadata().get("originalTransactionId");
        List<LedgerEntry> original = ledgerRepository.findByTransactionId(originalId);
        List<LedgerEntry> reversed = original.stream()
                .map(e -> new LedgerEntry(
                        UUID.randomUUID().toString(),
                        tx.id(),
                        e.accountId(),
                        e.type() == LedgerEntryType.DEBIT ? LedgerEntryType.CREDIT : LedgerEntryType.DEBIT,
                        e.amount(),
                        e.currency(),
                        "Reversal of: " + e.description(),
                        Instant.now()
                ))
                .toList();
        reversed.forEach(ctx::addLedgerEntry);
        return new ActionResult("REVERSAL", 0,
                "Reversed " + reversed.size() + " entries from " + originalId);
    }
}
