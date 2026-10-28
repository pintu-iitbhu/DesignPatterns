package com.rcpky.factory;

import com.rcpky.model.Ticket;

import java.util.UUID;

/**
 * Factory pattern — SRP: ticket ID and QR-code generation is
 * centralised here, not embedded in BookingService business logic.
 */
public class TicketFactory {

    public Ticket create(String bookingId, String eventId, String seatId, String userId) {
        String ticketId = "tkt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String qrCode   = UUID.randomUUID().toString();
        return new Ticket(ticketId, bookingId, eventId, seatId, userId, qrCode);
    }
}
