package com.rcpky.model;

public final class Section {
    private final String id;
    private final String venueId;
    private final String name;
    private final int rows;
    private final int seatsPerRow;

    public Section(String id, String venueId, String name, int rows, int seatsPerRow) {
        this.id = id;
        this.venueId = venueId;
        this.name = name;
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
    }

    public String getId()        { return id; }
    public String getVenueId()   { return venueId; }
    public String getName()      { return name; }
    public int getRows()         { return rows; }
    public int getSeatsPerRow()  { return seatsPerRow; }
    public int getTotalSeats()   { return rows * seatsPerRow; }

    @Override
    public String toString() {
        return "Section{name='" + name + "', seats=" + getTotalSeats() + "}";
    }
}
