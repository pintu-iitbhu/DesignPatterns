package com.rcpky.factory;

import com.rcpky.model.Seat;
import com.rcpky.model.Section;

/**
 * Factory pattern — SRP: seat ID generation and construction logic is
 * centralised here, not scattered across InventoryService.
 */
public class SeatFactory {

    public Seat create(String eventId, Section section, int row, int num, double price) {
        String id = eventId + "::" + section.getId() + "::" + row + "::" + num;
        return new Seat(id, eventId, section.getId(), section.getName(), row, num, price);
    }
}
