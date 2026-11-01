package com.rcpky.pricing;

import com.rcpky.enums.PowerLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

public class PeakOffPeakPricingStrategy implements IPricingStrategy {
    private static final LocalTime PEAK_START = LocalTime.of(7, 0);
    private static final LocalTime PEAK_END = LocalTime.of(21, 0);

    private final BigDecimal peakRatePerKwh;
    private final BigDecimal offPeakRatePerKwh;

    public PeakOffPeakPricingStrategy(BigDecimal peakRatePerKwh, BigDecimal offPeakRatePerKwh) {
        this.peakRatePerKwh = peakRatePerKwh;
        this.offPeakRatePerKwh = offPeakRatePerKwh;
    }

    @Override
    public BigDecimal calculateCost(double energyKwh, long durationMinutes, PowerLevel powerLevel) {
        BigDecimal rate = isPeakHour() ? peakRatePerKwh : offPeakRatePerKwh;
        return rate.multiply(BigDecimal.valueOf(energyKwh))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPeakHour() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(PEAK_START) && now.isBefore(PEAK_END);
    }

    @Override
    public String getName() {
        return "PeakOffPeak(peak=$" + peakRatePerKwh + ", offPeak=$" + offPeakRatePerKwh + "/kWh)";
    }
}
