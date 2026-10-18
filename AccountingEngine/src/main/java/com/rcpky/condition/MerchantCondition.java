package com.rcpky.condition;

import com.rcpky.domain.Transaction;
import com.rcpky.rule.RuleCondition;

import java.util.Set;

public class MerchantCondition implements RuleCondition {

    private final Set<String> merchantIds;

    public MerchantCondition(String... merchantIds) {
        this.merchantIds = Set.of(merchantIds);
    }

    @Override
    public boolean evaluate(Transaction tx) {
        return merchantIds.contains(tx.merchantId());
    }
}
