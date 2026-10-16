package com.rcpky.store;

import com.rcpky.model.FlagDefinition;

import java.util.Optional;

public interface FlagStore {
    void put(FlagDefinition flag);
    Optional<FlagDefinition> get(String flagName);
    void remove(String flagName);
}
