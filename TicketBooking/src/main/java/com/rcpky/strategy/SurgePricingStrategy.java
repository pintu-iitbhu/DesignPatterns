package com.rcpky.strategy;

import com.rcpky.model.Event;
import com.rcpky.model.Section;

import java.util.Map;

/**
 * Multiplies base prices by a surge factor.
 * Demonstrates OCP: a second pricing rule added without touching InventoryService.
 * In production the multiplier could be driven by real-time fill-rate metrics.
 */
public class SurgePricingStrategy implements IPricingStrategy {

    private final Map<String, Double> basePrices;
    private final double surgeMultiplier;

    public SurgePricingStrategy(Map<String, Double> basePrices, double surgeMultiplier) {
        this.basePrices = basePrices;
        this.surgeMultiplier = surgeMultiplier;
    }

    @Override
    public double calculatePrice(Section section, Event event) {
        return basePrices.getOrDefault(section.getId(), 0.0) * surgeMultiplier;
    }
}
