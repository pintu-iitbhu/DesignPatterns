package com.rcpky.pricing;

import com.rcpky.enums.PowerLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FlatRatePricingStrategy implements IPricingStrategy {
    private final BigDecimal pricePerKwh;

    public FlatRatePricingStrategy(BigDecimal pricePerKwh) {
        this.pricePerKwh = pricePerKwh;
    }

    @Override
    public BigDecimal calculateCost(double energyKwh, long durationMinutes, PowerLevel powerLevel) {
        return pricePerKwh.multiply(BigDecimal.valueOf(energyKwh))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getName() {
        return "FlatRate($" + pricePerKwh + "/kWh)";
    }
}
