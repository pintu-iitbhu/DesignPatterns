package com.rcpky.repository;

import com.rcpky.model.SeatHold;

import java.util.Collection;
import java.util.Optional;

public interface IHoldRepository {
    void save(SeatHold hold);
    Optional<SeatHold> findById(String holdId);
    void delete(String holdId);
    Collection<SeatHold> findAll();
}
