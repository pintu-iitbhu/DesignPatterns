package com.rcpky.service;

import com.rcpky.model.Booking;
import com.rcpky.model.Ticket;
import com.rcpky.observer.IBookingEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer implementation — logs booking lifecycle events.
 * Additional channels (email, SMS, push) are independent implementations
 * of IBookingEventListener; none require touching BookingService.
 */
public class NotificationService implements IBookingEventListener {

    private final CopyOnWriteArrayList<String> log = new CopyOnWriteArrayList<>();

    @Override
    public void onBookingConfirmed(Booking booking, List<Ticket> tickets) {
        String msg = String.format("[CONFIRMATION] booking=%s user=%s seats=%d amount=$%.2f",
            booking.getId(), booking.getUserId(), tickets.size(), booking.getTotalAmount());
        log.add(msg);
        System.out.println(msg);
    }

    @Override
    public void onBookingCancelled(Booking booking) {
        String msg = String.format("[CANCELLATION] booking=%s user=%s refund=$%.2f",
            booking.getId(), booking.getUserId(), booking.getTotalAmount());
        log.add(msg);
        System.out.println(msg);
    }

    @Override
    public void onBookingFailed(Booking booking) {
        String msg = String.format("[FAILED] booking=%s user=%s",
            booking.getId(), booking.getUserId());
        log.add(msg);
        System.out.println(msg);
    }

    public List<String> getLog() {
        return Collections.unmodifiableList(new ArrayList<>(log));
    }
}
