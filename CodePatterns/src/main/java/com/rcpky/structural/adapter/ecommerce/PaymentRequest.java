package com.rcpky.structural.adapter.ecommerce;

import java.math.BigDecimal;

public class PaymentRequest {
    private String customerId;
    private BigDecimal amount;
    private String currency;

    public PaymentRequest(String customerId, BigDecimal amount, String currency) {
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
