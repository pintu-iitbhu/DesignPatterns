package com.rcpky.service;

import com.rcpky.model.*;
import com.rcpky.strategy.IPricingStrategy;

import java.time.LocalDateTime;
import java.util.List;

public interface IEventService {
    Venue createVenue(String name, String city, String address, int capacity);
    Section createSection(String venueId, String name, int rows, int seatsPerRow);
    Event createEvent(String name, String venueId, LocalDateTime dateTime,
                      Category category, List<String> artists, String description);
    void initializeInventory(String eventId, List<String> sectionIds, IPricingStrategy pricingStrategy);
    void cancelEvent(String eventId);
    Event getEvent(String eventId);
    Venue getVenue(String venueId);
    Section getSection(String sectionId);
}
