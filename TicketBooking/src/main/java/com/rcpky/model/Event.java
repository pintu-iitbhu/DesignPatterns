package com.rcpky.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class Event {
    private final String id;
    private final String name;
    private final Venue venue;
    private final LocalDateTime dateTime;
    private final Category category;
    private final List<String> artists;
    private final String description;
    private final AtomicReference<EventStatus> status;

    public Event(String id, String name, Venue venue, LocalDateTime dateTime,
                 Category category, List<String> artists, String description) {
        this.id = id;
        this.name = name;
        this.venue = venue;
        this.dateTime = dateTime;
        this.category = category;
        this.artists = Collections.unmodifiableList(artists);
        this.description = description;
        this.status = new AtomicReference<>(EventStatus.UPCOMING);
    }

    public String getId()              { return id; }
    public String getName()            { return name; }
    public Venue getVenue()            { return venue; }
    public String getCity()            { return venue.getCity(); }
    public LocalDateTime getDateTime() { return dateTime; }
    public Category getCategory()      { return category; }
    public List<String> getArtists()   { return artists; }
    public String getDescription()     { return description; }
    public EventStatus getStatus()     { return status.get(); }

    public void setStatus(EventStatus s) { status.set(s); }

    @Override
    public String toString() {
        return "Event{id='" + id + "', name='" + name + "', venue='" + venue.getName()
               + "', city='" + getCity() + "', date=" + dateTime + ", category=" + category + "}";
    }
}
