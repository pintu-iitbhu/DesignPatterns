package com.rcpky.service;

import com.rcpky.exception.TicketingException;
import com.rcpky.model.*;
import com.rcpky.repository.IEventRepository;
import com.rcpky.repository.ISectionRepository;
import com.rcpky.repository.IVenueRepository;
import com.rcpky.strategy.IPricingStrategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin-facing service for creating and managing events, venues, and sections.
 *
 * SOLID applied:
 *  S — only orchestrates entity creation + index/inventory delegation.
 *  D — depends on IEventRepository, IVenueRepository, ISectionRepository,
 *      ISearchService, IInventoryService (all abstractions, not concrete classes).
 */
public class EventService implements IEventService {

    private final IEventRepository   eventRepository;
    private final IVenueRepository   venueRepository;
    private final ISectionRepository sectionRepository;
    private final ISearchService     searchService;
    private final IInventoryService  inventoryService;

    public EventService(IEventRepository eventRepository,
                        IVenueRepository venueRepository,
                        ISectionRepository sectionRepository,
                        ISearchService searchService,
                        IInventoryService inventoryService) {
        this.eventRepository   = eventRepository;
        this.venueRepository   = venueRepository;
        this.sectionRepository = sectionRepository;
        this.searchService     = searchService;
        this.inventoryService  = inventoryService;
    }

    @Override
    public Venue createVenue(String name, String city, String address, int capacity) {
        String id = "venue-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Venue venue = new Venue(id, name, city, address, capacity);
        venueRepository.save(venue);
        return venue;
    }

    @Override
    public Section createSection(String venueId, String name, int rows, int seatsPerRow) {
        venueRepository.findById(venueId)
            .orElseThrow(() -> new TicketingException("Venue not found: " + venueId));
        String id = "sec-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Section section = new Section(id, venueId, name, rows, seatsPerRow);
        sectionRepository.save(section);
        return section;
    }

    @Override
    public Event createEvent(String name, String venueId, LocalDateTime dateTime,
                              Category category, List<String> artists, String description) {
        Venue venue = venueRepository.findById(venueId)
            .orElseThrow(() -> new TicketingException("Venue not found: " + venueId));
        String id = "evt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Event event = new Event(id, name, venue, dateTime, category, artists, description);
        eventRepository.save(event);
        searchService.indexEvent(event);
        return event;
    }

    @Override
    public void initializeInventory(String eventId, List<String> sectionIds,
                                     IPricingStrategy pricingStrategy) {
        Event event = getEvent(eventId);
        List<Section> sections = sectionIds.stream()
            .map(id -> sectionRepository.findById(id)
                .orElseThrow(() -> new TicketingException("Section not found: " + id)))
            .collect(Collectors.toList());
        inventoryService.initializeSeats(event, sections, pricingStrategy);
    }

    @Override
    public void cancelEvent(String eventId) {
        Event event = getEvent(eventId);
        event.setStatus(EventStatus.CANCELLED);
        searchService.removeEvent(eventId);
    }

    @Override
    public Event getEvent(String eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new TicketingException("Event not found: " + eventId));
    }

    @Override
    public Venue getVenue(String venueId) {
        return venueRepository.findById(venueId)
            .orElseThrow(() -> new TicketingException("Venue not found: " + venueId));
    }

    @Override
    public Section getSection(String sectionId) {
        return sectionRepository.findById(sectionId)
            .orElseThrow(() -> new TicketingException("Section not found: " + sectionId));
    }
}
