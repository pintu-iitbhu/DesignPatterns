package com.rcpky.service;

import com.rcpky.model.Booking;
import com.rcpky.model.SeatHold;

import java.time.Duration;
import java.util.List;

public interface IBookingService {
    SeatHold initiateBooking(String userId, String eventId, List<String> seatIds);
    SeatHold initiateBooking(String userId, String eventId, List<String> seatIds, Duration holdDuration);
    Booking completeBooking(String holdId, String userId, String idempotencyKey);
    void cancelBooking(String bookingId, String userId);
    Booking getBooking(String bookingId);
    List<Booking> getBookingsForUser(String userId);
}
