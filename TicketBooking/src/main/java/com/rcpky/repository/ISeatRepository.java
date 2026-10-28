package com.rcpky.repository;

import com.rcpky.model.Seat;

import java.util.List;
import java.util.Optional;

public interface ISeatRepository {
    void save(Seat seat);
    Optional<Seat> findById(String seatId);
    void registerEventSeats(String eventId, List<String> seatIds);
    List<String> getSeatIdsByEvent(String eventId);
}
