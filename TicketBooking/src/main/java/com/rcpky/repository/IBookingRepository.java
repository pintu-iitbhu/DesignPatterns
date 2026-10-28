package com.rcpky.repository;

import com.rcpky.model.Booking;

import java.util.Optional;
import java.util.Set;

public interface IBookingRepository {
    void save(Booking booking);
    Optional<Booking> findById(String bookingId);

    /**
     * Atomically inserts booking under idempotencyKey if absent.
     * Returns null if insertion succeeded; returns the existing Booking if key was already present.
     */
    Booking putIfAbsentByIdempotencyKey(String key, Booking booking);
    Optional<Booking> findByIdempotencyKey(String key);

    void addBookingForUser(String userId, String bookingId);
    Set<String> getBookingIdsByUser(String userId);
}
