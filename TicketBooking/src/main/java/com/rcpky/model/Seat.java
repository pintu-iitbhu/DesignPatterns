package com.rcpky.model;

import java.util.concurrent.atomic.AtomicReference;

public final class Seat {
    private final String id;
    private final String eventId;
    private final String sectionId;
    private final String sectionName;
    private final int row;
    private final int seatNumber;
    private final double price;
    private final AtomicReference<SeatStatus> status;

    public Seat(String id, String eventId, String sectionId, String sectionName,
                int row, int seatNumber, double price) {
        this.id = id;
        this.eventId = eventId;
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.row = row;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = new AtomicReference<>(SeatStatus.AVAILABLE);
    }

    public String getId()          { return id; }
    public String getEventId()     { return eventId; }
    public String getSectionId()   { return sectionId; }
    public String getSectionName() { return sectionName; }
    public int getRow()            { return row; }
    public int getSeatNumber()     { return seatNumber; }
    public double getPrice()       { return price; }
    public SeatStatus getStatus()  { return status.get(); }

    public boolean compareAndSetStatus(SeatStatus expected, SeatStatus update) {
        return status.compareAndSet(expected, update);
    }

    @Override
    public String toString() {
        return "Seat{id='" + id + "', section='" + sectionName + "', row=" + row
               + ", num=" + seatNumber + ", price=" + price + ", status=" + status.get() + "}";
    }
}
