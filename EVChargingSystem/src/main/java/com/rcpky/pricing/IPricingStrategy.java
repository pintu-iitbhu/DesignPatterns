package com.rcpky.pricing;

import com.rcpky.enums.PowerLevel;

import java.math.BigDecimal;

public interface IPricingStrategy {
    BigDecimal calculateCost(double energyKwh, long durationMinutes, PowerLevel powerLevel);
    String getName();
}
