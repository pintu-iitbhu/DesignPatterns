package com.rcpky.condition;

import com.rcpky.domain.Transaction;
import com.rcpky.rule.RuleCondition;

import java.util.Set;

public class CurrencyCondition implements RuleCondition {

    private final Set<String> currencies;

    public CurrencyCondition(String... currencies) {
        this.currencies = Set.of(currencies);
    }

    @Override
    public boolean evaluate(Transaction tx) {
        return currencies.contains(tx.currency());
    }
}
