package com.rcpky.strategy;

import com.rcpky.model.Event;
import com.rcpky.model.Section;

import java.util.Map;

/** Fixed price per section regardless of demand or remaining availability. */
public class FlatPricingStrategy implements IPricingStrategy {

    private final Map<String, Double> prices; // sectionId → price

    public FlatPricingStrategy(Map<String, Double> prices) {
        this.prices = prices;
    }

    @Override
    public double calculatePrice(Section section, Event event) {
        return prices.getOrDefault(section.getId(), 0.0);
    }
}
