package com.rcpky.service;

import com.rcpky.exception.SeatNotAvailableException;
import com.rcpky.factory.SeatFactory;
import com.rcpky.model.*;
import com.rcpky.repository.IHoldRepository;
import com.rcpky.repository.ISeatRepository;
import com.rcpky.strategy.IPricingStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Strongly-consistent seat inventory.
 *
 * SOLID applied:
 *  S — manages seat state transitions only; data storage delegated to repositories,
 *      seat creation delegated to SeatFactory.
 *  D — depends on ISeatRepository, IHoldRepository, SeatFactory (all abstractions).
 *
 * Concurrency:
 *  One ReentrantLock per event (partition-by-event). holdSeats / confirmSeats /
 *  releaseHold / cancelBookedSeats all acquire it, making check-then-act atomic.
 *  SeatHold.deactivate() (CAS AtomicBoolean) runs inside the lock so only one
 *  caller wins the deactivation race.
 */
public class InventoryService implements IInventoryService {

    private final ISeatRepository  seatRepository;
    private final IHoldRepository  holdRepository;
    private final SeatFactory      seatFactory;
    private final ConcurrentHashMap<String, ReentrantLock> eventLocks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService expiryScheduler;

    public InventoryService(ISeatRepository seatRepository,
                             IHoldRepository holdRepository,
                             SeatFactory seatFactory) {
        this.seatRepository = seatRepository;
        this.holdRepository = holdRepository;
        this.seatFactory    = seatFactory;
        expiryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hold-expiry-thread");
            t.setDaemon(true);
            return t;
        });
        expiryScheduler.scheduleAtFixedRate(this::expireHolds, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void initializeSeats(Event event, List<Section> sections, IPricingStrategy pricingStrategy) {
        String eventId = event.getId();
        eventLocks.computeIfAbsent(eventId, k -> new ReentrantLock());

        List<String> seatIds = new ArrayList<>();
        for (Section section : sections) {
            double price = pricingStrategy.calculatePrice(section, event);
            for (int row = 1; row <= section.getRows(); row++) {
                for (int num = 1; num <= section.getSeatsPerRow(); num++) {
                    Seat seat = seatFactory.create(eventId, section, row, num, price);
                    seatRepository.save(seat);
                    seatIds.add(seat.getId());
                }
            }
        }
        seatRepository.registerEventSeats(eventId, seatIds);
    }

    @Override
    public SeatHold holdSeats(String userId, String eventId,
                               List<String> requestedSeatIds, Duration holdDuration) {
        ReentrantLock lock = eventLocks.computeIfAbsent(eventId, k -> new ReentrantLock());
        lock.lock();
        try {
            for (String seatId : requestedSeatIds) {
                Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new SeatNotAvailableException("Seat not found: " + seatId));
                if (seat.getStatus() != SeatStatus.AVAILABLE)
                    throw new SeatNotAvailableException(
                        "Seat " + seatId + " is not available (status: " + seat.getStatus() + ")");
            }
            for (String seatId : requestedSeatIds)
                seatRepository.findById(seatId)
                    .ifPresent(s -> s.compareAndSetStatus(SeatStatus.AVAILABLE, SeatStatus.HELD));

            String holdId   = "hold-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            Instant expires = Instant.now().plus(holdDuration);
            SeatHold hold   = new SeatHold(holdId, userId, eventId, requestedSeatIds, expires);
            holdRepository.save(hold);
            return hold;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean confirmSeats(SeatHold hold) {
        ReentrantLock lock = eventLocks.get(hold.getEventId());
        if (lock == null) return false;
        lock.lock();
        try {
            if (!hold.isActive() || hold.isExpired()) return false;
            for (String seatId : hold.getSeatIds()) {
                Seat seat = seatRepository.findById(seatId).orElse(null);
                if (seat == null || seat.getStatus() != SeatStatus.HELD) return false;
            }
            for (String seatId : hold.getSeatIds())
                seatRepository.findById(seatId)
                    .ifPresent(s -> s.compareAndSetStatus(SeatStatus.HELD, SeatStatus.BOOKED));
            hold.deactivate();
            holdRepository.delete(hold.getId());
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void releaseHold(SeatHold hold) {
        ReentrantLock lock = eventLocks.get(hold.getEventId());
        if (lock == null) { hold.deactivate(); return; }
        lock.lock();
        try {
            if (!hold.deactivate()) return;
            for (String seatId : hold.getSeatIds())
                seatRepository.findById(seatId)
                    .ifPresent(s -> s.compareAndSetStatus(SeatStatus.HELD, SeatStatus.AVAILABLE));
            holdRepository.delete(hold.getId());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cancelBookedSeats(String eventId, List<String> seatIds) {
        ReentrantLock lock = eventLocks.get(eventId);
        if (lock == null) return;
        lock.lock();
        try {
            for (String seatId : seatIds)
                seatRepository.findById(seatId)
                    .ifPresent(s -> s.compareAndSetStatus(SeatStatus.BOOKED, SeatStatus.AVAILABLE));
        } finally {
            lock.unlock();
        }
    }

    @Override public SeatHold getHold(String holdId)  { return holdRepository.findById(holdId).orElse(null); }
    @Override public Seat getSeat(String seatId)       { return seatRepository.findById(seatId).orElse(null); }

    @Override
    public List<Seat> getAvailableSeats(String eventId) {
        return seatRepository.getSeatIdsByEvent(eventId).stream()
            .map(id -> seatRepository.findById(id).orElse(null))
            .filter(s -> s != null && s.getStatus() == SeatStatus.AVAILABLE)
            .collect(Collectors.toList());
    }

    @Override
    public int getAvailableSeatCount(String eventId) {
        return getAvailableSeats(eventId).size();
    }

    @Override
    public void shutdown() { expiryScheduler.shutdown(); }

    private void expireHolds() {
        holdRepository.findAll().stream()
            .filter(h -> h.isActive() && h.isExpired())
            .forEach(this::releaseHold);
    }
}
