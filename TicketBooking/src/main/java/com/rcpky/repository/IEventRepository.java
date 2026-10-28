package com.rcpky.repository;

import com.rcpky.model.Event;

import java.util.Optional;

public interface IEventRepository {
    void save(Event event);
    Optional<Event> findById(String eventId);
    void delete(String eventId);
}
