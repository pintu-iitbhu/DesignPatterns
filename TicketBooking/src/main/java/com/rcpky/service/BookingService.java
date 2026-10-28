package com.rcpky.service;

import com.rcpky.exception.*;
import com.rcpky.factory.TicketFactory;
import com.rcpky.model.*;
import com.rcpky.observer.IBookingEventListener;
import com.rcpky.repository.IBookingRepository;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the full booking lifecycle:
 *
 *   initiateBooking  →  hold seats (strongly consistent)
 *   completeBooking  →  process payment → confirm seats → issue tickets
 *   cancelBooking    →  release seats + refund payment
 *
 * SOLID applied:
 *  S — pure orchestration; storage → IBookingRepository, tickets → TicketFactory,
 *      notifications → IBookingEventListener list (no direct notification concern).
 *  O — new notification channels extend via IBookingEventListener; BookingService unchanged.
 *  D — every dependency is an interface: IInventoryService, IPaymentService,
 *      IBookingRepository, TicketFactory, List<IBookingEventListener>.
 *
 * Reliability:
 *  - Idempotency key deduplication (putIfAbsentByIdempotencyKey) prevents duplicate charges.
 *  - Payment failure atomically releases the hold.
 *  - If confirmSeats fails after payment, payment is immediately refunded.
 */
public class BookingService implements IBookingService {

    private static final Duration DEFAULT_HOLD_DURATION = Duration.ofMinutes(10);

    private final IInventoryService             inventoryService;
    private final IPaymentService               paymentService;
    private final IBookingRepository            bookingRepository;
    private final TicketFactory                 ticketFactory;
    private final List<IBookingEventListener>   eventListeners;

    public BookingService(IInventoryService inventoryService,
                          IPaymentService paymentService,
                          IBookingRepository bookingRepository,
                          TicketFactory ticketFactory,
                          List<IBookingEventListener> eventListeners) {
        this.inventoryService  = inventoryService;
        this.paymentService    = paymentService;
        this.bookingRepository = bookingRepository;
        this.ticketFactory     = ticketFactory;
        this.eventListeners    = eventListeners;
    }

    @Override
    public SeatHold initiateBooking(String userId, String eventId, List<String> seatIds) {
        return initiateBooking(userId, eventId, seatIds, DEFAULT_HOLD_DURATION);
    }

    @Override
    public SeatHold initiateBooking(String userId, String eventId,
                                     List<String> seatIds, Duration holdDuration) {
        return inventoryService.holdSeats(userId, eventId, seatIds, holdDuration);
    }

    @Override
    public Booking completeBooking(String holdId, String userId, String idempotencyKey) {
        Booking cached = bookingRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (cached != null) return cached;

        SeatHold hold = inventoryService.getHold(holdId);
        if (hold == null)
            throw new HoldExpiredException("Hold not found or already expired: " + holdId);
        if (!hold.isActive())
            throw new HoldExpiredException("Hold is no longer active: " + holdId);
        if (hold.isExpired()) {
            inventoryService.releaseHold(hold);
            throw new HoldExpiredException("Hold has expired: " + holdId);
        }
        if (!hold.getUserId().equals(userId))
            throw new TicketingException("Hold does not belong to user: " + userId);

        double totalAmount = hold.getSeatIds().stream()
            .mapToDouble(id -> {
                Seat seat = inventoryService.getSeat(id);
                return seat != null ? seat.getPrice() : 0.0;
            })
            .sum();

        String bookingId = "bkg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Booking booking  = new Booking(bookingId, userId, holdId, hold.getEventId(),
                                       hold.getSeatIds(), totalAmount, idempotencyKey);

        Booking existing = bookingRepository.putIfAbsentByIdempotencyKey(idempotencyKey, booking);
        if (existing != null) return existing;

        bookingRepository.save(booking);
        bookingRepository.addBookingForUser(userId, bookingId);

        // --- Payment ---
        Payment payment;
        try {
            payment = paymentService.processPayment(userId, bookingId, totalAmount, idempotencyKey);
        } catch (PaymentFailedException e) {
            booking.transitionStatus(BookingStatus.PENDING, BookingStatus.FAILED);
            inventoryService.releaseHold(hold);
            eventListeners.forEach(l -> l.onBookingFailed(booking));
            throw e;
        }
        booking.setPayment(payment);

        // --- Confirm seats ---
        boolean confirmed = inventoryService.confirmSeats(hold);
        if (!confirmed) {
            booking.transitionStatus(BookingStatus.PENDING, BookingStatus.FAILED);
            paymentService.refundPayment(payment.getId());
            eventListeners.forEach(l -> l.onBookingFailed(booking));
            throw new TicketingException(
                "Seat confirmation failed (hold expired mid-payment): " + bookingId);
        }

        // --- Issue tickets via TicketFactory ---
        List<Ticket> tickets = hold.getSeatIds().stream()
            .map(seatId -> ticketFactory.create(bookingId, hold.getEventId(), seatId, userId))
            .collect(Collectors.toList());
        booking.setTickets(tickets);
        booking.transitionStatus(BookingStatus.PENDING, BookingStatus.CONFIRMED);

        eventListeners.forEach(l -> l.onBookingConfirmed(booking, tickets));
        return booking;
    }

    @Override
    public void cancelBooking(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new TicketingException("Booking not found: " + bookingId));
        if (!booking.getUserId().equals(userId))
            throw new TicketingException("Booking does not belong to user: " + userId);
        if (!booking.transitionStatus(BookingStatus.CONFIRMED, BookingStatus.CANCELLED))
            throw new TicketingException(
                "Booking cannot be cancelled in current state: " + booking.getStatus());

        inventoryService.cancelBookedSeats(booking.getEventId(), booking.getSeatIds());
        if (booking.getPayment() != null)
            paymentService.refundPayment(booking.getPayment().getId());

        eventListeners.forEach(l -> l.onBookingCancelled(booking));
    }

    @Override
    public Booking getBooking(String bookingId) {
        return bookingRepository.findById(bookingId)
            .orElseThrow(() -> new TicketingException("Booking not found: " + bookingId));
    }

    @Override
    public List<Booking> getBookingsForUser(String userId) {
        return bookingRepository.getBookingIdsByUser(userId).stream()
            .map(id -> bookingRepository.findById(id).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
