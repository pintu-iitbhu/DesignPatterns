package com.rcpky.repository.impl;

import com.rcpky.model.Seat;
import com.rcpky.repository.ISeatRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySeatRepository implements ISeatRepository {
    private final ConcurrentHashMap<String, Seat>         byId        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> byEvent     = new ConcurrentHashMap<>();

    @Override public void save(Seat seat)                          { byId.put(seat.getId(), seat); }
    @Override public Optional<Seat> findById(String id)            { return Optional.ofNullable(byId.get(id)); }

    @Override
    public void registerEventSeats(String eventId, List<String> seatIds) {
        byEvent.put(eventId, Collections.unmodifiableList(seatIds));
    }

    @Override
    public List<String> getSeatIdsByEvent(String eventId) {
        return byEvent.getOrDefault(eventId, Collections.emptyList());
    }
}
