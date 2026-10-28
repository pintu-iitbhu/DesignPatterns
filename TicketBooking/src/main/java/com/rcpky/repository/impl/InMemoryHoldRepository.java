package com.rcpky.repository.impl;

import com.rcpky.model.SeatHold;
import com.rcpky.repository.IHoldRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryHoldRepository implements IHoldRepository {
    private final ConcurrentHashMap<String, SeatHold> store = new ConcurrentHashMap<>();

    @Override public void save(SeatHold hold)                    { store.put(hold.getId(), hold); }
    @Override public Optional<SeatHold> findById(String id)      { return Optional.ofNullable(store.get(id)); }
    @Override public void delete(String id)                      { store.remove(id); }
    @Override public Collection<SeatHold> findAll()              { return store.values(); }
}
