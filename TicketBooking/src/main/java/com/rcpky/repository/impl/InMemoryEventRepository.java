package com.rcpky.repository.impl;

import com.rcpky.model.Event;
import com.rcpky.repository.IEventRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEventRepository implements IEventRepository {
    private final ConcurrentHashMap<String, Event> store = new ConcurrentHashMap<>();

    @Override public void save(Event event)                  { store.put(event.getId(), event); }
    @Override public Optional<Event> findById(String id)    { return Optional.ofNullable(store.get(id)); }
    @Override public void delete(String id)                  { store.remove(id); }
}
