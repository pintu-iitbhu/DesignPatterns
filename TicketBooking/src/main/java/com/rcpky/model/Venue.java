package com.rcpky.model;

public final class Venue {
    private final String id;
    private final String name;
    private final String city;
    private final String address;
    private final int totalCapacity;

    public Venue(String id, String name, String city, String address, int totalCapacity) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.address = address;
        this.totalCapacity = totalCapacity;
    }

    public String getId()           { return id; }
    public String getName()         { return name; }
    public String getCity()         { return city; }
    public String getAddress()      { return address; }
    public int getTotalCapacity()   { return totalCapacity; }

    @Override
    public String toString() {
        return "Venue{name='" + name + "', city='" + city + "'}";
    }
}
