package com.rcpky;

import com.rcpky.factory.*;
import com.rcpky.model.*;
import com.rcpky.observer.IBookingEventListener;
import com.rcpky.repository.*;
import com.rcpky.repository.impl.*;
import com.rcpky.search.EventSearchIndex;
import com.rcpky.search.SearchQuery;
import com.rcpky.service.*;
import com.rcpky.strategy.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketingApp {

    public static void main(String[] args) throws InterruptedException {

        // ── Repositories ───────────────────────────────────────────────────────
        IEventRepository   eventRepo   = new InMemoryEventRepository();
        IVenueRepository   venueRepo   = new InMemoryVenueRepository();
        ISectionRepository sectionRepo = new InMemorySectionRepository();
        ISeatRepository    seatRepo    = new InMemorySeatRepository();
        IHoldRepository    holdRepo    = new InMemoryHoldRepository();
        IBookingRepository bookingRepo = new InMemoryBookingRepository();
        IPaymentRepository paymentRepo = new InMemoryPaymentRepository();

        // ── Factories ──────────────────────────────────────────────────────────
        SeatFactory   seatFactory   = new SeatFactory();
        TicketFactory ticketFactory = new TicketFactory();

        // ── Search ─────────────────────────────────────────────────────────────
        ISearchService searchService = new SearchService(new EventSearchIndex());

        // ── Services ───────────────────────────────────────────────────────────
        IInventoryService inventoryService = new InventoryService(seatRepo, holdRepo, seatFactory);
        IPaymentService   paymentService   = new PaymentService(paymentRepo, 0.0);
        NotificationService notifications  = new NotificationService();

        IEventService eventService = new EventService(
            eventRepo, venueRepo, sectionRepo, searchService, inventoryService);

        IBookingService bookingService = new BookingService(
            inventoryService, paymentService, bookingRepo, ticketFactory,
            Collections.singletonList(notifications));

        System.out.println("=== Ticketmaster-like Booking System ===\n");

        // ------------------------------------------------------------------
        // Scenario 1: Admin creates venues, sections, events, inventory
        // ------------------------------------------------------------------
        System.out.println("--- Scenario 1: Setup venues & events ---");

        Venue madison = eventService.createVenue("Madison Square Garden", "New York", "4 Penn Plaza, NY", 20000);
        Venue staples = eventService.createVenue("Crypto.com Arena",      "Los Angeles", "1111 S Figueroa St, LA", 18997);

        Section nyFloor   = eventService.createSection(madison.getId(), "Floor",   10, 20);
        Section nyVip     = eventService.createSection(madison.getId(), "VIP",      5, 10);
        Section nyBalcony = eventService.createSection(madison.getId(), "Balcony", 20, 30);
        Section laFloor   = eventService.createSection(staples.getId(), "Floor",    8, 15);
        Section laVip     = eventService.createSection(staples.getId(), "VIP",      3, 10);

        Event coldplayNY = eventService.createEvent("Coldplay World Tour",
            madison.getId(), LocalDateTime.of(2025, 6, 15, 20, 0),
            Category.CONCERT, Arrays.asList("Coldplay", "Chris Martin"),
            "Music of the Spheres World Tour 2025 - New York");

        Event coldplayLA = eventService.createEvent("Coldplay World Tour",
            staples.getId(), LocalDateTime.of(2025, 6, 20, 20, 0),
            Category.CONCERT, Arrays.asList("Coldplay", "Chris Martin"),
            "Music of the Spheres World Tour 2025 - Los Angeles");

        Event knicksGame = eventService.createEvent("Knicks vs Lakers",
            madison.getId(), LocalDateTime.of(2025, 3, 10, 19, 30),
            Category.SPORTS, Arrays.asList("New York Knicks", "Los Angeles Lakers"),
            "NBA Regular Season Game");

        Map<String, Double> nyPrices = new LinkedHashMap<>();
        nyPrices.put(nyFloor.getId(),   250.0);
        nyPrices.put(nyVip.getId(),     500.0);
        nyPrices.put(nyBalcony.getId(),  75.0);
        eventService.initializeInventory(coldplayNY.getId(),
            Arrays.asList(nyFloor.getId(), nyVip.getId(), nyBalcony.getId()),
            new FlatPricingStrategy(nyPrices));

        Map<String, Double> laPrices = new LinkedHashMap<>();
        laPrices.put(laFloor.getId(), 200.0);
        laPrices.put(laVip.getId(),   450.0);
        eventService.initializeInventory(coldplayLA.getId(),
            Arrays.asList(laFloor.getId(), laVip.getId()),
            new FlatPricingStrategy(laPrices));

        Map<String, Double> knicksPrices = new LinkedHashMap<>();
        knicksPrices.put(nyFloor.getId(),   300.0);
        knicksPrices.put(nyVip.getId(),     600.0);
        knicksPrices.put(nyBalcony.getId(), 100.0);
        eventService.initializeInventory(knicksGame.getId(),
            Arrays.asList(nyFloor.getId(), nyVip.getId(), nyBalcony.getId()),
            new FlatPricingStrategy(knicksPrices));

        int nyTotal    = nyFloor.getTotalSeats() + nyVip.getTotalSeats() + nyBalcony.getTotalSeats();
        int laTotal    = laFloor.getTotalSeats() + laVip.getTotalSeats();
        int knixTotal  = nyFloor.getTotalSeats() + nyVip.getTotalSeats() + nyBalcony.getTotalSeats();
        System.out.printf("Seats initialised – ColdplayNY: %d | ColdplayLA: %d | Knicks: %d%n",
            inventoryService.getAvailableSeatCount(coldplayNY.getId()),
            inventoryService.getAvailableSeatCount(coldplayLA.getId()),
            inventoryService.getAvailableSeatCount(knicksGame.getId()));

        // ------------------------------------------------------------------
        // Scenario 2: Search  (eventually consistent)
        // ------------------------------------------------------------------
        System.out.println("\n--- Scenario 2: Event search ---");

        List<Event> nyEvents = searchService.search(SearchQuery.builder().city("New York").build());
        System.out.println("Events in New York (" + nyEvents.size() + "):");
        nyEvents.forEach(e -> System.out.println("  " + e.getName() + " @ " + e.getVenue().getName()));

        List<Event> coldplayMatches = searchService.search(SearchQuery.builder().keyword("coldplay").build());
        System.out.println("Keyword 'coldplay' → " + coldplayMatches.size() + " event(s)");

        List<Event> concerts = searchService.search(SearchQuery.builder().category(Category.CONCERT).build());
        System.out.println("Category CONCERT → " + concerts.size() + " event(s)");

        List<Event> marchEvents = searchService.search(SearchQuery.builder()
            .dateFrom(LocalDateTime.of(2025, 3, 1, 0, 0))
            .dateTo(LocalDateTime.of(2025, 3, 31, 23, 59)).build());
        System.out.println("March 2025 events → " + marchEvents.size() + " event(s)");

        // ------------------------------------------------------------------
        // Scenario 3: Normal booking flow  hold → pay → confirm
        // ------------------------------------------------------------------
        System.out.println("\n--- Scenario 3: Normal booking ---");
        String eid = coldplayNY.getId();
        List<Seat> available = inventoryService.getAvailableSeats(eid);
        List<String> wanted  = Arrays.asList(available.get(0).getId(), available.get(1).getId());

        SeatHold hold = bookingService.initiateBooking("user-alice", eid, wanted);
        System.out.println("Hold created: " + hold.getId() + "  expires: " + hold.getExpiresAt());
        System.out.println("Available after hold: " + inventoryService.getAvailableSeatCount(eid));

        Booking booking = bookingService.completeBooking(hold.getId(), "user-alice", "idem-alice-1");
        System.out.println("Booking confirmed: " + booking.getId() + "  amount=$" + booking.getTotalAmount());
        System.out.println("Tickets issued: "    + booking.getTickets().size());

        // ------------------------------------------------------------------
        // Scenario 4: Idempotent retry  (same key → same booking, no double charge)
        // ------------------------------------------------------------------
        System.out.println("\n--- Scenario 4: Idempotent retry ---");
        Booking retry = bookingService.completeBooking(hold.getId(), "user-alice", "idem-alice-1");
        System.out.println("Same booking returned: " + retry.getId().equals(booking.getId()));

        // ------------------------------------------------------------------
        // Scenario 5: Concurrent race for one seat  (only 1 of N threads wins)
        // ------------------------------------------------------------------
        System.out.println("\n--- Scenario 5: Concurrent race for one seat ---");
        List<Seat> fresh = inventoryService.getAvailableSeats(eid);
        String contestedId = fresh.get(0).getId();

        int N = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(N);
        AtomicInteger wins   = new AtomicInteger(0);
        AtomicInteger fails  = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(N);

        for (int i = 0; i < N; i++) {
            final String uid = "racer-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    SeatHold h = bookingService.initiateBooking(uid, eid,
                        Collections.singletonList(contestedId));
                    bookingService.completeBooking(h.getId(), uid, "idem-race-" + uid);
                    wins.incrementAndGet();
                } catch (Exception e) {
                    fails.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await(15, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.printf("Race result → wins: %d  fails: %d  (expected wins=1)%n",
            wins.get(), fails.get());
        Seat contested = inventoryService.getSeat(contestedId);
        System.out.println("Contested seat final status: " + contested.getStatus());

        // ------------------------------------------------------------------
        // Scenario 6: Cancel booking  (seats return to AVAILABLE, payment refunded)
        // ------------------------------------------------------------------
        System.out.println("\n--- Scenario 6: Cancel booking ---");
        int beforeCancel = inventoryService.getAvailableSeatCount(eid);
        bookingService.cancelBooking(booking.getId(), "user-alice");
        int afterCancel  = inventoryService.getAvailableSeatCount(eid);
        System.out.printf("Available seats before=%d  after=%d (delta=%d)%n",
            beforeCancel, afterCancel, afterCancel - beforeCancel);
        System.out.println("Booking status: " + bookingService.getBooking(booking.getId()).getStatus());
        System.out.println("Payment status: " + booking.getPayment().getStatus());

        // ------------------------------------------------------------------
        // Scenario 7: Hold expiry (released hold blocks completion)
        // ------------------------------------------------------------------
        System.out.println("\n--- Scenario 7: Hold expiry ---");
        List<Seat> expAvail = inventoryService.getAvailableSeats(eid);
        SeatHold shortHold = bookingService.initiateBooking("user-bob", eid,
            Collections.singletonList(expAvail.get(0).getId()), Duration.ofMillis(50));
        System.out.println("Short hold created: " + shortHold.getId());
        Thread.sleep(150);
        inventoryService.releaseHold(shortHold);
        System.out.println("Hold released. Attempting complete → expect HoldExpiredException...");
        try {
            bookingService.completeBooking(shortHold.getId(), "user-bob", "idem-bob-exp");
            System.out.println("ERROR: should not reach here");
        } catch (Exception e) {
            System.out.println("Caught: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        // ------------------------------------------------------------------
        // Scenario 8: Payment failure releases hold
        // ------------------------------------------------------------------
        System.out.println("\n--- Scenario 8: Payment failure ---");
        IPaymentService failPay = new PaymentService(new InMemoryPaymentRepository(), 1.0);
        IBookingService failBk  = new BookingService(inventoryService, failPay,
            new InMemoryBookingRepository(), ticketFactory,
            Collections.singletonList(notifications));

        List<Seat> fa  = inventoryService.getAvailableSeats(eid);
        String     fid = fa.get(0).getId();
        SeatHold   fh  = failBk.initiateBooking("user-carol", eid,
            Collections.singletonList(fid));
        System.out.println("Seat status after hold: " + inventoryService.getSeat(fid).getStatus());
        try {
            failBk.completeBooking(fh.getId(), "user-carol", "idem-carol-fail");
        } catch (Exception e) {
            System.out.println("Payment failed as expected: " + e.getMessage());
        }
        System.out.println("Seat status after failure: " + inventoryService.getSeat(fid).getStatus());

        inventoryService.shutdown();
        System.out.println("\n=== Demo complete ===");
    }
}
