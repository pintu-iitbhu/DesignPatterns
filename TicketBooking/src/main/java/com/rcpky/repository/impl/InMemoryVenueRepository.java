package com.rcpky.repository.impl;

import com.rcpky.model.Venue;
import com.rcpky.repository.IVenueRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryVenueRepository implements IVenueRepository {
    private final ConcurrentHashMap<String, Venue> store = new ConcurrentHashMap<>();

    @Override public void save(Venue venue)                   { store.put(venue.getId(), venue); }
    @Override public Optional<Venue> findById(String id)      { return Optional.ofNullable(store.get(id)); }
}
