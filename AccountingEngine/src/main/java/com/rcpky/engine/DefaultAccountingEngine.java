package com.rcpky.engine;

import com.rcpky.audit.AuditEvent;
import com.rcpky.audit.AuditEventType;
import com.rcpky.audit.AuditRepository;
import com.rcpky.audit.InMemoryAuditRepository;
import com.rcpky.domain.RiskLevel;
import com.rcpky.domain.Transaction;
import com.rcpky.exception.LedgerImbalanceException;
import com.rcpky.idempotency.InMemoryProcessedEventStore;
import com.rcpky.idempotency.ProcessedEventStore;
import com.rcpky.ledger.InMemoryLedgerRepository;
import com.rcpky.ledger.LedgerEntry;
import com.rcpky.ledger.LedgerRepository;
import com.rcpky.repository.RuleRepository;
import com.rcpky.rule.ActionResult;
import com.rcpky.rule.Rule;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DefaultAccountingEngine implements AccountingEngine {

    private final RuleRepository ruleRepository;
    private final ScoreCalculator scoreCalculator;
    private final LedgerValidator ledgerValidator;
    private final ProcessedEventStore processedEventStore;
    private final AuditRepository auditRepository;
    private final LedgerRepository ledgerRepository;

    public DefaultAccountingEngine(RuleRepository ruleRepository) {
        this(ruleRepository,
                new LedgerValidator(),
                new InMemoryProcessedEventStore(),
                new InMemoryAuditRepository(),
                new InMemoryLedgerRepository());
    }

    public DefaultAccountingEngine(
            RuleRepository ruleRepository,
            LedgerValidator ledgerValidator,
            ProcessedEventStore processedEventStore,
            AuditRepository auditRepository,
            LedgerRepository ledgerRepository) {
        this.ruleRepository = ruleRepository;
        this.scoreCalculator = new ScoreCalculator();
        this.ledgerValidator = ledgerValidator;
        this.processedEventStore = processedEventStore;
        this.auditRepository = auditRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public EngineResult evaluate(Transaction transaction) {
        Optional<EngineResult> cached = processedEventStore.getResult(transaction.id());
        if (cached.isPresent()) {
            audit(transaction.id(), AuditEventType.DUPLICATE_DETECTED,
                    "Duplicate event ignored: " + transaction.id());
            return cached.get();
        }

        audit(transaction.id(), AuditEventType.PROCESSING_STARTED,
                "Processing started for: " + transaction.id());

        List<Rule> rules = ruleRepository.findAllEnabled()
                .stream()
                .sorted(Comparator.comparingInt(Rule::priority))
                .toList();

        EvaluationContext ctx = new EvaluationContext();
        boolean anyRuleMatched = false;

        for (Rule rule : rules) {
            if (rule.condition().evaluate(transaction)) {
                anyRuleMatched = true;
                ActionResult actionResult = rule.action().execute(transaction, ctx);
                ctx.addRuleResult(new RuleResult(rule.id(), rule.name(), true,
                        actionResult.actionType(), actionResult.scoreDelta(), actionResult.message()));
                audit(transaction.id(), AuditEventType.RULE_MATCHED,
                        "Rule matched: " + rule.name() + " → " + actionResult.message());
                if (ctx.isRejected()) break;
            } else {
                ctx.addRuleResult(new RuleResult(
                        rule.id(), rule.name(), false, "NONE", 0, "Condition not met"));
                audit(transaction.id(), AuditEventType.RULE_SKIPPED,
                        "Rule not matched: " + rule.name());
            }
        }

        EngineStatus status = resolveStatus(anyRuleMatched, ctx);

        if (!ctx.ledgerEntries().isEmpty() && status == EngineStatus.APPROVED) {
            try {
                ledgerValidator.validate(ctx.ledgerEntries());
                ledgerRepository.saveAll(ctx.ledgerEntries());
            } catch (LedgerImbalanceException e) {
                status = EngineStatus.VALIDATION_FAILED;
                audit(transaction.id(), AuditEventType.VALIDATION_FAILED, e.getMessage());
            }
        }

        RiskLevel riskLevel = scoreCalculator.toRiskLevel(ctx.totalScore());
        EngineResult result = new EngineResult(
                transaction.id(), status, ctx.totalScore(), riskLevel,
                ctx.flags(), ctx.categories(), ctx.ledgerEntries(),
                ctx.ruleResults(), ctx.rejectionReason());

        processedEventStore.markProcessed(transaction.id(), result);
        audit(transaction.id(), AuditEventType.PROCESSING_COMPLETE,
                "Completed with status: " + status);
        return result;
    }

    @Override
    public void addRule(Rule rule) { ruleRepository.save(rule); }

    @Override
    public void removeRule(String ruleId) { ruleRepository.delete(ruleId); }

    @Override
    public void updateRule(Rule rule) { ruleRepository.update(rule); }

    @Override
    public List<AuditEvent> getAuditTrail(String transactionId) {
        return auditRepository.findByTransactionId(transactionId);
    }

    @Override
    public List<LedgerEntry> getLedgerEntries(String transactionId) {
        return ledgerRepository.findByTransactionId(transactionId);
    }

    private EngineStatus resolveStatus(boolean anyRuleMatched, EvaluationContext ctx) {
        if (!anyRuleMatched) return EngineStatus.NO_RULE_MATCHED;
        if (ctx.isRejected())  return EngineStatus.REJECTED;
        return EngineStatus.APPROVED;
    }

    private void audit(String transactionId, AuditEventType type, String message) {
        auditRepository.save(new AuditEvent(
                UUID.randomUUID().toString(), transactionId, type, message, Instant.now()));
    }
}
