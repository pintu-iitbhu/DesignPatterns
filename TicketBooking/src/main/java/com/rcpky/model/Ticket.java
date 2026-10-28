package com.rcpky.model;

public final class Ticket {
    private final String id;
    private final String bookingId;
    private final String eventId;
    private final String seatId;
    private final String userId;
    private final String qrCode;

    public Ticket(String id, String bookingId, String eventId,
                  String seatId, String userId, String qrCode) {
        this.id = id;
        this.bookingId = bookingId;
        this.eventId = eventId;
        this.seatId = seatId;
        this.userId = userId;
        this.qrCode = qrCode;
    }

    public String getId()        { return id; }
    public String getBookingId() { return bookingId; }
    public String getEventId()   { return eventId; }
    public String getSeatId()    { return seatId; }
    public String getUserId()    { return userId; }
    public String getQrCode()    { return qrCode; }

    @Override
    public String toString() {
        return "Ticket{id='" + id + "', seat='" + seatId + "', qrCode='" + qrCode + "'}";
    }
}
