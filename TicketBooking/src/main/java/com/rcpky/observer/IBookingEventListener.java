package com.rcpky.observer;

import com.rcpky.model.Booking;
import com.rcpky.model.Ticket;

import java.util.List;

/**
 * Observer pattern — SRP + OCP:
 * BookingService publishes events; listeners (email, SMS, audit, analytics)
 * are added without modifying BookingService.
 */
public interface IBookingEventListener {
    void onBookingConfirmed(Booking booking, List<Ticket> tickets);
    void onBookingCancelled(Booking booking);
    void onBookingFailed(Booking booking);
}
