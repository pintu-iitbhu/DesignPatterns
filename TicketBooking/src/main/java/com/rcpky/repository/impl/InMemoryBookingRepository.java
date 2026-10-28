package com.rcpky.repository.impl;

import com.rcpky.model.Booking;
import com.rcpky.repository.IBookingRepository;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookingRepository implements IBookingRepository {
    private final ConcurrentHashMap<String, Booking> byId             = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Booking> byIdempotencyKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> byUserId     = new ConcurrentHashMap<>();

    @Override public void save(Booking booking)                  { byId.put(booking.getId(), booking); }
    @Override public Optional<Booking> findById(String id)       { return Optional.ofNullable(byId.get(id)); }

    @Override
    public Booking putIfAbsentByIdempotencyKey(String key, Booking booking) {
        return byIdempotencyKey.putIfAbsent(key, booking);
    }

    @Override
    public Optional<Booking> findByIdempotencyKey(String key) {
        return Optional.ofNullable(byIdempotencyKey.get(key));
    }

    @Override
    public void addBookingForUser(String userId, String bookingId) {
        byUserId.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(bookingId);
    }

    @Override
    public Set<String> getBookingIdsByUser(String userId) {
        return byUserId.getOrDefault(userId, Collections.emptySet());
    }
}
