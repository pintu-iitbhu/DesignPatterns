package com.rcpky.condition;

import com.rcpky.domain.PaymentMethod;
import com.rcpky.domain.Transaction;
import com.rcpky.rule.RuleCondition;

import java.util.Set;

public class PaymentMethodCondition implements RuleCondition {

    private final Set<PaymentMethod> methods;

    public PaymentMethodCondition(PaymentMethod... methods) {
        this.methods = Set.of(methods);
    }

    @Override
    public boolean evaluate(Transaction tx) {
        return methods.contains(tx.paymentMethod());
    }
}
