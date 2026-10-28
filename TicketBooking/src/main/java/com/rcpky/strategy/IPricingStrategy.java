package com.rcpky.strategy;

import com.rcpky.model.Event;
import com.rcpky.model.Section;

/**
 * Strategy pattern — OCP: add new pricing rules (surge, discount, tier)
 * without modifying InventoryService.
 */
@FunctionalInterface
public interface IPricingStrategy {
    double calculatePrice(Section section, Event event);
}
