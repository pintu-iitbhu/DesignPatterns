package com.rcpky.service;

import com.rcpky.model.*;
import com.rcpky.strategy.IPricingStrategy;

import java.time.Duration;
import java.util.List;

public interface IInventoryService {
    void initializeSeats(Event event, List<Section> sections, IPricingStrategy pricingStrategy);
    SeatHold holdSeats(String userId, String eventId, List<String> seatIds, Duration holdDuration);
    boolean confirmSeats(SeatHold hold);
    void releaseHold(SeatHold hold);
    void cancelBookedSeats(String eventId, List<String> seatIds);
    SeatHold getHold(String holdId);
    Seat getSeat(String seatId);
    List<Seat> getAvailableSeats(String eventId);
    int getAvailableSeatCount(String eventId);
    void shutdown();
}
