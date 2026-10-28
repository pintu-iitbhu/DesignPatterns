package com.rcpky.repository;

import com.rcpky.model.Venue;

import java.util.Optional;

public interface IVenueRepository {
    void save(Venue venue);
    Optional<Venue> findById(String venueId);
}
