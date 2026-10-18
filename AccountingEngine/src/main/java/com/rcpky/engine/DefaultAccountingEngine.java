package com.rcpky.engine;

import com.rcpky.domain.RiskLevel;
import com.rcpky.domain.Transaction;
import com.rcpky.repository.RuleRepository;
import com.rcpky.rule.ActionResult;
import com.rcpky.rule.Rule;

import java.util.Comparator;
import java.util.List;

public class DefaultAccountingEngine implements AccountingEngine {

    private final RuleRepository ruleRepository;
    private final ScoreCalculator scoreCalculator;

    public DefaultAccountingEngine(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
        this.scoreCalculator = new ScoreCalculator();
    }

    @Override
    public EngineResult evaluate(Transaction transaction) {
        List<Rule> rules = ruleRepository.findAllEnabled()
                .stream()
                .sorted(Comparator.comparingInt(Rule::priority))
                .toList();

        EvaluationContext ctx = new EvaluationContext();

        for (Rule rule : rules) {
            if (rule.condition().evaluate(transaction)) {
                ActionResult actionResult = rule.action().execute(transaction, ctx);
                ctx.addRuleResult(new RuleResult(
                        rule.id(), rule.name(), true,
                        actionResult.actionType(), actionResult.scoreDelta(), actionResult.message()
                ));
                if (ctx.isRejected()) break;
            } else {
                ctx.addRuleResult(new RuleResult(
                        rule.id(), rule.name(), false, "NONE", 0, "Condition not met"
                ));
            }
        }

        RiskLevel riskLevel = scoreCalculator.toRiskLevel(ctx.totalScore());
        return new EngineResult(
                transaction.id(),
                ctx.totalScore(),
                riskLevel,
                ctx.flags(),
                ctx.categories(),
                ctx.ruleResults(),
                !ctx.isRejected(),
                ctx.rejectionReason()
        );
    }

    @Override
    public void addRule(Rule rule) {
        ruleRepository.save(rule);
    }

    @Override
    public void removeRule(String ruleId) {
        ruleRepository.delete(ruleId);
    }

    @Override
    public void updateRule(Rule rule) {
        ruleRepository.update(rule);
    }
}
