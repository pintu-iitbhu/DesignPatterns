package com.rcpky.pricing;

import com.rcpky.enums.PowerLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TimeBasedPricingStrategy implements IPricingStrategy {
    private final BigDecimal pricePerMinute;

    public TimeBasedPricingStrategy(BigDecimal pricePerMinute) {
        this.pricePerMinute = pricePerMinute;
    }

    @Override
    public BigDecimal calculateCost(double energyKwh, long durationMinutes, PowerLevel powerLevel) {
        long billableMinutes = Math.max(1, durationMinutes);
        return pricePerMinute.multiply(BigDecimal.valueOf(billableMinutes))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getName() {
        return "TimeBased($" + pricePerMinute + "/min)";
    }
}
