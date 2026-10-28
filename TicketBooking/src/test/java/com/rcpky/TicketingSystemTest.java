package com.rcpky;

import com.rcpky.exception.*;
import com.rcpky.factory.*;
import com.rcpky.model.*;
import com.rcpky.repository.*;
import com.rcpky.repository.impl.*;
import com.rcpky.search.EventSearchIndex;
import com.rcpky.search.SearchQuery;
import com.rcpky.service.*;
import com.rcpky.strategy.FlatPricingStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TicketingSystemTest {

    private IInventoryService   inventoryService;
    private IBookingService     bookingService;
    private IEventService       eventService;
    private ISearchService      searchService;
    private NotificationService notifications;
    private TicketFactory       ticketFactory;

    private Venue   testVenue;
    private Section floorSection;
    private Section vipSection;
    private Event   testEvent;

    @Before
    public void setUp() {
        // ── Repositories ─────────────────────────────────────────────────────
        ISeatRepository    seatRepo    = new InMemorySeatRepository();
        IHoldRepository    holdRepo    = new InMemoryHoldRepository();
        IBookingRepository bookingRepo = new InMemoryBookingRepository();
        IPaymentRepository paymentRepo = new InMemoryPaymentRepository();

        // ── Factories ─────────────────────────────────────────────────────────
        SeatFactory seatFactory = new SeatFactory();
        ticketFactory = new TicketFactory();

        // ── Services ──────────────────────────────────────────────────────────
        inventoryService = new InventoryService(seatRepo, holdRepo, seatFactory);
        IPaymentService paymentService = new PaymentService(paymentRepo, 0.0);
        notifications    = new NotificationService();

        ISearchService search = new SearchService(new EventSearchIndex());
        searchService = search;

        eventService = new EventService(
            new InMemoryEventRepository(),
            new InMemoryVenueRepository(),
            new InMemorySectionRepository(),
            search,
            inventoryService);

        bookingService = new BookingService(
            inventoryService, paymentService, bookingRepo, ticketFactory,
            Collections.singletonList(notifications));

        // ── Test data ─────────────────────────────────────────────────────────
        testVenue    = eventService.createVenue("Test Arena", "TestCity", "1 Test St", 5000);
        floorSection = eventService.createSection(testVenue.getId(), "Floor", 5, 10);
        vipSection   = eventService.createSection(testVenue.getId(), "VIP",   2,  5);

        testEvent = eventService.createEvent("Test Concert",
            testVenue.getId(), LocalDateTime.of(2025, 8, 1, 20, 0),
            Category.CONCERT, Collections.singletonList("Test Artist"),
            "Great test concert");

        Map<String, Double> prices = new LinkedHashMap<>();
        prices.put(floorSection.getId(), 100.0);
        prices.put(vipSection.getId(),   300.0);
        eventService.initializeInventory(testEvent.getId(),
            Arrays.asList(floorSection.getId(), vipSection.getId()),
            new FlatPricingStrategy(prices));
    }

    @After
    public void tearDown() { inventoryService.shutdown(); }

    // =========================================================================
    // Correctness: no double booking
    // =========================================================================

    @Test
    public void testNoDoubleBooking_secondHoldFails() {
        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        String seatId = available.get(0).getId();

        SeatHold hold1 = bookingService.initiateBooking("user-1", testEvent.getId(),
            Collections.singletonList(seatId));
        assertNotNull(hold1);

        try {
            bookingService.initiateBooking("user-2", testEvent.getId(),
                Collections.singletonList(seatId));
            fail("Should throw SeatNotAvailableException");
        } catch (SeatNotAvailableException e) {
            // expected
        }

        Booking booking = bookingService.completeBooking(hold1.getId(), "user-1", "idem-nb-1");
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(SeatStatus.BOOKED,
            inventoryService.getSeat(seatId).getStatus());
    }

    @Test
    public void testBookingConfirmsCorrectSeatStatuses() {
        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        List<String> ids = Arrays.asList(available.get(0).getId(), available.get(1).getId());

        SeatHold hold = bookingService.initiateBooking("user-1", testEvent.getId(), ids);
        ids.forEach(id ->
            assertEquals(SeatStatus.HELD, inventoryService.getSeat(id).getStatus()));

        bookingService.completeBooking(hold.getId(), "user-1", "idem-status-1");
        ids.forEach(id ->
            assertEquals(SeatStatus.BOOKED, inventoryService.getSeat(id).getStatus()));
    }

    // =========================================================================
    // Concurrency: race condition on a single seat
    // =========================================================================

    @Test
    public void testConcurrentRace_exactlyOneWins() throws InterruptedException {
        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        String seatId = available.get(0).getId();

        int N = 30;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(N);
        AtomicInteger wins  = new AtomicInteger(0);
        AtomicInteger losses = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(N);
        for (int i = 0; i < N; i++) {
            final String uid = "racer-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    SeatHold h = bookingService.initiateBooking(uid, testEvent.getId(),
                        Collections.singletonList(seatId));
                    bookingService.completeBooking(h.getId(), uid, "idem-race-" + uid);
                    wins.incrementAndGet();
                } catch (Exception e) {
                    losses.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals("Exactly one thread must win", 1, wins.get());
        assertEquals("Rest must fail", N - 1, losses.get());
        assertEquals(SeatStatus.BOOKED, inventoryService.getSeat(seatId).getStatus());
    }

    @Test
    public void testConcurrentBooking_differentSeatsAllSucceed() throws InterruptedException {
        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        int K = Math.min(10, available.size());

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(K);
        AtomicInteger wins  = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(K);
        for (int i = 0; i < K; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    SeatHold h = bookingService.initiateBooking("user-" + idx, testEvent.getId(),
                        Collections.singletonList(available.get(idx).getId()));
                    bookingService.completeBooking(h.getId(), "user-" + idx, "idem-diff-" + idx);
                    wins.incrementAndGet();
                } catch (Exception e) {
                    // no-op
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals("All K bookings on distinct seats must succeed", K, wins.get());
    }

    // =========================================================================
    // Reliability: idempotency, payment failure, hold expiry
    // =========================================================================

    @Test
    public void testIdempotentBooking_sameKeyReturnsSameBooking() {
        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        SeatHold hold = bookingService.initiateBooking("user-1", testEvent.getId(),
            Collections.singletonList(available.get(0).getId()));

        Booking b1 = bookingService.completeBooking(hold.getId(), "user-1", "idem-key-xyz");
        Booking b2 = bookingService.completeBooking(hold.getId(), "user-1", "idem-key-xyz");

        assertSame(b1, b2);
        assertEquals(BookingStatus.CONFIRMED, b2.getStatus());
    }

    @Test
    public void testPaymentFailure_holdReleasedSeatReturnsToAvailable() {
        IPaymentService failPay = new PaymentService(new InMemoryPaymentRepository(), 1.0);
        IBookingService failBk  = new BookingService(inventoryService, failPay,
            new InMemoryBookingRepository(), ticketFactory,
            Collections.singletonList(notifications));

        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        int initialCount = available.size();
        String seatId = available.get(0).getId();

        SeatHold hold = failBk.initiateBooking("user-1", testEvent.getId(),
            Collections.singletonList(seatId));
        assertEquals(SeatStatus.HELD, inventoryService.getSeat(seatId).getStatus());

        try {
            failBk.completeBooking(hold.getId(), "user-1", "idem-fail-1");
            fail("Expected PaymentFailedException");
        } catch (PaymentFailedException e) {
            // expected
        }

        assertEquals(SeatStatus.AVAILABLE, inventoryService.getSeat(seatId).getStatus());
        assertEquals(initialCount, inventoryService.getAvailableSeatCount(testEvent.getId()));
    }

    @Test
    public void testHoldExpiry_seatReturnsToAvailable() throws InterruptedException {
        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        String seatId = available.get(0).getId();
        int initialCount = available.size();

        SeatHold hold = bookingService.initiateBooking("user-1", testEvent.getId(),
            Collections.singletonList(seatId), Duration.ofMillis(50));
        assertEquals(SeatStatus.HELD, inventoryService.getSeat(seatId).getStatus());
        assertEquals(initialCount - 1, inventoryService.getAvailableSeatCount(testEvent.getId()));

        Thread.sleep(150);
        inventoryService.releaseHold(hold);

        assertEquals(SeatStatus.AVAILABLE, inventoryService.getSeat(seatId).getStatus());
        assertEquals(initialCount, inventoryService.getAvailableSeatCount(testEvent.getId()));
    }

    @Test(expected = HoldExpiredException.class)
    public void testExpiredHold_completionThrows() throws InterruptedException {
        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        SeatHold hold = bookingService.initiateBooking("user-1", testEvent.getId(),
            Collections.singletonList(available.get(0).getId()), Duration.ofMillis(50));

        Thread.sleep(150);
        inventoryService.releaseHold(hold);

        bookingService.completeBooking(hold.getId(), "user-1", "idem-exp-1");
    }

    // =========================================================================
    // Cancellation
    // =========================================================================

    @Test
    public void testCancelBooking_seatsReleasedAndPaymentRefunded() {
        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        List<String> ids = Arrays.asList(available.get(0).getId(), available.get(1).getId());
        int initialCount = available.size();

        SeatHold hold    = bookingService.initiateBooking("user-1", testEvent.getId(), ids);
        Booking  booking = bookingService.completeBooking(hold.getId(), "user-1", "idem-cancel-1");
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        bookingService.cancelBooking(booking.getId(), "user-1");

        assertEquals(BookingStatus.CANCELLED, bookingService.getBooking(booking.getId()).getStatus());
        assertEquals(initialCount, inventoryService.getAvailableSeatCount(testEvent.getId()));
        ids.forEach(id ->
            assertEquals(SeatStatus.AVAILABLE, inventoryService.getSeat(id).getStatus()));
        assertEquals(PaymentStatus.REFUNDED, booking.getPayment().getStatus());
    }

    // =========================================================================
    // Performance / Search accuracy
    // =========================================================================

    @Test
    public void testSearchByCity_returnsOnlyMatchingCity() {
        List<Event> results = searchService.search(SearchQuery.builder().city("TestCity").build());
        assertEquals(1, results.size());
        assertEquals(testEvent.getId(), results.get(0).getId());

        List<Event> none = searchService.search(SearchQuery.builder().city("Atlantis").build());
        assertTrue(none.isEmpty());
    }

    @Test
    public void testSearchByKeyword_matchesEventName() {
        List<Event> results = searchService.search(SearchQuery.builder().keyword("Concert").build());
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getId().equals(testEvent.getId())));
    }

    @Test
    public void testSearchByCategory_filtersCorrectly() {
        List<Event> concerts = searchService.search(SearchQuery.builder()
            .category(Category.CONCERT).build());
        assertFalse(concerts.isEmpty());
        assertTrue(concerts.stream().anyMatch(e -> e.getId().equals(testEvent.getId())));

        List<Event> sports = searchService.search(SearchQuery.builder()
            .category(Category.SPORTS).build());
        assertTrue(sports.isEmpty());
    }

    @Test
    public void testSearchByDateRange_includesAndExcludes() {
        List<Event> inRange = searchService.search(SearchQuery.builder()
            .dateFrom(LocalDateTime.of(2025, 7, 1, 0, 0))
            .dateTo(LocalDateTime.of(2025, 9, 1, 0, 0)).build());
        assertEquals(1, inRange.size());

        List<Event> outOfRange = searchService.search(SearchQuery.builder()
            .dateFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
            .dateTo(LocalDateTime.of(2025, 6, 30, 0, 0)).build());
        assertTrue(outOfRange.isEmpty());
    }

    @Test
    public void testSearchByArtist_matchesArtistName() {
        List<Event> results = searchService.search(SearchQuery.builder()
            .artistName("Test Artist").build());
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getId().equals(testEvent.getId())));
    }

    @Test
    public void testCancelEvent_removedFromSearchIndex() {
        assertFalse(searchService.search(SearchQuery.builder().city("TestCity").build()).isEmpty());

        eventService.cancelEvent(testEvent.getId());

        assertTrue(searchService.search(SearchQuery.builder().city("TestCity").build()).isEmpty());
    }

    @Test
    public void testInventoryAccuracy_afterBookingAndCancel() {
        int total = floorSection.getTotalSeats() + vipSection.getTotalSeats();
        assertEquals(total, inventoryService.getAvailableSeatCount(testEvent.getId()));

        List<Seat> available = inventoryService.getAvailableSeats(testEvent.getId());
        List<String> ids = Arrays.asList(
            available.get(0).getId(), available.get(1).getId(), available.get(2).getId());

        SeatHold hold    = bookingService.initiateBooking("user-1", testEvent.getId(), ids);
        assertEquals(total - 3, inventoryService.getAvailableSeatCount(testEvent.getId()));

        Booking booking = bookingService.completeBooking(hold.getId(), "user-1", "idem-acc-1");
        assertEquals(total - 3, inventoryService.getAvailableSeatCount(testEvent.getId()));

        bookingService.cancelBooking(booking.getId(), "user-1");
        assertEquals(total, inventoryService.getAvailableSeatCount(testEvent.getId()));
    }
}
